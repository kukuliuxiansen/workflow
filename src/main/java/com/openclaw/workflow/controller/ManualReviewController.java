package com.openclaw.workflow.controller;

import com.openclaw.workflow.dto.ApiResponse;
import com.openclaw.workflow.dto.ManualReviewRequest;
import com.openclaw.workflow.engine.smartdecompose.v2.DecomposeOrchestrator;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecomposeContext;
import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.DecomposeStatus;
import com.openclaw.workflow.engine.smartdecompose.v2.persistence.StatePersister;
import com.openclaw.workflow.entity.ManualReviewRecord;
import com.openclaw.workflow.repository.ManualReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 人工审核控制器
 */
@RestController
@RequestMapping("/api/manual-reviews")
public class ManualReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ManualReviewController.class);

    @Autowired
    private ManualReviewRepository manualReviewRepository;

    @Autowired
    private StatePersister statePersister;

    @Autowired
    private DecomposeOrchestrator orchestrator;

    /**
     * 获取等待审核的列表
     */
    @GetMapping("/waiting")
    public ResponseEntity<List<ManualReviewRecord>> getWaitingReviews() {
        List<ManualReviewRecord> reviews = manualReviewRepository
            .findByStatusOrderByCreatedAtDesc(ManualReviewRecord.STATUS_WAITING);
        return ResponseEntity.ok(reviews);
    }

    /**
     * 获取审核详情
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<?> getReview(@PathVariable String reviewId) {
        return manualReviewRepository.findById(reviewId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 处理人工审核
     */
    @PostMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleReview(
            @PathVariable String reviewId,
            @RequestBody ManualReviewRequest request) {

        logger.info("处理人工审核: reviewId={}, action={}", reviewId, request.getAction());

        // 1. 查找审核记录
        ManualReviewRecord record = manualReviewRepository.findById(reviewId).orElse(null);

        if (record == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.badRequest("审核记录不存在: " + reviewId));
        }

        if (!record.isWaiting()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.badRequest("审核记录已处理: " + record.getStatus()));
        }

        // 2. 更新审核记录
        if (request.isApprove()) {
            record.setStatus(ManualReviewRecord.STATUS_APPROVED);
        } else if (request.isReject()) {
            record.setStatus(ManualReviewRecord.STATUS_REJECTED);
        } else {
            return ResponseEntity.badRequest()
                .body(ApiResponse.badRequest("无效的操作: " + request.getAction()));
        }

        record.setReviewer(request.getReviewer());
        record.setComment(request.getComment());
        record.setReviewedAt(LocalDateTime.now());

        manualReviewRepository.save(record);

        // 3. 如果通过，恢复执行
        if (record.isApproved()) {
            try {
                resumeExecution(record);
            } catch (Exception e) {
                logger.error("恢复执行失败: {}", e.getMessage(), e);
                Map<String, Object> result = new HashMap<>();
                result.put("reviewId", reviewId);
                result.put("status", record.getStatus());
                result.put("error", e.getMessage());
                return ResponseEntity.ok(ApiResponse.success(result));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("reviewId", reviewId);
        result.put("status", record.getStatus());

        logger.info("人工审核完成: reviewId={}, status={}", reviewId, record.getStatus());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 恢复执行
     */
    private void resumeExecution(ManualReviewRecord record) {
        logger.info("恢复执行: executionId={}", record.getExecutionId());

        // 加载执行状态
        DecomposeContext context = statePersister.load(record.getExecutionId(), record.getNodeId());

        // 恢复状态为运行中
        context.setStatus(DecomposeStatus.RUNNING);

        // 恢复 OpenClaw 会话
        if (context.getOpenClawSessionId() != null) {
            orchestrator.setOpenClawSessionId(context.getOpenClawSessionId());
            logger.debug("恢复 OpenClaw 会话: sessionId={}", context.getOpenClawSessionId());
        }

        // 重新执行
        orchestrator.run(context);
    }

    /**
     * 按执行ID查询审核记录
     */
    @GetMapping("/execution/{executionId}")
    public ResponseEntity<List<ManualReviewRecord>> getByExecutionId(@PathVariable String executionId) {
        List<ManualReviewRecord> reviews = manualReviewRepository.findByExecutionId(executionId);
        return ResponseEntity.ok(reviews);
    }
}