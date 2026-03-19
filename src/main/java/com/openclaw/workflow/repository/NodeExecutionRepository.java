package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.NodeExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NodeExecutionRepository extends JpaRepository<NodeExecution, String> {

    List<NodeExecution> findByExecutionId(String executionId);

    Optional<NodeExecution> findByExecutionIdAndNodeId(String executionId, String nodeId);

    List<NodeExecution> findByExecutionIdOrderByCreatedAtAsc(String executionId);
}