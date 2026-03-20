package com.openclaw.workflow.engine.smartdecompose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 启动子Agent动作处理器
 */
public class SpawnAgentActionHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SpawnAgentActionHandler.class);

    private final SubAgentExecutor subAgentExecutor;

    public SpawnAgentActionHandler(String gatewayUrl, String gatewayToken) {
        this.subAgentExecutor = new SubAgentExecutor(gatewayUrl, gatewayToken);
    }

    @Override
    public ActionResult handle(DecomposeContext context, AgentAction action) {
        Map<String, Object> params = action.getParameters();
        if (params == null) {
            return ActionResult.error("缺少参数");
        }

        String agentType = getStringValue(params, "agent_type", "general-purpose");
        String prompt = getStringValue(params, "prompt", "");

        logger.info("启动子Agent: type={}", agentType);

        try {
            SubAgentRequest request = new SubAgentRequest();
            request.setAgentType(SubAgentType.fromName(agentType));
            request.setPrompt(prompt);
            request.setParentExecutionId(context.getExecutionId());

            SubAgentResult result = subAgentExecutor.execute(request);

            DecomposeContext.ToolCallRecord record = new DecomposeContext.ToolCallRecord();
            record.setToolName("spawn_agent:" + agentType);
            record.setParameters(params);
            record.setResult(result);
            record.setSuccess(result.isSuccess());
            context.getToolCalls().add(record);

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

    @Override
    public DecomposeTool getSupportedTool() {
        return DecomposeTool.SPAWN_AGENT;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}