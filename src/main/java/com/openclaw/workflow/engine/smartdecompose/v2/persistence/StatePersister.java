package com.openclaw.workflow.engine.smartdecompose.v2.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecomposeContext;
import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;
import com.openclaw.workflow.entity.SmartDecomposeState;
import com.openclaw.workflow.repository.SmartDecomposeStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * 状态持久化器
 *
 * 保存和恢复 DecomposeContext 的执行状态。
 * 支持中断后恢复执行。
 */
@Component
public class StatePersister {

    @Autowired
    private SmartDecomposeStateRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 保存执行状态
     *
     * 将 DecomposeContext 序列化并保存到数据库
     *
     * @param context 执行上下文
     */
    public void save(DecomposeContext context) {
        SmartDecomposeState state = repository
            .findByExecutionIdAndNodeId(context.getExecutionId(), context.getNodeId())
            .orElse(new SmartDecomposeState());

        state.setExecutionId(context.getExecutionId());
        state.setNodeId(context.getNodeId());
        state.setStatus(context.getStatus().name());
        state.setErrorMessage(context.getErrorMessage());
        state.setCurrentIteration(context.getIterationCount());
        state.setMaxIterations(context.getMaxIterations());
        state.setTaskStack(serializeDeque(context.getTaskQueue()));
        state.setCompletedTasks(serializeList(context.getCompletedTasks()));
        state.setCurrentTaskId(context.getCurrentTask() != null ? context.getCurrentTask().getId() : null);

        if (state.getId() == null) {
            state.setId(context.getExecutionId() + "_" + context.getNodeId());
            state.setCreatedAt(LocalDateTime.now());
        }
        state.setUpdatedAt(LocalDateTime.now());

        repository.save(state);
    }

    /**
     * 加载执行状态
     *
     * 从数据库恢复 DecomposeContext
     *
     * @param executionId 执行ID
     * @param nodeId      节点ID
     * @return 恢复的执行上下文
     * @throws IllegalStateException 状态不存在时抛出
     */
    public DecomposeContext load(String executionId, String nodeId) {
        SmartDecomposeState state = repository
            .findByExecutionIdAndNodeId(executionId, nodeId)
            .orElseThrow(() -> new IllegalStateException("执行状态不存在: " + executionId));

        DecomposeContext context = new DecomposeContext();
        context.setExecutionId(state.getExecutionId());
        context.setNodeId(state.getNodeId());
        context.setStatus(com.openclaw.workflow.engine.smartdecompose.v2.model.enums.DecomposeStatus.valueOf(state.getStatus()));
        context.setErrorMessage(state.getErrorMessage());
        context.setIterationCount(state.getCurrentIteration());
        context.setMaxIterations(state.getMaxIterations());
        context.setTaskQueue(deserializeDeque(state.getTaskStack()));
        context.setCompletedTasks(deserializeList(state.getCompletedTasks()));

        return context;
    }

    /**
     * 删除执行状态
     *
     * @param executionId 执行ID
     */
    public void delete(String executionId) {
        repository.deleteByExecutionId(executionId);
    }

    /**
     * 序列化任务队列
     *
     * @param deque 任务队列
     * @return JSON 字符串
     */
    private String serializeDeque(Deque<SubTask> deque) {
        try {
            return objectMapper.writeValueAsString(new ArrayList<>(deque));
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 反序列化任务队列
     *
     * @param json JSON 字符串
     * @return 任务队列
     */
    private Deque<SubTask> deserializeDeque(String json) {
        if (json == null || json.isEmpty()) {
            return new LinkedList<>();
        }
        try {
            List<SubTask> list = objectMapper.readValue(json, new TypeReference<List<SubTask>>() {});
            return new LinkedList<>(list);
        } catch (Exception e) {
            return new LinkedList<>();
        }
    }

    /**
     * 序列化任务列表
     *
     * @param tasks 任务列表
     * @return JSON 字符串
     */
    private String serializeList(List<SubTask> tasks) {
        try {
            return objectMapper.writeValueAsString(tasks);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 反序列化任务列表
     *
     * @param json JSON 字符串
     * @return 任务列表
     */
    private List<SubTask> deserializeList(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<SubTask>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}