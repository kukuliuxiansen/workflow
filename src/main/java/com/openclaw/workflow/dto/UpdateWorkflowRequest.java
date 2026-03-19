package com.openclaw.workflow.dto;

/**
 * 更新工作流请求
 */
public class UpdateWorkflowRequest {

    private String name;
    private String description;
    private String globalConfig;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGlobalConfig() { return globalConfig; }
    public void setGlobalConfig(String globalConfig) { this.globalConfig = globalConfig; }
}