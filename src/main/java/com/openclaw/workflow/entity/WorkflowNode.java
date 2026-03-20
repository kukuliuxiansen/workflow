package com.openclaw.workflow.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 工作流节点实体
 */
@Entity
@Table(name = "workflow_node")
public class WorkflowNode {

    @Id
    private String id;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeType type;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "position_x")
    private Integer positionX = 0;

    @Column(name = "position_y")
    private Integer positionY = 0;

    @Column(columnDefinition = "TEXT")
    private String config; // JSON格式配置

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public WorkflowNode() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public enum NodeType {
        START,
        FINISH,
        AGENT_EXECUTION,
        API_CALL,
        CONDITION,
        PARALLEL,
        LOOP,
        WAIT,
        SUBWORKFLOW,
        HUMAN_REVIEW,
        SMART_DECOMPOSE;

        @JsonValue
        public String toLowerCase() {
            return name().toLowerCase();
        }

        @JsonCreator
        public static NodeType fromString(String value) {
            if (value == null) return AGENT_EXECUTION;
            return NodeType.valueOf(value.toUpperCase());
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public NodeType getType() { return type; }
    public void setType(NodeType type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPositionX() { return positionX; }
    public void setPositionX(Integer positionX) { this.positionX = positionX; }

    public Integer getPositionY() { return positionY; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }

    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}