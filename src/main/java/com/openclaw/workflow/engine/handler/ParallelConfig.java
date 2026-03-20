package com.openclaw.workflow.engine.handler;

import java.util.ArrayList;
import java.util.List;

/**
 * 并行节点配置
 */
public class ParallelConfig {
    String executionMode = "ALL";
    List<Branch> branches = new ArrayList<>();
    String mergeNodeId;
    List<String> defaultBranches;
    String customPrompt;

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    public List<Branch> getBranches() {
        return branches;
    }

    public void setBranches(List<Branch> branches) {
        this.branches = branches;
    }

    public String getMergeNodeId() {
        return mergeNodeId;
    }

    public void setMergeNodeId(String mergeNodeId) {
        this.mergeNodeId = mergeNodeId;
    }

    public List<String> getDefaultBranches() {
        return defaultBranches;
    }

    public void setDefaultBranches(List<String> defaultBranches) {
        this.defaultBranches = defaultBranches;
    }

    public String getCustomPrompt() {
        return customPrompt;
    }

    public void setCustomPrompt(String customPrompt) {
        this.customPrompt = customPrompt;
    }
}