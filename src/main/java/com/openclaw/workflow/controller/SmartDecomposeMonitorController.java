package com.openclaw.workflow.controller;

import com.openclaw.workflow.engine.smartdecompose.v2.persistence.DecisionRecorder;
import com.openclaw.workflow.engine.smartdecompose.v2.persistence.StatePersister;
import com.openclaw.workflow.entity.DecisionHistory;
import com.openclaw.workflow.entity.Execution;
import com.openclaw.workflow.repository.ExecutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SmartDecompose 监控控制器
 */
@RestController
@RequestMapping("/api/smart-decompose")
public class SmartDecomposeMonitorController {

    @Autowired
    private DecisionRecorder decisionRecorder;

    @Autowired
    private StatePersister statePersister;

    @Autowired
    private ExecutionRepository executionRepository;

    /**
     * 查询决策历史
     *
     * GET /api/smart-decompose/executions/{executionId}/decisions
     */
    @GetMapping("/executions/{executionId}/decisions")
    public ResponseEntity<?> getDecisions(
            @PathVariable String executionId,
            @RequestParam(required = false) String nodeId) {

        List<DecisionHistory> decisions;
        if (nodeId != null && !nodeId.isEmpty()) {
            decisions = decisionRecorder.findByExecutionId(executionId, nodeId);
        } else {
            // 获取所有
            decisions = decisionRecorder.findByExecutionId(executionId, "");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("executionId", executionId);
        result.put("decisions", decisions);

        return ResponseEntity.ok(result);
    }

    /**
     * 查询执行状态
     *
     * GET /api/smart-decompose/executions/{executionId}/status
     */
    @GetMapping("/executions/{executionId}/status")
    public ResponseEntity<?> getStatus(
            @PathVariable String executionId,
            @RequestParam(required = false) String nodeId) {

        if (nodeId == null || nodeId.isEmpty()) {
            // 从执行记录获取节点ID
            Execution execution = executionRepository.findById(executionId).orElse(null);
            if (execution == null) {
                return ResponseEntity.notFound().build();
            }
            // 简化处理：返回基本信息
            Map<String, Object> result = new HashMap<>();
            result.put("executionId", executionId);
            result.put("status", execution.getStatus());
            return ResponseEntity.ok(result);
        }

        try {
            com.openclaw.workflow.engine.smartdecompose.v2.model.DecomposeContext context =
                statePersister.load(executionId, nodeId);

            Map<String, Object> result = new HashMap<>();
            result.put("executionId", context.getExecutionId());
            result.put("nodeId", context.getNodeId());
            result.put("status", context.getStatus().name());
            result.put("iterationCount", context.getIterationCount());
            result.put("maxIterations", context.getMaxIterations());
            result.put("queueSize", context.getTaskQueue().size());
            result.put("completedCount", context.getCompletedTasks().size());
            result.put("failedCount", context.getFailedTasks().size());

            if (context.getCurrentTask() != null) {
                Map<String, Object> currentTask = new HashMap<>();
                currentTask.put("id", context.getCurrentTask().getId());
                currentTask.put("description", context.getCurrentTask().getDescription());
                result.put("currentTask", currentTask);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取统计信息
     *
     * GET /api/smart-decompose/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Map<String, Object> stats = new HashMap<>();
        // 可以添加更多统计信息
        stats.put("message", "Statistics endpoint - implement as needed");
        return ResponseEntity.ok(stats);
    }
}