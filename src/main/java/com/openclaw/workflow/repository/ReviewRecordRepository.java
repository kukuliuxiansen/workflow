package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.ReviewRecord;
import com.openclaw.workflow.entity.ReviewRecord.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, String> {

    List<ReviewRecord> findByStatus(ReviewStatus status);

    List<ReviewRecord> findByExecutionId(String executionId);

    List<ReviewRecord> findByExecutionIdOrderByCreatedAtDesc(String executionId);

    Optional<ReviewRecord> findByExecutionIdAndNodeId(String executionId, String nodeId);

    List<ReviewRecord> findByExecutionIdAndNodeIdOrderByCreatedAtDesc(String executionId, String nodeId);

    List<ReviewRecord> findAllByOrderBySubmitTimeDesc();

    List<ReviewRecord> findByReviewer(String reviewer);

    List<ReviewRecord> findByNodeId(String nodeId);

    void deleteByExecutionId(String executionId);
}