package com.openclaw.workflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.handler.BaseNodeHandler;
import com.openclaw.workflow.engine.model.*;
import com.openclaw.workflow.entity.*;
import com.openclaw.workflow.repository.*;
import com.openclaw.workflow.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 工作流引擎主类
 */
@Component
public class WorkflowEngine {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEngine.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ContextManager contextManager;
    private final NodeHandlerFactory handlerFactory;
    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private final ExecutionRepository executionRepository;
    private OperationLogService logService;

    private final RetryManager retryManager = new RetryManager();

    private String executionId;
    private String workflowId;
    private ExecutionStatus status = ExecutionStatus.PENDING;
    private String currentNodeId;
    private String previousNodeId;

    private volatile boolean isPaused = false;
    private volatile boolean isWaitingRetry = false;
    private volatile boolean isStopped = false;

    private ExecutionOptions options = new ExecutionOptions();

    public WorkflowEngine(ContextManager contextManager,
                          NodeHandlerFactory handlerFactory,
                          WorkflowRepository workflowRepository,
                          WorkflowNodeRepository nodeRepository,
                          WorkflowEdgeRepository edgeRepository,
                          ExecutionRepository executionRepository) {
        this.contextManager = contextManager;
        this.handlerFactory = handlerFactory;
        this.workflowRepository = workflowRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.executionRepository = executionRepository;
    }

    @Autowired
    public void setLogService(OperationLogService logService) {
        this.logService = logService;
    }

    private void resetEngineState() {
        this.executionId = null;
        this.workflowId = null;
        this.status = ExecutionStatus.PENDING;
        this.currentNodeId = null;
        this.previousNodeId = null;
        this.retryManager.reset();
        this.isPaused = false;
        this.isWaitingRetry = false;
        this.isStopped = false;
    }

    public ExecutionResult execute(String workflowId, String startNodeId,
                                   Map<String, Object> input, ExecutionOptions options) {
        resetEngineState();

        if (options != null) {
            this.options = options;
        }

        this.workflowId = workflowId;
        this.executionId = generateExecutionId();
        this.status = ExecutionStatus.RUNNING;

        try {
            Workflow workflow = workflowRepository.findById(workflowId)
                    .orElseThrow(() -> new RuntimeException("工作流不存在: " + workflowId));

            Execution execution = initExecution(workflow);
            contextManager.setExecutionId(executionId);
            contextManager.setInput(input);

            String nodeId = startNodeId != null ? startNodeId : findStartNode(workflow);
            this.currentNodeId = nodeId;

            while (nodeId != null && !isStopped) {
                checkWait();

                if (isStopped) break;

                WorkflowNode node = nodeRepository.findById(nodeId)
                        .orElseThrow(() -> new RuntimeException("节点不存在: " + nodeId));

                this.previousNodeId = this.currentNodeId;
                this.currentNodeId = nodeId;
                retryManager.incrementLoopCount();

                if (retryManager.getGlobalLoopCount() > options.getMaxLoopIterations()) {
                    logger.warn("循环次数超限: {}", retryManager.getGlobalLoopCount());
                    break;
                }

                NodeResult result = executeNode(node);
                updateExecutionStatus(execution, status.name().toLowerCase());

                if (result.getNextNodeId() != null) {
                    nodeId = result.getNextNodeId();
                } else {
                    nodeId = getNextNode(node, result.getStatus());
                }

                if (nodeId == null || "finish".equals(result.getStatus())) {
                    this.status = ExecutionStatus.COMPLETED;
                    updateExecutionStatus(execution, "completed");
                    break;
                }

                if ("failed".equals(result.getStatus()) && nodeId == null) {
                    this.status = ExecutionStatus.FAILED;
                    updateExecutionStatus(execution, "failed");
                    break;
                }
            }

            if (isStopped) {
                updateExecutionStatus(execution, "stopped");
            }

            return ExecutionResult.success(executionId, contextManager.getAllOutputs());

        } catch (Exception e) {
            logger.error("工作流执行异常: {}", e.getMessage(), e);
            this.status = ExecutionStatus.FAILED;
            return ExecutionResult.failed(executionId, e.getMessage());
        }
    }

    private NodeResult executeNode(WorkflowNode node) {
        logger.info("执行节点: {} ({})", node.getName(), node.getType());

        long startTime = System.currentTimeMillis();
        Object nodeInput = null;

        try {
            NodeExecutionContext context = new NodeExecutionContext();
            context.setNode(node);
            context.setWorkflowId(workflowId);
            context.setExecutionId(executionId);
            context.setInput(contextManager.getInput());
            context.setPreviousOutputs(contextManager.getAllOutputs());
            context.setTaskDescription(contextManager.getTaskDescription());
            context.setProjectPath(contextManager.getProjectPath());
            context.setGlobalPrompt(contextManager.getGlobalPrompt());

            nodeInput = Map.of(
                "nodeId", node.getId(),
                "nodeName", node.getName(),
                "nodeType", node.getType().toString(),
                "input", truncateForLog(context.getInput(), 500),
                "previousOutputs", truncateForLog(context.getPreviousOutputs(), 500)
            );

            BaseNodeHandler handler = handlerFactory.getHandler(node.getType());
            NodeResult result = handler.execute(context);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("节点 {} 执行完成, 状态: {}, 耗时: {}ms", node.getName(), result.getStatus(), duration);

            if (logService != null) {
                Object nodeOutput = Map.of(
                    "status", result.getStatus(),
                    "output", truncateForLog(result.getOutput(), 1000),
                    "error", result.getError() != null ? result.getError() : "",
                    "duration", duration + "ms"
                );
                logService.logNodeOperation(executionId, node.getId(), node.getType().toString(),
                    "EXECUTE", nodeInput, nodeOutput);
            }

            contextManager.saveNodeOutput(node.getId(), result);

            if ("retry".equals(result.getStatus())) {
                retryManager.incrementNodeRetry(node.getId());
                retryManager.incrementGlobalRetry();

                if (!retryManager.checkAllLimits(node.getId())) {
                    status = ExecutionStatus.WAITING_RETRY;
                    isWaitingRetry = true;
                    return NodeResult.retry();
                }
            }

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("节点 {} 执行异常: {}", node.getName(), e.getMessage(), e);

            if (logService != null) {
                logService.logNodeOperation(executionId, node.getId(), node.getType().toString(),
                    "ERROR", nodeInput, Map.of("error", e.getMessage(), "duration", duration + "ms"));
            }

            return NodeResult.failed(e.getMessage());
        }
    }

