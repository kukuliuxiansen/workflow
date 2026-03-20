package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.engine.util.AgentDecisionParser;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Agent提示词构建器
 *
 * 负责构建Agent执行所需的完整提示词，包括：
 * - 工作流上下文信息
 * - 任务描述
 * - 全局提示
 * - 节点配置的提示词
 * - 上游节点输出
 * - 决策提示（如有下游节点）
 */
public class AgentPromptBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AgentPromptBuilder.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_OUTPUT_LENGTH = 2000;

    private boolean enableDecisionPrompt = true;
    private List<AgentDecisionParser.DownstreamNode> downstreamNodes;

    /**
     * 构建Agent提示词
     *
     * @param node 工作流节点
     * @param context 执行上下文
     * @return 构建完成的提示词
     */
    public String buildPrompt(WorkflowNode node, NodeExecutionContext context) {
        StringBuilder prompt = new StringBuilder();

        appendWorkflowContext(prompt, node, context);
        appendTaskDescription(prompt, context);
        appendGlobalPrompt(prompt, context);
        appendNodePrompt(prompt, node);
        appendPreviousOutputs(prompt, context);
        appendDecisionPrompt(prompt, node);

        logger.debug("构建的Agent提示词长度: {}", prompt.length());
        return prompt.toString();
    }

    /**
     * 添加工作流上下文信息
     */
    private void appendWorkflowContext(StringBuilder prompt, WorkflowNode node, NodeExecutionContext context) {
        prompt.append("## 工作流上下文\n");
        prompt.append("- 工作流ID: ").append(context.getWorkflowId()).append("\n");
        prompt.append("- 执行ID: ").append(context.getExecutionId()).append("\n");
        if (context.getWorkflowName() != null) {
            prompt.append("- 工作流名称: ").append(context.getWorkflowName()).append("\n");
        }
        prompt.append("- 当前节点: ").append(node.getName()).append(" (").append(node.getId()).append(")\n");
        if (context.getProjectPath() != null) {
            prompt.append("- 项目路径: ").append(context.getProjectPath()).append("\n");
        }
        prompt.append("\n");
    }

    /**
     * 添加任务描述
     */
    private void appendTaskDescription(StringBuilder prompt, NodeExecutionContext context) {
        if (context.getTaskDescription() != null && !context.getTaskDescription().isEmpty()) {
            prompt.append("## 任务描述\n").append(context.getTaskDescription()).append("\n\n");
        }
    }

    /**
     * 添加全局提示
     */
    private void appendGlobalPrompt(StringBuilder prompt, NodeExecutionContext context) {
        if (context.getGlobalPrompt() != null && !context.getGlobalPrompt().isEmpty()) {
            prompt.append("## 全局提示\n").append(context.getGlobalPrompt()).append("\n\n");
        }
    }

    /**
     * 添加节点配置的提示词
     */
    private void appendNodePrompt(StringBuilder prompt, WorkflowNode node) {
        try {
            if (node.getConfig() != null) {
                JsonNode config = objectMapper.readTree(node.getConfig());
                if (config.has("prompt") && config.get("prompt").asText() != null
                        && !config.get("prompt").asText().isEmpty()) {
                    prompt.append("## 执行任务\n").append(config.get("prompt").asText()).append("\n\n");
                }
            }
        } catch (Exception e) {
            logger.warn("解析节点配置失败: {}", e.getMessage());
        }
    }

    /**
     * 添加上游节点输出
     */
    private void appendPreviousOutputs(StringBuilder prompt, NodeExecutionContext context) {
        Map<String, NodeResult> previousOutputs = context.getPreviousOutputs();
        if (previousOutputs != null && !previousOutputs.isEmpty()) {
            prompt.append("## 上游节点输出\n");
            for (Map.Entry<String, NodeResult> entry : previousOutputs.entrySet()) {
                prompt.append("### ").append(entry.getKey()).append("\n");
                if (entry.getValue() != null && entry.getValue().getOutput() != null) {
                    String output = entry.getValue().getOutput().toString();
                    if (output.length() > MAX_OUTPUT_LENGTH) {
                        prompt.append(output.substring(0, MAX_OUTPUT_LENGTH))
                              .append("\n... (输出已截断)\n");
                    } else {
                        prompt.append(output).append("\n");
                    }
                }
            }
            prompt.append("\n");
        }
    }

    /**
     * 添加决策提示（如果配置了下游节点）
     */
    private void appendDecisionPrompt(StringBuilder prompt, WorkflowNode node) {
        if (enableDecisionPrompt && downstreamNodes != null && !downstreamNodes.isEmpty()) {
            String decisionPrompt = AgentDecisionParser.buildDecisionPrompt(
                    node.getType().name(),
                    node.getName(),
                    downstreamNodes);
            prompt.append(decisionPrompt);
        }
    }

    // ==================== 配置方法 ====================

    public void setEnableDecisionPrompt(boolean enableDecisionPrompt) {
        this.enableDecisionPrompt = enableDecisionPrompt;
    }

    public void setDownstreamNodes(List<AgentDecisionParser.DownstreamNode> downstreamNodes) {
        this.downstreamNodes = downstreamNodes;
    }

    public void addDownstreamNode(String id, String name, String description) {
        if (this.downstreamNodes == null) {
            this.downstreamNodes = new java.util.ArrayList<>();
        }
        this.downstreamNodes.add(new AgentDecisionParser.DownstreamNode(id, name, description));
    }
}