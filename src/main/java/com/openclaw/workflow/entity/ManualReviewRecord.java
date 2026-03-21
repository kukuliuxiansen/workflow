package com.openclaw.workflow.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 人工审核记录实体
 *
 * 当任务审核失败且重试次数超过限制时，触发人工审核。
 */
@Entity
@Table(name = "manual_review")
public class ManualReviewRecord {

    @Id
    private String id;

    /** 执行ID */
    @Column(name = "execution_id", nullable = false)
    private String executionId;

    /** 节点ID */
    @Column(name = "node_id")
    private String nodeId;

    /** 任务ID */
    @Column(name = "task_id", nullable = false)
    private String taskId;

    /** 任务描述 */
    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;

    /** 执行结果 */
    @Column(name = "execution_result", columnDefinition = "TEXT")
    private String executionResult;

    /** 审核问题 JSON */
    @Column(name = "review_issues", columnDefinition = "TEXT")
    private String reviewIssues;

    /** 审核状态 */
    @Column(nullable = false)
    private String status;

    /** 审核人 */
    private String reviewer;

    /** 审核意见 */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /** 审核时间 */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ==================== 状态常量 ====================

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    // ==================== 构造方法 ====================

    public ManualReviewRecord() {
        this.status = STATUS_WAITING;
        this.createdAt = LocalDateTime.now();
    }

    // ==================== 业务方法 ====================

    public boolean isWaiting() {
        return STATUS_WAITING.equals(status);
    }

    public boolean isApproved() {
        return STATUS_APPROVED.equals(status);
    }

    public boolean isRejected() {
        return STATUS_REJECTED.equals(status);
    }

    // ==================== Getters & Setters ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTaskDescription() { return taskDescription; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }

    public String getExecutionResult() { return executionResult; }
    public void setExecutionResult(String executionResult) { this.executionResult = executionResult; }

    public String getReviewIssues() { return reviewIssues; }
    public void setReviewIssues(String reviewIssues) { this.reviewIssues = reviewIssues; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}