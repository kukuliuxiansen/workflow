package com.openclaw.workflow.service;

import com.openclaw.workflow.engine.WorkflowEngine;
import com.openclaw.workflow.engine.handler.HumanReviewNodeHandler;
import com.openclaw.workflow.engine.model.ExecutionOptions;
import com.openclaw.workflow.engine.model.ExecutionResult;
import com.openclaw.workflow.entity.Execution;
import com.openclaw.workflow.entity.NodeExecution;
import com.openclaw.workflow.entity.ReviewRecord;
import com.openclaw.workflow.repository.ExecutionRepository;
import com.openclaw.workflow.repository.NodeExecutionRepository;
import com.openclaw.workflow.repository.ReviewRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);

    private final ExecutionRepository executionRepository;
    private final WorkflowEngine workflowEngine;
    private final ReviewRecordRepository reviewRecordRepository;
    private final NodeExecutionRepository nodeExecutionRepository;

    // 存储活跃的执行实例
    private final Map<String, WorkflowEngine> activeEngines = new ConcurrentHashMap<>();

    public ExecutionService(ExecutionRepository executionRepository,
                            WorkflowEngine workflowEngine,
                            ReviewRecordRepository reviewRecordRepository,
                            NodeExecutionRepository nodeExecutionRepository) {
        this.executionRepository = executionRepository;
        this.workflowEngine = workflowEngine;
        this.reviewRecordRepository = reviewRecordRepository;
        this.nodeExecutionRepository = nodeExecutionRepository;
    }

    public List<Execution> findAll() {
        return executionRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Execution> findByWorkflowId(String workflowId) {
        return executionRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId);
    }

    public List<Execution> findByStatus(String status) {
        return executionRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * 查找工作流的活跃执行（running或paused状态）
     */
    public List<Execution> findActiveByWorkflowId(String workflowId) {
        List<Execution> allExecutions = executionRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId);
        List<Execution> result = new java.util.ArrayList<>();
        for (Execution e : allExecutions) {
            if ("running".equals(e.getStatus()) || "paused".equals(e.getStatus())) {
                result.add(e);
            }
        }
        return result;
    }

    public Execution findById(String executionId) {
        return executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("执行记录不存在: " + executionId));
    }

    @Async
    public Execution start(String workflowId, Map<String, Object> inputData, String taskDescription,
                           String projectPath, String globalPrompt) {
        // 创建执行记录
        Execution execution = new Execution();
        execution.setId(generateExecutionId());
        execution.setWorkflowId(workflowId);
        execution.setStatus("running");
        execution.setStartTime(LocalDateTime.now());
        execution.setCreatedAt(LocalDateTime.now());

        // 设置输入数据
        if (inputData != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                execution.setTaskConfig(mapper.writeValueAsString(inputData));
            } catch (Exception e) {
                logger.warn("序列化输入数据失败", e);
            }
        }

        executionRepository.save(execution);

        // 异步执行工作流
        ExecutionOptions options = new ExecutionOptions();
        options.setExecutionId(execution.getId());

        if (inputData == null) {
            inputData = new java.util.HashMap<>();
        }
        if (taskDescription != null) {
            inputData.put("task_description", taskDescription);
        }
        if (projectPath != null) {
            inputData.put("project_path", projectPath);
        }
        if (globalPrompt != null) {
            inputData.put("global_prompt", globalPrompt);
        }

        try {
            ExecutionResult result = workflowEngine.execute(workflowId, null, inputData, options);
            execution.setStatus(result.isSuccess() ? "completed" : "failed");
            if (!result.isSuccess() && result.getError() != null) {
                execution.setContextData(result.getError());
            }
        } catch (Exception e) {
            logger.error("工作流执行异常", e);
            execution.setStatus("failed");
            execution.setContextData(e.getMessage());
        }

        execution.setEndTime(LocalDateTime.now());
        return executionRepository.save(execution);
    }

    // 兼容旧的方法签名
    public Execution start(String workflowId, Map<String, Object> inputData, String taskDescription) {
        return start(workflowId, inputData, taskDescription, null, null);
    }

    public void pause(String executionId) {
        Execution execution = findById(executionId);
        if ("running".equals(execution.getStatus())) {
            workflowEngine.pause();
            execution.setStatus("paused");
            executionRepository.save(execution);
        }
    }

    public void resume(String executionId) {
        Execution execution = findById(executionId);
        if ("paused".equals(execution.getStatus())) {
            workflowEngine.resume(false);
            execution.setStatus("running");
            executionRepository.save(execution);
        }
    }

    public void stop(String executionId) {
        Execution execution = findById(executionId);
        if ("running".equals(execution.getStatus()) || "paused".equals(execution.getStatus())) {
            workflowEngine.stop();
            execution.setStatus("stopped");
            execution.setEndTime(LocalDateTime.now());
            executionRepository.save(execution);
        }
    }

    public Execution rerun(String executionId) {
        Execution original = findById(executionId);

        // 解析原始输入数据
        Map<String, Object> inputData = new java.util.HashMap<>();
        if (original.getTaskConfig() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                inputData = mapper.readValue(original.getTaskConfig(), Map.class);
            } catch (Exception e) {
                logger.warn("解析原始输入数据失败", e);
            }
        }

        // 启动新的执行
        return start(original.getWorkflowId(), inputData, null);
    }

    public Map<String, Object> getStatus(String executionId) {
        Execution execution = findById(executionId);

        Map<String, Object> status = new java.util.HashMap<>();
        status.put("executionId", execution.getId());
        status.put("status", execution.getStatus());
        status.put("currentNodeId", execution.getCurrentNodeId());
        status.put("previousNodeId", execution.getPreviousNodeId());
        status.put("nodeRetryCount", execution.getNodeRetryCount());
        status.put("globalRetryCount", execution.getGlobalRetryCount());
        status.put("startTime", execution.getStartTime());
        status.put("endTime", execution.getEndTime());

        return status;
    }

    // ==================== 人工审核相关方法 ====================

    /**
     * 处理审核结果
     */
    public Map<String, Object> processReview(String executionId, String nodeId, String token,
                                               String action, String comment, String reviewer) {
        Execution execution = findById(executionId);

        // 验证Token
        if (!HumanReviewNodeHandler.validateToken(token, executionId, nodeId)) {
            throw new RuntimeException("无效的审核Token");
        }

        // 检查执行状态
        if (!"waiting_review".equals(execution.getStatus()) && !"running".equals(execution.getStatus())) {
            throw new RuntimeException("当前执行不在等待审核状态");
        }

        // 创建审核记录
        ReviewRecord reviewRecord = new ReviewRecord();
        reviewRecord.setExecutionId(executionId);
        reviewRecord.setNodeId(nodeId);
        reviewRecord.setReviewer(reviewer != null ? reviewer : "unknown");
        reviewRecord.setComment(comment);
        reviewRecord.setCreatedAt(LocalDateTime.now());

        if ("approve".equalsIgnoreCase(action)) {
            reviewRecord.setDecision(ReviewRecord.ReviewDecision.APPROVED);
        } else if ("reject".equalsIgnoreCase(action)) {
            reviewRecord.setDecision(ReviewRecord.ReviewDecision.REJECTED);
        } else {
            throw new RuntimeException("无效的审核动作: " + action);
        }

        reviewRecordRepository.save(reviewRecord);

        // 恢复工作流执行
        workflowEngine.resume(false);

        // 更新执行状态
        execution.setStatus("running");
        executionRepository.save(execution);

        logger.info("审核处理完成: executionId={}, nodeId={}, action={}", executionId, nodeId, action);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("executionId", executionId);
        result.put("nodeId", nodeId);
        result.put("status", "APPROVED".equalsIgnoreCase(action) ? "APPROVED" : "REJECTED");
        result.put("message", "审批成功，工作流将继续执行");
        result.put("reviewRecord", reviewRecord);

        return result;
    }

    /**
     * 获取审核状态
     */
    public Map<String, Object> getReviewStatus(String executionId, String nodeId) {
        Execution execution = findById(executionId);

        List<ReviewRecord> records = reviewRecordRepository.findByExecutionIdAndNodeIdOrderByCreatedAtDesc(
                executionId, nodeId);

        Map<String, Object> status = new java.util.HashMap<>();
        status.put("executionId", executionId);
        status.put("nodeId", nodeId);
        status.put("executionStatus", execution.getStatus());

        if (records.isEmpty()) {
            status.put("reviewStatus", "PENDING");
            status.put("message", "等待审核");
        } else {
            ReviewRecord latest = records.get(0);
            status.put("reviewStatus", latest.getDecision().name());
            status.put("reviewer", latest.getReviewer());
            status.put("comment", latest.getComment());
            status.put("reviewTime", latest.getCreatedAt());
        }

        return status;
    }

    private String generateExecutionId() {
        return "exec-" + java.time.LocalDate.now().toString().replace("-", "") +
                "-" + Integer.toHexString((int) (Math.random() * 0xFFFFFF));
    }

    /**
     * 删除执行记录
     */
    public void delete(String executionId) {
        Execution execution = findById(executionId);

        // 不允许删除正在运行的执行
        if ("running".equals(execution.getStatus()) || "paused".equals(execution.getStatus())) {
            throw new RuntimeException("无法删除正在执行或暂停的记录，请先停止执行");
        }

        // 删除相关的审核记录
        reviewRecordRepository.deleteByExecutionId(executionId);

        // 删除执行记录
        executionRepository.delete(execution);

        logger.info("已删除执行记录: {}", executionId);
    }

    /**
     * 获取执行记录的节点状态
     */
    public Map<String, String> getNodeStatuses(String executionId) {
        Map<String, String> result = new HashMap<>();

        // 从 node_execution 表获取节点状态
        List<NodeExecution> nodeExecutions = nodeExecutionRepository.findByExecutionIdOrderByCreatedAtAsc(executionId);
        for (NodeExecution ne : nodeExecutions) {
            NodeExecution.NodeStatus nodeStatus = ne.getStatus();
            String status = "pending";
            // 转换状态格式以匹配前端
            if (nodeStatus == NodeExecution.NodeStatus.SUCCESS) {
                status = "success";
            } else if (nodeStatus == NodeExecution.NodeStatus.FAILED) {
                status = "failed";
            } else if (nodeStatus == NodeExecution.NodeStatus.RUNNING) {
                status = "running";
            } else if (nodeStatus == NodeExecution.NodeStatus.RETRY) {
                status = "running";
            }
            result.put(ne.getNodeId(), status);
        }

        // 如果没有记录，根据执行状态推断整体状态
        if (result.isEmpty()) {
            Execution execution = findById(executionId);
            String execStatus = execution.getStatus();
            logger.debug("执行 {} 无节点状态记录，整体状态: {}", executionId, execStatus);
        }

        return result;
    }
}