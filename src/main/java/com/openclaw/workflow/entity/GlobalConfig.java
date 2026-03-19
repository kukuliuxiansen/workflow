package com.openclaw.workflow.entity;

import javax.persistence.*;

/**
 * 全局配置实体
 */
@Entity
@Table(name = "global_config")
public class GlobalConfig {

    @Id
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;

    public GlobalConfig() {}

    public GlobalConfig(String key, String value) {
        this.key = key;
        this.value = value;
    }

    // Getters and Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}