package com.openclaw.workflow.service.ai;

/**
 * AI 提示词构建器
 */
public class AIPromptBuilder {

    /**
     * 构建工作流生成的系统提示词
     */
    public static String buildWorkflowSystemPrompt() {
        return "你是一个工作流设计专家。用户会描述他们的需求，你需要生成一个工作流 JSON 定义。\n\n" +
                "工作流支持的节点类型：\n" +
                "1. start - 开始节点（必须有，ID为\"start\"）\n" +
                "2. agent_execution - Agent执行节点，需要 agentId 和 prompt\n" +
                "3. api_call - API调用节点，需要 url 和 method\n" +
                "4. condition - 条件判断节点\n" +
                "5. human_review - 人工审核节点\n" +
                "6. finish - 结束节点（必须有，ID为\"finish\"）\n\n" +
                "请直接返回 JSON 格式的工作流定义，不要有任何其他文字。";
    }

    /**
     * 构建工作流生成的用户提示词
     */
    public static String buildWorkflowUserPrompt(String description, String name) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下需求生成工作流定义：\n\n");
        prompt.append("需求描述：").append(description).append("\n\n");
        if (name != null && !name.isEmpty()) {
            prompt.append("工作流名称：").append(name).append("\n\n");
        }
        prompt.append("请生成 JSON 格式的工作流定义：");
        return prompt.toString();
    }

    /**
     * 构建中间提示词生成的系统提示词
     */
    public static String buildIntermediateSystemPrompt() {
        return "你是一个工作流节点提示词设计专家。生成简洁明了的中间提示词草案。";
    }

    /**
     * 构建中间提示词生成的用户提示词
     */
    public static String buildIntermediateUserPrompt(String requirement) {
        return "需求：" + requirement + "\n\n请生成中间提示词草案：";
    }

    /**
     * 构建最终提示词生成的系统提示词
     */
    public static String buildFinalSystemPrompt() {
        return "你是一个工作流节点执行提示词专家。生成完整、详细的可执行提示词。";
    }

    /**
     * 构建最终提示词生成的用户提示词
     */
    public static String buildFinalUserPrompt(String intermediatePrompt) {
        return "中间草案：" + intermediatePrompt + "\n\n请生成完整的可执行提示词：";
    }

    /**
     * 构建节点名称生成的提示词
     */
    public static String buildNodeNamePrompt(String nodeType, String originalName) {
        return "节点类型: " + nodeType + "\n原始名称: " + originalName + "\n\n" +
                "请为这个工作流节点生成一个简洁友好的中文名称（最多6个字），只返回名称，不要其他文字：";
    }

    /**
     * 截断字符串
     */
    public static String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}