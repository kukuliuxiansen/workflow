package com.openclaw.workflow.engine.smartdecompose;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 子Agent结果解析器
 * 负责解析Agent返回的结果内容
 */
public class SubAgentResultParser {

    private static final Logger logger = LoggerFactory.getLogger(SubAgentResultParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析子Agent结果
     */
    @SuppressWarnings("unchecked")
    public SubAgentResult parse(String content) {
        if (content == null || content.isEmpty()) {
            return SubAgentResult.error("子Agent返回空内容");
        }

        String jsonStr = extractJson(content);

        if (jsonStr == null) {
            SubAgentResult result = new SubAgentResult();
            result.setSuccess(true);
            result.setStatus("success");
            result.setSummary(truncate(content, 500));
            result.setOutputs(new HashMap<>());
            return result;
        }

        try {
            Map<String, Object> data = objectMapper.readValue(jsonStr, Map.class);
            return parseFromMap(data, content);
        } catch (Exception e) {
            logger.warn("解析子Agent结果失败: {}", e.getMessage());
            return createFallbackResult(content);
        }
    }

    /**
     * 从Map解析结果
     */
    @SuppressWarnings("unchecked")
    private SubAgentResult parseFromMap(Map<String, Object> data, String originalContent) {
        SubAgentResult result = new SubAgentResult();

        String status = (String) data.getOrDefault("status", "success");
        result.setStatus(status);
        result.setSuccess("success".equalsIgnoreCase(status));

        String summary = (String) data.get("summary");
        if (summary == null) {
            summary = data.containsKey("result") ? String.valueOf(data.get("result")) : "任务完成";
        }
        result.setSummary(summary);

        Map<String, Object> outputs = (Map<String, Object>) data.get("outputs");
        if (outputs == null) {
            outputs = extractOtherFields(data);
        }
        result.setOutputs(outputs);

        return result;
    }

    /**
     * 提取非标准字段作为outputs
     */
    private Map<String, Object> extractOtherFields(Map<String, Object> data) {
        Map<String, Object> outputs = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (!"status".equals(key) && !"summary".equals(key) && !"recommendations".equals(key)) {
                outputs.put(key, entry.getValue());
            }
        }
        return outputs;
    }

    /**
     * 创建降级结果
     */
    private SubAgentResult createFallbackResult(String content) {
        SubAgentResult result = new SubAgentResult();
        result.setSuccess(true);
        result.setStatus("success");
        result.setSummary(truncate(content, 500));
        result.setOutputs(new HashMap<>());
        return result;
    }

    /**
     * 从内容中提取JSON
     */
    private String extractJson(String content) {
        // 尝试找到```json块
        int jsonStart = content.indexOf("```json");
        if (jsonStart >= 0) {
            int jsonEnd = content.indexOf("```", jsonStart + 7);
            if (jsonEnd > jsonStart) {
                return content.substring(jsonStart + 7, jsonEnd).trim();
            }
        }

        // 尝试找到```块
        jsonStart = content.indexOf("```");
        if (jsonStart >= 0) {
            int jsonEnd = content.indexOf("```", jsonStart + 3);
            if (jsonEnd > jsonStart) {
                String block = content.substring(jsonStart + 3, jsonEnd).trim();
                if (block.startsWith("{") || block.startsWith("[")) {
                    return block;
                }
            }
        }

        // 尝试找到{}块
        int braceStart = content.indexOf("{");
        if (braceStart >= 0) {
            int braceEnd = content.lastIndexOf("}");
            if (braceEnd > braceStart) {
                return content.substring(braceStart, braceEnd + 1);
            }
        }

        return null;
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}