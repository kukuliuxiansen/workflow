package com.openclaw.workflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.smartdecompose.v2.persistence.DecisionRecorder;
import com.openclaw.workflow.engine.smartdecompose.v2.persistence.StatePersister;
import com.openclaw.workflow.entity.DecisionHistory;
import com.openclaw.workflow.entity.Execution;
import com.openclaw.workflow.repository.ExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * SmartDecompose 监控控制器
 */
@RestController
@RequestMapping("/api/smart-decompose")
public class SmartDecomposeMonitorController {

    private static final Logger logger = LoggerFactory.getLogger(SmartDecomposeMonitorController.class);

    @Autowired
    private DecisionRecorder decisionRecorder;

    @Autowired
    private StatePersister statePersister;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private ObjectMapper objectMapper;

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

    /**
     * 获取任务拆分层级树
     *
     * GET /api/smart-decompose/executions/{executionId}/task-tree
     */
    @GetMapping("/executions/{executionId}/task-tree")
    public ResponseEntity<?> getTaskTree(
            @PathVariable String executionId,
            @RequestParam(required = false) String nodeId) {

        List<DecisionHistory> decisions = decisionRecorder.findByExecutionId(executionId, nodeId != null ? nodeId : "");

        if (decisions.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("executionId", executionId);
            result.put("nodeId", nodeId);
            result.put("totalDecisions", 0);
            result.put("totalTasks", 0);
            result.put("maxDepth", 0);
            result.put("taskTree", null);
            result.put("error", "未找到决策记录");
            return ResponseEntity.ok(result);
        }

        // 按迭代顺序排序
        decisions.sort(Comparator.comparing(DecisionHistory::getIteration));

        // 任务数据存储：taskId -> 任务信息
        Map<String, Map<String, Object>> taskDataMap = new LinkedHashMap<>();

        // 父子关系：childId -> parentId
        Map<String, String> parentMap = new HashMap<>();

        // 记录每个决策产生的子任务列表
        Map<String, List<String>> decisionChildrenMap = new LinkedHashMap<>();

        // 处理每条决策记录
        for (DecisionHistory decision : decisions) {
            String currentTaskId = decision.getTaskId();
            String action = decision.getAction();

            if (currentTaskId == null) continue;

            // 保存当前任务的决策信息
            Map<String, Object> taskData = taskDataMap.computeIfAbsent(currentTaskId, k -> new LinkedHashMap<>());
            taskData.put("id", currentTaskId);
            taskData.put("action", action);
            taskData.put("thought", decision.getThought());
            taskData.put("iteration", decision.getIteration());
            taskData.put("timestamp", decision.getTimestamp() != null ? decision.getTimestamp().toString() : null);

            // 处理split决策产生的子任务
            if ("split".equals(action) && decision.getResultMessage() != null) {
                try {
                    JsonNode subTasks = objectMapper.readTree(decision.getResultMessage());
                    if (subTasks.isArray()) {
                        List<String> childIds = new ArrayList<>();
                        for (JsonNode subTask : subTasks) {
                            String childId = subTask.path("id").asText();
                            if (childId != null && !childId.isEmpty()) {
                                childIds.add(childId);

                                // 记录父子关系
                                parentMap.put(childId, currentTaskId);

                                // 保存子任务信息
                                Map<String, Object> childData = taskDataMap.computeIfAbsent(childId, k -> new LinkedHashMap<>());
                                childData.put("id", childId);
                                childData.put("description", subTask.path("description").asText());
                                childData.put("criteria", subTask.path("criteria").asText());
                                childData.put("estimatedMinutes", subTask.path("estimatedMinutes").asInt(0));
                            }
                        }
                        decisionChildrenMap.put(currentTaskId, childIds);
                    }
                } catch (Exception e) {
                    logger.warn("解析子任务JSON失败: {}", e.getMessage());
                }
            }
        }

        // 找到根任务（没有父节点的任务）
        String rootTaskId = null;
        for (String taskId : taskDataMap.keySet()) {
            if (!parentMap.containsKey(taskId)) {
                rootTaskId = taskId;
                break;
            }
        }

        // 构建树结构
        Map<String, Object> rootTree = null;
        if (rootTaskId != null) {
            Set<String> visited = new HashSet<>();
            rootTree = buildTree(rootTaskId, taskDataMap, decisionChildrenMap, 0, visited);

            // 为根节点添加默认描述
            if (rootTree != null && rootTree.get("description") == null) {
                rootTree.put("description", "根任务");
            }
        }

        // 计算统计
        int maxDepth = rootTree != null ? calculateMaxDepth(rootTree) : 0;

        // 构建返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("executionId", executionId);
        result.put("nodeId", nodeId);
        result.put("totalDecisions", decisions.size());
        result.put("totalTasks", taskDataMap.size());
        result.put("maxDepth", maxDepth);
        result.put("taskTree", rootTree);

        return ResponseEntity.ok(result);
    }

    /**
     * 递归构建树节点
     */
    private Map<String, Object> buildTree(String taskId, Map<String, Map<String, Object>> taskDataMap,
                                          Map<String, List<String>> decisionChildrenMap, int depth,
                                          Set<String> visited) {
        // 防止循环引用 - 检查当前路径是否已包含此任务
        if (visited.contains(taskId)) {
            Map<String, Object> cycleNode = new LinkedHashMap<>();
            cycleNode.put("id", taskId);
            cycleNode.put("depth", depth);
            cycleNode.put("description", "[循环引用: 任务ID与祖先节点重复]");
            cycleNode.put("isCycleReference", true);
            cycleNode.put("children", new ArrayList<>());
            return cycleNode;
        }

        Map<String, Object> node = new LinkedHashMap<>();
        Map<String, Object> data = taskDataMap.get(taskId);

        node.put("id", taskId);
        node.put("depth", depth);

        if (data != null) {
            node.put("description", data.get("description"));
            node.put("action", data.get("action"));
            node.put("thought", data.get("thought"));
            node.put("iteration", data.get("iteration"));
        }

        // 获取子任务列表
        List<String> childIds = decisionChildrenMap.get(taskId);
        if (childIds != null && !childIds.isEmpty()) {
            // 创建新的 visited 集合用于子树（只检查当前路径）
            Set<String> childVisited = new HashSet<>(visited);
            childVisited.add(taskId);

            List<Map<String, Object>> children = new ArrayList<>();
            for (String childId : childIds) {
                children.add(buildTree(childId, taskDataMap, decisionChildrenMap, depth + 1, childVisited));
            }
            node.put("children", children);
        } else {
            node.put("children", new ArrayList<>());
        }

        return node;
    }

    /**
     * 计算最大深度
     */
    private int calculateMaxDepth(Map<String, Object> node) {
        int currentDepth = node.get("depth") != null ? (Integer) node.get("depth") : 0;
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
        if (children == null || children.isEmpty()) {
            return currentDepth;
        }
        int maxChildDepth = currentDepth;
        for (Map<String, Object> child : children) {
            maxChildDepth = Math.max(maxChildDepth, calculateMaxDepth(child));
        }
        return maxChildDepth;
    }
}