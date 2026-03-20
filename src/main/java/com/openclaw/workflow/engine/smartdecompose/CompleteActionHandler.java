package com.openclaw.workflow.engine.smartdecompose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

/**
 * 标记完成动作处理器
 */
public class CompleteActionHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CompleteActionHandler.class);

    @Override
    public ActionResult handle(DecomposeContext context, AgentAction action) {
        TaskState currentTask = context.getCurrentTask();
        if (currentTask == null) {
            return ActionResult.error("没有当前任务");
        }

        Map<String, Object> params = action.getParameters();
        Object result = params != null ? params.get("result") : null;
        String summary = params != null ? getStringValue(params, "summary", "") : "";

        currentTask.setStatus(TaskState.TaskStatus.COMPLETED);
        currentTask.setResult(result);
        currentTask.setCompleteTime(new Date());

        context.getCompletedTasks().add(currentTask);
        logger.info("任务完成: {} -> {}", currentTask.getDescription(), summary);

        context.setCurrentTask(null);

        if (currentTask.getParent() != null) {
            TaskState parent = currentTask.getParent();
            boolean allSubtasksComplete = parent.getSubtasks().stream()
                .allMatch(t -> t.getStatus() == TaskState.TaskStatus.COMPLETED);

            if (allSubtasksComplete) {
                parent.setStatus(TaskState.TaskStatus.COMPLETED);
                parent.setResult("所有子任务已完成");
                parent.setCompleteTime(new Date());
                context.getCompletedTasks().add(parent);
                logger.info("父任务也完成: {}", parent.getDescription());
            }
        }

        return ActionResult.success(
            "任务完成: " + currentTask.getDescription(),
            result,
            true
        );
    }

    @Override
    public DecomposeTool getSupportedTool() {
        return DecomposeTool.MARK_COMPLETE;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}