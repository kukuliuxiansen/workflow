package com.openclaw.workflow.engine.smartdecompose;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.entity.SmartDecomposeState;
import com.openclaw.workflow.repository.SmartDecomposeStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * 智能分解状态管理器
 */
public class DecomposeStateManager {

    private static final Logger logger = LoggerFactory.getLogger(DecomposeStateManager.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SmartDecomposeStateRepository stateRepository;

    public DecomposeStateManager(SmartDecomposeStateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    public void saveState(DecomposeContext context) {
        if (stateRepository == null) return;

        try {
            SmartDecomposeState state = new SmartDecomposeState();
            state.setId(context.getExecutionId() + "_" + context.getNodeId());
            state.setExecutionId(context.getExecutionId());
            state.setNodeId(context.getNodeId());
            state.setStatus(context.getStatus().name());
            state.setErrorMessage(context.getErrorMessage());
            state.setCurrentIteration(context.getIterationCount());
            state.setMaxIterations(context.getMaxIterations());
            state.setCurrentDepth(context.getCurrentTask() != null ? context.getCurrentTask().getDepth() : 0);
            state.setMaxDepth(context.getMaxDepth());
            state.setCurrentTaskId(context.getCurrentTask() != null ? context.getCurrentTask().getTaskId() : null);

            if (!context.getTaskStack().isEmpty()) {
                state.setTaskStack(objectMapper.writeValueAsString(context.getTaskStack()));
            }

            if (!context.getCompletedTasks().isEmpty()) {
                state.setCompletedTasks(objectMapper.writeValueAsString(context.getCompletedTasks()));
            }

            if (!context.getTaskMap().isEmpty()) {
                state.setTaskMap(objectMapper.writeValueAsString(context.getTaskMap()));
            }

            if (!context.getDecisionHistory().isEmpty()) {
                state.setDecisionHistory(objectMapper.writeValueAsString(context.getDecisionHistory()));
            }

            state.setUpdatedAt(LocalDateTime.now());
            if (state.getCreatedAt() == null) {
                state.setCreatedAt(LocalDateTime.now());
            }

            stateRepository.save(state);
            logger.debug("状态已保存: executionId={}, status={}", context.getExecutionId(), state.getStatus());
        } catch (Exception e) {
            logger.warn("保存状态失败: {}", e.getMessage());
        }
    }

    public DecomposeContext loadState(String executionId, String nodeId) {
        if (stateRepository == null) return null;

        try {
            String id = executionId + "_" + nodeId;
            SmartDecomposeState state = stateRepository.findById(id).orElse(null);
            if (state == null) return null;

            DecomposeContext context = new DecomposeContext();
            context.setExecutionId(state.getExecutionId());
            context.setNodeId(state.getNodeId());
            context.setStatus(DecomposeContext.DecomposeStatus.valueOf(state.getStatus()));
            context.setErrorMessage(state.getErrorMessage());
            context.setIterationCount(state.getCurrentIteration());
            context.setMaxIterations(state.getMaxIterations());
            context.setMaxDepth(state.getMaxDepth());

            if (state.getTaskStack() != null) {
                context.setTaskStack(objectMapper.readValue(state.getTaskStack(),
                    objectMapper.getTypeFactory().constructCollectionType(java.util.Stack.class, TaskState.class)));
            }

            return context;
        } catch (Exception e) {
            logger.warn("加载状态失败: {}", e.getMessage());
            return null;
        }
    }
}