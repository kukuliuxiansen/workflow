package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.ExecutionRecord;
import com.openclaw.workflow.entity.ExecutionRecord.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, String> {

    List<ExecutionRecord> findByWorkflowIdOrderByCreatedAtDesc(String workflowId);

    List<ExecutionRecord> findByStatus(ExecutionStatus status);

    List<ExecutionRecord> findAllByOrderByCreatedAtDesc();

    long countByWorkflowId(String workflowId);
}