package com.openclaw.workflow.dto;

import com.openclaw.workflow.entity.WorkflowNode.NodeType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 创建节点请求
 */
public class CreateNodeRequest {

    private String type;

    private String name;

    @JsonProperty("positionX")
    private Integer positionX;

    @JsonProperty("positionY")
    private Integer positionY;

    private String config;

    @JsonIgnore
    public NodeType getNodeType() {
        if (type == null) return NodeType.AGENT_EXECUTION;
        try {
            return NodeType.valueOf(type.toUpperCase());
        } catch (Exception e) {
            return NodeType.AGENT_EXECUTION;
        }
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getPositionX() { return positionX; }
    public void setPositionX(Integer positionX) { this.positionX = positionX; }

    public Integer getPositionY() { return positionY; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }

    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
}