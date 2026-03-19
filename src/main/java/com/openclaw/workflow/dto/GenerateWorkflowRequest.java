package com.openclaw.workflow.dto;

/**
 * AI生成工作流请求
 */
public class GenerateWorkflowRequest {

    private String name;
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
