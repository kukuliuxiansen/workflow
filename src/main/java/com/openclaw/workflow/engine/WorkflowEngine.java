package com.openclaw.workflow.engine;

import com.openclaw.workflow.engine.model.*;
import com.openclaw.workflow.entity.*;
import com.openclaw.workflow.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 工作流引擎主类
 */
@Component
public class WorkflowEngine {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEngine.class);

    private final ContextManager contextManager;
    private final NodeHandlerFactory handlerFactory;
    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private final ExecutionRepository executionRepository;
    private final NodeExecutionHelper executionHelper;
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
        this.executionHelper = new NodeExecutionHelper(handlerFactory, contextManager);
    }

    public void setLogService(com.openclaw.workflow.service.OperationLogService logService) {
        this.executionHelper.setLogService(logService);
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
            contextManager.init(input, null);

            String nodeId = startNodeId != null ? startNodeId : findStartNode(workflow);
            this.currentNodeId = nodeId;

            while (nodeId != null && !isStopped) {
                checkWait();
                if (isStopped) break;

                final String currentLookupNodeId = nodeId;
                WorkflowNode node = nodeRepository.findById(currentLookupNodeId)
                        .orElseThrow(() -> new RuntimeException("节点不存在: " + currentLookupNodeId));

                this.previousNodeId = this.currentNodeId;
                this.currentNodeId = nodeId;
                retryManager.incrementLoopCount();

                if (retryManager.getGlobalLoopCount() > options.getMaxGlobalLoop()) {
                    logger.warn("循环次数超限: {}", retryManager.getGlobalLoopCount());
                    break;
                }

                NodeResult result = executionHelper.execute(node, workflowId, executionId);
                updateExecutionStatus(execution, status.name().toLowerCase());

                nodeId = determineNextNode(node, result, execution);

                if (nodeId == null || "finish".equals(result.getStatus())) {
                    this.status = ExecutionStatus.COMPLETED;
                    updateExecutionStatus(execution, "completed");
                    break;
                }
            }

            if (isStopped) {
                updateExecutionStatus(execution, "stopped");
            }

            Map<String, Object> allOutputs = new HashMap<>();
            for (Map.Entry<String, NodeResult> entry : contextManager.getAllOutputs().entrySet()) {
                allOutputs.put(entry.getKey(), entry.getValue().getOutput());
            }
            return ExecutionResult.success(executionId, allOutputs);

        } catch (Exception e) {
            logger.error("工作流执行异常: {}", e.getMessage(), e);
            this.status = ExecutionStatus.FAILED;
            return ExecutionResult.failed(executionId, e.getMessage());
        }
    }

    private String determineNextNode(WorkflowNode node, NodeResult result, Execution execution) {
        if (result.hasNextNode()) {
            return result.getFirstNextNodeId();
        }

        String resultStatus = result.getStatus();

        if ("success".equals(resultStatus) || "completed".equals(resultStatus)) {
            retryManager.incrementNodeRetry(node.getId());
            retryManager.incrementGlobalRetry();
            return findNextEdge(node, WorkflowEdge.EdgeType.SUCCESS);

        } else if ("failed".equals(resultStatus) || "fail".equals(resultStatus)) {
            if (!retryManager.checkAllLimits(node.getId())) {
                return null;
            }
            return findNextEdge(node, WorkflowEdge.EdgeType.FAIL);

        } else if ("retry".equals(resultStatus)) {
            return handleRetry(node, execution);

        } else if ("finish".equals(resultStatus)) {
            return null;
        }

        return node.getId();
    }

    private String findNextEdge(WorkflowNode node, WorkflowEdge.EdgeType edgeType) {
        return edgeRepository.findByWorkflowIdAndSourceNodeId(workflowId, node.getId())
                .stream()
                .filter(e -> e.getEdgeType() == edgeType)
                .findFirst()
                .map(WorkflowEdge::getTargetNodeId)
                .orElse(null);
    }

    private String handleRetry(WorkflowNode node, Execution execution) {
        retryManager.incrementNodeRetry(node.getId());
        retryManager.incrementGlobalRetry();

        if (!retryManager.checkAllLimits(node.getId())) {
            status = ExecutionStatus.WAITING_RETRY;
            isWaitingRetry = true;
            return null;
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