package com.openclaw.workflow.engine.smartdecompose;

import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 子Agent执行器
 * 负责启动和管理子Agent的执行
 */
public class SubAgentExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SubAgentExecutor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String gatewayUrl;
    private final String gatewayToken;

    public SubAgentExecutor(String gatewayUrl, String gatewayToken) {
        this.gatewayUrl = gatewayUrl;
        this.gatewayToken = gatewayToken;
    }

    /**
     * 执行子Agent
     */
    public SubAgentResult execute(SubAgentRequest request) {
        logger.info(">>> 启动子Agent: type={}, prompt={}",
            request.getAgentType().getName(),
            truncate(request.getPrompt(), 100));

        long startTime = System.currentTimeMillis();

        try {
            // 1. 构建系统提示
            String systemPrompt = buildSystemPrompt(request.getAgentType());

            // 2. 构建用户提示
            String userPrompt = buildUserPrompt(request);

            // 3. 执行Agent
            OpenClawGatewayClient client = new OpenClawGatewayClient(gatewayUrl, gatewayToken);

            String agentId = getAgentId(request.getAgentType());
            String contextKey = "subagent_" + request.getParentExecutionId() + "_" + System.currentTimeMillis();

            logger.info("子Agent API请求: agentId={}, context={}", agentId, contextKey);

            OpenClawGatewayClient.AgentRequest agentRequest = OpenClawGatewayClient.AgentRequest.builder()
                .agentId(agentId)
                .systemPrompt(systemPrompt)
                .message(userPrompt)
                .context(contextKey)
                .maxTokens(2048)
                .temperature(0.7)
                .build();

            OpenClawGatewayClient.AgentResponse response = client.executeAgent(agentRequest);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("子Agent API响应: success={}, duration={}ms, tokens={}",
                response.isSuccess(), duration, response.getTotalTokens());

            if (!response.isSuccess()) {
                return SubAgentResult.error("子Agent执行失败: " + response.getErrorMessage());
            }

            // 4. 解析结果
            String content = response.getContent();
            logger.info("子Agent输出 (前500字符):\n{}", truncate(content, 500));

            SubAgentResult result = parseSubAgentResult(content);
            result.setTotalTokens(response.getTotalTokens());
            result.setDurationMs(duration);

            logger.info("子Agent执行完成: success={}, summary={}",
                result.isSuccess(), truncate(result.getSummary(), 100));

            return result;

        } catch (Exception e) {
            logger.error("子Agent执行异常: {}", e.getMessage(), e);
            return SubAgentResult.error("执行异常: " + e.getMessage());
        }
    }

    /**
     * 并行执行多个子Agent
     */
    public List<SubAgentResult> executeParallel(List<SubAgentRequest> requests) {
        logger.info("并行执行 {} 个子Agent", requests.size());

        List<SubAgentResult> results = new ArrayList<>();

        // 目前简化为顺序执行，后续可以改为真正的并行
        for (SubAgentRequest request : requests) {
            results.add(execute(request));
        }

        return results;
    }

    /**
     * 构建系统提示
     */
    private String buildSystemPrompt(SubAgentType agentType) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个专门的 ").append(agentType.getName()).append(" Agent。\n\n");
        prompt.append("## 职责\n\n");
        prompt.append(agentType.getDescription()).append("\n\n");
        prompt.append("## 使用场景\n\n");
        prompt.append(agentType.getUsageGuide()).append("\n\n");

        prompt.append("## 输出格式\n\n");
        prompt.append("完成任务后，请按以下JSON格式返回结果：\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"status\": \"success | failed | timeout\",\n");
        prompt.append("  \"summary\": \"任务完成摘要，简明扼要\",\n");
        prompt.append("  \"outputs\": {\n");
        prompt.append("    \"key1\": \"value1\",\n");
        prompt.append("    \"key2\": \"value2\"\n");
        prompt.append("  },\n");
        prompt.append("  \"recommendations\": [\n");
        prompt.append("    \"建议1\",\n");
        prompt.append("    \"建议2\"\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        prompt.append("## 限制\n\n");
        prompt.append("- 超时时间: ").append(agentType.getTimeoutMinutes()).append(" 分钟\n");
        prompt.append("- 只返回摘要信息，不要返回大量原始数据\n");
        prompt.append("- 确保输出是有效的JSON格式\n");

        return prompt.toString();
    }

    /**
     * 构建用户提示
     */
    private String buildUserPrompt(SubAgentRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("# 任务\n\n");
        prompt.append(request.getPrompt()).append("\n\n");

        if (request.getContext() != null && !request.getContext().isEmpty()) {
            prompt.append("# 上下文信息\n\n");
            for (Map.Entry<String, Object> entry : request.getContext().entrySet()) {
                prompt.append("- **").append(entry.getKey()).append("**: ")
                      .append(entry.getValue()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("请完成上述任务，并按要求返回JSON格式的结果。");

        return prompt.toString();
    }

    /**
     * 获取Agent ID
     */
    private String getAgentId(SubAgentType agentType) {
        // 根据类型返回对应的Agent ID
        // 这里使用默认的project-manager，实际使用时可以配置不同的Agent
        switch (agentType) {
            case EXPLORE:
                return "code-explorer";
            case PLAN:
                return "architect";
            case EXECUTE:
                return "developer";
            case TEST:
                return "tester";
            case REVIEW:
                return "code-reviewer";
            case DOCUMENT:
                return "documenter";
            default:
                return "project-manager";
        }
    }

    /**
     * 解析子Agent结果
     */
    @SuppressWarnings("unchecked")
    private SubAgentResult parseSubAgentResult(String content) {
        if (content == null || content.isEmpty()) {
            return SubAgentResult.error("子Agent返回空内容");
        }

        // 尝试提取JSON
        String jsonStr = extractJson(content);

        if (jsonStr == null) {
            // 如果没有找到JSON，将整个内容作为摘要
            SubAgentResult result = new SubAgentResult();
            result.setSuccess(true);
            result.setStatus("success");
            result.setSummary(truncate(content, 500));
            result.setOutputs(new HashMap<>());
            return result;
        }

        try {
            Map<String, Object> data = objectMapper.readValue(jsonStr, Map.class);

            SubAgentResult result = new SubAgentResult();

            // 解析status
            String status = (String) data.getOrDefault("status", "success");
            result.setStatus(status);
            result.setSuccess("success".equalsIgnoreCase(status));

            // 解析summary
            String summary = (String) data.get("summary");
            if (summary == null) {
                summary = data.containsKey("result") ? String.valueOf(data.get("result")) : "任务完成";
            }
            result.setSummary(summary);

            // 解析outputs
            Map<String, Object> outputs = (Map<String, Object>) data.get("outputs");
            if (outputs == null) {
                outputs = new HashMap<>();
                // 将其他字段也放入outputs
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    if (!"status".equals(entry.getKey()) &&
                        !"summary".equals(entry.getKey()) &&
                        !"recommendations".equals(entry.getKey())) {
                        outputs.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            result.setOutputs(outputs);

            return result;

        } catch (Exception e) {
            logger.warn("解析子Agent结果失败: {}", e.getMessage());

            // 解析失败，返回原始内容
            SubAgentResult result = new SubAgentResult();
            result.setSuccess(true);
            result.setStatus("success");
            result.setSummary(truncate(content, 500));
            result.setOutputs(new HashMap<>());
            return result;
        }
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
                // 检查是否是JSON
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