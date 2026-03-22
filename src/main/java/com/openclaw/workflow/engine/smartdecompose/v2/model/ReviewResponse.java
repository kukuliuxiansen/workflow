package com.openclaw.workflow.engine.smartdecompose.v2.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 审核响应模型
 *
 * 解析 OpenClaw 返回的审核 JSON 响应。
 * status 可能是 "APPROVED"（通过）或 "REJECTED"（拒绝）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewResponse {

    /** 审核状态：APPROVED 或 REJECTED */
    private String status;

    /** 审核分析过程 */
    private String thought;

    /** 完成总结（status=APPROVED 时） */
    private String summary;

    /** 问题列表（status=REJECTED 时） */
    private List<String> issues;

    /** 修改建议（status=REJECTED 时） */
    private String suggestion;

    // ==================== 业务方法 ====================

    /**
     * 判断是否审核通过
     *
     * @return true 表示审核通过
     */
    public boolean isApproved() {
        // 实现思路：判断 status 是否等于 "APPROVED"
        return "APPROVED".equals(status);
    }

    /**
     * 判断是否审核拒绝
     *
     * @return true 表示审核拒绝
     */
    public boolean isRejected() {
        // 实现思路：判断 status 是否等于 "REJECTED"
        return "REJECTED".equals(status);
    }

    // ==================== Getters & Setters ====================

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getThought() { return thought; }
    public void setThought(String thought) { this.thought = thought; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getIssues() { return issues; }
    public void setIssues(List<String> issues) { this.issues = issues; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
}