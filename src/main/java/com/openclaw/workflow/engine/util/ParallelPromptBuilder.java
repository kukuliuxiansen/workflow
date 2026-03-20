package com.openclaw.workflow.engine.util;

import com.openclaw.workflow.engine.model.NodeResult;

import java.util.List;
import java.util.Map;

import static com.openclaw.workflow.engine.util.PromptUtils.BranchInfo;
import static com.openclaw.workflow.engine.util.PromptUtils.formatOutput;

/**
 * 并行执行节点提示词构建器
 */
public class ParallelPromptBuilder {

    /**
     * 构建并行执行节点提示词
     */
    public static String build(
            String workflowId,
            String executionId,
            String nodeId,
            String nodeName,
            String executionMode,
            List<BranchInfo> branches,
            Map<String, NodeResult> previousOutputs,
            String taskDescription,
            String customPrompt) {

        StringBuilder prompt = new StringBuilder();

        if (customPrompt != null && !customPrompt.isEmpty()) {
            prompt.append(customPrompt).append("\n\n");
        }

        prompt.append("## 并行执行决策任务\n\n");
        prompt.append("你当前在工作流的【").append(nodeName).append("】节点，需要决定并行执行哪些分支。\n\n");

        // 工作流上下文
        prompt.append("### 当前工作流上下文\n");
        prompt.append("- 工作流ID: ").append(workflowId).append("\n");
        prompt.append("- 执行ID: ").append(executionId).append("\n");
        prompt.append("- 当前节点: ").append(nodeName).append(" (").append(nodeId).append(")\n");
        prompt.append("- 执行模式: **").append(executionMode).append("**\n\n");

        // 执行模式说明
        appendExecutionModeDescription(prompt, executionMode);

        // 任务背景
        if (taskDescription != null && !taskDescription.isEmpty()) {
            prompt.append("### 任务背景\n").append(taskDescription).append("\n\n");
        }

        // 上游节点输出
        appendPreviousOutputs(prompt, previousOutputs);

        // 可选分支表格
        prompt.append("### 可选分支\n");
        if ("ALL".equalsIgnoreCase(executionMode)) {
            prompt.append("当前是ALL模式，需要执行以下所有分支：\n\n");
        } else {
            prompt.append("请从以下分支中选择需要执行的分支（可选多个）：\n\n");
        }
        prompt.append("| 分支ID | 分支名称 | 执行条件说明 |\n");
        prompt.append("|--------|---------|-------------|\n");
        for (BranchInfo branch : branches) {
            prompt.append("| `").append(branch.id).append("` | ");
            prompt.append(branch.name).append(" | ");
            prompt.append(branch.conditionDesc != null ? branch.conditionDesc : branch.description).append(" |\n");
        }
        prompt.append("\n");

        // 决策要求
        appendDecisionRequirements(prompt, executionMode);

        // 输出格式
        prompt.append("### 输出格式\n");
        prompt.append("```\n");
        prompt.append("[NODE_DECISION]\n");
        prompt.append("node_ids: {分支ID1}, {分支ID2}, ...\n");
        prompt.append("reason: {选择这些分支的原因}\n");
        prompt.append("[/NODE_DECISION]\n");
        prompt.append("```\n");

        return prompt.toString();
    }

    private static void appendExecutionModeDescription(StringBuilder prompt, String executionMode) {
        prompt.append("### 执行模式说明\n");
        if ("ALL".equalsIgnoreCase(executionMode)) {
            prompt.append("- **ALL模式**: 必须执行所有分支\n\n");
        } else {
            prompt.append("- **DYNAMIC模式**: 由Agent根据上下文动态选择需要执行的分支\n\n");
        }
    }

    private static void appendPreviousOutputs(StringBuilder prompt, Map<String, NodeResult> previousOutputs) {
        if (previousOutputs == null || previousOutputs.isEmpty()) {
            return;
        }
        prompt.append("### 上游节点输出\n");
        for (Map.Entry<String, NodeResult> entry : previousOutputs.entrySet()) {
            prompt.append("#### ").append(entry.getKey()).append("\n");
            if (entry.getValue() != null && entry.getValue().getOutput() != null) {
                String output = formatOutput(entry.getValue().getOutput());
                prompt.append("```\n").append(output).append("\n```\n\n");
            }
        }
    }

    private static void appendDecisionRequirements(StringBuilder prompt, String executionMode) {
        prompt.append("### 决策要求\n");
        if ("ALL".equalsIgnoreCase(executionMode)) {
            prompt.append("当前是ALL模式，必须选择所有分支。\n\n");
        } else {
            prompt.append("1. 根据上游输出分析需要执行哪些分支\n");
            prompt.append("2. 可以选择多个分支，用逗号分隔\n");
            prompt.append("3. 如果不需要执行任何分支，输出 `none`\n\n");
        }
    }
}