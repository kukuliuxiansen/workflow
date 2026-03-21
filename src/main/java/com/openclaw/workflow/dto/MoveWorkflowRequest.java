package com.openclaw.workflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 移动工作流请求
 */
public class MoveWorkflowRequest {

    @JsonProperty("targetFolderId")
    private String targetFolderId;

    public String getTargetFolderId() { return targetFolderId; }
    public void setTargetFolderId(String targetFolderId) { this.targetFolderId = targetFolderId; }
}