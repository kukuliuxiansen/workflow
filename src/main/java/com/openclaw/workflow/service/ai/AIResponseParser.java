package com.openclaw.workflow.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.dto.WorkflowDto;
import com.openclaw.workflow.entity.Workflow;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.entity.WorkflowEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * AI 响应解析器
 */
public class AIResponseParser {

    private static final Logger logger = LoggerFactory.getLogger(AIResponseParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从响应中提取 JSON
     */
    public static String extractJson(String response) {
        String jsonStr = response;
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.lastIndexOf("```");
            jsonStr = response.substring(start, end).trim();
            if (jsonStr.startsWith("json")) {
                jsonStr = jsonStr.substring(4).trim();
            }
        }
        return jsonStr;
    }

    /**
     * 解析节点类型
     */
    public static WorkflowNode.NodeType parseNodeType(String type) {
        if (type == null) return WorkflowNode.NodeType.AGENT_EXECUTION;

        switch (type.toLowerCase()) {
            case "start": return WorkflowNode.NodeType.START;
            case "finish": case "end": return WorkflowNode.NodeType.FINISH;
            case "agent_execution": case "agent": return WorkflowNode.NodeType.AGENT_EXECUTION;
            case "api_call": case "api": return WorkflowNode.NodeType.API_CALL;
            case "condition": case "conditional": return WorkflowNode.NodeType.CONDITION;
            case "human_review": case "review": return WorkflowNode.NodeType.HUMAN_REVIEW;
            case "parallel": return WorkflowNode.NodeType.PARALLEL;
            case "loop": return WorkflowNode.NodeType.LOOP;
            default: return WorkflowNode.NodeType.AGENT_EXECUTION;
        }
    }

    /**
     * 生成友好的节点名称
     */
    public static String generateFriendlyName(String nodeId) {
        if (nodeId == null) return "未命名节点";

        String name = nodeId
                .replace("_", " ")
                .replace("-", " ");

        StringBuilder result = new StringBuilder();
        String[] words = name.split("\\s+");
        for (String word : words) {
            if (word.isEmpty()) continue;
            result.append(Character.toUpperCase(word.charAt(0)))
                   .append(word.substring(1))
                   .append(" ");
        }

        return result.toString().trim();
    }

    /**
     * 解析节点配置
     */
    @SuppressWarnings("unchecked")
    public static String parseNodeConfig(Map<String, Object> nodeData) {
        Map<String, Object> config = new HashMap<>();

        // 提取常用配置字段
        copyIfPresent(nodeData, config, "agentId", "prompt", "url", "method",
                "condition", "trueBranch", "falseBranch", "timeout", "reviewers");

        if (config.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            return null;
        }
    }

    private static void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String... keys) {
        for (String key : keys) {
            if (source.containsKey(key)) {
                target.put(key, source.get(key));
            }
        }
    }
}