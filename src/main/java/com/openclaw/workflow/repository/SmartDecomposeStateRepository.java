package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.SmartDecomposeState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 智能分解状态Repository
 */
@Repository
public interface SmartDecomposeStateRepository extends JpaRepository<SmartDecomposeState, String> {

    Optional<SmartDecomposeState> findByExecutionIdAndNodeId(String executionId, String nodeId);

    void deleteByExecutionId(String executionId);
}