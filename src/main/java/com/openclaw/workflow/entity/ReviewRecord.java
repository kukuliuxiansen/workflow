package com.openclaw.workflow.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 审核记录实体
 */
@Entity
@Table(name = "review_record")
public class ReviewRecord {

    @Id
    private String id;

    @Column(name = "execution_id", nullable = false)
    private String executionId;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Enumerated(EnumType.STRING)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "reviewer_agent_id")
    private String reviewerAgentId;

    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 3600;

    @Column(name = "response_time")
    private LocalDateTime responseTime;

    @Enumerated(EnumType.STRING)
    private ReviewDecision decision;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ReviewRecord() {
        this.createdAt = LocalDateTime.now();
    }

    public enum ReviewStatus {
        PENDING,
        APPROVED,
        REJECTED,
        TIMEOUT
    }

    public enum ReviewDecision {
        APPROVE,
        REJECT
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public ReviewStatus getStatus() { return status; }
    public void setStatus(ReviewStatus status) { this.status = status; }

    public String getReviewerAgentId() { return reviewerAgentId; }
    public void setReviewerAgentId(String reviewerAgentId) { this.reviewerAgentId = reviewerAgentId; }

    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }

    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public LocalDateTime getResponseTime() { return responseTime; }
    public void setResponseTime(LocalDateTime responseTime) { this.responseTime = responseTime; }

    public ReviewDecision getDecision() { return decision; }
    public void setDecision(ReviewDecision decision) { this.decision = decision; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}