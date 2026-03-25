package com.openclaw.workflow.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 工作流连线实体
 */
@Entity
@Table(name = "workflow_edge")
public class WorkflowEdge {

    @Id
    private String id;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Column(name = "source_node_id", nullable = false)
    @JsonProperty("sourceNodeId")
    @JsonAlias({"source", "source_node_id"})
    private String sourceNodeId;

    @Column(name = "target_node_id", nullable = false)
    @JsonProperty("targetNodeId")
    @JsonAlias({"target", "target_node_id"})
    private String targetNodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "edge_type")
    private EdgeType edgeType = EdgeType.SUCCESS;

    @Column(name = "condition_expr")
    private String conditionExpr;

    private String label;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public WorkflowEdge() {
        this.createdAt = LocalDateTime.now();
    }

    public enum EdgeType {
        SUCCESS,
        FAIL;

        @JsonValue
        public String toLowerCase() {
            return name().toLowerCase();
        }

        @JsonCreator
        public static EdgeType fromString(String value) {
            if (value == null) return SUCCESS;
            return EdgeType.valueOf(value.toUpperCase());
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }

    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }

    public EdgeType getEdgeType() { return edgeType; }
    public void setEdgeType(EdgeType edgeType) { this.edgeType = edgeType; }

    public String getConditionExpr() { return conditionExpr; }
    public void setConditionExpr(String conditionExpr) { this.conditionExpr = conditionExpr; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}