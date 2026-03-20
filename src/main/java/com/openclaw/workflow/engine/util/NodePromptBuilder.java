package com.openclaw.workflow.engine.util;

import com.openclaw.workflow.engine.model.NodeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 节点提示词构建器
 *
 * 为各类决策节点生成标准化、通用的提示词模板
 */
public class NodePromptBuilder {

    private static final Logger logger = LoggerFactory.getLogger(NodePromptBuilder.class);

    /**
     * 构建条件判断节点提示词
     */
    public static String buildConditionPrompt(
            String workflowId,
            String executionId,
            String nodeId,
            String nodeName,
            List<BranchInfo> branches,
            String defaultBranch,
            Map<String, NodeResult> previousOutputs,
            String taskDescription,
            String customPrompt) {

        StringBuilder prompt = new StringBuilder();

        // 如果有自定义提示词，优先使用
        if (customPrompt != null && !customPrompt.isEmpty()) {
            prompt.append(customPrompt).append("\n\n");
        }

        prompt.append("## 条件判断决策任务\n\n");
        prompt.append("你当前在工作流的【").append(nodeName).append("】节点，需要根据上游输出决定执行哪个分支。\n\n");

        // 工作流上下文
        prompt.append("### 当前工作流上下文\n");
        prompt.append("- 工作流ID: ").append(workflowId).append("\n");
        prompt.append("- 执行ID: ").append(executionId).append("\n");
        prompt.append("- 当前节点: ").append(nodeName).append(" (").append(nodeId).append(")\n\n");

        // 任务背景
        if (taskDescription != null && !taskDescription.isEmpty()) {
            prompt.append("### 任务背景\n").append(taskDescription).append("\n\n");
        }

        // 上游节点输出
        if (previousOutputs != null && !previousOutputs.isEmpty()) {
            prompt.append("### 上游节点输出\n");
            prompt.append("以下是之前节点执行产生的输出，请仔细阅读作为决策依据：\n\n");
            for (Map.Entry<String, NodeResult> entry : previousOutputs.entrySet()) {
                prompt.append("#### ").append(entry.getKey()).append("\n");
                if (entry.getValue() != null && entry.getValue().getOutput() != null) {
                    String output = formatOutput(entry.getValue().getOutput());
                    prompt.append("```\n").append(output).append("\n```\n\n");
                }
            }
        }

        // 可选分支表格
        prompt.append("### 可选分支\n");
        prompt.append("请从以下分支中选择**一个**执行：\n\n");
        prompt.append("| 分支ID | 分支名称 | 触发条件说明 |\n");
        prompt.append("|--------|---------|-------------|\n");
        for (BranchInfo branch : branches) {
            prompt.append("| `").append(branch.id).append("` | ");
            prompt.append(branch.name).append(" | ");
            prompt.append(branch.conditionDesc != null ? branch.conditionDesc : branch.description).append(" |\n");
        }
        prompt.append("\n");

        // 默认分支提示
        if (defaultBranch != null) {
            prompt.append("**默认分支**: `").append(defaultBranch).append("` （当无法判断时使用）\n\n");
        }

        // 决策要求
        prompt.append("### 决策要求\n");
        prompt.append("1. 仔细分析上游节点的输出内容\n");
        prompt.append("2. 根据上述条件说明，判断应该走哪个分支\n");
        prompt.append("3. **必须选择一个分支**，不能不选\n");
        prompt.append("4. 选择后请说明选择原因\n\n");

        // 输出格式
        prompt.append("### 输出格式\n");
        prompt.append("请严格按照以下格式输出你的决策：\n\n");
        prompt.append("```\n");
        prompt.append("[NODE_DECISION]\n");
        prompt.append("node_ids: {选择的分支ID}\n");
        prompt.append("reason: {选择该分支的原因}\n");
        prompt.append("[/NODE_DECISION]\n");
        prompt.append("```\n");

        return prompt.toString();
    }

