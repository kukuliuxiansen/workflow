package com.openclaw.workflow.engine.smartdecompose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

/**
 * 标记失败动作处理器
 */
public class FailedActionHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(FailedActionHandler.class);

    @Override
    public ActionResult handle(DecomposeContext context, AgentAction action) {
        TaskState currentTask = context.getCurrentTask();
        if (currentTask == null) {
            return ActionResult.error("没有当前任务");
        }

        Map<String, Object> params = action.getParameters();
        String errorMessage = params != null ? getStringValue(params, "error", "未知错误") : "未知错误";

        currentTask.setStatus(TaskState.TaskStatus.FAILED);
        currentTask.setResult(errorMessage);
        currentTask.setCompleteTime(new Date());

        context.getFailedTasks().add(currentTask);
        logger.warn("任务失败: {} -> {}", currentTask.getDescription(), errorMessage);

        context.setCurrentTask(null);

        return ActionResult.error("任务失败: " + errorMessage);
    }

    @Override
    public DecomposeTool getSupportedTool() {
        return DecomposeTool.MARK_FAILED;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}