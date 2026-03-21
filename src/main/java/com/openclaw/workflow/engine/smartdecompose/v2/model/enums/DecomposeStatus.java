package com.openclaw.workflow.engine.smartdecompose.v2.model.enums;

/**
 * 执行状态枚举
 *
 * 用于表示 SmartDecompose 执行的整体状态
 */
public enum DecomposeStatus {

    /** 执行中 */
    RUNNING,

    /** 全部完成 */
    COMPLETED,

    /** 执行失败（不可恢复） */
    FAILED,

    /** 等待人工审核 */
    WAITING_MANUAL_REVIEW,

    /** 超过最大迭代次数 */
    ITERATION_EXCEEDED
}