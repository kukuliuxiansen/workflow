package com.openclaw.workflow.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.config.NodePromptConfig;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.repository.WorkflowEdgeRepository;
import com.openclaw.workflow.repository.WorkflowNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 节点提示词构建服务
 *
 * 负责根据模板和上下文构建完整的提示词
 * 自动注入上下游节点关系等上下文信息
 */
@Service
public class NodePromptService {

    private static final Logger logger = LoggerFactory.getLogger(NodePromptService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final NodePromptConfig promptConfig;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;

    public NodePromptService(NodePromptConfig promptConfig,
                            WorkflowNodeRepository nodeRepository,
                            WorkflowEdgeRepository edgeRepository) {
        this.promptConfig = promptConfig;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    /**
     * 构建条件判断节点提示词
     */
    public String buildConditionPrompt(
            String workflowId,
            String executionId,
            String nodeId,
            String nodeName,
            List<BranchInfo> branches,
            String defaultBranch,
            Map<String, NodeResult> previousOutputs,
            String taskDescription,
            String customPrompt) {

        // 获取模板（优先使用自定义提示词，其次使用配置的模板）
        String template = customPrompt != null && !customPrompt.isEmpty()
                ? customPrompt
                : promptConfig.getPromptTemplate("condition");

        if (template == null || template.isEmpty()) {
            template = getDefaultConditionTemplate();
        }

        // 构建变量映射
        Map<String, String> variables = new HashMap<>();
        variables.put("workflowId", workflowId != null ? workflowId : "");
        variables.put("executionId", executionId != null ? executionId : "");
        variables.put("nodeId", nodeId != null ? nodeId : "");
        variables.put("nodeName", nodeName != null ? nodeName : "");

        // 任务上下文
        String taskContext = taskDescription != null && !taskDescription.isEmpty()
                ? "\n- 任务描述: " + taskDescription
                : "";
        variables.put("taskContext", taskContext);

        // 上游节点输出
        variables.put("upstreamOutputs", formatUpstreamOutputs(previousOutputs));

        // 下游节点信息
        variables.put("downstreamNodes", getDownstreamNodesInfo(workflowId, nodeId));

        // 分支表格
        variables.put("branchTable", buildBranchTable(branches));

        // 默认分支提示
        String defaultBranchHint = defaultBranch != null && !defaultBranch.isEmpty()
                ? "**默认分支**: `" + defaultBranch + "` （当无法判断时使用）"
                : "";
        variables.put("defaultBranchHint", defaultBranchHint);

        return fillTemplate(template, variables);
    }

    /**
     * 构建并行执行节点提示词
     */
    public String buildParallelPrompt(
            String workflowId,
            String executionId,
            String nodeId,
            String nodeName,
            String executionMode,
            List<BranchInfo> branches,
            Map<String, NodeResult> previousOutputs,
            String taskDescription,
            String customPrompt) {

        String template = customPrompt != null && !customPrompt.isEmpty()
                ? customPrompt
                : promptConfig.getPromptTemplate("parallel");

        if (template == null || template.isEmpty()) {
            template = getDefaultParallelTemplate();
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("workflowId", workflowId != null ? workflowId : "");
        variables.put("executionId", executionId != null ? executionId : "");
        variables.put("nodeId", nodeId != null ? nodeId : "");
        variables.put("nodeName", nodeName != null ? nodeName : "");
        variables.put("executionMode", executionMode != null ? executionMode : "ALL");

        String taskContext = taskDescription != null && !taskDescription.isEmpty()
                ? "\n- 任务描述: " + taskDescription
                : "";
        variables.put("taskContext", taskContext);

        variables.put("upstreamOutputs", formatUpstreamOutputs(previousOutputs));
        variables.put("downstreamNodes", getDownstreamNodesInfo(workflowId, nodeId));
        variables.put("branchTable", buildBranchTable(branches));

        // 执行模式说明
        String executionModeDesc = "ALL".equalsIgnoreCase(executionMode)
                ? "- **ALL模式**: 必须执行所有分支"
                : "- **DYNAMIC模式**: 由Agent根据上下文动态选择需要执行的分支";
        variables.put("executionModeDesc", executionModeDesc);

        // 决策要求
        String decisionRequirements = "ALL".equalsIgnoreCase(executionMode)
                ? "当前是ALL模式，必须选择所有分支。"
                : "1. 根据上游输出分析需要执行哪些分支\n2. 可以选择多个分支，用逗号分隔\n3. 如果不需要执行任何分支，输出 `none`";
        variables.put("decisionRequirements", decisionRequirements);

        return fillTemplate(template, variables);
    }

    /**
     * 构建循环节点提示词
     */
    public String buildLoopPrompt(
            String workflowId,
            String executionId,
            String nodeId,
            String nodeName,
            String loopMode,
            int currentIteration,
            int maxIterations,
            String exitCondition,
            String loopVariable,
            Object loopValue,
            Map<String, NodeResult> previousOutputs,
            String customPrompt) {

        String template = customPrompt != null && !customPrompt.isEmpty()
                ? customPrompt
                : promptConfig.getPromptTemplate("loop");

        if (template == null || template.isEmpty()) {
            template = getDefaultLoopTemplate();
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("workflowId", workflowId != null ? workflowId : "");
        variables.put("executionId", executionId != null ? executionId : "");
        variables.put("nodeId", nodeId != null ? nodeId : "");
        variables.put("nodeName", nodeName != null ? nodeName : "");
        variables.put("loopMode", loopMode != null ? loopMode : "condition");
        variables.put("currentIteration", String.valueOf(currentIteration));
        variables.put("maxIterations", String.valueOf(maxIterations));
        variables.put("loopVariable", loopVariable != null ? loopVariable : "item");
        variables.put("currentValue", loopValue != null ? formatValue(loopValue) : "(无)");

        // 进度条
        int progress = (int) ((currentIteration * 100.0) / maxIterations);
        StringBuilder progressBar = new StringBuilder("进度: [");
        int filled = progress / 5;
        for (int i = 0; i < 20; i++) {
            progressBar.append(i < filled ? "█" : "░");
        }
        progressBar.append("] ").append(progress).append("%");
        variables.put("progressBar", progressBar.toString());

        // 退出条件
        String exitCond = exitCondition != null && !exitCondition.isEmpty()
                ? exitCondition
                : "（未配置具体退出条件，由Agent根据执行结果判断）";
        variables.put("exitCondition", exitCond);

        // 循环执行结果
        variables.put("loopResults", formatUpstreamOutputs(previousOutputs));

        return fillTemplate(template, variables);
    }

    /**
     * 构建Agent执行节点提示词
     */
    public String buildAgentPrompt(
            String workflowId,
            String executionId,
            String nodeId,
            String nodeName,
            String taskDescription,
            String globalPrompt,
            String nodePrompt,
            Map<String, NodeResult> previousOutputs) {

        String template = promptConfig.getPromptTemplate("agent_execution");

        if (template == null || template.isEmpty()) {
            template = getDefaultAgentTemplate();
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("workflowId", workflowId != null ? workflowId : "");
        variables.put("executionId", executionId != null ? executionId : "");
        variables.put("nodeId", nodeId != null ? nodeId : "");
        variables.put("nodeName", nodeName != null ? nodeName : "");
        variables.put("taskDescription", taskDescription != null ? taskDescription : "无具体任务描述");
        variables.put("globalPrompt", globalPrompt != null && !globalPrompt.isEmpty() ? globalPrompt : "无全局提示");
        variables.put("nodePrompt", nodePrompt != null && !nodePrompt.isEmpty() ? nodePrompt : "无节点特定提示");
        variables.put("upstreamOutputs", formatUpstreamOutputs(previousOutputs));
        variables.put("downstreamNodes", getDownstreamNodesInfo(workflowId, nodeId));

        return fillTemplate(template, variables);
    }

    /**
     * 获取下游节点信息（代码写死获取逻辑）
     */
    private String getDownstreamNodesInfo(String workflowId, String nodeId) {
        if (workflowId == null || nodeId == null) {
            return "无下游节点信息";
        }

        try {
            List<com.openclaw.workflow.entity.WorkflowEdge> edges =
                    edgeRepository.findByWorkflowIdAndSourceNodeId(workflowId, nodeId);

            if (edges.isEmpty()) {
                return "当前节点没有下游节点";
            }

            StringBuilder info = new StringBuilder();
            info.append("当前节点的下游节点：\n");

            for (com.openclaw.workflow.entity.WorkflowEdge edge : edges) {
                Optional<WorkflowNode> targetNode = nodeRepository.findByWorkflowIdAndId(workflowId, edge.getTargetNodeId());
                if (targetNode.isPresent()) {
                    WorkflowNode node = targetNode.get();
                    info.append("- ").append(node.getName())
                        .append(" (").append(node.getId()).append(", 类型: ").append(node.getType()).append(")\n");
                }
            }

            return info.toString();
        } catch (Exception e) {
            logger.warn("获取下游节点信息失败: {}", e.getMessage());
            return "无法获取下游节点信息";
        }
    }

    /**
     * 格式化上游节点输出
     */
    private String formatUpstreamOutputs(Map<String, NodeResult> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return "暂无上游节点输出";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, NodeResult> entry : outputs.entrySet()) {
            sb.append("#### ").append(entry.getKey()).append("\n");
            if (entry.getValue() != null && entry.getValue().getOutput() != null) {
                sb.append("```\n").append(formatValue(entry.getValue().getOutput())).append("\n```\n\n");
            }
        }
        return sb.toString();
    }

    /**
     * 构建分支表格
     */
    private String buildBranchTable(List<BranchInfo> branches) {
        if (branches == null || branches.isEmpty()) {
            return "无可用分支";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("| 分支ID | 分支名称 | 条件说明 |\n");
        sb.append("|--------|---------|----------|\n");

        for (BranchInfo branch : branches) {
            sb.append("| `").append(branch.id).append("` | ");
            sb.append(branch.name).append(" | ");
            sb.append(branch.conditionDesc != null ? branch.conditionDesc : branch.description != null ? branch.description : "-");
            sb.append(" |\n");
        }

        return sb.toString();
    }

    /**
     * 填充模板变量
     */
    private String fillTemplate(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /**
     * 格式化值
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "(空)";
        }
        try {
            if (value instanceof String) {
                return (String) value;
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }

    // 默认模板（兜底）
    private String getDefaultConditionTemplate() {
        return "条件判断节点 - 请配置决策分支";
    }

    private String getDefaultParallelTemplate() {
        return "并行执行节点 - 请配置执行分支";
    }

    private String getDefaultLoopTemplate() {
        return "循环节点 - 请配置循环条件";
    }

    private String getDefaultAgentTemplate() {
        return "Agent执行节点 - 请配置任务提示";
    }

    /**
     * 分支信息
     */
    public static class BranchInfo {
        public String id;
        public String name;
        public String description;
        public String conditionDesc;
        public String targetNodeId;

        public BranchInfo(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public BranchInfo(String id, String name, String description, String conditionDesc) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.conditionDesc = conditionDesc;
        }
    }
}