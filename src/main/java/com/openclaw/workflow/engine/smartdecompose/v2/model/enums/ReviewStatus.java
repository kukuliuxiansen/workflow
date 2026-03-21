package com.openclaw.workflow.engine.smartdecompose.v2.model.enums;

/**
 * 审核状态枚举
 *
 * 用于表示任务的审核结果
 */
public enum ReviewStatus {

    /** 等待审核 */
    PENDING,

    /** 审核通过 */
    APPROVED,

    /** 审核拒绝 */
    REJECTED
}