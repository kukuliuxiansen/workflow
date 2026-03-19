package com.openclaw.workflow.engine.handler;

import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.entity.WorkflowNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点处理器基类
 */
public abstract class BaseNodeHandler {

    /**
     * 执行节点
     */
    public abstract NodeResult execute(NodeExecutionContext context) throws Exception;

    /**
     * 验证节点配置
     */
    public List<String> validate(WorkflowNode node) {
        return new ArrayList<>();
    }

    /**
     * 获取超时时间
     */
    protected int getTimeout(WorkflowNode node, int defaultTimeout) {
        return defaultTimeout;
    }

    /**
     * 睡眠
     */
    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}