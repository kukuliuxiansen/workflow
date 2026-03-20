package com.openclaw.workflow.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 子Agent执行记录
 */
@Entity
@Table(name = "subagent_execution")
public class SubAgentExecution {

    @Id
    private String id;

    @Column(name = "parent_execution_id", nullable = false)
    private String parentExecutionId;

    @Column(name = "parent_node_id", nullable = false)
    private String parentNodeId;

    @Column(name = "agent_type", nullable = false)
    private String agentType;

    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    // 执行状态
    @Column(name = "status")
    private String status = "PENDING";

    // 结果摘要
    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    // 结果输出（JSON）
    @Column(name = "result_outputs", columnDefinition = "TEXT")
    private String resultOutputs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // 耗时
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getParentExecutionId() { return parentExecutionId; }
    public void setParentExecutionId(String parentExecutionId) { this.parentExecutionId = parentExecutionId; }

    public String getParentNodeId() { return parentNodeId; }
    public void setParentNodeId(String parentNodeId) { this.parentNodeId = parentNodeId; }

    public String getAgentType() { return agentType; }
    public void setAgentType(String agentType) { this.agentType = agentType; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }

    public String getResultOutputs() { return resultOutputs; }
    public void setResultOutputs(String resultOutputs) { this.resultOutputs = resultOutputs; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}