package com.openclaw.workflow.engine.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * OpenClaw Gateway API 客户端
 *
 * 通过 Gateway API 调用 Agent，支持会话隔离
 *
 * 使用方式：
 * 1. 确保已启用 chatCompletions 端点（在 openclaw.json 中设置 gateway.http.endpoints.chatCompletions.enabled: true）
 * 2. 使用唯一的 x-openclaw-session-key 实现会话隔离
 */
public class OpenClawGatewayClient {

    private final String gatewayUrl;
    private final String authToken;
    private final ObjectMapper objectMapper;

    /**
     * 创建 Gateway 客户端
     *
     * @param gatewayUrl Gateway 地址，如 http://localhost:18789
     * @param authToken 认证 Token（在 openclaw.json 的 gateway.auth.token 中配置）
     */
    public OpenClawGatewayClient(String gatewayUrl, String authToken) {
        this.gatewayUrl = gatewayUrl.endsWith("/") ? gatewayUrl.substring(0, gatewayUrl.length() - 1) : gatewayUrl;
        this.authToken = authToken;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 使用默认配置创建客户端
     */
    public OpenClawGatewayClient() {
        this("http://localhost:18789", "56b640cc2d91411f63255af68355c19ee33c88ec458878ca");
    }

    /**
     * 执行 Agent（隔离会话）
     *
     * @param request 执行请求
     * @return 执行结果
     */
    public AgentResponse executeAgent(AgentRequest request) throws Exception {
        // 生成唯一的 sessionKey
        String sessionKey = generateSessionKey(request);

        // 构建 OpenAI 兼容请求
        ObjectNode requestBody = buildChatCompletionsRequest(request);

        // 发送请求
        HttpURLConnection connection = createConnection(sessionKey, request.getAgentId());

        try (OutputStream os = connection.getOutputStream()) {
            os.write(objectMapper.writeValueAsBytes(requestBody));
            os.flush();
        }

        // 读取响应
        int responseCode = connection.getResponseCode();
        String responseBody = readResponseBody(connection);

        if (responseCode != 200) {
            throw new RuntimeException("Gateway request failed: " + responseCode + " - " + responseBody);
        }

        return parseResponse(responseBody, sessionKey);
    }

    /**
     * 执行 Agent（流式响应）
     *
     * @param request 执行请求
     * @param listener 流式响应监听器
     * @return 最终结果
     */
    public AgentResponse executeAgentStreaming(AgentRequest request, StreamListener listener) throws Exception {
        String sessionKey = generateSessionKey(request);
        ObjectNode requestBody = buildChatCompletionsRequest(request);
        // 启用流式
        requestBody.put("stream", true);

        HttpURLConnection connection = createConnection(sessionKey, request.getAgentId());

        try (OutputStream os = connection.getOutputStream()) {
            os.write(objectMapper.writeValueAsBytes(requestBody));
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            String errorBody = readResponseBody(connection);
            throw new RuntimeException("Gateway request failed: " + responseCode + " - " + errorBody);
        }

        // 处理 SSE 流式响应
        StringBuilder fullContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if ("[DONE]".equals(data)) {
                        break;
                    }

                    JsonNode chunk = objectMapper.readTree(data);
                    JsonNode choices = chunk.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        JsonNode delta = choices.get(0).path("delta");
                        String content = delta.path("content").asText("");
                        if (!content.isEmpty()) {
                            fullContent.append(content);
                            if (listener != null) {
                                listener.onChunk(content);
                            }
                        }
                    }
                }
            }
        }

        AgentResponse response = new AgentResponse();
        response.setSuccess(true);
        response.setContent(fullContent.toString());
        response.setSessionKey(sessionKey);
        response.setAgentId(request.getAgentId());

        if (listener != null) {
            listener.onComplete(response);
        }

        return response;
    }

    /**
     * 生成唯一的 SessionKey
     */
    private String generateSessionKey(AgentRequest request) {
        if (request.getSessionKey() != null && !request.getSessionKey().isEmpty()) {
            return request.getSessionKey();
        }

        // 生成格式: agent:{agentId}:isolated_{context}_{timestamp}_{random}
        String context = request.getContext() != null ? request.getContext() : "wf";
        long timestamp = System.currentTimeMillis();
        String random = UUID.randomUUID().toString().substring(0, 8);

        return String.format("agent:%s:isolated_%s_%d_%s",
                request.getAgentId(), context, timestamp, random);
    }

    /**
     * 构建 Chat Completions 请求体
     */
    private ObjectNode buildChatCompletionsRequest(AgentRequest request) {
        ObjectNode body = objectMapper.createObjectNode();

        // 使用特殊的 model 格式指定 agent
        // 格式: openclaw:{agentId} 或 agent:{agentId}
        body.put("model", "openclaw:" + request.getAgentId());

        // 构建消息
        ArrayNode messages = body.putArray("messages");

        // 添加用户消息
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", request.getMessage());

        // 可选：添加系统提示
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            ObjectNode systemMessage = ((ArrayNode) messages).insertObject(0);
            systemMessage.put("role", "system");
            systemMessage.put("content", request.getSystemPrompt());
        }

        // 其他可选参数
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }

        return body;
    }

    /**
     * 创建 HTTP 连接
     */
    private HttpURLConnection createConnection(String sessionKey, String agentId) throws Exception {
        URL url = new URL(gatewayUrl + "/v1/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        // 关键：设置 session-key 实现会话隔离
        connection.setRequestProperty("x-openclaw-session-key", sessionKey);

        // 可选：设置 agent-id（如果需要）
        connection.setRequestProperty("x-openclaw-agent-id", agentId);

        // 认证
        if (authToken != null && !authToken.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
        }

        connection.setDoOutput(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(600000); // 10 分钟超时

        return connection;
    }

    /**
     * 读取响应体
     */
    private String readResponseBody(HttpURLConnection connection) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    /**
     * 解析响应
     */
    private AgentResponse parseResponse(String responseBody, String sessionKey) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        AgentResponse response = new AgentResponse();
        response.setSessionKey(sessionKey);
        response.setSuccess(true);

        // 解析 OpenAI 格式的响应
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).path("message");
            response.setContent(message.path("content").asText(""));
        }

        // 解析使用情况
        JsonNode usage = root.path("usage");
        if (!usage.isMissingNode()) {
            response.setPromptTokens(usage.path("prompt_tokens").asInt(0));
            response.setCompletionTokens(usage.path("completion_tokens").asInt(0));
            response.setTotalTokens(usage.path("total_tokens").asInt(0));
        }

        return response;
    }

    /**
     * 清理 Agent 的所有会话
     *
     * @param agentId Agent ID
     */
    public void clearAgentSessions(String agentId) {
        // 这个需要直接操作文件系统
        // 在 Java 中实现比较复杂，建议通过脚本或其他方式处理
    }

    // ==================== 内部类 ====================

    /**
     * Agent 执行请求
     */
    public static class AgentRequest {
        private String agentId;
        private String message;
        private String systemPrompt;
        private String sessionKey;
        private String context;  // 用于生成 sessionKey 的上下文信息
        private Integer maxTokens;
        private Double temperature;

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }

        public String getSessionKey() {
            return sessionKey;
        }

        public void setSessionKey(String sessionKey) {
            this.sessionKey = sessionKey;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        /**
         * 构建器模式
         */
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final AgentRequest request = new AgentRequest();

            public Builder agentId(String agentId) {
                request.setAgentId(agentId);
                return this;
            }

            public Builder message(String message) {
                request.setMessage(message);
                return this;
            }

            public Builder systemPrompt(String systemPrompt) {
                request.setSystemPrompt(systemPrompt);
                return this;
            }

            public Builder sessionKey(String sessionKey) {
                request.setSessionKey(sessionKey);
                return this;
            }

            public Builder context(String context) {
                request.setContext(context);
                return this;
            }

            public Builder maxTokens(Integer maxTokens) {
                request.setMaxTokens(maxTokens);
                return this;
            }

            public Builder temperature(Double temperature) {
                request.setTemperature(temperature);
                return this;
            }

            public AgentRequest build() {
                return request;
            }
        }
    }

    /**
     * Agent 执行响应
     */
    public static class AgentResponse {
        private boolean success;
        private String content;
        private String sessionKey;
        private String agentId;
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
        private String errorMessage;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getSessionKey() {
            return sessionKey;
        }

        public void setSessionKey(String sessionKey) {
            this.sessionKey = sessionKey;
        }

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public int getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
        }

        public int getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(int completionTokens) {
            this.completionTokens = completionTokens;
        }

        public int getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return "AgentResponse{" +
                    "success=" + success +
                    ", sessionKey='" + sessionKey + '\'' +
                    ", agentId='" + agentId + '\'' +
                    ", contentLength=" + (content != null ? content.length() : 0) +
                    ", totalTokens=" + totalTokens +
                    '}';
        }
    }

    /**
     * 流式响应监听器
     */
    public interface StreamListener {
        /**
         * 收到内容块
         */
        void onChunk(String chunk);

        /**
         * 完成时调用
         */
        void onComplete(AgentResponse response);
    }
}