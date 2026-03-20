package com.openclaw.workflow.engine.service;

/**
 * 分支信息
 */
public class BranchInfo {
    public String id;
    public String name;
    public String description;
    public String conditionDesc;
    public String targetNodeId;

    public BranchInfo(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public BranchInfo(String id, String name, String description, String conditionDesc) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.conditionDesc = conditionDesc;
    }
}