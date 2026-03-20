package com.openclaw.workflow.engine.handler;

import java.util.List;

/**
 * 人工审核节点配置
 */
public class HumanReviewConfig {

    /**
     * 审批人列表
     */
    private List<String> approvers;

    /**
     * 审批类型: ANY_ONE, ALL, MAJORITY
     */
    private String approvalType = "ANY_ONE";

    /**
     * 超时时间（秒）
     */
    private int timeoutSeconds = 86400; // 24小时

    /**
     * 超时动作: REJECT, IGNORE
     */
    private String timeoutAction = "REJECT";

    /**
     * 审批消息
     */
    private String message;

    /**
     * 通过时跳转的节点ID
     */
    private String onApprove;

    /**
     * 拒绝时跳转的节点ID
     */
    private String onReject;

    /**
     * 超时时跳转的节点ID
     */
    private String onTimeout;

    // Getters and Setters

    public List<String> getApprovers() {
        return approvers;
    }

    public void setApprovers(List<String> approvers) {
        this.approvers = approvers;
    }

    public String getApprovalType() {
        return approvalType;
    }

    public void setApprovalType(String approvalType) {
        this.approvalType = approvalType;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getTimeoutAction() {
        return timeoutAction;
    }

    public void setTimeoutAction(String timeoutAction) {
        this.timeoutAction = timeoutAction;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOnApprove() {
        return onApprove;
    }

    public void setOnApprove(String onApprove) {
        this.onApprove = onApprove;
    }

    public String getOnReject() {
        return onReject;
    }

    public void setOnReject(String onReject) {
        this.onReject = onReject;
    }

    public String getOnTimeout() {
        return onTimeout;
    }

    public void setOnTimeout(String onTimeout) {
        this.onTimeout = onTimeout;
    }
}