    /**
     * 构建并行执行节点提示词
     */
    public static String buildParallelPrompt(
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
        prompt.append("### 执行模式说明\n");
        if ("ALL".equalsIgnoreCase(executionMode)) {
            prompt.append("- **ALL模式**: 必须执行所有分支\n\n");
        } else {
            prompt.append("- **DYNAMIC模式**: 由Agent根据上下文动态选择需要执行的分支\n\n");
        }

        // 任务背景
        if (taskDescription != null && !taskDescription.isEmpty()) {
            prompt.append("### 任务背景\n").append(taskDescription).append("\n\n");
        }

        // 上游节点输出
        if (previousOutputs != null && !previousOutputs.isEmpty()) {
            prompt.append("### 上游节点输出\n");
            for (Map.Entry<String, NodeResult> entry : previousOutputs.entrySet()) {
                prompt.append("#### ").append(entry.getKey()).append("\n");
                if (entry.getValue() != null && entry.getValue().getOutput() != null) {
                    String output = formatOutput(entry.getValue().getOutput());
                    prompt.append("```\n").append(output).append("\n```\n\n");
                }
            }
        }

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
        prompt.append("### 决策要求\n");
        if ("ALL".equalsIgnoreCase(executionMode)) {
            prompt.append("当前是ALL模式，必须选择所有分支。\n\n");
        } else {
            prompt.append("1. 根据上游输出分析需要执行哪些分支\n");
            prompt.append("2. 可以选择多个分支，用逗号分隔\n");
            prompt.append("3. 如果不需要执行任何分支，输出 `none`\n\n");
        }

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

    /**
     * 构建循环节点提示词
     */
    public static String buildLoopPrompt(
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

        StringBuilder prompt = new StringBuilder();

        if (customPrompt != null && !customPrompt.isEmpty()) {
            prompt.append(customPrompt).append("\n\n");
        }

        prompt.append("## 循环决策任务\n\n");
        prompt.append("你当前在工作流的【").append(nodeName).append("】节点，需要判断是否继续循环。\n\n");

        // 循环状态
        prompt.append("### 循环状态\n");
        prompt.append("- 当前迭代: **第 ").append(currentIteration).append(" 次**\n");
        prompt.append("- 最大迭代: ").append(maxIterations).append(" 次\n");
        prompt.append("- 循环模式: ").append(loopMode).append("\n\n");

        // 进度条
        int progress = (int) ((currentIteration * 100.0) / maxIterations);
        prompt.append("进度: [");
        int filled = progress / 5;
        for (int i = 0; i < 20; i++) {
            prompt.append(i < filled ? "█" : "░");
        }
        prompt.append("] ").append(progress).append("%\n\n");

        // 循环变量
        if (loopVariable != null && loopValue != null) {
            prompt.append("### 当前循环变量\n");
            prompt.append("- 变量名: `").append(loopVariable).append("`\n");
            prompt.append("- 当前值:\n```\n").append(formatOutput(loopValue)).append("\n```\n\n");
        }

        // 上游输出
        if (previousOutputs != null && !previousOutputs.isEmpty()) {
            prompt.append("### 本次循环执行结果\n");
            for (Map.Entry<String, NodeResult> entry : previousOutputs.entrySet()) {
                prompt.append("#### ").append(entry.getKey()).append("\n");
                if (entry.getValue() != null && entry.getValue().getOutput() != null) {
                    prompt.append("```\n").append(formatOutput(entry.getValue().getOutput())).append("\n```\n\n");
                }
            }
        }

        // 退出条件
        prompt.append("### 退出条件\n");
        if (exitCondition != null && !exitCondition.isEmpty()) {
            prompt.append(exitCondition).append("\n\n");
        } else {
            prompt.append("（未配置具体退出条件，由Agent根据执行结果判断）\n\n");
        }

        // 决策选项
        prompt.append("### 决策选项\n");
        prompt.append("| 选项 | 说明 |\n");
        prompt.append("|------|------|\n");
        prompt.append("| `continue` | 继续执行下一次循环 |\n");
        prompt.append("| `exit` | 退出循环，继续后续流程 |\n\n");

        // 决策要求
        prompt.append("### 决策要求\n");
        prompt.append("1. 分析当前循环的执行结果\n");
        prompt.append("2. 判断是否满足退出条件\n");
        prompt.append("3. 如果达到最大迭代次数，应该选择退出\n");
        prompt.append("4. 如果遇到错误无法继续，应该选择退出\n\n");

        // 输出格式
        prompt.append("### 输出格式\n");
        prompt.append("```\n");
        prompt.append("[NODE_DECISION]\n");
        prompt.append("node_ids: continue 或 exit\n");
        prompt.append("reason: {继续或退出的原因}\n");
        prompt.append("[/NODE_DECISION]\n");
        prompt.append("```\n");

        return prompt.toString();
    }

    /**
     * 格式化输出对象
     */
    private static String formatOutput(Object output) {
        if (output == null) {
            return "(无输出)";
        }
        String str = output.toString();
        if (str.length() > 2000) {
            return str.substring(0, 2000) + "\n... (输出过长，已截断)";
        }
        return str;
    }

    /**
     * 分支信息
     */
    public static class BranchInfo {
        public String id;
        public String name;
        public String description;
        public String conditionDesc;  // 条件描述（给Agent看）
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