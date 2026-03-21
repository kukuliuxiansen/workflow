package com.openclaw.workflow.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 提示词模板实体
 *
 * 存储 SmartDecompose 使用的决策、审核、重试提示词模板。
 * 模板使用 {{variable}} 占位符，支持条件块和循环。
 */
@Entity
@Table(name = "prompt_template")
public class PromptTemplate {

    @Id
    private String id;

    /** 模板类型: decision / review / retry */
    @Column(nullable = false)
    private String type;

    /** 模板名称 */
    @Column(nullable = false)
    private String name;

    /** 模板内容（带占位符） */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 变量定义 JSON */
    @Column(columnDefinition = "TEXT")
    private String variables;

    /** 是否默认模板 */
    @Column(name = "is_default")
    private boolean isDefault;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== 构造方法 ====================

    public PromptTemplate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建决策模板
     */
    public static PromptTemplate decisionTemplate(String id, String name, String content) {
        PromptTemplate template = new PromptTemplate();
        template.setId(id);
        template.setType("decision");
        template.setName(name);
        template.setContent(content);
        template.setDefault(true);
        return template;
    }

    /**
     * 创建审核模板
     */
    public static PromptTemplate reviewTemplate(String id, String name, String content) {
        PromptTemplate template = new PromptTemplate();
        template.setId(id);
        template.setType("review");
        template.setName(name);
        template.setContent(content);
        template.setDefault(true);
        return template;
    }

    /**
     * 创建重试模板
     */
    public static PromptTemplate retryTemplate(String id, String name, String content) {
        PromptTemplate template = new PromptTemplate();
        template.setId(id);
        template.setType("retry");
        template.setName(name);
        template.setContent(content);
        template.setDefault(true);
        return template;
    }

    // ==================== Getters & Setters ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getVariables() { return variables; }
    public void setVariables(String variables) { this.variables = variables; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}