package com.openclaw.workflow.engine.smartdecompose.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.smartdecompose.v2.client.OpenClawClient;
import com.openclaw.workflow.engine.smartdecompose.v2.client.ResponseParser;
import com.openclaw.workflow.engine.smartdecompose.v2.extension.ExtensionRegistry;
import com.openclaw.workflow.engine.smartdecompose.v2.extension.ReviewStrategy;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecomposeContext;
import com.openclaw.workflow.engine.smartdecompose.v2.model.ReviewResponse;
import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;
import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.DecomposeStatus;
import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.ReviewStatus;
import com.openclaw.workflow.engine.smartdecompose.v2.prompt.PromptBuilder;
import com.openclaw.workflow.entity.ManualReviewRecord;
import com.openclaw.workflow.repository.ManualReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 审核处理器
 *
 * 负责任务执行的审核、重试和人工审核触发。
 */
@Component
public class ReviewProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ReviewProcessor.class);

    @Autowired
    private PromptBuilder promptBuilder;

    @Autowired
    private OpenClawClient openClawClient;

    @Autowired
    private ResponseParser responseParser;

    @Autowired
    private ManualReviewRepository manualReviewRepository;

    @Autowired
    private ExtensionRegistry extensionRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 审核与重试
     *
     * 入参: DecomposeContext context, SubTask task, String executionResult
     * 出参: boolean (true=审核通过, false=审核失败)
     */
    public boolean reviewAndRetry(DecomposeContext context, SubTask task, String executionResult) {
        logger.info("========================================");
        logger.info("[REVIEW] ===== 审核阶段开始 =====");
        logger.info("[REVIEW] 入参 - taskId: {}", task.getId());
        logger.info("[REVIEW] 入参 - taskDescription: {}", truncate(task.getDescription(), 100));
        logger.info("[REVIEW] 入参 - executionResult: {}", truncate(executionResult, 200));
        logger.info("[REVIEW] 入参 - maxRetries: {}", context.getMaxRetries());
        logger.info("[REVIEW] 入参 - requireManualReview: {}", context.isRequireManualReview());

        int retryCount = 0;
        List<String> previousIssues = new ArrayList<>();
        ReviewStrategy strategy = extensionRegistry.getReviewStrategy();
        logger.info("[REVIEW] ReviewStrategy: {}", strategy != null ? strategy.getName() : "null");

        while (retryCount <= context.getMaxRetries()) {
            logger.info("[REVIEW] ----- 第 {} 次审核 -----", retryCount + 1);

            String reviewPrompt = promptBuilder.buildReviewPrompt(context, task, executionResult, previousIssues);
            logger.info("[REVIEW] 审核提示词长度: {} 字符", reviewPrompt.length());
            logger.debug("[REVIEW] 审核提示词:\n{}", truncate(reviewPrompt, 500));

            logger.info("[REVIEW] 调用 OpenClaw 进行审核...");
            long startTime = System.currentTimeMillis();
            String rawReview = openClawClient.execute(reviewPrompt);
            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("[REVIEW] OpenClaw 审核响应 (耗时 {}ms):", elapsed);
            logger.info("[REVIEW] 原始响应:\n{}", rawReview);

            ReviewResponse review = responseParser.parseReview(rawReview);
            logger.info("[REVIEW] 解析结果: status={}, thought={}",
                review.getStatus(), truncate(review.getThought(), 100));

            if (review.isApproved()) {
                logger.info("[REVIEW] 审核通过: {}", review.getSummary());
                task.setReviewStatus(ReviewStatus.APPROVED);
                logger.info("[REVIEW] ===== 审核阶段结束(通过) =====");
                logger.info("========================================");
                return true;
            }

            previousIssues = review.getIssues();
            task.setReviewStatus(ReviewStatus.REJECTED);
            task.setReviewIssues(previousIssues);
            retryCount++;
            task.setRetryCount(retryCount);

            logger.warn("[REVIEW] 审核拒绝 (第{}次): {}", retryCount, previousIssues);

            if (retryCount > context.getMaxRetries()) {
                // 使用 ReviewStrategy 判断是否需要人工审核
                boolean needManualReview = context.isRequireManualReview();
                if (strategy != null) {
                    needManualReview = strategy.requireManualReview(task, retryCount);
                    logger.info("[REVIEW] ReviewStrategy 判断需要人工审核: {}", needManualReview);
                }

                if (needManualReview) {
                    logger.warn("[REVIEW] 触发人工审核...");
                    boolean result = triggerManualReview(context, task, executionResult, review);
                    logger.info("[REVIEW] ===== 审核阶段结束(人工审核) =====");
                    logger.info("========================================");
                    return result;
                }
                logger.info("[REVIEW] ===== 审核阶段结束(失败) =====");
                logger.info("========================================");
                return false;
            }

            // 构建 retry 提示词（优先使用 ReviewStrategy）
            String retryPrompt;
            if (strategy != null) {
                retryPrompt = strategy.buildRetryPrompt(task, previousIssues);
                logger.info("[REVIEW] 使用 ReviewStrategy 构建重试提示词");
            } else {
                retryPrompt = promptBuilder.buildRetryPrompt(context, task, previousIssues);
                logger.info("[REVIEW] 使用 PromptBuilder 构建重试提示词");
            }
            logger.info("[REVIEW] 重试提示词长度: {} 字符", retryPrompt.length());

            logger.info("[REVIEW] 调用 OpenClaw 重新执行...");
            executionResult = openClawClient.execute(retryPrompt);
            logger.info("[REVIEW] 重试执行结果:\n{}", truncate(executionResult, 200));
        }

        logger.info("[REVIEW] ===== 审核阶段结束(失败) =====");
        logger.info("========================================");
        return false;
    }

    /**
     * 触发人工审核
     */
    private boolean triggerManualReview(DecomposeContext context, SubTask task,
                                         String executionResult, ReviewResponse review) {
        String reviewId = UUID.randomUUID().toString();

        logger.info("[MANUAL_REVIEW] ===== 触发人工审核 =====");
        logger.info("[MANUAL_REVIEW] reviewId: {}", reviewId);
        logger.info("[MANUAL_REVIEW] taskId: {}", task.getId());
        logger.info("[MANUAL_REVIEW] executionId: {}", context.getExecutionId());
        logger.info("[MANUAL_REVIEW] issues: {}", review.getIssues());

        // 创建人工审核记录
        ManualReviewRecord record = new ManualReviewRecord();
        record.setId(reviewId);
        record.setExecutionId(context.getExecutionId());
        record.setNodeId(context.getNodeId());
        record.setTaskId(task.getId());
        record.setTaskDescription(task.getDescription());
        record.setExecutionResult(executionResult);

        try {
            record.setReviewIssues(objectMapper.writeValueAsString(review.getIssues()));
        } catch (JsonProcessingException e) {
            record.setReviewIssues("[]");
        }

        manualReviewRepository.save(record);
        logger.info("[MANUAL_REVIEW] 人工审核记录已保存");

        // 更新上下文状态
        context.setStatus(DecomposeStatus.WAITING_MANUAL_REVIEW);
        context.setManualReviewId(reviewId);

        logger.info("[MANUAL_REVIEW] ===== 人工审核已触发 =====");
        return false;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "null";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...";
    }
}