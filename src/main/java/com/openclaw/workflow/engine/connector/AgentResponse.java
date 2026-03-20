package com.openclaw.workflow.engine.connector;

/**
 * Agent 执行响应
 */
public class AgentResponse {
    private boolean success;
    private String content;
    private String sessionKey;
    private String agentId;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private String errorMessage;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSessionKey() { return sessionKey; }
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }

    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }

    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public static AgentResponse success(String content, String sessionKey) {
        AgentResponse response = new AgentResponse();
        response.setSuccess(true);
        response.setContent(content);
        response.setSessionKey(sessionKey);
        return response;
    }

    public static AgentResponse failure(String errorMessage) {
        AgentResponse response = new AgentResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
}