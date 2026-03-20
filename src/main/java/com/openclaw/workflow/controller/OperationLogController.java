package com.openclaw.workflow.controller;

import com.openclaw.workflow.dto.ApiResponse;
import com.openclaw.workflow.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "OperationLog", description = "操作日志接口")
@RestController
@RequestMapping("/api/logs")
public class OperationLogController {

    private final OperationLogService logService;

    public OperationLogController(OperationLogService logService) {
        this.logService = logService;
    }

    @Operation(summary = "获取最近的日志")
    @GetMapping("/recent")
    public ApiResponse<List<OperationLogService.LogEntry>> getRecentLogs(
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(logService.getRecentLogs(limit));
    }

    @Operation(summary = "获取指定执行ID的日志")
    @GetMapping("/execution/{executionId}")
    public ApiResponse<List<OperationLogService.LogEntry>> getLogsByExecutionId(
            @PathVariable String executionId) {
        return ApiResponse.success(logService.getLogsByExecutionId(executionId));
    }

    @Operation(summary = "从文件读取日志")
    @GetMapping("/file")
    public ApiResponse<List<Map<String, Object>>> readLogFromFile(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "500") int limit) {
        return ApiResponse.success(logService.readLogFromFile(date, limit));
    }

    @Operation(summary = "保存前端日志")
    @PostMapping("/save")
    public ApiResponse<Void> saveFrontendLog(@RequestBody Map<String, Object> logEntry) {
        logService.saveFrontendLog(logEntry);
        return ApiResponse.success();
    }

    @Operation(summary = "清空内存日志")
    @DeleteMapping("/recent")
    public ApiResponse<Void> clearRecentLogs() {
        logService.clearRecentLogs();
        return ApiResponse.success();
    }
}