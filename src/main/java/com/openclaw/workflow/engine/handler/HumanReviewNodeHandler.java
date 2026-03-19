package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_TIMEOUT = 86400; // 24小时

    // 飞书Webhook URL（需要在配置中设置）
    private String feishuWebhookUrl;

    // 审核服务器URL（用于生成审核链接）
    private String reviewServerUrl = "http://localhost:8080";

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        WorkflowNode node = context.getNode();
        ReviewConfig config = parseConfig(node);

        logger.info("执行人工审核节点: {} (审批人: {}, 超时: {}秒)",
                node.getName(), config.approvers, config.timeoutSeconds);

        // 生成审核Token
        String reviewToken = generateReviewToken(context, node);

        // 创建审核上下文
        NodeResult.ReviewContext reviewContext = new NodeResult.ReviewContext();
        reviewContext.setReviewNodeId(node.getId());
        reviewContext.setReviewToken(reviewToken);
        reviewContext.setApprovers(config.approvers);
        reviewContext.setMessage(config.message);
        reviewContext.setTimeoutSeconds(config.timeoutSeconds);
        reviewContext.setTimeoutAction(config.timeoutAction);

        // 发送飞书通知
        boolean notifySuccess = sendFeishuNotification(context, node, config, reviewToken);

        if (!notifySuccess) {
            logger.warn("飞书通知发送失败，请检查Webhook配置");
        }

        // 返回等待审核状态
        NodeResult result = NodeResult.waitingReview(reviewContext);
        result.addMetadata("reviewToken", reviewToken);
        result.addMetadata("approvers", config.approvers);
        result.addMetadata("message", config.message);
        result.addMetadata("timeoutSeconds", config.timeoutSeconds);
        result.addMetadata("notifySuccess", notifySuccess);

        return result;
    }

    /**
     * 发送飞书通知
     */
    private boolean sendFeishuNotification(NodeExecutionContext context, WorkflowNode node,
                                            ReviewConfig config, String reviewToken) {
        if (feishuWebhookUrl == null || feishuWebhookUrl.isEmpty()) {
            logger.warn("未配置飞书Webhook URL，跳过通知");
            return false;
        }

        try {
            // 构建审核链接
            String approveUrl = String.format("%s/api/executions/%s/review/%s?token=%s&action=approve",
                    reviewServerUrl, context.getExecutionId(), node.getId(), reviewToken);
            String rejectUrl = String.format("%s/api/executions/%s/review/%s?token=%s&action=reject",
                    reviewServerUrl, context.getExecutionId(), node.getId(), reviewToken);

            // 构建飞书消息卡片
            Map<String, Object> card = new LinkedHashMap<>();
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("title", createTextElement("工作流审批通知"));
            header.put("template", "blue");
            card.put("header", header);

            List<Map<String, Object>> elements = new ArrayList<>();

            // 内容区
            Map<String, Object> contentElement = new LinkedHashMap<>();
            contentElement.put("tag", "div");

            StringBuilder contentText = new StringBuilder();
            contentText.append("**工作流**: ").append(context.getWorkflowName() != null ? context.getWorkflowName() : context.getWorkflowId()).append("\n");
            contentText.append("**节点**: ").append(node.getName()).append("\n");
            contentText.append("**审批人**: ").append(String.join(", ", config.approvers)).append("\n");
            contentText.append("**消息**: ").append(config.message).append("\n");
            contentText.append("**提交时间**: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            contentElement.put("text", createLarkMdElement(contentText.toString()));
            elements.add(contentElement);

            // 按钮区
            Map<String, Object> actionElement = new LinkedHashMap<>();
            actionElement.put("tag", "action");
            List<Map<String, Object>> actions = new ArrayList<>();

            // 通过按钮
            Map<String, Object> approveButton = new LinkedHashMap<>();
            approveButton.put("tag", "button");
            approveButton.put("text", createTextElement("通过"));
            approveButton.put("type", "primary");
            approveButton.put("url", approveUrl);
            actions.add(approveButton);

            // 拒绝按钮
            Map<String, Object> rejectButton = new LinkedHashMap<>();
            rejectButton.put("tag", "button");
            rejectButton.put("text", createTextElement("拒绝"));
            rejectButton.put("type", "danger");
            rejectButton.put("url", rejectUrl);
            actions.add(rejectButton);

            actionElement.put("actions", actions);
            elements.add(actionElement);

            card.put("elements", elements);

            // 构建完整消息
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("msg_type", "interactive");
            message.put("card", card);

            // 发送HTTP请求
            String jsonBody = objectMapper.writeValueAsString(message);

            HttpURLConnection connection = (HttpURLConnection) new URL(feishuWebhookUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                logger.info("飞书通知发送成功");
                return true;
            } else {
                logger.error("飞书通知发送失败，响应码: {}", responseCode);
                return false;
            }

        } catch (Exception e) {
            logger.error("发送飞书通知异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 创建文本元素
     */
    private Map<String, Object> createTextElement(String content) {
        Map<String, Object> element = new LinkedHashMap<>();
        element.put("tag", "plain_text");
        element.put("content", content);
        return element;
    }

    /**
     * 创建Lark Markdown元素
     */
    private Map<String, Object> createLarkMdElement(String content) {
        Map<String, Object> element = new LinkedHashMap<>();
        element.put("tag", "lark_md");
        element.put("content", content);
        return element;
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

    /**
     * 解析节点配置
     */
    private ReviewConfig parseConfig(WorkflowNode node) {
        ReviewConfig config = new ReviewConfig();
        config.timeoutSeconds = DEFAULT_TIMEOUT;
        config.timeoutAction = "REJECT";
        config.approvalType = "ANY_ONE";

        try {
            if (node.getConfig() != null) {
                JsonNode jsonConfig = objectMapper.readTree(node.getConfig());

                // 解析审核配置
                if (jsonConfig.has("reviewConfig")) {
                    JsonNode reviewConfig = jsonConfig.get("reviewConfig");

                    if (reviewConfig.has("approvers") && reviewConfig.get("approvers").isArray()) {
                        config.approvers = new ArrayList<>();
                        for (JsonNode approver : reviewConfig.get("approvers")) {
                            config.approvers.add(approver.asText());
                        }
                    }

                    if (reviewConfig.has("approvalType")) {
                        config.approvalType = reviewConfig.get("approvalType").asText();
                    }

                    if (reviewConfig.has("timeoutSeconds")) {
                        config.timeoutSeconds = reviewConfig.get("timeoutSeconds").asInt();
                    }

                    if (reviewConfig.has("timeoutAction")) {
                        config.timeoutAction = reviewConfig.get("timeoutAction").asText();
                    }

                    if (reviewConfig.has("message")) {
                        config.message = reviewConfig.get("message").asText();
                    }
                }

                // 解析分支配置
                if (jsonConfig.has("branches")) {
                    JsonNode branches = jsonConfig.get("branches");
                    if (branches.has("on_approve")) {
                        config.onApprove = branches.get("on_approve").asText();
                    }
                    if (branches.has("on_reject")) {
                        config.onReject = branches.get("on_reject").asText();
                    }
                    if (branches.has("on_timeout")) {
                        config.onTimeout = branches.get("on_timeout").asText();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析人工审核节点配置失败: {}", e.getMessage(), e);
        }

        // 默认审批人
        if (config.approvers == null || config.approvers.isEmpty()) {
            config.approvers = Arrays.asList("admin");
        }

        // 默认消息
        if (config.message == null || config.message.isEmpty()) {
            config.message = "请审批";
        }

        return config;
    }

    // ==================== 内部类 ====================

    private static class ReviewConfig {
        List<String> approvers;
        String approvalType; // ANY_ONE, ALL, MAJORITY
        int timeoutSeconds;
        String timeoutAction; // REJECT, IGNORE
        String message;
        String onApprove;
        String onReject;
        String onTimeout;
    }

    // ==================== 配置方法 ====================

    public void setFeishuWebhookUrl(String feishuWebhookUrl) {
        this.feishuWebhookUrl = feishuWebhookUrl;
    }

    public void setReviewServerUrl(String reviewServerUrl) {
        this.reviewServerUrl = reviewServerUrl;
    }
}