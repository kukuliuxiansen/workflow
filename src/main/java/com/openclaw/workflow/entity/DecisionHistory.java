package com.openclaw.workflow.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 决策历史记录
 * 记录智能分解节点每一轮的决策过程
 */
@Entity
@Table(name = "decision_history")
@IdClass(DecisionHistoryId.class)
public class DecisionHistory {

    @Id
    @Column(name = "id")
    private String id;

    @Id
    @Column(name = "iteration")
    private Integer iteration;

    @Column(name = "execution_id", nullable = false)
    private String executionId;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Column(name = "task_id")
    private String taskId;

    // 思考内容
    @Column(name = "thought", columnDefinition = "TEXT")
    private String thought;

    // 执行的动作
    @Column(name = "action")
    private String action;

    // 动作参数（JSON）
    @Column(name = "action_parameters", columnDefinition = "TEXT")
    private String actionParameters;

    // 执行结果状态
    @Column(name = "result_status")
    private String resultStatus;

    // 执行结果消息
    @Column(name = "result_message", columnDefinition = "TEXT")
    private String resultMessage;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getIteration() { return iteration; }
    public void setIteration(Integer iteration) { this.iteration = iteration; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getThought() { return thought; }
    public void setThought(String thought) { this.thought = thought; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getActionParameters() { return actionParameters; }
    public void setActionParameters(String actionParameters) { this.actionParameters = actionParameters; }

    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }

    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}