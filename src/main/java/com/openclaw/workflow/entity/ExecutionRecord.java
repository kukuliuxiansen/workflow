package com.openclaw.workflow.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 执行记录实体
 */
@Entity
@Table(name = "execution_record")
public class ExecutionRecord {

    @Id
    private String id;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Enumerated(EnumType.STRING)
    private ExecutionStatus status = ExecutionStatus.PENDING;

    private Integer progress = 0;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData; // JSON格式

    @Column(name = "output_data", columnDefinition = "TEXT")
    private String outputData; // JSON格式

    @Column(name = "error_info", columnDefinition = "TEXT")
    private String errorInfo; // JSON格式

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_mode")
    private TriggerMode triggerMode = TriggerMode.MANUAL;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "current_node_id")
    private String currentNodeId;

    @Column(name = "context_file_path")
    private String contextFilePath;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ExecutionRecord() {
        this.createdAt = LocalDateTime.now();
    }

    public enum ExecutionStatus {
        PENDING,
        RUNNING,
        PAUSED,
        STOPPED,
        COMPLETED,
        FAILED,
        WAITING_RETRY
    }

    public enum TriggerMode {
        MANUAL,
        SCHEDULED
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public String getInputData() { return inputData; }
    public void setInputData(String inputData) { this.inputData = inputData; }

    public String getOutputData() { return outputData; }
    public void setOutputData(String outputData) { this.outputData = outputData; }

    public String getErrorInfo() { return errorInfo; }
    public void setErrorInfo(String errorInfo) { this.errorInfo = errorInfo; }

    public TriggerMode getTriggerMode() { return triggerMode; }
    public void setTriggerMode(TriggerMode triggerMode) { this.triggerMode = triggerMode; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String currentNodeId) { this.currentNodeId = currentNodeId; }

    public String getContextFilePath() { return contextFilePath; }
    public void setContextFilePath(String contextFilePath) { this.contextFilePath = contextFilePath; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}