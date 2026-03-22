package com.openclaw.workflow.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * SmartDecompose 场景配置
 *
 * 场景是一套完整的配置集合，包括模板选择和参数设置。
 * 用户在节点配置时只需选择场景，即可快速定制行为。
 */
@Entity
@Table(name = "smart_decompose_scene")
public class SmartDecomposeScene {

    /** 场景ID */
    @Id
    private String id;

    /** 场景名称 */
    @Column(nullable = false)
    private String name;

    /** 场景描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    // ==================== 模板配置 ====================

    /** 决策模板ID */
    @Column(name = "decision_template_id")
    private String decisionTemplateId;

    /** 审核模板ID */
    @Column(name = "review_template_id")
    private String reviewTemplateId;

    /** 重试模板ID */
    @Column(name = "retry_template_id")
    private String retryTemplateId;

    // ==================== 参数配置 ====================

    /** 决策阈值（分钟），任务预估超过此值则拆分 */
    @Column(name = "decision_threshold_minutes")
    private Integer decisionThresholdMinutes;

    /** execute 输出格式模板 */
    @Column(name = "output_format_execute", columnDefinition = "TEXT")
    private String outputFormatExecute;

    /** split 输出格式模板 */
    @Column(name = "output_format_split", columnDefinition = "TEXT")
    private String outputFormatSplit;

    // ==================== 元数据 ====================

    /** 是否默认场景 */
    @Column(name = "is_default")
    private Boolean isDefault;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== 构造方法 ====================

    public SmartDecomposeScene() {
        this.isDefault = false;
        this.decisionThresholdMinutes = 8;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== Getters & Setters ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDecisionTemplateId() { return decisionTemplateId; }
    public void setDecisionTemplateId(String decisionTemplateId) { this.decisionTemplateId = decisionTemplateId; }

    public String getReviewTemplateId() { return reviewTemplateId; }
    public void setReviewTemplateId(String reviewTemplateId) { this.reviewTemplateId = reviewTemplateId; }

    public String getRetryTemplateId() { return retryTemplateId; }
    public void setRetryTemplateId(String retryTemplateId) { this.retryTemplateId = retryTemplateId; }

    public Integer getDecisionThresholdMinutes() { return decisionThresholdMinutes; }
    public void setDecisionThresholdMinutes(Integer decisionThresholdMinutes) { this.decisionThresholdMinutes = decisionThresholdMinutes; }

    public String getOutputFormatExecute() { return outputFormatExecute; }
    public void setOutputFormatExecute(String outputFormatExecute) { this.outputFormatExecute = outputFormatExecute; }

    public String getOutputFormatSplit() { return outputFormatSplit; }
    public void setOutputFormatSplit(String outputFormatSplit) { this.outputFormatSplit = outputFormatSplit; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}