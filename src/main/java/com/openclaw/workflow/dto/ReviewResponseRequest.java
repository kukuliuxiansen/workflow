package com.openclaw.workflow.dto;

import com.openclaw.workflow.entity.ReviewRecord.ReviewDecision;

/**
 * 审核响应请求
 */
public class ReviewResponseRequest {

    private ReviewDecision decision;
    private String comment;

    public ReviewDecision getDecision() { return decision; }
    public void setDecision(ReviewDecision decision) { this.decision = decision; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}