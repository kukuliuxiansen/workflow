package com.openclaw.workflow.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 执行记录实体
 */
@Entity
@Table(name = "execution")
public class Execution {

    @Id
    private String id;

    @Column(nullable = false)
    private String workflowId;

    @Column(nullable = false)
    private String status; // pending, running, paused, completed, failed, stopped

    private String currentNodeId;

    private String previousNodeId;

    private Integer nodeRetryCount = 0;

    private Integer globalRetryCount = 0;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String taskConfig; // JSON格式

    @Column(columnDefinition = "TEXT")
    private String contextData; // JSON格式

    @Column(columnDefinition = "TEXT")
    private String contextFilePath;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }

    public String getPreviousNodeId() { return previousNodeId; }
    public void setPreviousNodeId(String previousNodeId) { this.previousNodeId = previousNodeId; }

    public Integer getNodeRetryCount() { return nodeRetryCount; }
    public void setNodeRetryCount(Integer nodeRetryCount) { this.nodeRetryCount = nodeRetryCount; }

    public Integer getGlobalRetryCount() { return globalRetryCount; }
    public void setGlobalRetryCount(Integer globalRetryCount) { this.globalRetryCount = globalRetryCount; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getTaskConfig() { return taskConfig; }
    public void setTaskConfig(String taskConfig) { this.taskConfig = taskConfig; }

    public String getContextData() { return contextData; }
    public void setContextData(String contextData) { this.contextData = contextData; }

    public String getContextFilePath() { return contextFilePath; }
    public void setContextFilePath(String contextFilePath) { this.contextFilePath = contextFilePath; }
}