package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
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
 * 飞书通知发送器
 */
public class FeishuNotifier {

    private static final Logger logger = LoggerFactory.getLogger(FeishuNotifier.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String webhookUrl;
    private String reviewServerUrl = "http://localhost:8080";

    public FeishuNotifier(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void setReviewServerUrl(String reviewServerUrl) {
        this.reviewServerUrl = reviewServerUrl;
    }

    /**
     * 发送审批通知
     */
    public boolean sendReviewNotification(NodeExecutionContext context, WorkflowNode node,
                                          HumanReviewConfig config, String reviewToken) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            logger.warn("未配置飞书Webhook URL，跳过通知");
            return false;
        }

        try {
            Map<String, Object> message = buildMessage(context, node, config, reviewToken);
            return sendHttpRequest(message);
        } catch (Exception e) {
            logger.error("发送飞书通知异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建消息体
     */
    private Map<String, Object> buildMessage(NodeExecutionContext context, WorkflowNode node,
                                              HumanReviewConfig config, String reviewToken) {
        String approveUrl = buildReviewUrl(context, node, reviewToken, "approve");
        String rejectUrl = buildReviewUrl(context, node, reviewToken, "reject");

        Map<String, Object> card = new LinkedHashMap<>();
        card.put("header", buildHeader());
        card.put("elements", buildElements(context, node, config, approveUrl, rejectUrl));

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("msg_type", "interactive");
        message.put("card", card);
        return message;
    }

    /**
     * 构建消息头
     */
    private Map<String, Object> buildHeader() {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("title", createTextElement("工作流审批通知"));
        header.put("template", "blue");
        return header;
    }

    /**
     * 构建消息元素
     */
    private List<Map<String, Object>> buildElements(NodeExecutionContext context, WorkflowNode node,
                                                     HumanReviewConfig config, String approveUrl, String rejectUrl) {
        List<Map<String, Object>> elements = new ArrayList<>();
        elements.add(buildContentElement(context, node, config));
        elements.add(buildActionElement(approveUrl, rejectUrl));
        return elements;
    }

    /**
     * 构建内容元素
     */
    private Map<String, Object> buildContentElement(NodeExecutionContext context, WorkflowNode node,
                                                     HumanReviewConfig config) {
        StringBuilder contentText = new StringBuilder();
        contentText.append("**工作流**: ").append(context.getWorkflowName() != null ? context.getWorkflowName() : context.getWorkflowId()).append("\n");
        contentText.append("**节点**: ").append(node.getName()).append("\n");
        contentText.append("**审批人**: ").append(String.join(", ", config.getApprovers())).append("\n");
        contentText.append("**消息**: ").append(config.getMessage()).append("\n");
        contentText.append("**提交时间**: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        Map<String, Object> element = new LinkedHashMap<>();
        element.put("tag", "div");
        element.put("text", createLarkMdElement(contentText.toString()));
        return element;
    }

    /**
     * 构建按钮元素
     */
    private Map<String, Object> buildActionElement(String approveUrl, String rejectUrl) {
        List<Map<String, Object>> actions = new ArrayList<>();

        Map<String, Object> approveButton = new LinkedHashMap<>();
        approveButton.put("tag", "button");
        approveButton.put("text", createTextElement("通过"));
        approveButton.put("type", "primary");
        approveButton.put("url", approveUrl);
        actions.add(approveButton);

        Map<String, Object> rejectButton = new LinkedHashMap<>();
        rejectButton.put("tag", "button");
        rejectButton.put("text", createTextElement("拒绝"));
        rejectButton.put("type", "danger");
        rejectButton.put("url", rejectUrl);
        actions.add(rejectButton);

        Map<String, Object> actionElement = new LinkedHashMap<>();
        actionElement.put("tag", "action");
        actionElement.put("actions", actions);
        return actionElement;
    }

    /**
     * 构建审核链接
     */
    private String buildReviewUrl(NodeExecutionContext context, WorkflowNode node,
                                  String reviewToken, String action) {
        return String.format("%s/api/executions/%s/review/%s?token=%s&action=%s",
                reviewServerUrl, context.getExecutionId(), node.getId(), reviewToken, action);
    }

    /**
     * 发送HTTP请求
     */
    private boolean sendHttpRequest(Map<String, Object> message) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(message);

        HttpURLConnection connection = (HttpURLConnection) new URL(webhookUrl).openConnection();
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
}