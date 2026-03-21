package com.openclaw.workflow.engine.smartdecompose.v2.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecisionResponse;
import com.openclaw.workflow.engine.smartdecompose.v2.model.ReviewResponse;
import com.openclaw.workflow.engine.smartdecompose.v2.prompt.PromptOutputFormat;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 响应解析器
 *
 * 解析 OpenClaw 返回的 JSON 响应，提取和验证格式。
 * 支持 Markdown 包裹的 JSON 和纯 JSON。
 */
@Component
public class ResponseParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 公开方法 ====================

    /**
     * 解析决策响应
     *
     * @param rawResponse OpenClaw 返回的原始响应
     * @return 决策响应对象
     * @throws ResponseParseException 解析失败时抛出
     */
    public DecisionResponse parseDecision(String rawResponse) {
        String json = extractJson(rawResponse);
        try {
            DecisionResponse response = objectMapper.readValue(json, DecisionResponse.class);
            validateDecision(response);
            return response;
        } catch (Exception e) {
            throw new ResponseParseException("解析决策响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析审核响应
     *
     * @param rawResponse OpenClaw 返回的原始响应
     * @return 审核响应对象
     * @throws ResponseParseException 解析失败时抛出
     */
    public ReviewResponse parseReview(String rawResponse) {
        String json = extractJson(rawResponse);
        try {
            ReviewResponse response = objectMapper.readValue(json, ReviewResponse.class);
            validateReview(response);
            return response;
        } catch (Exception e) {
            throw new ResponseParseException("解析审核响应失败: " + e.getMessage(), e);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 从响应中提取 JSON
     *
     * 支持以下格式：
     * 1. ```json ... ``` (Markdown JSON 代码块)
     * 2. ``` ... ``` (Markdown 代码块)
     * 3. {...} (纯 JSON)
     *
     * @param response 原始响应
     * @return 提取的 JSON 字符串
     * @throws ResponseParseException 无法提取时抛出
     */
    private String extractJson(String response) {
        if (response == null || response.isEmpty()) {
            throw new ResponseParseException("响应内容为空");
        }

        // 处理 ```json ... ```
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }

        // 处理 ``` ... ```
        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            // 跳过可能的语言标识符行
            while (start < response.length() && response.charAt(start) != '\n') {
                start++;
            }
            if (start < response.length()) start++; // 跳过换行
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }

        // 直接提取第一个完整的 JSON 对象
        return extractFirstJsonObject(response);
    }

    /**
     * 提取第一个完整的 JSON 对象
     *
     * 通过括号匹配找到完整的 JSON 对象边界
     *
     * @param response 原始响应
     * @return 第一个完整的 JSON 对象字符串
     * @throws ResponseParseException 无法提取时抛出
     */
    private String extractFirstJsonObject(String response) {
        int start = -1;
        int depth = 0;

        for (int i = 0; i < response.length(); i++) {
            char c = response.charAt(i);

            if (c == '{') {
                if (start == -1) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    return response.substring(start, i + 1);
                }
            }
        }

        throw new ResponseParseException("无法从响应中提取 JSON: " +
            response.substring(0, Math.min(200, response.length())));
    }

    /**
     * 验证决策响应
     *
     * @param response 决策响应
     * @throws ResponseParseException 验证失败时抛出
     */
    private void validateDecision(DecisionResponse response) {
        // 实现思路：
        // 1. 检查 decision 字段是否存在且有效
        // 2. 检查 thought 字段是否存在
        // 3. 如果是 split，检查 tasks 是否非空
        // 4. 验证失败抛出 ResponseParseException

        if (response.getDecision() == null) {
            throw new ResponseParseException("决策响应缺少 decision 字段");
        }

        if (!PromptOutputFormat.VALID_DECISIONS.contains(response.getDecision())) {
            throw new ResponseParseException("decision 必须是 execute 或 split");
        }

        if (response.getThought() == null || response.getThought().isEmpty()) {
            throw new ResponseParseException("决策响应缺少 thought 字段");
        }

        if (response.isSplit()) {
            if (response.getTasks() == null || response.getTasks().isEmpty()) {
                throw new ResponseParseException("split 决策必须包含非空的 tasks 列表");
            }
        }
    }

    /**
     * 验证审核响应
     *
     * @param response 审核响应
     * @throws ResponseParseException 验证失败时抛出
     */
    private void validateReview(ReviewResponse response) {
        // 实现思路：
        // 1. 检查 status 字段是否存在且有效
        // 2. 检查 thought 字段是否存在
        // 3. 如果是 REJECTED，检查 issues 是否非空
        // 4. 验证失败抛出 ResponseParseException

        if (response.getStatus() == null) {
            throw new ResponseParseException("审核响应缺少 status 字段");
        }

        if (!PromptOutputFormat.VALID_REVIEW_STATUSES.contains(response.getStatus())) {
            throw new ResponseParseException("status 必须是 APPROVED 或 REJECTED");
        }

        if (response.getThought() == null || response.getThought().isEmpty()) {
            throw new ResponseParseException("审核响应缺少 thought 字段");
        }

        if (response.isRejected()) {
            if (response.getIssues() == null || response.getIssues().isEmpty()) {
                throw new ResponseParseException("REJECTED 审核必须包含非空的 issues 列表");
            }
        }
    }
}