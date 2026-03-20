package com.openclaw.workflow.engine.smartdecompose;

import java.util.Map;

/**
 * 读取上下文动作处理器
 */
public class ReadContextActionHandler implements ActionHandler {

    @Override
    public ActionResult handle(DecomposeContext context, AgentAction action) {
        Map<String, Object> params = action.getParameters();
        String key = params != null ? getStringValue(params, "key", null) : null;

        if (key != null) {
            Object value = context.getContextCache().get(key);
            if (value != null) {
                return ActionResult.success("读取: " + key, value, true);
            }
            return ActionResult.error("未找到键: " + key);
        }

        // 返回所有上下文
        return ActionResult.success("当前上下文", context.getContextCache(), true);
    }

    @Override
    public DecomposeTool getSupportedTool() {
        return DecomposeTool.READ_CONTEXT;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}