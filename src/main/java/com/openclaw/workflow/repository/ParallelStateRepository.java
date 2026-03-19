package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.ParallelState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParallelStateRepository extends JpaRepository<ParallelState, Long> {

    List<ParallelState> findByExecutionId(String executionId);

    List<ParallelState> findByExecutionIdAndParallelNodeId(String executionId, String parallelNodeId);

    Optional<ParallelState> findByExecutionIdAndParallelNodeIdAndBranchIndex(
        String executionId, String parallelNodeId, Integer branchIndex);
}