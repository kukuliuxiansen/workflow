package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 节点配置解析工具
 */
public class NodeConfigParser {

    private static final Logger logger = LoggerFactory.getLogger(NodeConfigParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static JsonNode parseJson(String config) {
        if (config == null || config.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(config);
        } catch (Exception e) {
            logger.warn("解析配置失败: {}", e.getMessage());
            return null;
        }
    }

    public static String getString(JsonNode node, String field, String defaultValue) {
        if (node == null || !node.has(field)) {
            return defaultValue;
        }
        return node.get(field).asText(defaultValue);
    }

    public static int getInt(JsonNode node, String field, int defaultValue) {
        if (node == null || !node.has(field)) {
            return defaultValue;
        }
        return node.get(field).asInt(defaultValue);
    }

    public static boolean getBoolean(JsonNode node, String field, boolean defaultValue) {
        if (node == null || !node.has(field)) {
            return defaultValue;
        }
        return node.get(field).asBoolean(defaultValue);
    }

    public static JsonNode getNode(JsonNode node, String field) {
        if (node == null || !node.has(field)) {
            return null;
        }
        return node.get(field);
    }
}