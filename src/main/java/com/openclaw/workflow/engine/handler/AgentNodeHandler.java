package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.engine.util.AgentDecisionParser;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent执行节点处理器
 *
 * 协调 AgentPromptBuilder 构建提示词，通过 AgentExecutor 执行Agent，
 * 并解析执行结果和决策信息。
 */
public class AgentNodeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(AgentNodeHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AgentPromptBuilder promptBuilder;
    private final AgentExecutor executor;

    // 决策提示配置
    private boolean enableDecisionPrompt = true;
    private List<AgentDecisionParser.DownstreamNode> downstreamNodes;

    public AgentNodeHandler() {
        this.promptBuilder = new AgentPromptBuilder();
        this.executor = new AgentExecutor();
    }

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        WorkflowNode node = context.getNode();
        String agentId = getAgentId(node);
        String prompt = promptBuilder.buildPrompt(node, context);
        int timeout = getTimeout(node, executor.getDefaultTimeout());

        try {
            String output = executor.execute(agentId, prompt, context, timeout);
            NodeResult result = parseAgentResult(output);

            // 如果配置了下游节点，尝试解析决策
            if (enableDecisionPrompt && downstreamNodes != null && !downstreamNodes.isEmpty()) {
                parseAndApplyDecision(output, result, node);
            }

            return result;

        } catch (Exception e) {
            logger.error("Agent执行异常: {}", e.getMessage(), e);
            return NodeResult.failed(e.getMessage());
        }
    }

    /**
     * 解析Agent输出中的决策并应用到结果
     */
    private void parseAndApplyDecision(String output, NodeResult result, WorkflowNode node) {
        AgentDecisionParser.AgentDecision decision = AgentDecisionParser.parse(output);
        if (decision != null && decision.getNodeIds() != null && !decision.getNodeIds().isEmpty()) {
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

    /**
     * 解析Agent执行结果
     */
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
            return NodeResult.success(output);
        } catch (Exception e) {
            // 解析失败，仍然返回成功（Agent输出可能不是JSON格式）
            return NodeResult.success(output);
        }
    }

    /**
     * 获取Agent ID
     */
    private String getAgentId(WorkflowNode node) {
        try {
            if (node.getConfig() != null) {
                JsonNode config = objectMapper.readTree(node.getConfig());
                // 支持两种字段名：agentId（camelCase）和 agent_id（snake_case）
                if (config.has("agentId")) {
                    return config.get("agentId").asText();
                }
                if (config.has("agent_id")) {
                    return config.get("agent_id").asText();
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

    // ==================== 配置方法 ====================

    public void setOpenclawCommand(String openclawCommand) {
        this.executor.setOpenclawCommand(openclawCommand);
    }

    public void setUseGatewayApi(boolean useGatewayApi) {
        this.executor.setUseGatewayApi(useGatewayApi);
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.executor.setGatewayUrl(gatewayUrl);
    }

    public void setGatewayToken(String gatewayToken) {
        this.executor.setGatewayToken(gatewayToken);
    }

    public void setEnableDecisionPrompt(boolean enableDecisionPrompt) {
        this.enableDecisionPrompt = enableDecisionPrompt;
        this.promptBuilder.setEnableDecisionPrompt(enableDecisionPrompt);
    }

    public void setDownstreamNodes(List<AgentDecisionParser.DownstreamNode> downstreamNodes) {
        this.downstreamNodes = downstreamNodes;
        this.promptBuilder.setDownstreamNodes(downstreamNodes);
    }

    public void addDownstreamNode(String id, String name, String description) {
        if (this.downstreamNodes == null) {
            this.downstreamNodes = new ArrayList<>();
        }
        AgentDecisionParser.DownstreamNode node = new AgentDecisionParser.DownstreamNode(id, name, description);
        this.downstreamNodes.add(node);
        this.promptBuilder.addDownstreamNode(id, name, description);
    }
}