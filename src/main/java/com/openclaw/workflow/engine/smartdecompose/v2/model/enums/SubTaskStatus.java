package com.openclaw.workflow.engine.smartdecompose.v2.model.enums;

/**
 * 子任务状态枚举
 *
 * 用于表示单个子任务的执行状态
 */
public enum SubTaskStatus {

    /** 等待执行 */
    PENDING,

    /** 执行中 */
    RUNNING,

    /** 已完成 */
    COMPLETED,

    /** 失败（重试耗尽） */
    FAILED
}