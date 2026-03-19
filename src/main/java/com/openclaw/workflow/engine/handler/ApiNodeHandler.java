package com.openclaw.workflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.entity.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * API调用节点处理器
 */
public class ApiNodeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiNodeHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_TIMEOUT = 30;

    @Override
    public NodeResult execute(NodeExecutionContext context) throws Exception {
        WorkflowNode node = context.getNode();
        ApiConfig config = parseConfig(node);

        logger.info("执行API调用: {} {}", config.method, config.url);

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(config.url).openConnection();
            connection.setRequestMethod(config.method);
            connection.setConnectTimeout(config.timeout * 1000);
            connection.setReadTimeout(config.timeout * 1000);

            if (config.headers != null) {
                for (Map.Entry<String, String> header : config.headers.entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }

            if ("POST".equalsIgnoreCase(config.method) || "PUT".equalsIgnoreCase(config.method)) {
                if (config.body != null && !config.body.isEmpty()) {
                    connection.setDoOutput(true);
                    connection.getOutputStream().write(config.body.getBytes(StandardCharsets.UTF_8));
                }
            }

            int responseCode = connection.getResponseCode();

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        responseCode >= 200 && responseCode < 300 ?
                        connection.getInputStream() : connection.getErrorStream(),
                        StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            if (responseCode >= 200 && responseCode < 300) {
                return NodeResult.success(response.toString());
            } else {
                return NodeResult.failed(response.toString(), "HTTP " + responseCode);
            }

        } catch (Exception e) {
            logger.error("API调用失败: {}", e.getMessage(), e);
            return NodeResult.failed(e.getMessage());
        }
    }

    private ApiConfig parseConfig(WorkflowNode node) {
        ApiConfig config = new ApiConfig();
        try {
            if (node.getConfig() != null) {
                JsonNode jsonConfig = objectMapper.readTree(node.getConfig());
                if (jsonConfig.has("apiConfig")) {
                    JsonNode apiConfig = jsonConfig.get("apiConfig");
                    config.url = apiConfig.has("url") ? apiConfig.get("url").asText() : "";
                    config.method = apiConfig.has("method") ? apiConfig.get("method").asText() : "GET";
                    config.body = apiConfig.has("body") ? apiConfig.get("body").asText() : "";
                    config.timeout = apiConfig.has("timeout") ? apiConfig.get("timeout").asInt() : DEFAULT_TIMEOUT;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return config;
    }

    private static class ApiConfig {
        String url = "";
        String method = "GET";
        Map<String, String> headers;
        String body = "";
        int timeout = DEFAULT_TIMEOUT;
    }
}