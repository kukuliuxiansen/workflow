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
     */
    public boolean reviewAndRetry(DecomposeContext context, SubTask task, String executionResult) {
        int retryCount = 0;
        List<String> previousIssues = new ArrayList<>();

        while (retryCount <= context.getMaxRetries()) {
            String reviewPrompt = promptBuilder.buildReviewPrompt(context, task, executionResult, previousIssues);
            String rawReview = openClawClient.execute(reviewPrompt);
            ReviewResponse review = responseParser.parseReview(rawReview);

            if (review.isApproved()) {
                logger.info("审核通过: {}", review.getSummary());
                task.setReviewStatus(ReviewStatus.APPROVED);
                return true;
            }

            previousIssues = review.getIssues();
            task.setReviewStatus(ReviewStatus.REJECTED);
            task.setReviewIssues(previousIssues);
            retryCount++;
            task.setRetryCount(retryCount);

            logger.warn("审核拒绝 (第{}次): {}", retryCount, previousIssues);

            if (retryCount > context.getMaxRetries()) {
                // 使用 ReviewStrategy 判断是否需要人工审核
                ReviewStrategy strategy = extensionRegistry.getReviewStrategy();
                boolean needManualReview = context.isRequireManualReview();

                if (strategy != null) {
                    needManualReview = strategy.requireManualReview(task, retryCount);
                }

                if (needManualReview) {
                    return triggerManualReview(context, task, executionResult, review);
                }
                return false;
            }

            // 构建 retry 提示词（优先使用 ReviewStrategy）
            String retryPrompt;
            ReviewStrategy strategy = extensionRegistry.getReviewStrategy();
            if (strategy != null) {
                retryPrompt = strategy.buildRetryPrompt(task, previousIssues);
            } else {
                retryPrompt = promptBuilder.buildRetryPrompt(context, task, previousIssues);
            }
            executionResult = openClawClient.execute(retryPrompt);
        }

        return false;
    }

    /**
     * 触发人工审核
     */
    private boolean triggerManualReview(DecomposeContext context, SubTask task,
                                         String executionResult, ReviewResponse review) {
        String reviewId = UUID.randomUUID().toString();

        logger.warn("触发人工审核: manualReviewId={}, taskId={}", reviewId, task.getId());

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

        // 更新上下文状态
        context.setStatus(DecomposeStatus.WAITING_MANUAL_REVIEW);
        context.setManualReviewId(reviewId);

        return false;
    }
}