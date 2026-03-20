package com.openclaw.workflow.engine.smartdecompose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 运行命令动作处理器
 */
public class RunCommandActionHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RunCommandActionHandler.class);

    @Override
    public ActionResult handle(DecomposeContext context, AgentAction action) {
        Map<String, Object> params = action.getParameters();
        if (params == null) {
            return ActionResult.error("缺少参数");
        }

        String command = getStringValue(params, "command", "");
        if (command.isEmpty()) {
            return ActionResult.error("缺少command参数");
        }

        logger.info("执行命令: {}", command);

        // 实际命令执行逻辑
        // 这里只是模拟
        try {
            // Process process = Runtime.getRuntime().exec(command);
            return ActionResult.success("命令执行完成", command, true);
        } catch (Exception e) {
            logger.error("命令执行失败: {}", e.getMessage());
            return ActionResult.error("命令执行失败: " + e.getMessage());
        }
    }

    @Override
    public DecomposeTool getSupportedTool() {
        return DecomposeTool.RUN_COMMAND;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}