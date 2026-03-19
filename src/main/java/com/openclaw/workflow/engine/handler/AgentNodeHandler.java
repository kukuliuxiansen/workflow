package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
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
 */
public class AgentNodeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(AgentNodeHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_TIMEOUT = 600;

    private String openclawCommand = "openclaw";

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        WorkflowNode node = context.getNode();
        String agentId = getAgentId(node);
        String prompt = buildPrompt(node, context);
        int timeout = getTimeout(node, DEFAULT_TIMEOUT);
        String sessionId = context.getExecutionId();

        logger.info("执行Agent: {} - 节点: {}", agentId, node.getName());

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

        if (sessionId != null) {
            command.add("--session-id");
            command.add(sessionId);
        }

        try {
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
                return NodeResult.failed("Agent执行超时");
            }

            String outputStr = output.toString().trim();
            return parseAgentResult(outputStr);

        } catch (Exception e) {
            logger.error("Agent执行异常: {}", e.getMessage(), e);
            return NodeResult.failed(e.getMessage());
        }
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
            return NodeResult.success(output);
        } catch (Exception e) {
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

        return prompt.toString();
    }

    public void setOpenclawCommand(String openclawCommand) {
        this.openclawCommand = openclawCommand;
    }
}