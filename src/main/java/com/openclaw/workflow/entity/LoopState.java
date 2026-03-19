package com.openclaw.workflow.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 循环执行状态实体
 */
@Entity
@Table(name = "loop_state")
public class LoopState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false)
    private String executionId;

    @Column(name = "loop_node_id", nullable = false)
    private String loopNodeId;

    @Column(name = "current_iteration")
    private Integer currentIteration = 0;

    @Column(name = "max_iterations")
    private Integer maxIterations;

    @Column(name = "loop_variable")
    private String loopVariable;

    private String status = "running";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public LoopState() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public String getLoopNodeId() { return loopNodeId; }
    public void setLoopNodeId(String loopNodeId) { this.loopNodeId = loopNodeId; }

    public Integer getCurrentIteration() { return currentIteration; }
    public void setCurrentIteration(Integer currentIteration) { this.currentIteration = currentIteration; }

    public Integer getMaxIterations() { return maxIterations; }
    public void setMaxIterations(Integer maxIterations) { this.maxIterations = maxIterations; }

    public String getLoopVariable() { return loopVariable; }
    public void setLoopVariable(String loopVariable) { this.loopVariable = loopVariable; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}