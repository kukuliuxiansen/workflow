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
import java.util.UUID;

/**
 * OpenClaw Gateway API 客户端
 */
public class OpenClawGatewayClient {

    private final String gatewayUrl;
    private final String authToken;
    private final ObjectMapper objectMapper;

    public OpenClawGatewayClient(String gatewayUrl, String authToken) {
        this.gatewayUrl = gatewayUrl.endsWith("/") ? gatewayUrl.substring(0, gatewayUrl.length() - 1) : gatewayUrl;
        this.authToken = authToken;
        this.objectMapper = new ObjectMapper();
    }

    public OpenClawGatewayClient() {
        this("http://localhost:18789", "56b640cc2d91411f63255af68355c19ee33c88ec458878ca");
    }

    public AgentResponse executeAgent(AgentRequest request) throws Exception {
        String sessionKey = generateSessionKey(request);
        ObjectNode requestBody = buildChatCompletionsRequest(request);
        HttpURLConnection connection = createConnection(sessionKey, request.getAgentId());

        try (OutputStream os = connection.getOutputStream()) {
            os.write(objectMapper.writeValueAsBytes(requestBody));
            os.flush();
        }

        int responseCode = connection.getResponseCode();
        String responseBody = readResponseBody(connection);

        if (responseCode != 200) {
            throw new RuntimeException("Gateway request failed: " + responseCode + " - " + responseBody);
        }

        return parseResponse(responseBody, sessionKey);
    }

    public AgentResponse executeAgentStreaming(AgentRequest request, StreamListener listener) throws Exception {
        String sessionKey = generateSessionKey(request);
        ObjectNode requestBody = buildChatCompletionsRequest(request);
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

        StringBuilder fullContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if ("[DONE]".equals(data)) break;

                    JsonNode chunk = objectMapper.readTree(data);
                    JsonNode choices = chunk.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        JsonNode delta = choices.get(0).path("delta");
                        String content = delta.path("content").asText("");
                        if (!content.isEmpty()) {
                            fullContent.append(content);
                            if (listener != null) listener.onChunk(content);
                        }
                    }
                }
            }
        }

        AgentResponse response = AgentResponse.success(fullContent.toString(), sessionKey);
        response.setAgentId(request.getAgentId());
        if (listener != null) listener.onComplete(response);
        return response;
    }

    private String generateSessionKey(AgentRequest request) {
        if (request.getSessionKey() != null && !request.getSessionKey().isEmpty()) {
            return request.getSessionKey();
        }
        String context = request.getContext() != null ? request.getContext() : "wf";
        long timestamp = System.currentTimeMillis();
        String random = UUID.randomUUID().toString().substring(0, 8);
        return String.format("agent:%s:isolated_%s_%d_%s", request.getAgentId(), context, timestamp, random);
    }

    private ObjectNode buildChatCompletionsRequest(AgentRequest request) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", "openclaw:" + request.getAgentId());

        ArrayNode messages = body.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", request.getMessage());

        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            ObjectNode systemMessage = messages.insertObject(0);
            systemMessage.put("role", "system");
            systemMessage.put("content", request.getSystemPrompt());
        }

        if (request.getMaxTokens() != null) body.put("max_tokens", request.getMaxTokens());
        if (request.getTemperature() != null) body.put("temperature", request.getTemperature());

        return body;
    }

    private HttpURLConnection createConnection(String sessionKey, String agentId) throws Exception {
        URL url = new URL(gatewayUrl + "/v1/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("x-openclaw-session-key", sessionKey);
        connection.setRequestProperty("x-openclaw-agent-id", agentId);

        if (authToken != null && !authToken.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
        }

        connection.setDoOutput(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(600000);

        return connection;
    }

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

    private AgentResponse parseResponse(String responseBody, String sessionKey) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        AgentResponse response = new AgentResponse();
        response.setSessionKey(sessionKey);
        response.setSuccess(true);

        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).path("message");
            response.setContent(message.path("content").asText(""));
        }

        JsonNode usage = root.path("usage");
        if (!usage.isMissingNode()) {
            response.setPromptTokens(usage.path("prompt_tokens").asInt(0));
            response.setCompletionTokens(usage.path("completion_tokens").asInt(0));
            response.setTotalTokens(usage.path("total_tokens").asInt(0));
        }

        return response;
    }

    public void clearAgentSessions(String agentId) {
        // 通过文件系统或其他方式处理
    }
}