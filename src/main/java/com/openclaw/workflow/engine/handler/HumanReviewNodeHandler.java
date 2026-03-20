package com.openclaw.workflow.engine.handler;

import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 人工审核节点处理器
 *
 * 功能：
 * 1. 创建审核记录
 * 2. 发送飞书通知
 * 3. 等待人工审批
 * 4. 根据审批结果决定后续流程
 *
 * 审批回调API:
 * POST /api/executions/{executionId}/review/{nodeId}
 * {
 *   "token": "review_token_xxx",
 *   "action": "approve", // approve | reject
 *   "comment": "审批意见",
 *   "reviewer": "user_001"
 * }
 */
public class HumanReviewNodeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(HumanReviewNodeHandler.class);

    private FeishuNotifier feishuNotifier;

    public HumanReviewNodeHandler() {
    }

    public HumanReviewNodeHandler(String feishuWebhookUrl) {
        this.feishuNotifier = new FeishuNotifier(feishuWebhookUrl);
    }

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        WorkflowNode node = context.getNode();
        HumanReviewConfig config = HumanReviewConfigParser.parse(node);

        logger.info("执行人工审核节点: {} (审批人: {}, 超时: {}秒)",
                node.getName(), config.getApprovers(), config.getTimeoutSeconds());

        // 生成审核Token
        String reviewToken = generateReviewToken(context, node);

        // 创建审核上下文
        NodeResult.ReviewContext reviewContext = new NodeResult.ReviewContext();
        reviewContext.setReviewNodeId(node.getId());
        reviewContext.setReviewToken(reviewToken);
        reviewContext.setApprovers(config.getApprovers());
        reviewContext.setMessage(config.getMessage());
        reviewContext.setTimeoutSeconds(config.getTimeoutSeconds());
        reviewContext.setTimeoutAction(config.getTimeoutAction());

        // 发送飞书通知
        boolean notifySuccess = sendNotification(context, node, config, reviewToken);

        if (!notifySuccess) {
            logger.warn("飞书通知发送失败，请检查Webhook配置");
        }

        // 返回等待审核状态
        NodeResult result = NodeResult.waitingReview(reviewContext);
        result.addMetadata("reviewToken", reviewToken);
        result.addMetadata("approvers", config.getApprovers());
        result.addMetadata("message", config.getMessage());
        result.addMetadata("timeoutSeconds", config.getTimeoutSeconds());
        result.addMetadata("notifySuccess", notifySuccess);

        return result;
    }

    /**
     * 发送通知（兼容新旧配置方式）
     */
    private boolean sendNotification(NodeExecutionContext context, WorkflowNode node,
                                     HumanReviewConfig config, String reviewToken) {
        if (feishuNotifier == null) {
            logger.warn("未配置飞书通知器");
            return false;
        }
        return feishuNotifier.sendReviewNotification(context, node, config, reviewToken);
    }

    /**
     * 生成审核Token
     */
    private String generateReviewToken(NodeExecutionContext context, WorkflowNode node) {
        // 简单的Token生成（生产环境应使用更安全的JWT）
        String data = context.getExecutionId() + ":" + node.getId() + ":" + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验证审核Token
     */
    public static boolean validateToken(String token, String executionId, String nodeId) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            return parts.length >= 2 && parts[0].equals(executionId) && parts[1].equals(nodeId);
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 配置方法 ====================

    public void setFeishuWebhookUrl(String feishuWebhookUrl) {
        this.feishuNotifier = new FeishuNotifier(feishuWebhookUrl);
    }

    public void setReviewServerUrl(String reviewServerUrl) {
        if (this.feishuNotifier != null) {
            this.feishuNotifier.setReviewServerUrl(reviewServerUrl);
        }
    }
}