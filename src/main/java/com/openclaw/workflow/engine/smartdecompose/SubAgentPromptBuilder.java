package com.openclaw.workflow.engine.smartdecompose;

/**
 * 子Agent提示构建器
 * 负责构建系统提示和用户提示
 */
public class SubAgentPromptBuilder {

    /**
     * 构建系统提示
     */
    public String buildSystemPrompt(SubAgentType agentType) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个专门的 ").append(agentType.getName()).append(" Agent。\n\n");
        prompt.append("## 职责\n\n");
        prompt.append(agentType.getDescription()).append("\n\n");
        prompt.append("## 使用场景\n\n");
        prompt.append(agentType.getUsageGuide()).append("\n\n");

        prompt.append("## 输出格式\n\n");
        prompt.append("完成任务后，请按以下JSON格式返回结果：\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"status\": \"success | failed | timeout\",\n");
        prompt.append("  \"summary\": \"任务完成摘要，简明扼要\",\n");
        prompt.append("  \"outputs\": {\n");
        prompt.append("    \"key1\": \"value1\",\n");
        prompt.append("    \"key2\": \"value2\"\n");
        prompt.append("  },\n");
        prompt.append("  \"recommendations\": [\n");
        prompt.append("    \"建议1\",\n");
        prompt.append("    \"建议2\"\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        prompt.append("## 限制\n\n");
        prompt.append("- 超时时间: ").append(agentType.getTimeoutMinutes()).append(" 分钟\n");
        prompt.append("- 只返回摘要信息，不要返回大量原始数据\n");
        prompt.append("- 确保输出是有效的JSON格式\n");

        return prompt.toString();
    }

    /**
     * 构建用户提示
     */
    public String buildUserPrompt(SubAgentRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("# 任务\n\n");
        prompt.append(request.getPrompt()).append("\n\n");

        if (request.getContext() != null && !request.getContext().isEmpty()) {
            prompt.append("# 上下文信息\n\n");
            for (java.util.Map.Entry<String, Object> entry : request.getContext().entrySet()) {
                prompt.append("- **").append(entry.getKey()).append("**: ")
                      .append(entry.getValue()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("请完成上述任务，并按要求返回JSON格式的结果。");

        return prompt.toString();
    }

    /**
     * 获取Agent ID
     */
    public String getAgentId(SubAgentType agentType) {
        switch (agentType) {
            case EXPLORE:
                return "code-explorer";
            case PLAN:
                return "architect";
            case EXECUTE:
                return "developer";
            case TEST:
                return "tester";
            case REVIEW:
                return "code-reviewer";
            case DOCUMENT:
                return "documenter";
            default:
                return "project-manager";
        }
    }
}