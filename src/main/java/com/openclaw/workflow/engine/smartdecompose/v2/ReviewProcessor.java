package com.openclaw.workflow.engine.smartdecompose.v2;

import com.openclaw.workflow.engine.smartdecompose.v2.client.OpenClawClient;
import com.openclaw.workflow.engine.smartdecompose.v2.client.ResponseParser;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecomposeContext;
import com.openclaw.workflow.engine.smartdecompose.v2.model.ReviewResponse;
import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;
import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.DecomposeStatus;
import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.ReviewStatus;
import com.openclaw.workflow.engine.smartdecompose.v2.prompt.PromptBuilder;
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

    /**
     * 审核与重试
     *
     * @param context         执行上下文
     * @param task            当前任务
     * @param executionResult 执行结果
     * @return true 表示审核通过，false 表示失败
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
                if (context.isRequireManualReview()) {
                    return triggerManualReview(context, task, executionResult, review);
                }
                return false;
            }

            String retryPrompt = promptBuilder.buildRetryPrompt(context, task, previousIssues);
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

        context.setStatus(DecomposeStatus.WAITING_MANUAL_REVIEW);
        context.setManualReviewId(reviewId);

        return false;
    }
}