package com.openclaw.workflow.engine.model;

/**
 * 节点执行状态枚举
 */
public enum NodeStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    RETRY
}