package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.engine.util.AgentDecisionParser;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Agent执行节点处理器
 *
 * 支持两种执行模式：
 * 1. Gateway API模式（推荐）：通过HTTP调用Agent，支持会话隔离
 * 2. CLI模式：通过命令行调用Agent（向后兼容）
 */
public class AgentNodeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(AgentNodeHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_TIMEOUT = 600;

    // CLI模式配置
    private String openclawCommand = "openclaw";

    // Gateway API配置
    private boolean useGatewayApi = true;
    private String gatewayUrl = "http://localhost:18789";
    private String gatewayToken = "56b640cc2d91411f63255af68355c19ee33c88ec458878ca";

    // 决策提示配置
    private boolean enableDecisionPrompt = true;
    private List<AgentDecisionParser.DownstreamNode> downstreamNodes;

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        WorkflowNode node = context.getNode();
        String agentId = getAgentId(node);
        String prompt = buildPrompt(node, context);
        int timeout = getTimeout(node, DEFAULT_TIMEOUT);

        logger.info("执行Agent: {} - 节点: {} (模式: {})", agentId, node.getName(),
                useGatewayApi ? "Gateway API" : "CLI");

        try {
            String output;
            if (useGatewayApi) {
                output = executeViaGateway(agentId, prompt, context, timeout);
            } else {
                output = executeViaCli(agentId, prompt, context, timeout);
            }

            // 解析Agent输出中的决策（如果有）
            NodeResult result = parseAgentResult(output);

            // 如果配置了下游节点，尝试解析决策
            if (enableDecisionPrompt && downstreamNodes != null && !downstreamNodes.isEmpty()) {
                AgentDecisionParser.AgentDecision decision = AgentDecisionParser.parse(output);
                if (decision != null && decision.getNodeIds() != null && !decision.getNodeIds().isEmpty()) {
                    // 验证决策
                    List<String> validNodeIds = new ArrayList<>();
                    for (AgentDecisionParser.DownstreamNode dn : downstreamNodes) {
                        validNodeIds.add(dn.getId());
                    }

                    if (AgentDecisionParser.validate(decision, validNodeIds)) {
                        result.setNextNodeIds(decision.getNodeIds());
                        result.setDecisionReason(decision.getReason());
                        logger.info("Agent决策: {} -> {}", node.getName(), decision.getNodeIds());
                    }
                }
            }

            return result;

        } catch (Exception e) {
            logger.error("Agent执行异常: {}", e.getMessage(), e);
            return NodeResult.failed(e.getMessage());
        }
    }

    /**
     * 通过Gateway API执行Agent（推荐方式）
     */
    private String executeViaGateway(String agentId, String prompt, NodeExecutionContext context, int timeout) throws Exception {
        OpenClawGatewayClient client = new OpenClawGatewayClient(gatewayUrl, gatewayToken);

        // 构建会话Key（实现会话隔离）
        String sessionContext = String.format("%s_%s_%s",
                context.getWorkflowId(),
                context.getExecutionId(),
                context.getNode().getId());

        OpenClawGatewayClient.AgentRequest request = OpenClawGatewayClient.AgentRequest.builder()
                .agentId(agentId)
                .message(prompt)
                .context(sessionContext)
                .build();

        long startTime = System.currentTimeMillis();
        OpenClawGatewayClient.AgentResponse response = client.executeAgent(request);
        long duration = System.currentTimeMillis() - startTime;

        logger.info("Gateway API调用完成，耗时: {}ms, Tokens: {}", duration, response.getTotalTokens());

        if (!response.isSuccess()) {
            throw new RuntimeException("Agent执行失败: " + response.getErrorMessage());
        }

        return response.getContent();
    }

    /**
     * 通过CLI执行Agent（向后兼容）
     */
    private String executeViaCli(String agentId, String prompt, NodeExecutionContext context, int timeout) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(openclawCommand);
        command.add("agent");
        command.add("--agent");
        command.add(agentId);
        command.add("--message");
        command.add(prompt);
        command.add("--local");
        command.add("--json");
        command.add("--timeout");
        command.add(String.valueOf(timeout));

        String sessionId = context.getExecutionId();
        if (sessionId != null) {
            command.add("--session-id");
            command.add(sessionId);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean completed = process.waitFor(timeout, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("Agent执行超时");
        }

        return output.toString().trim();
    }

    private NodeResult parseAgentResult(String output) {
        try {
            JsonNode jsonNode = objectMapper.readTree(output);
            if (jsonNode.has("result")) {
                JsonNode result = jsonNode.get("result");
                if (result.has("executionResult")) {
                    String executionResult = result.get("executionResult").asText();
                    if (executionResult.contains("成功")) {
                        return NodeResult.success(output);
                    } else if (executionResult.contains("失败")) {
                        return NodeResult.failed(output, executionResult);
                    }
                }
                return NodeResult.success(output);
            }
            // 非JSON格式，直接返回内容
            return NodeResult.success(output);
        } catch (Exception e) {
            // 解析失败，仍然返回成功（Agent输出可能不是JSON格式）
            return NodeResult.success(output);
        }
    }

    private String getAgentId(WorkflowNode node) {
        try {
            if (node.getConfig() != null) {
                JsonNode config = objectMapper.readTree(node.getConfig());
                if (config.has("agentId")) {
                    return config.get("agentId").asText();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "default";
    }

    @Override
    protected int getTimeout(WorkflowNode node, int defaultTimeout) {
        try {
            if (node.getConfig() != null) {
                JsonNode config = objectMapper.readTree(node.getConfig());
                if (config.has("timeout")) {
                    return config.get("timeout").asInt();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return defaultTimeout;
    }

    private String buildPrompt(WorkflowNode node, NodeExecutionContext context) {
        StringBuilder prompt = new StringBuilder();

        if (context.getTaskDescription() != null) {
            prompt.append("## 任务描述\n").append(context.getTaskDescription()).append("\n\n");
        }

        if (context.getGlobalPrompt() != null) {
            prompt.append("## 全局提示\n").append(context.getGlobalPrompt()).append("\n\n");
        }

        try {
            if (node.getConfig() != null) {
                JsonNode config = objectMapper.readTree(node.getConfig());
                if (config.has("prompt")) {
                    prompt.append("## 执行任务\n").append(config.get("prompt").asText()).append("\n\n");
                }
            }
        } catch (Exception e) {
            // ignore
        }

        Map<String, NodeResult> previousOutputs = context.getPreviousOutputs();
        if (previousOutputs != null && !previousOutputs.isEmpty()) {
            prompt.append("## 上游节点输出\n");
            for (Map.Entry<String, NodeResult> entry : previousOutputs.entrySet()) {
                prompt.append("### ").append(entry.getKey()).append("\n");
                if (entry.getValue() != null && entry.getValue().getOutput() != null) {
                    prompt.append(entry.getValue().getOutput().toString()).append("\n");
                }
            }
        }

        // 添加决策提示（如果配置了下游节点）
        if (enableDecisionPrompt && downstreamNodes != null && !downstreamNodes.isEmpty()) {
            String decisionPrompt = AgentDecisionParser.buildDecisionPrompt(
                    node.getType().name(),
                    node.getName(),
                    downstreamNodes);
            prompt.append(decisionPrompt);
        }

        return prompt.toString();
    }

    // ==================== 配置方法 ====================

    public void setOpenclawCommand(String openclawCommand) {
        this.openclawCommand = openclawCommand;
    }

    public void setUseGatewayApi(boolean useGatewayApi) {
        this.useGatewayApi = useGatewayApi;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public void setGatewayToken(String gatewayToken) {
        this.gatewayToken = gatewayToken;
    }

    public void setEnableDecisionPrompt(boolean enableDecisionPrompt) {
        this.enableDecisionPrompt = enableDecisionPrompt;
    }

    public void setDownstreamNodes(List<AgentDecisionParser.DownstreamNode> downstreamNodes) {
        this.downstreamNodes = downstreamNodes;
    }

    /**
     * 添加下游节点信息（用于生成决策提示）
     */
    public void addDownstreamNode(String id, String name, String description) {
        if (this.downstreamNodes == null) {
            this.downstreamNodes = new ArrayList<>();
        }
        this.downstreamNodes.add(new AgentDecisionParser.DownstreamNode(id, name, description));
    }
}