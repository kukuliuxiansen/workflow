package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.AgentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentLogRepository extends JpaRepository<AgentLog, Long> {

    List<AgentLog> findByExecutionIdOrderByCreatedAtAsc(String executionId);

    List<AgentLog> findByExecutionIdAndNodeId(String executionId, String nodeId);
}