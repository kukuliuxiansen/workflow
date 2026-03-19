package com.openclaw.workflow.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行日志WebSocket处理器
 */
@Component
public class ExecutionWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionWebSocketHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 存储活跃的WebSocket会话，按executionId分组
    private final Map<String, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String executionId = getExecutionId(session);
        if (executionId != null) {
            sessions.computeIfAbsent(executionId, k -> new ConcurrentHashMap<>())
                    .put(session.getId(), session);
            logger.info("WebSocket连接建立: executionId={}, sessionId={}", executionId, session.getId());
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        // 处理客户端消息（如果需要）
        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();
            logger.debug("收到WebSocket消息: {}", payload);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket传输错误: sessionId={}, error={}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String executionId = getExecutionId(session);
        if (executionId != null) {
            Map<String, WebSocketSession> executionSessions = sessions.get(executionId);
            if (executionSessions != null) {
                executionSessions.remove(session.getId());
                if (executionSessions.isEmpty()) {
                    sessions.remove(executionId);
                }
            }
        }
        logger.info("WebSocket连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 广播日志消息
     */
    public void broadcastLog(String executionId, Map<String, Object> logData) {
        broadcast(executionId, "log", logData);
    }

    /**
     * 广播状态更新
     */
    public void broadcastStatus(String executionId, Map<String, Object> statusData) {
        broadcast(executionId, "status", statusData);
    }

    /**
     * 广播节点状态
     */
    public void broadcastNodeStatus(String executionId, String nodeId, String status, String nodeName) {
        Map<String, Object> data = new ConcurrentHashMap<>();
        data.put("nodeId", nodeId);
        data.put("status", status);
        data.put("nodeName", nodeName);
        broadcast(executionId, "node:status", data);
    }

    /**
     * 通用广播方法
     */
    private void broadcast(String executionId, String type, Map<String, Object> data) {
        Map<String, WebSocketSession> executionSessions = sessions.get(executionId);
        if (executionSessions == null || executionSessions.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> message = new ConcurrentHashMap<>();
            message.put("type", type);
            message.put("data", data);

            String jsonMessage = objectMapper.writeValueAsString(message);

            for (WebSocketSession session : executionSessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(jsonMessage));
                    } catch (IOException e) {
                        logger.error("发送WebSocket消息失败: sessionId={}, error={}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("序列化WebSocket消息失败: {}", e.getMessage());
        }
    }

    /**
     * 从URI中获取executionId
     */
    private String getExecutionId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("executionId=")) {
            for (String param : query.split("&")) {
                if (param.startsWith("executionId=")) {
                    return param.substring("executionId=".length());
                }
            }
        }
        return null;
    }
}