    private Object truncateForLog(Object obj, int maxLen) {
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

    private String getNextNode(WorkflowNode node, String resultStatus) {
        logger.debug("getNextNode: status={}, current={}", resultStatus, node.getId());

        if ("success".equals(resultStatus) || "completed".equals(resultStatus)) {
            retryManager.incrementNodeRetry(node.getId());
            retryManager.incrementGlobalRetry();

            Optional<WorkflowEdge> edge = edgeRepository.findByWorkflowIdAndSourceNodeId(workflowId, node.getId())
                    .stream()
                    .filter(e -> e.getEdgeType() == WorkflowEdge.EdgeType.SUCCESS)
                    .findFirst();

            return edge.map(WorkflowEdge::getTargetNodeId).orElse(null);

        } else if ("failed".equals(resultStatus) || "fail".equals(resultStatus)) {
            if (!retryManager.checkAllLimits(node.getId())) {
                return null;
            }

            Optional<WorkflowEdge> edge = edgeRepository.findByWorkflowIdAndSourceNodeId(workflowId, node.getId())
                    .stream()
                    .filter(e -> e.getEdgeType() == WorkflowEdge.EdgeType.FAIL)
                    .findFirst();

            return edge.map(WorkflowEdge::getTargetNodeId).orElse(node.getId());

        } else if ("retry".equals(resultStatus)) {
            return isWaitingRetry ? null : node.getId();
        }

        return node.getId();
    }

    private void checkWait() {
        while (isPaused || isWaitingRetry) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void pause() {
        isPaused = true;
        status = ExecutionStatus.PAUSED;
        logger.info("工作流已暂停");
    }

    public void resume(boolean resetRetry) {
        if (isWaitingRetry) {
            retryManager.reset();
            isWaitingRetry = false;
        } else if (isPaused) {
            isPaused = false;
        }
        status = ExecutionStatus.RUNNING;
        logger.info("工作流已恢复");
    }

    public void stop() {
        isStopped = true;
        isPaused = false;
        isWaitingRetry = false;
        status = ExecutionStatus.STOPPED;
        logger.info("工作流已终止");
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("executionId", executionId);
        statusMap.put("status", status);
        statusMap.put("currentNodeId", currentNodeId);
        statusMap.put("previousNodeId", previousNodeId);
        statusMap.put("globalLoopCount", retryManager.getGlobalLoopCount());
        statusMap.put("nodeRetryCount", retryManager.getNodeRetryCount(currentNodeId));
        statusMap.put("globalRetryCount", retryManager.getGlobalRetryCount());
        statusMap.put("isPaused", isPaused);
        statusMap.put("isWaitingRetry", isWaitingRetry);
        return statusMap;
    }

    private String generateExecutionId() {
        return "exec-" + System.currentTimeMillis() + "-" +
                Integer.toHexString((int) (Math.random() * 0xFFFFFF));
    }

    private String findStartNode(Workflow workflow) {
        List<WorkflowNode> nodes = nodeRepository.findByWorkflowIdOrderByCreatedAtAsc(workflow.getId());
        List<WorkflowNode> startNodes = new ArrayList<>();

        for (WorkflowNode node : nodes) {
            if (node.getType() == WorkflowNode.NodeType.START) {
                startNodes.add(node);
            }
        }

        if (startNodes.isEmpty()) {
            throw new RuntimeException("工作流缺少开始节点，请添加一个开始节点");
        }
        if (startNodes.size() > 1) {
            throw new RuntimeException("工作流只能有一个开始节点，当前有 " + startNodes.size() + " 个");
        }

        return startNodes.get(0).getId();
    }

    private Execution initExecution(Workflow workflow) {
        Execution execution = new Execution();
        execution.setId(executionId);
        execution.setWorkflowId(workflow.getId());
        execution.setStatus("running");
        execution.setStartTime(LocalDateTime.now());
        execution.setCreatedAt(LocalDateTime.now());
        execution.setNodeRetryCount(0);
        execution.setGlobalRetryCount(0);
        return executionRepository.save(execution);
    }

    private void updateExecutionStatus(Execution execution, String status) {
        execution.setStatus(status);
        executionRepository.save(execution);
    }
}