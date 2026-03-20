package com.openclaw.workflow.engine.util;

import com.openclaw.workflow.engine.model.NodeResult;

import java.util.Map;

import static com.openclaw.workflow.engine.util.PromptUtils.formatOutput;

/**
 * 循环节点提示词构建器
 */
public class LoopPromptBuilder {

    /**
     * 构建循环节点提示词
     */
    public static String build(
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
        appendLoopStatus(prompt, currentIteration, maxIterations, loopMode);

        // 循环变量
        appendLoopVariable(prompt, loopVariable, loopValue);

        // 上游输出
        appendPreviousOutputs(prompt, previousOutputs);

        // 退出条件
        appendExitCondition(prompt, exitCondition);

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

    private static void appendLoopStatus(StringBuilder prompt, int currentIteration, int maxIterations, String loopMode) {
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
    }

    private static void appendLoopVariable(StringBuilder prompt, String loopVariable, Object loopValue) {
        if (loopVariable == null || loopValue == null) {
            return;
        }
        prompt.append("### 当前循环变量\n");
        prompt.append("- 变量名: `").append(loopVariable).append("`\n");
        prompt.append("- 当前值:\n```\n").append(formatOutput(loopValue)).append("\n```\n\n");
    }

    private static void appendPreviousOutputs(StringBuilder prompt, Map<String, NodeResult> previousOutputs) {
        if (previousOutputs == null || previousOutputs.isEmpty()) {
            return;
        }
        prompt.append("### 本次循环执行结果\n");
        for (Map.Entry<String, NodeResult> entry : previousOutputs.entrySet()) {
            prompt.append("#### ").append(entry.getKey()).append("\n");
            if (entry.getValue() != null && entry.getValue().getOutput() != null) {
                prompt.append("```\n").append(formatOutput(entry.getValue().getOutput())).append("\n```\n\n");
            }
        }
    }

    private static void appendExitCondition(StringBuilder prompt, String exitCondition) {
        prompt.append("### 退出条件\n");
        if (exitCondition != null && !exitCondition.isEmpty()) {
            prompt.append(exitCondition).append("\n\n");
        } else {
            prompt.append("（未配置具体退出条件，由Agent根据执行结果判断）\n\n");
        }
    }
}