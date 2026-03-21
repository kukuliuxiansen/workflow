package com.openclaw.workflow.engine.smartdecompose.v2.extension;

import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 最大深度拦截器
 *
 * 限制任务拆分的最大深度，防止无限拆分。
 */
@Component
public class MaxDepthInterceptor implements TaskSplitInterceptor {

    /** 默认最大深度 */
    private static final int DEFAULT_MAX_DEPTH = 5;

    private int maxDepth = DEFAULT_MAX_DEPTH;

    @Override
    public boolean beforeSplit(SubTask parentTask, List<SubTask> subTasks) {
        if (parentTask.getDepth() >= maxDepth) {
            throw new IllegalStateException(
                "超过最大拆分深度: " + maxDepth + ", 当前深度: " + parentTask.getDepth()
            );
        }
        return true;
    }

    @Override
    public void afterSplit(SubTask parentTask, List<SubTask> subTasks) {
        // 无需处理
    }

    @Override
    public String getName() {
        return "MaxDepthInterceptor";
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }
}