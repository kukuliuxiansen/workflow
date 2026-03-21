package com.openclaw.workflow.dto;

/**
 * 人工审核请求
 */
public class ManualReviewRequest {

    /** 操作：approve 或 reject */
    private String action;

    /** 审核意见 */
    private String comment;

    /** 审核人 */
    private String reviewer;

    // Getters & Setters
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }

    public boolean isApprove() {
        return "approve".equalsIgnoreCase(action);
    }

    public boolean isReject() {
        return "reject".equalsIgnoreCase(action);
    }
}