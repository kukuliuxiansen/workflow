package com.openclaw.workflow.controller;

import com.openclaw.workflow.dto.ApiResponse;
import com.openclaw.workflow.entity.Execution;
import com.openclaw.workflow.entity.TaskConfig;
import com.openclaw.workflow.repository.ExecutionRepository;
import com.openclaw.workflow.repository.TaskConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Execution Records", description = "执行记录接口")
@RestController
@RequestMapping("/api/executions")
public class ExecutionRecordController {

    private final ExecutionRepository executionRepository;
    private final TaskConfigRepository taskConfigRepository;

    public ExecutionRecordController(ExecutionRepository executionRepository,
                                      TaskConfigRepository taskConfigRepository) {
        this.executionRepository = executionRepository;
        this.taskConfigRepository = taskConfigRepository;
    }

    @Operation(summary = "获取执行记录列表")
    @GetMapping("/records")
    public ApiResponse<List<Map<String, Object>>> getRecords(
            @RequestParam(required = false) String workflowId,
            @RequestParam(defaultValue = "10") int limit) {
        List<Execution> executions;
        if (workflowId != null) {
            executions = executionRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId);
        } else {
            executions = executionRepository.findAllByOrderByCreatedAtDesc();
        }
        List<Map<String, Object>> records = executions.stream()
                .limit(limit)
                .map(this::toRecordMap)
                .collect(Collectors.toList());
        return ApiResponse.success(records);
    }

    @Operation(summary = "获取执行记录详情")
    @GetMapping("/records/{executionId}")
    public ApiResponse<Map<String, Object>> getRecord(@PathVariable String executionId) {
        Execution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("执行记录不存在: " + executionId));
        return ApiResponse.success(toRecordMap(execution));
    }

    @Operation(summary = "重新启动执行")
    @PostMapping("/records/{executionId}/restart")
    public ApiResponse<Void> restart(@PathVariable String executionId) {
        // TODO: 实现重新启动逻辑
        return ApiResponse.success();
    }

    @Operation(summary = "获取任务配置")
    @GetMapping("/{executionId}/task-config")
    public ApiResponse<TaskConfig> getTaskConfig(@PathVariable String executionId) {
        TaskConfig config = taskConfigRepository.findByExecutionId(executionId)
                .orElseGet(() -> {
                    TaskConfig newConfig = new TaskConfig();
                    newConfig.setExecutionId(executionId);
                    return newConfig;
                });
        return ApiResponse.success(config);
    }

    @Operation(summary = "保存任务配置")
    @PostMapping("/{executionId}/task-config")
    public ApiResponse<TaskConfig> saveTaskConfig(@PathVariable String executionId,
                                                   @RequestBody TaskConfig config) {
        config.setExecutionId(executionId);
        TaskConfig saved = taskConfigRepository.save(config);
        return ApiResponse.success(saved);
    }

    @Operation(summary = "获取执行日志")
    @GetMapping("/{executionId}/logs/execution")
    public ApiResponse<List<Map<String, Object>>> getExecutionLogs(@PathVariable String executionId) {
        // TODO: 实现从文件读取日志
        return ApiResponse.success(List.of());
    }

    @Operation(summary = "获取Agent日志")
    @GetMapping("/{executionId}/logs/agent")
    public ApiResponse<List<Map<String, Object>>> getAgentLogs(@PathVariable String executionId) {
        // TODO: 实现从文件读取日志
        return ApiResponse.success(List.of());
    }

    @Operation(summary = "获取节点日志")
    @GetMapping("/{executionId}/logs/node")
    public ApiResponse<List<Map<String, Object>>> getNodeLogs(@PathVariable String executionId) {
        // TODO: 实现从文件读取日志
        return ApiResponse.success(List.of());
    }

    private Map<String, Object> toRecordMap(Execution execution) {
        Map<String, Object> map = new HashMap<>();
        map.put("executionId", execution.getId());
        map.put("workflowId", execution.getWorkflowId());
        map.put("status", execution.getStatus());
        map.put("startTime", execution.getStartTime());
        map.put("endTime", execution.getEndTime());
        map.put("createdAt", execution.getCreatedAt());
        map.put("currentNodeId", execution.getCurrentNodeId());
        map.put("previousNodeId", execution.getPreviousNodeId());
        map.put("nodeRetryCount", execution.getNodeRetryCount());
        map.put("globalRetryCount", execution.getGlobalRetryCount());
        return map;
    }
}