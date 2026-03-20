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

    // 兼容snake_case
    @JsonProperty("position_x")
    private Integer positionXSnake;

    @JsonProperty("position_y")
    private Integer positionYSnake;

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

    @JsonIgnore
    public Integer getPositionX() {
        return positionX != null ? positionX : positionXSnake;
    }

    @JsonIgnore
    public Integer getPositionY() {
        return positionY != null ? positionY : positionYSnake;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public void setPositionX(Integer positionX) { this.positionX = positionX; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }

    public void setPosition_x(Integer positionXSnake) { this.positionXSnake = positionXSnake; }
    public void setPosition_y(Integer positionYSnake) { this.positionYSnake = positionYSnake; }

    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
}