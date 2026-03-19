package com.openclaw.workflow.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 创建连线请求
 */
public class CreateEdgeRequest {

    @JsonProperty("source")
    @JsonAlias("sourceNodeId")
    private String sourceNodeId;

    @JsonProperty("target")
    @JsonAlias("targetNodeId")
    private String targetNodeId;

    @JsonProperty("type")
    @JsonAlias("edgeType")
    private String edgeType;

    private String label;

    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }

    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }

    public String getEdgeType() { return edgeType; }
    public void setEdgeType(String edgeType) { this.edgeType = edgeType; }

    @JsonIgnore
    public com.openclaw.workflow.entity.WorkflowEdge.EdgeType getEdgeTypeEnum() {
        if (edgeType == null) return com.openclaw.workflow.entity.WorkflowEdge.EdgeType.SUCCESS;
        try {
            return com.openclaw.workflow.entity.WorkflowEdge.EdgeType.valueOf(edgeType.toUpperCase());
        } catch (Exception e) {
            return com.openclaw.workflow.entity.WorkflowEdge.EdgeType.SUCCESS;
        }
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}