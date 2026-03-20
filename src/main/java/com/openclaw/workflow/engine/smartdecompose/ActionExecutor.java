package com.openclaw.workflow.engine.smartdecompose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 动作执行器
 */
public class ActionExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ActionExecutor.class);

    private final Map<DecomposeTool, ActionHandler> handlers = new HashMap<>();

    public ActionExecutor(String gatewayUrl, String gatewayToken) {
        handlers.put(DecomposeTool.DECOMPOSE, new DecomposeActionHandler());
        handlers.put(DecomposeTool.SPAWN_AGENT, new SpawnAgentActionHandler(gatewayUrl, gatewayToken));
        handlers.put(DecomposeTool.MARK_COMPLETE, new CompleteActionHandler());
        handlers.put(DecomposeTool.MARK_FAILED, new FailedActionHandler());
        handlers.put(DecomposeTool.READ_CONTEXT, new ReadContextActionHandler());
        handlers.put(DecomposeTool.WRITE_ARTIFACT, new WriteArtifactActionHandler());
        handlers.put(DecomposeTool.RUN_COMMAND, new RunCommandActionHandler());
    }

    public ActionResult execute(DecomposeContext context, AgentAction action) {
        if (action == null || action.getTool() == null) {
            return ActionResult.error("无效的动作");
        }

        logger.info("执行动作: {}", action.getTool().getName());

        try {
            ActionHandler handler = handlers.get(action.getTool());
            if (handler != null) {
                return handler.handle(context, action);
            }

            // 处理没有专门处理器的情况
            switch (action.getTool()) {
                case EXECUTE:
                    return executeDirect(context, action);
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

    private ActionResult executeDirect(DecomposeContext context, AgentAction action) {
        Map<String, Object> params = action.getParameters();
        String actionType = getStringValue(params, "action_type", "default");
        logger.info("执行原子任务: {}", actionType);
        return ActionResult.success("执行完成: " + actionType, null, true);
    }

    private ActionResult executeContinue(DecomposeContext context, AgentAction action) {
        String thought = action.getThought();
        if (thought != null && !thought.isEmpty()) {
            context.getOutputBuffer().append(thought).append("\n");
        }
        return ActionResult.success("继续执行", null, true);
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}