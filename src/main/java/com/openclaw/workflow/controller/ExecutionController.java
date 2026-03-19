package com.openclaw.workflow.controller;

import com.openclaw.workflow.dto.ApiResponse;
import com.openclaw.workflow.dto.StartExecutionRequest;
import com.openclaw.workflow.dto.ReviewResponseRequest;
import com.openclaw.workflow.entity.Execution;
import com.openclaw.workflow.service.ExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Execution", description = "执行管理接口")
@RestController
@RequestMapping("/api")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @Operation(summary = "启动工作流执行")
    @PostMapping("/workflows/{workflowId}/executions")
    public ApiResponse<Execution> start(@PathVariable String workflowId,
                                         @RequestBody StartExecutionRequest request) {
        Execution execution = executionService.start(workflowId, request.getInputData(), request.getTaskDescription());
        return ApiResponse.success(execution);
    }

    @Operation(summary = "获取执行记录列表")
    @GetMapping("/executions")
    public ApiResponse<List<Execution>> list(
            @RequestParam(required = false) String workflowId,
            @RequestParam(required = false) String status) {
        List<Execution> executions;
        if (workflowId != null) {
            executions = executionService.findByWorkflowId(workflowId);
        } else if (status != null) {
            executions = executionService.findByStatus(status);
        } else {
            executions = executionService.findAll();
        }
        return ApiResponse.success(executions);
    }

    @Operation(summary = "获取执行详情")
    @GetMapping("/executions/{executionId}")
    public ApiResponse<Execution> get(@PathVariable String executionId) {
        return ApiResponse.success(executionService.findById(executionId));
    }

    @Operation(summary = "暂停执行")
    @PostMapping("/executions/{executionId}/pause")
    public ApiResponse<Void> pause(@PathVariable String executionId) {
        executionService.pause(executionId);
        return ApiResponse.success();
    }

    @Operation(summary = "恢复执行")
    @PostMapping("/executions/{executionId}/resume")
    public ApiResponse<Void> resume(@PathVariable String executionId) {
        executionService.resume(executionId);
        return ApiResponse.success();
    }

    @Operation(summary = "停止执行")
    @PostMapping("/executions/{executionId}/stop")
    public ApiResponse<Void> stop(@PathVariable String executionId) {
        executionService.stop(executionId);
        return ApiResponse.success();
    }

    @Operation(summary = "重新执行")
    @PostMapping("/executions/rerun/{executionId}")
    public ApiResponse<Execution> rerun(@PathVariable String executionId) {
        Execution execution = executionService.rerun(executionId);
        return ApiResponse.success(execution);
    }

    @Operation(summary = "获取执行状态")
    @GetMapping("/executions/{executionId}/status")
    public ApiResponse<Map<String, Object>> getStatus(@PathVariable String executionId) {
        return ApiResponse.success(executionService.getStatus(executionId));
    }

    @Operation(summary = "检查工作流是否有活跃执行")
    @GetMapping("/workflows/{workflowId}/executions/active")
    public ApiResponse<Map<String, Object>> checkActiveExecution(@PathVariable String workflowId) {
        Map<String, Object> result = new HashMap<>();
        List<Execution> activeExecutions = executionService.findActiveByWorkflowId(workflowId);
        result.put("hasActive", !activeExecutions.isEmpty());
        result.put("activeExecutions", activeExecutions);
        return ApiResponse.success(result);
    }

    // ==================== 人工审核回调API ====================

    @Operation(summary = "人工审核回调")
    @PostMapping("/executions/{executionId}/review/{nodeId}")
    public ApiResponse<Map<String, Object>> submitReview(
            @PathVariable String executionId,
            @PathVariable String nodeId,
            @RequestBody ReviewResponseRequest request) {
        try {
            // 处理审核结果
            Map<String, Object> result = executionService.processReview(
                    executionId,
                    nodeId,
                    request.getToken(),
                    request.getAction(),
                    request.getComment(),
                    request.getReviewer()
            );
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(500, "审核处理失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取审核状态")
    @GetMapping("/executions/{executionId}/review/{nodeId}")
    public ApiResponse<Map<String, Object>> getReviewStatus(
            @PathVariable String executionId,
            @PathVariable String nodeId) {
        try {
            Map<String, Object> status = executionService.getReviewStatus(executionId, nodeId);
            return ApiResponse.success(status);
        } catch (Exception e) {
            return ApiResponse.error(404, "审核记录不存在");
        }
    }
}