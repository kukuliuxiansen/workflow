package com.openclaw.workflow.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流节点实体
 */
@Entity
@Table(name = "workflow_node")
public class WorkflowNode {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    private String id;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeType type;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "position_x")
    private Integer positionX = 0;

    @Column(name = "position_y")
    private Integer positionY = 0;

    @Column(columnDefinition = "TEXT")
    private String config; // JSON格式配置

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 临时存储额外的配置属性（不存入数据库）
    @Transient
    private Map<String, Object> extraConfig = new HashMap<>();

    public WorkflowNode() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 捕获所有未映射的JSON属性，存入extraConfig
     */
    @JsonAnySetter
    public void addExtraConfig(String key, Object value) {
        // 忽略null值和已存在的实体字段
        if (value != null && !isEntityField(key)) {
            extraConfig.put(key, value);
        }
    }

    /**
     * 序列化时输出extraConfig中的属性
     */
    @JsonAnyGetter
    public Map<String, Object> getExtraConfig() {
        // 从config字段解析已有的配置，合并到extraConfig
        mergeConfigToExtra();
        return extraConfig;
    }

    /**
     * 判断是否是实体字段
     */
    private boolean isEntityField(String key) {
        return key.equals("id") || key.equals("workflowId") || key.equals("workflow_id") ||
               key.equals("type") || key.equals("name") || key.equals("description") ||
               key.equals("positionX") || key.equals("position_x") ||
               key.equals("positionY") || key.equals("position_y") ||
               key.equals("config") || key.equals("createdAt") || key.equals("created_at") ||
               key.equals("updatedAt") || key.equals("updated_at");
    }

    /**
     * 从config字段解析配置到extraConfig
     */
    @SuppressWarnings("unchecked")
    private void mergeConfigToExtra() {
        if (config != null && !config.isEmpty()) {
            try {
                Map<String, Object> configMap = objectMapper.readValue(config, Map.class);
                // 只添加extraConfig中不存在的key，避免覆盖新设置的值
                for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                    if (!extraConfig.containsKey(entry.getKey())) {
                        extraConfig.put(entry.getKey(), entry.getValue());
                    }
                }
            } catch (Exception e) {
                // 解析失败，忽略
            }
        }
    }

    /**
     * 将extraConfig序列化到config字段（保存前调用）
     */
    @PrePersist
    @PreUpdate
    public void saveExtraConfigToDb() {
        if (!extraConfig.isEmpty()) {
            try {
                this.config = objectMapper.writeValueAsString(extraConfig);
            } catch (Exception e) {
                // 序列化失败，忽略
            }
        }
    }

    public enum NodeType {
        START,
        FINISH,
        AGENT_EXECUTION,
        API_CALL,
        CONDITION,
        PARALLEL,
        LOOP,
        WAIT,
        SUBWORKFLOW,
        HUMAN_REVIEW,
        SMART_DECOMPOSE;

        @JsonValue
        public String toLowerCase() {
            return name().toLowerCase();
        }

        @JsonCreator
        public static NodeType fromString(String value) {
            if (value == null) return AGENT_EXECUTION;
            return NodeType.valueOf(value.toUpperCase());
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public NodeType getType() { return type; }
    public void setType(NodeType type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPositionX() { return positionX; }
    public void setPositionX(Integer positionX) { this.positionX = positionX; }

    public Integer getPositionY() { return positionY; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }

    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}