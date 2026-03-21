package com.openclaw.workflow.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 工作流实体
 */
@Entity
@Table(name = "workflow")
public class Workflow {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String version;

    @Enumerated(EnumType.STRING)
    private WorkflowStatus status = WorkflowStatus.DRAFT;

    @Column(name = "folder_id")
    private String folderId;

    private Integer sortOrder = 0;

    @Column(columnDefinition = "TEXT")
    private String config; // JSON格式存储nodes、edges等

    @Column(name = "global_config", columnDefinition = "TEXT")
    private String globalConfig; // JSON格式

    @Column(name = "task_config", columnDefinition = "TEXT")
    private String taskConfig; // JSON格式存储任务配置

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getFolderId() { return folderId; }
    public void setFolderId(String folderId) { this.folderId = folderId; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public WorkflowStatus getStatus() { return status; }
    public void setStatus(WorkflowStatus status) { this.status = status; }

    public String getGlobalConfig() { return globalConfig; }
    public void setGlobalConfig(String globalConfig) { this.globalConfig = globalConfig; }

    public String getTaskConfig() { return taskConfig; }
    public void setTaskConfig(String taskConfig) { this.taskConfig = taskConfig; }

    public enum WorkflowStatus {
        DRAFT,
        PUBLISHED;

        @JsonValue
        public String toLowerCase() {
            return name().toLowerCase();
        }

        @JsonCreator
        public static WorkflowStatus fromString(String value) {
            if (value == null) return DRAFT;
            return WorkflowStatus.valueOf(value.toUpperCase());
        }
    }
}