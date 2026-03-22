package com.openclaw.workflow.engine.smartdecompose.v2.config;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SmartDecompose 节点配置
 *
 * 从节点的 config JSON 字段解析，控制执行行为。
 */
public class SmartDecomposeConfig {

    /** 场景ID，选择场景后自动加载对应配置 */
    private String sceneId;

    /** 最大重试次数，默认 5 */
    private int maxRetries = 5;

    /** 最大迭代次数，默认 50 */
    private int maxIterations = 50;

    /** 是否需要人工审核，默认 true */
    private boolean requireManualReview = true;

    /** 指定决策模板ID，为空使用默认 */
    private String decisionTemplateId;

    /** 指定审核模板ID，为空使用默认 */
    private String reviewTemplateId;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 静态工厂方法 ====================

    /**
     * 从 JSON 字符串解析配置
     *
     * @param json JSON 字符串
     * @return 配置对象，解析失败返回默认配置
     */
    public static SmartDecomposeConfig fromJson(String json) {
        // 实现思路：
        // 1. 判断 json 是否为空，空则返回默认配置
        // 2. 使用 ObjectMapper 解析 JSON
        // 3. 解析失败时返回默认配置，不抛异常
        if (json == null || json.isEmpty()) {
            return new SmartDecomposeConfig();
        }
        try {
            return objectMapper.readValue(json, SmartDecomposeConfig.class);
        } catch (Exception e) {
            return new SmartDecomposeConfig();
        }
    }

    /**
     * 序列化为 JSON
     *
     * @return JSON 字符串
     */
    public String toJson() {
        // 实现思路：使用 ObjectMapper 序列化为 JSON
        try {
            return objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            return "{}";
        }
    }

    // ==================== Getters & Setters ====================

    public String getSceneId() { return sceneId; }
    public void setSceneId(String sceneId) { this.sceneId = sceneId; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public int getMaxIterations() { return maxIterations; }
    public void setMaxIterations(int maxIterations) { this.maxIterations = maxIterations; }

    public boolean isRequireManualReview() { return requireManualReview; }
    public void setRequireManualReview(boolean requireManualReview) { this.requireManualReview = requireManualReview; }

    public String getDecisionTemplateId() { return decisionTemplateId; }
    public void setDecisionTemplateId(String decisionTemplateId) { this.decisionTemplateId = decisionTemplateId; }

    public String getReviewTemplateId() { return reviewTemplateId; }
    public void setReviewTemplateId(String reviewTemplateId) { this.reviewTemplateId = reviewTemplateId; }
}