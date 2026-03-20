package com.openclaw.workflow.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * AI API 客户端
 * 支持 Gateway API 和直接 API 两种调用方式
 */
public class AIClient {

    private static final Logger logger = LoggerFactory.getLogger(AIClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String gatewayUrl;
    private final String gatewayToken;
    private final String aiAgentId;

    public AIClient(String baseUrl, String apiKey, String model,
                    String gatewayUrl, String gatewayToken, String aiAgentId) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.gatewayUrl = gatewayUrl;
        this.gatewayToken = gatewayToken;
        this.aiAgentId = aiAgentId;
    }

    /**
     * 调用 AI（自动选择 Gateway 或直接 API）
     */
    public String call(String systemPrompt, String userPrompt) throws Exception {
        if (gatewayUrl != null && !gatewayUrl.isEmpty()) {
            return callViaGateway(systemPrompt, userPrompt);
        } else if (apiKey != null && !apiKey.isEmpty()) {
            return callViaDirectApi(systemPrompt, userPrompt);
        } else {
            throw new RuntimeException("未配置AI API Key或Gateway URL");
        }
    }

    /**
     * 通过 Gateway API 调用 AI
     */
    private String callViaGateway(String systemPrompt, String userPrompt) throws Exception {
        logger.info("通过Gateway API调用AI: {}", gatewayUrl);

        OpenClawGatewayClient client = new OpenClawGatewayClient(gatewayUrl, gatewayToken);

        OpenClawGatewayClient.AgentRequest request = OpenClawGatewayClient.AgentRequest.builder()
                .agentId(aiAgentId)
                .systemPrompt(systemPrompt)
                .message(userPrompt)
                .context("workflow_gen")
                .build();

        OpenClawGatewayClient.AgentResponse response = client.executeAgent(request);

        if (!response.isSuccess()) {
            throw new RuntimeException("Gateway API调用失败: " + response.getErrorMessage());
        }

        String content = response.getContent();
        logger.info("Gateway API调用成功，返回内容长度: {}", content.length());
        return content;
    }

    /**
     * 通过直接 API 调用 AI（阿里云 DashScope）
     */
    private String callViaDirectApi(String systemPrompt, String userPrompt) throws Exception {
        logger.info("通过直接API调用AI: {}", baseUrl);

        HttpURLConnection connection = null;
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Arrays.asList(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 4096);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            URL url = new URL(baseUrl + "/chat/completions");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(120000);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes("UTF-8"));
                os.flush();
            }

            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                String error = readErrorStream(connection);
                throw new RuntimeException("AI API调用失败: " + statusCode + " - " + error);
            }

            return parseSuccessResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readErrorStream(HttpURLConnection connection) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), "UTF-8"))) {
            StringBuilder error = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line);
            }
            return error.toString();
        }
    }

    private String parseSuccessResponse(HttpURLConnection connection) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            StringBuilder responseBody = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBody.append(line);
            }

            Map<String, Object> responseMap = objectMapper.readValue(responseBody.toString(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }

            throw new RuntimeException("AI返回内容为空");
        }
    }

    public boolean isConfigured() {
        return (gatewayUrl != null && !gatewayUrl.isEmpty()) ||
               (apiKey != null && !apiKey.isEmpty());
    }
}