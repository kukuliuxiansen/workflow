package com.openclaw.workflow.engine.smartdecompose;

import java.util.Map;

/**
 * 子Agent执行请求
 */
public class SubAgentRequest {

    private SubAgentType agentType;
    private String prompt;
    private Map<String, Object> context;
    private String parentExecutionId;

    // Getters and Setters
    public SubAgentType getAgentType() { return agentType; }
    public void setAgentType(SubAgentType agentType) { this.agentType = agentType; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }

    public String getParentExecutionId() { return parentExecutionId; }
    public void setParentExecutionId(String parentExecutionId) { this.parentExecutionId = parentExecutionId; }
}