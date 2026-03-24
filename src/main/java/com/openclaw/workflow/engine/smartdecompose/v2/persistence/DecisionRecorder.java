package com.openclaw.workflow.engine.smartdecompose.v2.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecomposeContext;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecisionResponse;
import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;
import com.openclaw.workflow.entity.DecisionHistory;
import com.openclaw.workflow.entity.DecisionHistoryId;
import com.openclaw.workflow.repository.DecisionHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 决策记录器
 *
 * 记录每次决策的历史到数据库，用于调试和分析。
 */
@Component
public class DecisionRecorder {

    @Autowired
    private DecisionHistoryRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 记录决策
     *
     * 将决策信息保存到决策历史表
     *
     * @param context  执行上下文
     * @param task     当前任务
     * @param response 决策响应
     * @param prompt   发送给Agent的提示词
     * @param rawResponse Agent的原始响应
     */
    public void record(DecomposeContext context, SubTask task, DecisionResponse response, String prompt, String rawResponse) {
        DecisionHistory history = new DecisionHistory();
        history.setId(UUID.randomUUID().toString());
        history.setIteration(context.getIterationCount());
        history.setExecutionId(context.getExecutionId());
        history.setNodeId(context.getNodeId());
        history.setTaskId(task.getId());
        history.setThought(response.getThought());
        history.setAction(response.getDecision());
        history.setTimestamp(LocalDateTime.now());
        history.setPrompt(prompt);
        history.setRawResponse(rawResponse);

        if (response.isExecute()) {
            history.setResultStatus("EXECUTE");
            history.setResultMessage(response.getResult());
        } else {
            history.setResultStatus("SPLIT");
            try {
                history.setResultMessage(objectMapper.writeValueAsString(response.getTasks()));
            } catch (JsonProcessingException e) {
                history.setResultMessage("[]");
            }
        }

        repository.save(history);
    }

    /**
     * 记录决策（兼容旧调用）
     */
    public void record(DecomposeContext context, SubTask task, DecisionResponse response) {
        record(context, task, response, null, null);
    }

    /**
     * 查询执行的决策历史
     *
     * @param executionId 执行ID
     * @param nodeId      节点ID
     * @return 决策历史列表
     */
    public List<DecisionHistory> findByExecutionId(String executionId, String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return repository.findByExecutionIdOrderByIterationAsc(executionId);
        }
        return repository.findByExecutionIdAndNodeIdOrderByIterationAsc(executionId, nodeId);
    }

    /**
     * 删除执行的决策历史
     *
     * @param executionId 执行ID
     */
    public void deleteByExecutionId(String executionId) {
        repository.deleteByExecutionId(executionId);
    }

    /**
     * 查询任务的决策历史（按时间倒序）
     *
     * @param executionId 执行ID
     * @param taskId      任务ID
     * @return 决策历史列表
     */
    public List<DecisionHistory> findByExecutionIdAndTaskId(String executionId, String taskId) {
        return repository.findByExecutionIdAndTaskIdOrderByIterationDesc(executionId, taskId);
    }
}