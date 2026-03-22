package com.openclaw.workflow.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 智能节点模板实体
 */
@Entity
@Table(name = "template_config")
public class TemplateConfig {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "decision_template", columnDefinition = "TEXT")
    @JsonProperty("decisionTemplate")
    private String decisionTemplate;

    @Column(name = "review_template", columnDefinition = "TEXT")
    @JsonProperty("reviewTemplate")
    private String reviewTemplate;

    @Column(name = "retry_template", columnDefinition = "TEXT")
    @JsonProperty("retryTemplate")
    private String retryTemplate;

    @Column(name = "is_default")
    @JsonProperty("isDefault")
    private Boolean isDefault;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDecisionTemplate() { return decisionTemplate; }
    public void setDecisionTemplate(String decisionTemplate) { this.decisionTemplate = decisionTemplate; }

    public String getReviewTemplate() { return reviewTemplate; }
    public void setReviewTemplate(String reviewTemplate) { this.reviewTemplate = reviewTemplate; }

    public String getRetryTemplate() { return retryTemplate; }
    public void setRetryTemplate(String retryTemplate) { this.retryTemplate = retryTemplate; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}