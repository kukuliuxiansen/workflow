package com.openclaw.workflow.dto;

/**
 * 移动工作流请求
 */
public class MoveWorkflowRequest {

    private String targetFolderId;

    public String getTargetFolderId() { return targetFolderId; }
    public void setTargetFolderId(String targetFolderId) { this.targetFolderId = targetFolderId; }
}