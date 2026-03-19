package com.openclaw.workflow.dto;

import com.openclaw.workflow.entity.ReviewRecord.ReviewDecision;

/**
 * 审核响应请求
 */
public class ReviewResponseRequest {

    private String token;
    private String action; // approve, reject
    private String comment;
    private String reviewer;
    private ReviewDecision decision; // 兼容旧字段

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }

    public ReviewDecision getDecision() { return decision; }
    public void setDecision(ReviewDecision decision) { this.decision = decision; }

    /**
     * 获取决策（兼容action和decision字段）
     */
    public ReviewDecision getDecisionEnum() {
        if (decision != null) {
            return decision;
        }
        if ("approve".equalsIgnoreCase(action)) {
            return ReviewDecision.APPROVED;
        } else if ("reject".equalsIgnoreCase(action)) {
            return ReviewDecision.REJECTED;
        }
        return null;
    }
}