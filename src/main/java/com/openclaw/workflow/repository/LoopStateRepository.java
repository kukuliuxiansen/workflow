package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.LoopState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoopStateRepository extends JpaRepository<LoopState, Long> {

    List<LoopState> findByExecutionId(String executionId);

    Optional<LoopState> findByExecutionIdAndLoopNodeId(String executionId, String loopNodeId);
}