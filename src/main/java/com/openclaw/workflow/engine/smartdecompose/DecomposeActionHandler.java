package com.openclaw.workflow.engine.smartdecompose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分解动作处理器
 */
public class DecomposeActionHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DecomposeActionHandler.class);

    @Override
    public ActionResult handle(DecomposeContext context, AgentAction action) {
        TaskState currentTask = context.getCurrentTask();
        if (currentTask == null) {
            return ActionResult.error("没有当前任务可分解");
        }

        if (currentTask.getDepth() >= context.getMaxDepth()) {
            logger.warn("已达到最大递归深度: {} >= {}", currentTask.getDepth(), context.getMaxDepth());
            return ActionResult.error("已达到最大递归深度，无法继续分解");
        }

        Map<String, Object> params = action.getParameters();
        if (params == null || !params.containsKey("subtasks")) {
            return ActionResult.error("缺少subtasks参数");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subtaskDefs = (List<Map<String, Object>>) params.get("subtasks");

        if (subtaskDefs == null || subtaskDefs.isEmpty()) {
            return ActionResult.error("子任务列表为空");
        }

        List<TaskState> subtaskStates = new ArrayList<>();
        int taskIndex = 1;

        for (Map<String, Object> def : subtaskDefs) {
            String taskId = generateTaskId(context, def, taskIndex);
            String description = getStringValue(def, "description", "子任务" + taskIndex);
            int priority = getIntValue(def, "priority", 5);
            List<String> dependencies = getDependencies(def);

            TaskState subtask = TaskState.builder()
                .taskId(taskId)
                .description(description)
                .status(TaskState.TaskStatus.PENDING)
                .depth(currentTask.getDepth() + 1)
                .priority(priority)
                .dependencies(dependencies)
                .parent(currentTask)
                .build();

            subtaskStates.add(subtask);
            context.getTaskMap().put(taskId, subtask);
            logger.info("创建子任务: {} (depth={}, priority={})", description, subtask.getDepth(), priority);

            taskIndex++;
        }

        currentTask.setStatus(TaskState.TaskStatus.DECOMPOSED);
        currentTask.setSubtasks(subtaskStates);

        List<TaskState> sortedSubtasks = subtaskStates.stream()
            .sorted(Comparator.comparingInt(TaskState::getPriority))
            .collect(Collectors.toList());

        for (int i = sortedSubtasks.size() - 1; i >= 0; i--) {
            context.getTaskStack().push(sortedSubtasks.get(i));
        }

        String message = String.format("任务已分解为 %d 个子任务", subtaskStates.size());
        logger.info(message);

        return ActionResult.success(message, subtaskStates.size(), true);
    }

    @Override
    public DecomposeTool getSupportedTool() {
        return DecomposeTool.DECOMPOSE;
    }

    private String generateTaskId(DecomposeContext context, Map<String, Object> def, int index) {
        String id = getStringValue(def, "id", null);
        if (id != null) return id;
        return "task_" + context.getExecutionId() + "_" + System.currentTimeMillis() + "_" + index;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private List<String> getDependencies(Map<String, Object> def) {
        Object deps = def.get("dependencies");
        if (deps instanceof List) {
            return ((List<?>) deps).stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}