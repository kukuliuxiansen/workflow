package com.openclaw.workflow.engine.smartdecompose;

/**
 * 动作处理器接口
 */
public interface ActionHandler {
    /**
     * 处理动作
     */
    ActionResult handle(DecomposeContext context, AgentAction action);

    /**
     * 获取支持的工具类型
     */
    DecomposeTool getSupportedTool();
}