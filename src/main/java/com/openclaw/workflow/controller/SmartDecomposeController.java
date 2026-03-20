package com.openclaw.workflow.controller;

import com.openclaw.workflow.dto.ApiResponse;
import com.openclaw.workflow.entity.DecisionHistory;
import com.openclaw.workflow.entity.SmartDecomposeState;
import com.openclaw.workflow.repository.DecisionHistoryRepository;
import com.openclaw.workflow.repository.SmartDecomposeStateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 智能分解节点API
 */
@Tag(name = "SmartDecompose", description = "智能分解节点接口")
@RestController
@RequestMapping("/api/executions")
public class SmartDecomposeController {

    private static final Logger logger = LoggerFactory.getLogger(SmartDecomposeController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SmartDecomposeStateRepository stateRepository;
    private final DecisionHistoryRepository decisionHistoryRepository;

    public SmartDecomposeController(SmartDecomposeStateRepository stateRepository,
                                     DecisionHistoryRepository decisionHistoryRepository) {
        this.stateRepository = stateRepository;
        this.decisionHistoryRepository = decisionHistoryRepository;
    }

    /**
     * 获取智能分解执行状态
     */
    @Operation(summary = "获取智能分解执行状态")
    @GetMapping("/{executionId}/smart-decompose/{nodeId}/status")
    public ApiResponse<Map<String, Object>> getStatus(
            @PathVariable String executionId,
            @PathVariable String nodeId) {

        Optional<SmartDecomposeState> stateOpt = stateRepository.findByExecutionIdAndNodeId(executionId, nodeId);

        if (stateOpt.isEmpty()) {
            return ApiResponse.error(404, "执行状态不存在");
        }

        SmartDecomposeState state = stateOpt.get();
        Map<String, Object> result = new LinkedHashMap<>();

        // 基本信息
        result.put("executionId", state.getExecutionId());
        result.put("nodeId", state.getNodeId());
        result.put("status", state.getStatus());

        // 循环状态
        result.put("iteration", state.getCurrentIteration());
        result.put("maxIterations", state.getMaxIterations());
        result.put("currentDepth", state.getCurrentDepth());
        result.put("maxDepth", state.getMaxDepth());

        // 当前任务
        if (state.getCurrentTaskId() != null) {
            Map<String, Object> currentTask = new LinkedHashMap<>();
            currentTask.put("id", state.getCurrentTaskId());
            // 从taskMap解析当前任务详情
            try {
                if (state.getTaskMap() != null) {
                    Map<String, Object> taskMap = objectMapper.readValue(state.getTaskMap(), Map.class);
                    Object taskObj = taskMap.get(state.getCurrentTaskId());
                    if (taskObj != null) {
                        currentTask.put("details", taskObj);
                    }
                }
            } catch (Exception e) {
                logger.warn("解析任务映射失败", e);
            }
            result.put("currentTask", currentTask);
        }

        // 任务统计
        result.put("pendingTasks", countTasks(state.getTaskStack()));
        result.put("completedTasks", countTasks(state.getCompletedTasks()));

        // 最近的决策
        List<DecisionHistory> recentDecisions = decisionHistoryRepository
                .findTop10ByExecutionIdAndNodeIdOrderByIterationDesc(executionId, nodeId);
        List<Map<String, Object>> decisions = new ArrayList<>();
        for (DecisionHistory dh : recentDecisions) {
            Map<String, Object> decision = new LinkedHashMap<>();
            decision.put("iteration", dh.getIteration());
            decision.put("taskId", dh.getTaskId());
            decision.put("thought", truncate(dh.getThought(), 200));
            decision.put("action", dh.getAction());
            decision.put("resultStatus", dh.getResultStatus());
            decisions.add(decision);
        }
        result.put("recentDecisions", decisions);

        // 错误信息
        if (state.getErrorMessage() != null) {
            result.put("errorMessage", state.getErrorMessage());
        }

        return ApiResponse.success(result);
    }

    /**
     * 获取决策历史
     */
    @Operation(summary = "获取决策历史")
    @GetMapping("/{executionId}/smart-decompose/{nodeId}/decisions")
    public ApiResponse<List<DecisionHistory>> getDecisions(
            @PathVariable String executionId,
            @PathVariable String nodeId) {

        List<DecisionHistory> decisions = decisionHistoryRepository
                .findByExecutionIdAndNodeIdOrderByIterationAsc(executionId, nodeId);
        return ApiResponse.success(decisions);
    }

    /**
     * 用户输入回调
     */
    @Operation(summary = "用户输入回调")
    @PostMapping("/{executionId}/smart-decompose/{nodeId}/user-input")
    public ApiResponse<Map<String, Object>> submitUserInput(
            @PathVariable String executionId,
            @PathVariable String nodeId,
            @RequestBody Map<String, Object> input) {

        logger.info("收到用户输入: executionId={}, nodeId={}, input={}", executionId, nodeId, input);

        // TODO: 实现用户输入处理逻辑
        // 1. 验证执行状态是否在等待用户输入
        // 2. 将用户输入存入上下文
        // 3. 恢复执行

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "RECEIVED");
        result.put("message", "用户输入已接收，工作流将继续执行");

        return ApiResponse.success(result);
    }

    /**
     * 子Agent结果回调
     */
    @Operation(summary = "子Agent结果回调")
    @PostMapping("/{executionId}/subagent/{subagentId}/result")
    public ApiResponse<Map<String, Object>> submitSubAgentResult(
            @PathVariable String executionId,
            @PathVariable String subagentId,
            @RequestBody Map<String, Object> result) {

        logger.info("收到子Agent结果: executionId={}, subagentId={}", executionId, subagentId);

        // TODO: 实现子Agent结果处理逻辑
        // 1. 更新子Agent执行记录
        // 2. 将结果存入主上下文
        // 3. 恢复主循环执行

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "RECEIVED");
        response.put("message", "子Agent结果已接收");

        return ApiResponse.success(response);
    }

    // 辅助方法
    private int countTasks(String json) {
        if (json == null || json.isEmpty()) return 0;
        try {
            List<?> list = objectMapper.readValue(json, List.class);
            return list.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}