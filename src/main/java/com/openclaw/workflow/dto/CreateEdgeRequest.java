package com.openclaw.workflow.dto;

import com.openclaw.workflow.entity.WorkflowEdge.EdgeType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 创建连线请求
 */
public class CreateEdgeRequest {

    @JsonProperty("sourceNodeId")
    private String sourceNodeId;

    @JsonProperty("targetNodeId")
    private String targetNodeId;

    @JsonProperty("edgeType")
    private String edgeType;

    private String label;

    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }

    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }

    public String getEdgeType() { return edgeType; }
    public void setEdgeType(String edgeType) { this.edgeType = edgeType; }

    @JsonIgnore
    public EdgeType getEdgeTypeEnum() {
        if (edgeType == null) return EdgeType.SUCCESS;
        try {
            return EdgeType.valueOf(edgeType.toUpperCase());
        } catch (Exception e) {
            return EdgeType.SUCCESS;
        }
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}