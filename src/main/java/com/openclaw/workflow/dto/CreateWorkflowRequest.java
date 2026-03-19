package com.openclaw.workflow.dto;

/**
 * 创建工作流请求
 */
public class CreateWorkflowRequest {

    private String name;
    private String description;
    private String folderId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFolderId() { return folderId; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
}