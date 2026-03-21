package com.openclaw.workflow.engine.smartdecompose.v2.extension;

import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;

import java.util.List;

/**
 * 任务拆分拦截器
 *
 * 允许在任务拆分前后执行自定义逻辑。
 */
public interface TaskSplitInterceptor {

    /**
     * 拆分前调用
     *
     * @param parentTask 父任务
     * @param subTasks   子任务列表（可修改）
     * @return false 可阻止拆分
     */
    default boolean beforeSplit(SubTask parentTask, List<SubTask> subTasks) {
        return true;
    }

    /**
     * 拆分后调用
     *
     * @param parentTask 父任务
     * @param subTasks   子任务列表
     */
    default void afterSplit(SubTask parentTask, List<SubTask> subTasks) {
    }

    /**
     * 获取拦截器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}