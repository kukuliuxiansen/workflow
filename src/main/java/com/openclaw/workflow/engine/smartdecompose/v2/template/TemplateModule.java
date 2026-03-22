package com.openclaw.workflow.engine.smartdecompose.v2.template;

/**
 * 模板模块DTO
 *
 * 用于前后端传输模块数据
 */
public class TemplateModule {

    /** 模块唯一标识 */
    private String id;

    /** 模块类型：fixed（固定）或 custom（自定义） */
    private String type;

    /** 模块标题 */
    private String title;

    /** 模块内容 */
    private String content;

    /** 排序序号 */
    private Integer order;

    /** 是否启用 */
    private Boolean enabled;

    /** 是否必须（必须模块不可删除禁用） */
    private Boolean required;

    public TemplateModule() {}

    public TemplateModule(String id, String type, String title, String content, Integer order, Boolean enabled, Boolean required) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.order = order;
        this.enabled = enabled;
        this.required = required;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
}