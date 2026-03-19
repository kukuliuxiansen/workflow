package com.openclaw.workflow.engine.model;

/**
 * 执行状态枚举
 */
public enum ExecutionStatus {
    PENDING,
    RUNNING,
    PAUSED,
    STOPPED,
    COMPLETED,
    FAILED,
    WAITING_RETRY
}