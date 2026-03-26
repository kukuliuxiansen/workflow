package com.openclaw.workflow.engine.connector;

/**
 * Agent 执行请求
 */
public class AgentRequest {
    private String agentId;
    private String message;
    private String systemPrompt;
    private String sessionKey;
    private String context;
    private String channel;
    private Integer maxTokens;
    private Double temperature;

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    public String getSessionKey() { return sessionKey; }
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AgentRequest request = new AgentRequest();

        public Builder agentId(String agentId) { request.setAgentId(agentId); return this; }
        public Builder message(String message) { request.setMessage(message); return this; }
        public Builder systemPrompt(String systemPrompt) { request.setSystemPrompt(systemPrompt); return this; }
        public Builder sessionKey(String sessionKey) { request.setSessionKey(sessionKey); return this; }
        public Builder context(String context) { request.setContext(context); return this; }
        public Builder channel(String channel) { request.setChannel(channel); return this; }
        public Builder maxTokens(Integer maxTokens) { request.setMaxTokens(maxTokens); return this; }
        public Builder temperature(Double temperature) { request.setTemperature(temperature); return this; }
        public AgentRequest build() { return request; }
    }
}