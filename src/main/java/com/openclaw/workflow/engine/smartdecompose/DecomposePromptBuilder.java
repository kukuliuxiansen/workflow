package com.openclaw.workflow.engine.smartdecompose;

import java.util.List;

/**
 * 智能分解提示词构建器
 */
public class DecomposePromptBuilder {

    public static String buildPrompt(DecomposeContext context, TaskState currentTask) {
        StringBuilder prompt = new StringBuilder();

        appendSystemStatus(prompt, context, currentTask);
        appendCurrentTask(prompt, currentTask);
        appendAvailableTools(prompt);
        appendDecisionFramework(prompt);
        appendRecentHistory(prompt, context);

        return prompt.toString();
    }

    private static void appendSystemStatus(StringBuilder prompt, DecomposeContext context, TaskState currentTask) {
        prompt.append("# 当前状态\n\n");
        prompt.append("- 迭代次数: ").append(context.getIterationCount())
              .append(" / ").append(context.getMaxIterations()).append("\n");
        prompt.append("- 递归深度: ").append(currentTask.getDepth())
              .append(" / ").append(context.getMaxDepth()).append("\n");
        prompt.append("- 待处理任务数: ").append(context.getTaskStack().size()).append("\n");
        prompt.append("- 已完成任务数: ").append(context.getCompletedTasks().size()).append("\n\n");
    }

    private static void appendCurrentTask(StringBuilder prompt, TaskState currentTask) {
        prompt.append("# 当前任务\n\n");
        prompt.append("任务ID: ").append(currentTask.getTaskId()).append("\n");
        prompt.append("描述: ").append(currentTask.getDescription()).append("\n");
        prompt.append("深度: ").append(currentTask.getDepth()).append("\n\n");
    }

    private static void appendAvailableTools(StringBuilder prompt) {
        prompt.append("# 可用工具\n\n");
        for (DecomposeTool tool : DecomposeTool.values()) {
            prompt.append("- **").append(tool.getName()).append("**: ")
                  .append(tool.getDescription()).append("\n");
            prompt.append("  - 使用场景: ").append(tool.getUsageGuide()).append("\n");
            prompt.append("  - 必需参数: ").append(tool.getRequiredParameters()).append("\n\n");
        }
    }

    private static void appendDecisionFramework(StringBuilder prompt) {
        prompt.append("# 决策框架\n\n");
        prompt.append("1. **评估任务复杂度**: 判断任务是否需要分解\n");
        prompt.append("   - 复杂任务（多模块、多技能、预估>10分钟）→ 使用 `decompose`\n");
        prompt.append("   - 简单任务（单一操作、预估<5分钟）→ 使用 `execute`\n");
        prompt.append("   - 需要专门技能 → 使用 `spawn_agent`\n\n");

        prompt.append("2. **输出格式**:\n");
        prompt.append("```\n");
        prompt.append("[THOUGHT]\n");
        prompt.append("分析当前任务状态、评估复杂度、决定下一步行动。\n");
        prompt.append("[/THOUGHT]\n\n");
        prompt.append("[ACTION]\n");
        prompt.append("tool: <工具名称>\n");
        prompt.append("<参数名>: <参数值>\n");
        prompt.append("[/ACTION]\n");
        prompt.append("```\n\n");

        prompt.append("3. **注意事项**:\n");
        prompt.append("- 任务完成后调用 `mark_complete`\n");
        prompt.append("- 无法继续时调用 `mark_failed`\n");
        prompt.append("- 需要更多信息时使用 `read_context`\n\n");
    }

    private static void appendRecentHistory(StringBuilder prompt, DecomposeContext context) {
        List<DecomposeContext.DecisionRecord> history = context.getDecisionHistory();
        if (!history.isEmpty()) {
            prompt.append("# 最近决策\n\n");
            int start = Math.max(0, history.size() - 3);
            for (int i = start; i < history.size(); i++) {
                DecomposeContext.DecisionRecord record = history.get(i);
                prompt.append("- **迭代 ").append(record.getIteration()).append("**: ")
                      .append(record.getAction()).append(" -> ")
                      .append(record.getResult() != null && record.getResult().isSuccess() ? "成功" : "失败")
                      .append("\n");
            }
            prompt.append("\n");
        }
    }

    public static String buildSystemPrompt() {
        return "你是一个智能任务执行引擎，负责分析和分解复杂任务。\n\n" +
            "你的职责是：\n" +
            "1. 分析任务复杂度，判断是否需要分解\n" +
            "2. 对于复杂任务，分解为更小的子任务\n" +
            "3. 对于简单任务，直接执行或标记完成\n\n" +
            "输出格式要求：\n" +
            "[THOUGHT]\n" +
            "思考过程...\n" +
            "[/THOUGHT]\n\n" +
            "[ACTION]\n" +
            "tool: <工具名>\n" +
            "参数...\n" +
            "[/ACTION]\n";
    }
}