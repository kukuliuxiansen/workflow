package com.openclaw.workflow.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 任务配置实体
 */
@Entity
@Table(name = "task_config")
public class TaskConfig {

    @Id
    private String id;

    @Column(name = "execution_id", unique = true, nullable = false)
    @JsonProperty("executionId")
    private String executionId;

    @Column(name = "workflow_id")
    @JsonProperty("workflowId")
    private String workflowId;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_path")
    @JsonProperty("projectPath")
    private String projectPath;

    @Column(name = "workflow_path")
    @JsonProperty("workflowPath")
    private String workflowPath;

    @Column(name = "global_prompt", columnDefinition = "TEXT")
    @JsonProperty("globalPrompt")
    private String globalPrompt;

    @Column(name = "created_at")
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    public TaskConfig() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProjectPath() { return projectPath; }
    public void setProjectPath(String projectPath) { this.projectPath = projectPath; }

    public String getWorkflowPath() { return workflowPath; }
    public void setWorkflowPath(String workflowPath) { this.workflowPath = workflowPath; }

    public String getGlobalPrompt() { return globalPrompt; }
    public void setGlobalPrompt(String globalPrompt) { this.globalPrompt = globalPrompt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}