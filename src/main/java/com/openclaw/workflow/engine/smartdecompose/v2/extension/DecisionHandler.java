package com.openclaw.workflow.engine.smartdecompose.v2.extension;

import com.openclaw.workflow.engine.smartdecompose.v2.model.DecomposeContext;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecisionResponse;
import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;

/**
 * 决策处理器接口
 *
 * 允许自定义决策处理逻辑。
 */
public interface DecisionHandler {

    /**
     * 处理决策
     *
     * @param context  执行上下文
     * @param task     当前任务
     * @param decision 决策响应
     * @return true 表示已处理，false 表示使用默认处理
     */
    boolean handle(DecomposeContext context, SubTask task, DecisionResponse decision);

    /**
     * 获取处理器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}