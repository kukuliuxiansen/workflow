package com.openclaw.workflow.dto;

/**
 * 更新节点请求
 */
public class UpdateNodeRequest {

    private String name;
    private Integer positionX;
    private Integer positionY;
    private String config;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getPositionX() { return positionX; }
    public void setPositionX(Integer positionX) { this.positionX = positionX; }

    public Integer getPositionY() { return positionY; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }

    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
}