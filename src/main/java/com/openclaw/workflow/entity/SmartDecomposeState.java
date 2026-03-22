package com.openclaw.workflow.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 智能分解执行状态
 * 用于持久化ReAct循环的执行状态
 */
@Entity
@Table(name = "smart_decompose_state")
public class SmartDecomposeState {

    @Id
    private String id;

    @Column(name = "execution_id", nullable = false)
    private String executionId;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    // 任务栈（JSON序列化）
    @Column(name = "task_stack", columnDefinition = "TEXT")
    private String taskStack;

    // 已完成任务（JSON序列化）
    @Column(name = "completed_tasks", columnDefinition = "TEXT")
    private String completedTasks;

    // 任务映射（JSON序列化）
    @Column(name = "task_map", columnDefinition = "TEXT")
    private String taskMap;

    // 循环状态
    @Column(name = "current_iteration")
    private Integer currentIteration = 0;

    @Column(name = "max_iterations")
    private Integer maxIterations = 100;

    @Column(name = "current_depth")
    private Integer currentDepth = 0;

    @Column(name = "max_depth")
    private Integer maxDepth = 5;

    // 当前任务ID
    @Column(name = "current_task_id")
    private String currentTaskId;

    // 决策历史（JSON序列化）
    @Column(name = "decision_history", columnDefinition = "TEXT")
    private String decisionHistory;

    // 上下文缓存（JSON序列化）
    @Column(name = "context_cache", columnDefinition = "TEXT")
    private String contextCache;

    // 工具调用记录（JSON序列化）
    @Column(name = "tool_calls", columnDefinition = "TEXT")
    private String toolCalls;

    // 产物列表（JSON序列化）
    @Column(name = "artifacts", columnDefinition = "TEXT")
    private String artifacts;

    // 执行状态
    @Column(name = "status")
    private String status = "RUNNING";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // 失败任务（JSON序列化）
    @Column(name = "failed_tasks", columnDefinition = "TEXT")
    private String failedTasks;

    // OpenClaw 会话ID
    @Column(name = "openclaw_session_id")
    private String openClawSessionId;

    // 配置：最大重试次数
    @Column(name = "max_retries")
    private Integer maxRetries = 5;

    // 配置：是否需要人工审核
    @Column(name = "require_manual_review")
    private Boolean requireManualReview = true;

    // 当前人工审核ID
    @Column(name = "manual_review_id")
    private String manualReviewId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getTaskStack() { return taskStack; }
    public void setTaskStack(String taskStack) { this.taskStack = taskStack; }

    public String getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(String completedTasks) { this.completedTasks = completedTasks; }

    public String getTaskMap() { return taskMap; }
    public void setTaskMap(String taskMap) { this.taskMap = taskMap; }

    public Integer getCurrentIteration() { return currentIteration; }
    public void setCurrentIteration(Integer currentIteration) { this.currentIteration = currentIteration; }

    public Integer getMaxIterations() { return maxIterations; }
    public void setMaxIterations(Integer maxIterations) { this.maxIterations = maxIterations; }

    public Integer getCurrentDepth() { return currentDepth; }
    public void setCurrentDepth(Integer currentDepth) { this.currentDepth = currentDepth; }

    public Integer getMaxDepth() { return maxDepth; }
    public void setMaxDepth(Integer maxDepth) { this.maxDepth = maxDepth; }

    public String getCurrentTaskId() { return currentTaskId; }
    public void setCurrentTaskId(String currentTaskId) { this.currentTaskId = currentTaskId; }

    public String getDecisionHistory() { return decisionHistory; }
    public void setDecisionHistory(String decisionHistory) { this.decisionHistory = decisionHistory; }

    public String getContextCache() { return contextCache; }
    public void setContextCache(String contextCache) { this.contextCache = contextCache; }

    public String getToolCalls() { return toolCalls; }
    public void setToolCalls(String toolCalls) { this.toolCalls = toolCalls; }

    public String getArtifacts() { return artifacts; }
    public void setArtifacts(String artifacts) { this.artifacts = artifacts; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getFailedTasks() { return failedTasks; }
    public void setFailedTasks(String failedTasks) { this.failedTasks = failedTasks; }

    public String getOpenClawSessionId() { return openClawSessionId; }
    public void setOpenClawSessionId(String openClawSessionId) { this.openClawSessionId = openClawSessionId; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public Boolean getRequireManualReview() { return requireManualReview; }
    public void setRequireManualReview(Boolean requireManualReview) { this.requireManualReview = requireManualReview; }

    public String getManualReviewId() { return manualReviewId; }
    public void setManualReviewId(String manualReviewId) { this.manualReviewId = manualReviewId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}