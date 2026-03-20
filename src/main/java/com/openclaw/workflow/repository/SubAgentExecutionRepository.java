package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.SubAgentExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 子Agent执行记录Repository
 */
@Repository
public interface SubAgentExecutionRepository extends JpaRepository<SubAgentExecution, String> {

    List<SubAgentExecution> findByParentExecutionId(String parentExecutionId);

    List<SubAgentExecution> findByParentExecutionIdAndParentNodeId(String parentExecutionId, String parentNodeId);

    Optional<SubAgentExecution> findByIdAndParentExecutionId(String id, String parentExecutionId);
}