package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 人工审核节点配置解析器
 */
public class HumanReviewConfigParser {

    private static final Logger logger = LoggerFactory.getLogger(HumanReviewConfigParser.class);
    private static final int DEFAULT_TIMEOUT = 86400; // 24小时

    /**
     * 解析节点配置
     */
    public static HumanReviewConfig parse(WorkflowNode node) {
        HumanReviewConfig config = new HumanReviewConfig();
        config.setTimeoutSeconds(DEFAULT_TIMEOUT);
        config.setTimeoutAction("REJECT");
        config.setApprovalType("ANY_ONE");

        JsonNode jsonConfig = NodeConfigParser.parseJson(node.getConfig());
        if (jsonConfig == null) {
            applyDefaults(config);
            return config;
        }

        parseReviewConfig(jsonConfig, config);
        parseBranches(jsonConfig, config);
        applyDefaults(config);

        return config;
    }

    /**
     * 解析审核配置
     */
    private static void parseReviewConfig(JsonNode jsonConfig, HumanReviewConfig config) {
        JsonNode reviewConfig = NodeConfigParser.getNode(jsonConfig, "reviewConfig");
        if (reviewConfig == null) {
            return;
        }

        // 解析审批人列表
        if (reviewConfig.has("approvers") && reviewConfig.get("approvers").isArray()) {
            List<String> approvers = new ArrayList<>();
            for (JsonNode approver : reviewConfig.get("approvers")) {
                approvers.add(approver.asText());
            }
            config.setApprovers(approvers);
        }

        config.setApprovalType(NodeConfigParser.getString(reviewConfig, "approvalType", config.getApprovalType()));
        config.setTimeoutSeconds(NodeConfigParser.getInt(reviewConfig, "timeoutSeconds", config.getTimeoutSeconds()));
        config.setTimeoutAction(NodeConfigParser.getString(reviewConfig, "timeoutAction", config.getTimeoutAction()));
        config.setMessage(NodeConfigParser.getString(reviewConfig, "message", null));
    }

    /**
     * 解析分支配置
     */
    private static void parseBranches(JsonNode jsonConfig, HumanReviewConfig config) {
        JsonNode branches = NodeConfigParser.getNode(jsonConfig, "branches");
        if (branches == null) {
            return;
        }

        config.setOnApprove(NodeConfigParser.getString(branches, "on_approve", null));
        config.setOnReject(NodeConfigParser.getString(branches, "on_reject", null));
        config.setOnTimeout(NodeConfigParser.getString(branches, "on_timeout", null));
    }

    /**
     * 应用默认值
     */
    private static void applyDefaults(HumanReviewConfig config) {
        if (config.getApprovers() == null || config.getApprovers().isEmpty()) {
            config.setApprovers(Arrays.asList("admin"));
        }
        if (config.getMessage() == null || config.getMessage().isEmpty()) {
            config.setMessage("请审批");
        }
    }
}