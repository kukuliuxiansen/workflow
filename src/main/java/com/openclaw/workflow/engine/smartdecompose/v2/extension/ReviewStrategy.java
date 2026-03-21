package com.openclaw.workflow.engine.smartdecompose.v2.extension;

import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;

import java.util.List;

/**
 * 审核策略接口
 *
 * 允许自定义审核逻辑。
 */
public interface ReviewStrategy {

    /**
     * 判断是否需要人工审核
     *
     * @param task       当前任务
     * @param retryCount 当前重试次数
     * @return true 表示需要人工审核
     */
    boolean requireManualReview(SubTask task, int retryCount);

    /**
     * 生成重试提示词
     *
     * @param task   当前任务
     * @param issues 发现的问题
     * @return 重试提示词
     */
    String buildRetryPrompt(SubTask task, List<String> issues);

    /**
     * 获取策略名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}