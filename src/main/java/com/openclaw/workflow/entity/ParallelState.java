package com.openclaw.workflow.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 并行执行状态实体
 */
@Entity
@Table(name = "parallel_state")
public class ParallelState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false)
    private String executionId;

    @Column(name = "parallel_node_id", nullable = false)
    private String parallelNodeId;

    @Column(name = "branch_index")
    private Integer branchIndex = 0;

    @Column(name = "branch_status")
    private String branchStatus = "pending";

    @Column(name = "branch_node_id")
    private String branchNodeId;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public ParallelState() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public String getParallelNodeId() { return parallelNodeId; }
    public void setParallelNodeId(String parallelNodeId) { this.parallelNodeId = parallelNodeId; }

    public Integer getBranchIndex() { return branchIndex; }
    public void setBranchIndex(Integer branchIndex) { this.branchIndex = branchIndex; }

    public String getBranchStatus() { return branchStatus; }
    public void setBranchStatus(String branchStatus) { this.branchStatus = branchStatus; }

    public String getBranchNodeId() { return branchNodeId; }
    public void setBranchNodeId(String branchNodeId) { this.branchNodeId = branchNodeId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}