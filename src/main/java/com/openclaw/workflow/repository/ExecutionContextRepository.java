package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.ExecutionContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExecutionContextRepository extends JpaRepository<ExecutionContext, Long> {

    Optional<ExecutionContext> findByExecutionId(String executionId);

    void deleteByExecutionId(String executionId);
}