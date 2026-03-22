package com.openclaw.workflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.handler.BaseNodeHandler;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点执行辅助类
 */
public class NodeExecutionHelper {

    private static final Logger logger = LoggerFactory.getLogger(NodeExecutionHelper.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final NodeHandlerFactory handlerFactory;
    private final ContextManager contextManager;
    private OperationLogService logService;

    public NodeExecutionHelper(NodeHandlerFactory handlerFactory, ContextManager contextManager) {
        this.handlerFactory = handlerFactory;
        this.contextManager = contextManager;
    }

    public void setLogService(OperationLogService logService) {
        this.logService = logService;
    }

    public NodeResult execute(WorkflowNode node, String workflowId, String executionId) {
        logger.info("执行节点: {} ({})", node.getName(), node.getType());

        long startTime = System.currentTimeMillis();
        Object nodeInput = null;

        try {
            NodeExecutionContext context = buildContext(node, workflowId, executionId);

            Map<String, Object> inputMap = new HashMap<>();
            inputMap.put("nodeId", node.getId());
            inputMap.put("nodeName", node.getName());
            inputMap.put("nodeType", node.getType().toString());
            inputMap.put("input", truncateForLog(context.getInput(), 500));
            inputMap.put("previousOutputs", truncateForLog(context.getPreviousOutputs(), 500));
            nodeInput = inputMap;

            BaseNodeHandler handler = handlerFactory.getHandler(node.getType());
            NodeResult result = handler.execute(context);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("节点 {} 执行完成, 状态: {}, 耗时: {}ms", node.getName(), result.getStatus(), duration);

            logSuccess(node, executionId, nodeInput, result, duration);
            contextManager.saveNodeOutput(node.getId(), result);

            return result;

        } catch (Exception e) {
            return handleException(node, executionId, nodeInput, startTime, e);
        }
    }

    private NodeExecutionContext buildContext(WorkflowNode node, String workflowId, String executionId) {
        NodeExecutionContext context = new NodeExecutionContext();
        context.setNode(node);
        context.setWorkflowId(workflowId);
        context.setExecutionId(executionId);
        context.setInput(contextManager.getInput());
        context.setPreviousOutputs(contextManager.getAllOutputs());
        context.setTaskDescription(contextManager.getTaskDescription());
        context.setProjectPath(contextManager.getProjectPath());
        context.setGlobalPrompt(contextManager.getGlobalPrompt());
        return context;
    }

    private void logSuccess(WorkflowNode node, String executionId, Object nodeInput, NodeResult result, long duration) {
        if (logService == null) return;

        Map<String, Object> outputMap = new HashMap<>();
        outputMap.put("status", result.getStatus());
        outputMap.put("output", truncateForLog(result.getOutput(), 1000));
        outputMap.put("error", result.getError() != null ? result.getError() : "");
        outputMap.put("duration", duration + "ms");
        logService.logNodeOperation(executionId, node.getId(), node.getType().toString(),
            "EXECUTE", nodeInput, outputMap);
    }

    private NodeResult handleException(WorkflowNode node, String executionId, Object nodeInput,
                                        long startTime, Exception e) {
        long duration = System.currentTimeMillis() - startTime;
        logger.error("节点 {} 执行异常: {}", node.getName(), e.getMessage(), e);

        if (logService != null) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", e.getMessage());
            errorMap.put("duration", duration + "ms");
            logService.logNodeOperation(executionId, node.getId(), node.getType().toString(),
                "ERROR", nodeInput, errorMap);
        }

        return NodeResult.failed(e.getMessage());
    }

    public Object truncateForLog(Object obj, int maxLen) {
        if (obj == null) return null;
        try {
            String json = objectMapper.writeValueAsString(obj);
            if (json.length() > maxLen) {
                return json.substring(0, maxLen) + "...(truncated)";
            }
            return obj;
        } catch (Exception e) {
            return obj.toString();
        }
    }
}