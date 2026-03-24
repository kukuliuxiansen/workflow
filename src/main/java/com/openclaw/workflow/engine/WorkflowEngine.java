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
    private volatile ExecutionStatus status = ExecutionStatus.PENDING;
    private volatile String currentNodeId;
    private volatile String previousNodeId;

    /** 执行控制对象，用于与长时间运行的节点共享暂停/停止状态 */
    private final ExecutionControl executionControl = new ExecutionControl();
    private volatile boolean isWaitingRetry = false;

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
        this.executionControl.reset();
        this.isWaitingRetry = false;
    }

    public ExecutionResult execute(String workflowId, String startNodeId,
                                   Map<String, Object> input, ExecutionOptions options) {
        resetEngineState();

        if (options != null) {
            this.options = options;
        }

        this.workflowId = workflowId;

        // 必须传入 executionId，由 ExecutionService 负责创建
        if (options == null || options.getExecutionId() == null || options.getExecutionId().isEmpty()) {
            throw new IllegalArgumentException("executionId 必须由调用方传入，WorkflowEngine 不再创建 Execution 记录");
        }
        this.executionId = options.getExecutionId();
        logger.info("开始执行: workflowId={}, executionId={}", workflowId, this.executionId);

        this.status = ExecutionStatus.RUNNING;
        this.executionControl.init(this.executionId);

        try {
            Workflow workflow = workflowRepository.findById(workflowId)
                    .orElseThrow(() -> new RuntimeException("工作流不存在: " + workflowId));

            // 加载已有的执行记录（由 ExecutionService 创建）
            Execution execution = executionRepository.findById(this.executionId)
                    .orElseThrow(() -> new RuntimeException("执行记录不存在: " + this.executionId));
            logger.info("加载执行记录: executionId={}, status={}", this.executionId, execution.getStatus());

            contextManager.init(input, null);

            String nodeId = startNodeId != null ? startNodeId : findStartNode(workflow);
            this.currentNodeId = nodeId;

            while (nodeId != null && !executionControl.isStopped()) {
                checkWait();
                if (executionControl.isStopped()) {
                    // 在等待期间被停止，保存当前节点
                    this.status = ExecutionStatus.STOPPED;
                    updateExecutionStatus(execution, "stopped", this.currentNodeId);
                    logger.info("执行在等待期间被停止，保存当前节点: {}", this.currentNodeId);
                    break;
                }

                final String currentLookupNodeId = nodeId;
                WorkflowNode node = nodeRepository.findById(currentLookupNodeId)
                        .orElseThrow(() -> new RuntimeException("节点不存在: " + currentLookupNodeId));

                // 在执行前保存当前节点ID，用于暂停时正确保存位置
                final String executingNodeId = nodeId;
                this.previousNodeId = this.currentNodeId;
                this.currentNodeId = nodeId;
                retryManager.incrementLoopCount();

                if (retryManager.getGlobalLoopCount() > options.getMaxGlobalLoop()) {
                    logger.warn("循环次数超限: {}", retryManager.getGlobalLoopCount());
                    break;
                }

                NodeResult result = executionHelper.execute(node, workflowId, executionId, executionControl);

                // 检查是否在执行过程中被停止
                if (executionControl.isStopped()) {
                    this.status = ExecutionStatus.STOPPED;
                    updateExecutionStatus(execution, "stopped", executingNodeId);
                    logger.info("执行被停止，保存当前节点: {}", executingNodeId);
                    break;
                }

                // 检查是否在执行过程中被暂停
                if (executionControl.isPaused()) {
                    // 暂停时保存正在执行的节点（而不是下一个节点）
                    this.status = ExecutionStatus.PAUSED;
                    updateExecutionStatus(execution, "paused", executingNodeId);
                    logger.info("执行被暂停，保存当前节点: {}", executingNodeId);
                    break;
                }

                nodeId = determineNextNode(node, result, execution);

                // 更新执行状态
                if (status == ExecutionStatus.PAUSED) {
                    // 智能节点返回暂停状态
                    updateExecutionStatus(execution, "paused", executingNodeId);
                    break;
                }

                updateExecutionStatus(execution, status.name().toLowerCase());

                if (nodeId == null || "finish".equals(result.getStatus())) {
                    this.status = ExecutionStatus.COMPLETED;
                    updateExecutionStatus(execution, "completed");
                    break;
                }
            }

            Map<String, Object> allOutputs = new HashMap<>();
            for (Map.Entry<String, NodeResult> entry : contextManager.getAllOutputs().entrySet()) {
                allOutputs.put(entry.getKey(), entry.getValue().getOutput());
            }
            return ExecutionResult.success(executionId, allOutputs);

        } catch (Exception e) {
            logger.error("工作流执行异常: {}", e.getMessage(), e);
            this.status = ExecutionStatus.PAUSED;
            return ExecutionResult.paused(executionId, e.getMessage());
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

        } else if ("paused".equals(resultStatus)) {
            // 智能节点返回暂停状态，更新引擎状态
            status = ExecutionStatus.PAUSED;
            executionControl.pause(); // 同步 executionControl 状态
            logger.info("智能节点返回暂停状态");
            return null;

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
        while (executionControl.isPaused() || isWaitingRetry) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void pause() {
        executionControl.pause();
        status = ExecutionStatus.PAUSED;
        logger.info("工作流已暂停");
    }

    public void resume(boolean resetRetry) {
        if (isWaitingRetry) {
            retryManager.reset();
            isWaitingRetry = false;
        } else if (executionControl.isPaused()) {
            executionControl.resume();
        }
        status = ExecutionStatus.RUNNING;
        logger.info("工作流已恢复");
    }

    public void stop() {
        executionControl.stop();
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
        statusMap.put("isPaused", executionControl.isPaused());
        statusMap.put("isStopped", executionControl.isStopped());
        statusMap.put("isWaitingRetry", isWaitingRetry);
        return statusMap;
    }

    /**
     * 获取执行控制对象
     * 用于长时间运行的节点检查暂停/停止状态
     */
    public ExecutionControl getExecutionControl() {
        return executionControl;
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

    /**
     * 更新执行状态到数据库
     *
     * @param execution 执行记录
     * @param newStatus 新状态
     * @param actualNodeId 实际的当前节点ID（用于暂停时保存正确的节点）
     */
    private void updateExecutionStatus(Execution execution, String newStatus, String actualNodeId) {
        execution.setStatus(newStatus);
        // 使用传入的 actualNodeId（如果提供），否则使用 this.currentNodeId
        execution.setCurrentNodeId(actualNodeId != null ? actualNodeId : this.currentNodeId);
        execution.setPreviousNodeId(this.previousNodeId);
        executionRepository.save(execution);
        logger.info("更新执行状态: executionId={}, status={}, currentNodeId={}",
            execution.getId(), newStatus, execution.getCurrentNodeId());
    }

    private void updateExecutionStatus(Execution execution, String status) {
        updateExecutionStatus(execution, status, null);
    }
}