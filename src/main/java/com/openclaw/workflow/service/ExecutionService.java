package com.openclaw.workflow.service;

import com.openclaw.workflow.engine.WorkflowEngine;
import com.openclaw.workflow.engine.model.ExecutionOptions;
import com.openclaw.workflow.engine.model.ExecutionResult;
import com.openclaw.workflow.entity.Execution;
import com.openclaw.workflow.repository.ExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);

    private final ExecutionRepository executionRepository;
    private final WorkflowEngine workflowEngine;

    // 存储活跃的执行实例
    private final Map<String, WorkflowEngine> activeEngines = new ConcurrentHashMap<>();

    public ExecutionService(ExecutionRepository executionRepository,
                            WorkflowEngine workflowEngine) {
        this.executionRepository = executionRepository;
        this.workflowEngine = workflowEngine;
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
        return allExecutions.stream()
                .filter(e -> "running".equals(e.getStatus()) || "paused".equals(e.getStatus()))
                .toList();
    }

    public Execution findById(String executionId) {
        return executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("执行记录不存在: " + executionId));
    }

    @Async
    public Execution start(String workflowId, Map<String, Object> inputData, String taskDescription) {
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

    private String generateExecutionId() {
        return "exec-" + java.time.LocalDate.now().toString().replace("-", "") +
                "-" + Integer.toHexString((int) (Math.random() * 0xFFFFFF));
    }
}