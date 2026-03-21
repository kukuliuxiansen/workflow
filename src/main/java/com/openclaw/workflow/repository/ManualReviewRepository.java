package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.ManualReviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 人工审核记录 Repository
 */
@Repository
public interface ManualReviewRepository extends JpaRepository<ManualReviewRecord, String> {

    /**
     * 按执行ID查询
     */
    List<ManualReviewRecord> findByExecutionId(String executionId);

    /**
     * 按状态查询
     */
    List<ManualReviewRecord> findByStatus(String status);

    /**
     * 查询等待中的审核
     */
    List<ManualReviewRecord> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * 按执行ID和任务ID查询
     */
    Optional<ManualReviewRecord> findByExecutionIdAndTaskId(String executionId, String taskId);
}