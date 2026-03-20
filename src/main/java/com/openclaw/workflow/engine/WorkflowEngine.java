package com.openclaw.workflow.engine;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.concurrent.ConcurrentHashMap;

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

    private String executionId;
    private String workflowId;
    private ExecutionStatus status = ExecutionStatus.PENDING;
    private String currentNodeId;
    private String previousNodeId;

    private Map<String, Integer> nodeRetryCount = new ConcurrentHashMap<>();
    private int globalRetryCount = 0;
    private int globalLoopCount = 0;

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
        this.nodeRetryCount.clear();
        this.globalRetryCount = 0;
        this.globalLoopCount = 0;
        this.isPaused = false;
        this.isWaitingRetry = false;
        this.isStopped = false;
        this.options = new ExecutionOptions();
        logger.debug("引擎状态已重置");
    }

    public ExecutionResult execute(String workflowId, String startNodeId,
                                    Map<String, Object> input, ExecutionOptions options) {
        // 重置引擎状态（解决单例Bean状态复用问题）
        resetEngineState();

        this.workflowId = workflowId;
        this.options = options != null ? options : new ExecutionOptions();
        this.executionId = this.options.getExecutionId() != null ?
                this.options.getExecutionId() : generateExecutionId();

        try {
            Workflow workflow = workflowRepository.findById(workflowId)
                    .orElseThrow(() -> new RuntimeException("工作流不存在: " + workflowId));

            if (startNodeId == null) {
                startNodeId = findStartNode(workflow);
            }

            Execution execution = initExecution(workflow);

            contextManager.init(input, null);

            if (this.options.isResume()) {
                isPaused = false;
                isWaitingRetry = false;
                if (this.options.isResetRetry()) {
                    resetRetryCount();
                }
            }

            this.status = ExecutionStatus.RUNNING;
            updateExecutionStatus(execution, "running");

            String current = startNodeId;
            this.globalLoopCount = 0;

            while (current != null) {
                checkWait();

                if (isStopped || status == ExecutionStatus.STOPPED) {
                    break;
                }

                this.globalLoopCount++;
                this.currentNodeId = current;

                final String currentNodeId = current;
                WorkflowNode node = nodeRepository.findByWorkflowIdAndId(workflowId, currentNodeId)
                        .orElseThrow(() -> new RuntimeException("节点不存在: " + currentNodeId));

                NodeResult result = executeNode(node);

                execution.setCurrentNodeId(current);
                execution.setPreviousNodeId(previousNodeId);
                executionRepository.save(execution);

                if (node.getType() == WorkflowNode.NodeType.FINISH) {
                    this.status = ExecutionStatus.COMPLETED;
                    updateExecutionStatus(execution, "completed");
                    break;
                }

                String next = getNextNode(node, result.getStatus());

                if (next == null) {
                    this.status = ExecutionStatus.FAILED;
                    updateExecutionStatus(execution, "failed");
                    break;
                }

                if (!next.equals(current)) {
                    previousNodeId = current;
                }
                current = next;
            }

            execution.setEndTime(LocalDateTime.now());
            executionRepository.save(execution);

            if (status == ExecutionStatus.FAILED) {
                return ExecutionResult.failed(executionId, "工作流执行失败");
            }

            return ExecutionResult.success(executionId, new HashMap<>());

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

            // 记录节点输入
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

            // 记录节点输出日志
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
                nodeRetryCount.merge(node.getId(), 1, Integer::sum);
                globalRetryCount++;

                if (!checkRetryLimits(node)) {
                    status = ExecutionStatus.WAITING_RETRY;
                    isWaitingRetry = true;
                    return NodeResult.retry();
                }
            }

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("节点 {} 执行异常: {}", node.getName(), e.getMessage(), e);

            // 记录错误日志
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
            nodeRetryCount.put(node.getId(), 0);

            Optional<WorkflowEdge> edge = edgeRepository.findByWorkflowIdAndSourceNodeId(workflowId, node.getId())
                    .stream()
                    .filter(e -> e.getEdgeType() == WorkflowEdge.EdgeType.SUCCESS)
                    .findFirst();

            return edge.map(WorkflowEdge::getTargetNodeId).orElse(null);

        } else if ("failed".equals(resultStatus) || "fail".equals(resultStatus)) {
            nodeRetryCount.merge(node.getId(), 1, Integer::sum);
            globalRetryCount++;

            if (!checkRetryLimits(node)) {
                return null;
            }

            Optional<WorkflowEdge> edge = edgeRepository.findByWorkflowIdAndSourceNodeId(workflowId, node.getId())
                    .stream()
                    .filter(e -> e.getEdgeType() == WorkflowEdge.EdgeType.FAIL)
                    .findFirst();

            if (edge.isPresent()) {
                return edge.get().getTargetNodeId();
            }

            return node.getId();

        } else if ("retry".equals(resultStatus)) {
            if (isWaitingRetry) {
                return null;
            }
            return node.getId();
        }

        return node.getId();
    }

    private boolean checkRetryLimits(WorkflowNode node) {
        int nodeMaxRetries = options.getMaxRetries();
        int globalMaxRetries = options.getMaxGlobalRetries();

        int currentNodeRetry = nodeRetryCount.getOrDefault(node.getId(), 0);

        if (currentNodeRetry >= nodeMaxRetries) {
            logger.warn("节点 {} 重试次数达到上限 ({})", node.getId(), nodeMaxRetries);
            return false;
        }

        if (globalRetryCount >= globalMaxRetries) {
            logger.warn("全局重试次数达到上限 ({})", globalMaxRetries);
            return false;
        }

        return true;
    }

    private void resetRetryCount() {
        nodeRetryCount.clear();
        globalRetryCount = 0;
        globalLoopCount = 0;
        logger.info("重试计数已重置");
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
            resetRetryCount();
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
        statusMap.put("globalLoopCount", globalLoopCount);
        statusMap.put("nodeRetryCount", new HashMap<>(nodeRetryCount));
        statusMap.put("globalRetryCount", globalRetryCount);
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

        // 统计开始节点数量
        List<WorkflowNode> startNodes = new java.util.ArrayList<>();
        for (WorkflowNode node : nodes) {
            if (node.getType() == WorkflowNode.NodeType.START) {
                startNodes.add(node);
            }
        }

        // 验证有且仅有一个开始节点
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