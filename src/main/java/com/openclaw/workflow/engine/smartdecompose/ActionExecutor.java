package com.openclaw.workflow.engine.smartdecompose;

import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 动作执行器
 * 执行Agent决策的各种动作
 */
public class ActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ActionExecutor.class);

    private final String gatewayUrl;
    private final String gatewayToken;
    private final SubAgentExecutor subAgentExecutor;

    public ActionExecutor(String gatewayUrl, String gatewayToken) {
        this.gatewayUrl = gatewayUrl;
        this.gatewayToken = gatewayToken;
        this.subAgentExecutor = new SubAgentExecutor(gatewayUrl, gatewayToken);
    }

    /**
     * 执行动作
     */
    public ActionResult execute(DecomposeContext context, AgentAction action) {
        if (action == null || action.getTool() == null) {
            return ActionResult.error("无效的动作");
        }

        logger.info("执行动作: {}", action.getTool().getName());
        logger.debug("动作参数: {}", action.getParameters());

        try {
            switch (action.getTool()) {
                case DECOMPOSE:
                    return executeDecompose(context, action);
                case EXECUTE:
                    return executeDirect(context, action);
                case SPAWN_AGENT:
                    return executeSpawnAgent(context, action);
                case MARK_COMPLETE:
                    return executeMarkComplete(context, action);
                case MARK_FAILED:
                    return executeMarkFailed(context, action);
                case READ_CONTEXT:
                    return executeReadContext(context, action);
                case WRITE_ARTIFACT:
                    return executeWriteArtifact(context, action);
                case RUN_COMMAND:
                    return executeRunCommand(context, action);
                case CONTINUE:
                    return executeContinue(context, action);
                default:
                    return ActionResult.error("未知工具: " + action.getTool());
            }
        } catch (Exception e) {
            logger.error("动作执行异常: {}", e.getMessage(), e);
            return ActionResult.error("执行异常: " + e.getMessage());
        }
    }

    /**
     * 执行分解动作
     */
    private ActionResult executeDecompose(DecomposeContext context, AgentAction action) {
        TaskState currentTask = context.getCurrentTask();
        if (currentTask == null) {
            return ActionResult.error("没有当前任务可分解");
        }

        // 检查深度限制
        if (currentTask.getDepth() >= context.getMaxDepth()) {
            logger.warn("已达到最大递归深度: {} >= {}", currentTask.getDepth(), context.getMaxDepth());
            return ActionResult.error("已达到最大递归深度，无法继续分解");
        }

        // 获取子任务定义
        Map<String, Object> params = action.getParameters();
        if (params == null || !params.containsKey("subtasks")) {
            return ActionResult.error("缺少subtasks参数");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subtaskDefs = (List<Map<String, Object>>) params.get("subtasks");

        if (subtaskDefs == null || subtaskDefs.isEmpty()) {
            return ActionResult.error("子任务列表为空");
        }

        // 创建子任务状态
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
            logger.info("创建子任务: {} (depth={}, priority={}, deps={})",
                description, subtask.getDepth(), priority, dependencies);

            taskIndex++;
        }

        // 更新当前任务状态
        currentTask.setStatus(TaskState.TaskStatus.DECOMPOSED);
        currentTask.setSubtasks(subtaskStates);

        // 按优先级排序后压入栈（高优先级先执行）
        List<TaskState> sortedSubtasks = subtaskStates.stream()
            .sorted(Comparator.comparingInt(TaskState::getPriority))
            .collect(Collectors.toList());

        // 逆序压入栈，这样高优先级会先弹出
        for (int i = sortedSubtasks.size() - 1; i >= 0; i--) {
            context.getTaskStack().push(sortedSubtasks.get(i));
        }

        String message = String.format("任务已分解为 %d 个子任务", subtaskStates.size());
        logger.info(message);

        return ActionResult.success(message, subtaskStates.size(), true);
    }

    /**
     * 执行直接执行动作
     */
    private ActionResult executeDirect(DecomposeContext context, AgentAction action) {
        Map<String, Object> params = action.getParameters();
        String actionType = getStringValue(params, "action_type", "default");

        logger.info("执行原子任务: {}", actionType);

        // 这里可以扩展各种原子操作
        // 目前先返回成功
        return ActionResult.success("执行完成: " + actionType, null, true);
    }

    /**
     * 执行启动子Agent动作
     */
    private ActionResult executeSpawnAgent(DecomposeContext context, AgentAction action) {
        Map<String, Object> params = action.getParameters();
        if (params == null) {
            return ActionResult.error("缺少参数");
        }

        String agentType = getStringValue(params, "agent_type", "general-purpose");
        String prompt = getStringValue(params, "prompt", "");

        logger.info("启动子Agent: type={}, prompt={}", agentType, truncate(prompt, 100));

        try {
            // 创建子Agent请求
            SubAgentRequest request = new SubAgentRequest();
            request.setAgentType(SubAgentType.fromName(agentType));
            request.setPrompt(prompt);
            request.setParentExecutionId(context.getExecutionId());

            // 执行子Agent
            SubAgentResult result = subAgentExecutor.execute(request);

            // 记录工具调用
            DecomposeContext.ToolCallRecord record = new DecomposeContext.ToolCallRecord();
            record.setToolName("spawn_agent:" + agentType);
            record.setParameters(params);
            record.setResult(result);
            record.setSuccess(result.isSuccess());
            context.getToolCalls().add(record);

            // 将结果合并到上下文缓存
            if (result.getOutputs() != null) {
                context.getContextCache().putAll(result.getOutputs());
            }

            if (result.isSuccess()) {
                return ActionResult.success(result.getSummary(), result.getOutputs(), true);
            } else {
                return ActionResult.error("子Agent执行失败: " + result.getErrorMessage());
            }

        } catch (Exception e) {
            logger.error("子Agent执行异常: {}", e.getMessage(), e);
            return ActionResult.error("子Agent执行异常: " + e.getMessage());
        }
    }

    /**
     * 执行标记完成动作
     */
    private ActionResult executeMarkComplete(DecomposeContext context, AgentAction action) {
        TaskState currentTask = context.getCurrentTask();
        if (currentTask == null) {
            return ActionResult.error("没有当前任务");
        }

        Map<String, Object> params = action.getParameters();
        Object result = params != null ? params.get("result") : null;
        String summary = params != null ? getStringValue(params, "summary", "") : "";

        // 更新任务状态
        currentTask.setStatus(TaskState.TaskStatus.COMPLETED);
        currentTask.setResult(result);
        currentTask.setCompleteTime(new Date());

        // 添加到已完成列表
        context.getCompletedTasks().add(currentTask);

        logger.info("任务完成: {} -> {}", currentTask.getDescription(), summary);

        // 清空当前任务
        context.setCurrentTask(null);

        // 如果有父任务，检查父任务是否可以继续
        if (currentTask.getParent() != null) {
            TaskState parent = currentTask.getParent();
            boolean allSubtasksComplete = parent.getSubtasks().stream()
                .allMatch(t -> t.getStatus() == TaskState.TaskStatus.COMPLETED);

            if (allSubtasksComplete) {
                // 所有子任务完成，父任务也标记为完成
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

    /**
     * 执行标记失败动作
     */
    private ActionResult executeMarkFailed(DecomposeContext context, AgentAction action) {
        TaskState currentTask = context.getCurrentTask();
        if (currentTask == null) {
            return ActionResult.error("没有当前任务");
        }

        Map<String, Object> params = action.getParameters();
        String reason = params != null ? getStringValue(params, "reason", "未知原因") : "未知原因";

        // 更新任务状态
        currentTask.setStatus(TaskState.TaskStatus.FAILED);
        currentTask.setResult(reason);
        currentTask.setCompleteTime(new Date());

        logger.warn("任务失败: {} - {}", currentTask.getDescription(), reason);

        // 清空当前任务
        context.setCurrentTask(null);

        // 检查是否所有任务都失败了
        if (context.getTaskStack().isEmpty()) {
            context.setStatus(DecomposeContext.DecomposeStatus.FAILED);
            context.setErrorMessage(reason);
        }

        return ActionResult.error("任务失败: " + reason, reason, true);
    }

    /**
     * 执行读取上下文动作
     */
    private ActionResult executeReadContext(DecomposeContext context, AgentAction action) {
        Map<String, Object> params = action.getParameters();
        String target = params != null ? getStringValue(params, "target", "all") : "all";

        logger.info("读取上下文: target={}", target);

        Map<String, Object> data = new HashMap<>();

        switch (target) {
            case "task_stack":
            case "tasks":
                data.put("pendingTasks", context.getTaskStack().size());
                data.put("completedTasks", context.getCompletedTasks().size());
                break;

            case "cache":
                data.put("cache", context.getContextCache());
                break;

            case "decision_history":
                data.put("decisions", context.getDecisionHistory().size());
                break;

            case "all":
            default:
                data.put("pendingTasks", context.getTaskStack().size());
                data.put("completedTasks", context.getCompletedTasks().size());
                data.put("iteration", context.getIterationCount());
                data.put("maxIterations", context.getMaxIterations());
                data.put("cache", context.getContextCache());
                break;
        }

        return ActionResult.success("上下文已读取", data, true);
    }

    /**
     * 执行写入产物动作
     */
    private ActionResult executeWriteArtifact(DecomposeContext context, AgentAction action) {
        Map<String, Object> params = action.getParameters();
        if (params == null) {
            return ActionResult.error("缺少参数");
        }

        String path = getStringValue(params, "path", "");
        String content = getStringValue(params, "content", "");
        String type = getStringValue(params, "type", "file");

        if (path.isEmpty()) {
            return ActionResult.error("缺少path参数");
        }

        logger.info("写入产物: path={}, type={}", path, type);

        // 创建产物记录
        DecomposeContext.Artifact artifact = new DecomposeContext.Artifact();
        artifact.setType(type);
        artifact.setPath(path);
        artifact.setContent(content.length() > 1000 ? content.substring(0, 1000) + "..." : content);
        artifact.setCreateTime(new Date());

        context.getArtifacts().add(artifact);

        return ActionResult.success("产物已记录: " + path, path, true);
    }

    /**
     * 执行命令动作
     */
    private ActionResult executeRunCommand(DecomposeContext context, AgentAction action) {
        Map<String, Object> params = action.getParameters();
        if (params == null) {
            return ActionResult.error("缺少参数");
        }

        String command = getStringValue(params, "command", "");

        if (command.isEmpty()) {
            return ActionResult.error("缺少command参数");
        }

        logger.info("执行命令: {}", command);

        // 记录工具调用
        DecomposeContext.ToolCallRecord record = new DecomposeContext.ToolCallRecord();
        record.setToolName("run_command");
        record.setParameters(params);
        record.setSuccess(true);
        context.getToolCalls().add(record);

        // 这里可以扩展实际执行命令的逻辑
        // 目前只记录

        return ActionResult.success("命令已记录: " + command, null, true);
    }

    /**
     * 执行继续动作
     */
    private ActionResult executeContinue(DecomposeContext context, AgentAction action) {
        logger.info("继续执行下一轮迭代");
        return ActionResult.success("继续执行", null, true);
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成任务ID
     */
    private String generateTaskId(DecomposeContext context, Map<String, Object> def, int index) {
        // 尝试从定义中获取ID
        if (def.containsKey("id")) {
            return String.valueOf(def.get("id"));
        }

        // 生成默认ID
        return "task_" + context.getExecutionId() + "_" + System.currentTimeMillis() + "_" + index;
    }

    /**
     * 获取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null || !map.containsKey(key)) {
            return defaultValue;
        }
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : defaultValue;
    }

    /**
     * 获取整数值
     */
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        if (map == null || !map.containsKey(key)) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取依赖列表
     */
    @SuppressWarnings("unchecked")
    private List<String> getDependencies(Map<String, Object> def) {
        if (!def.containsKey("dependencies")) {
            return Collections.emptyList();
        }

        Object deps = def.get("dependencies");
        if (deps instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object dep : (List<?>) deps) {
                if (dep != null) {
                    result.add(String.valueOf(dep));
                }
            }
            return result;
        }

        return Collections.emptyList();
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}