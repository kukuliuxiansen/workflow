package com.openclaw.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 操作日志服务
 */
@Service
public class OperationLogService {

    private static final Logger OPERATION_LOGGER = LoggerFactory.getLogger("OPERATION_LOG");
    private static final Logger logger = LoggerFactory.getLogger(OperationLogService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Deque<LogEntry> recentLogs = new ConcurrentLinkedDeque<>();
    private static final int MAX_RECENT_LOGS = 500;

    public void logNodeOperation(String executionId, String nodeId, String nodeType,
                                  String operation, Object input, Object output) {
        try {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(LocalDateTime.now());
            entry.setType("NODE");
            entry.setExecutionId(executionId);
            entry.setNodeId(nodeId);
            entry.setNodeType(nodeType);
            entry.setOperation(operation);
            entry.setInput(LogFormatter.truncateIfNeeded(input, 2000));
            entry.setOutput(LogFormatter.truncateIfNeeded(output, 2000));

            logEntry(entry);
        } catch (Exception e) {
            logger.error("记录节点操作日志失败", e);
        }
    }

    public void logAICall(String executionId, String agentId, String prompt,
                          Object responsePayload, boolean success, String error) {
        try {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(LocalDateTime.now());
            entry.setType("AI");
            entry.setExecutionId(executionId);
            entry.setAgentId(agentId);
            entry.setInput(LogFormatter.truncateIfNeeded(prompt, 2000));
            entry.setOutput(LogFormatter.truncateIfNeeded(responsePayload, 2000));
            entry.setSuccess(success);
            entry.setError(error);

            logEntry(entry);
        } catch (Exception e) {
            logger.error("记录AI调用日志失败", e);
        }
    }

    public void logWorkflowExecution(String executionId, String workflowId,
                                      String workflowName, String status, String message) {
        try {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(LocalDateTime.now());
            entry.setType("WORKFLOW");
            entry.setExecutionId(executionId);
            entry.setNodeId(workflowId);
            entry.setOperation(status);
            entry.setOutput(message);

            logEntry(entry);
        } catch (Exception e) {
            logger.error("记录工作流执行日志失败", e);
        }
    }

    public void logApiCall(String executionId, String nodeId, String url, String method,
                           Object request, Object response, int statusCode, long duration) {
        try {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(LocalDateTime.now());
            entry.setType("API");
            entry.setExecutionId(executionId);
            entry.setNodeId(nodeId);
            entry.setOperation(method + " " + url);
            entry.setInput(LogFormatter.truncateIfNeeded(request, 1000));
            entry.setOutput(LogFormatter.truncateIfNeeded(response, 1000));

            logEntry(entry);
        } catch (Exception e) {
            logger.error("记录API调用日志失败", e);
        }
    }

    public void logOperation(String type, String operation, Object input, Object output, String message) {
        try {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(LocalDateTime.now());
            entry.setType(type);
            entry.setOperation(operation);
            entry.setInput(LogFormatter.truncateIfNeeded(input, 1000));
            entry.setOutput(LogFormatter.truncateIfNeeded(output, 1000));
            entry.setError(message);

            logEntry(entry);
        } catch (Exception e) {
            logger.error("记录操作日志失败", e);
        }
    }

    public void logError(String type, String operation, String error, Object context) {
        try {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(LocalDateTime.now());
            entry.setType(type);
            entry.setOperation(operation);
            entry.setError(error);
            entry.setInput(LogFormatter.truncateIfNeeded(context, 500));

            logEntry(entry);
        } catch (Exception e) {
            logger.error("记录错误日志失败", e);
        }
    }

    private void logEntry(LogEntry entry) {
        entry.setTraceId(generateTraceId());

        // 添加到内存队列
        recentLogs.addFirst(entry);
        while (recentLogs.size() > MAX_RECENT_LOGS) {
            recentLogs.removeLast();
        }

        // 写入日志文件
        String logLine = LogFormatter.formatLogLine(entry);
        OPERATION_LOGGER.info(logLine);
    }

    private String generateTraceId() {
        return "TRC-" + System.currentTimeMillis() + "-" +
                Integer.toHexString((int) (Math.random() * 0xFFFF));
    }

    public List<LogEntry> getRecentLogs(int limit) {
        List<LogEntry> logs = new ArrayList<>();
        int count = 0;
        for (LogEntry entry : recentLogs) {
            logs.add(entry);
            if (++count >= limit) break;
        }
        return logs;
    }

    public List<LogEntry> getLogsByExecutionId(String executionId) {
        List<LogEntry> logs = new ArrayList<>();
        for (LogEntry entry : recentLogs) {
            if (executionId.equals(entry.getExecutionId())) {
                logs.add(entry);
            }
        }
        return logs;
    }

    public List<Map<String, Object>> readLogFromFile(String date, int limit) {
        return LogFormatter.readLogsFromFile(date, limit);
    }

    public void clearRecentLogs() {
        recentLogs.clear();
        logger.info("内存日志已清空");
    }

    public void saveFrontendLog(Map<String, Object> logEntry) {
        try {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(LocalDateTime.now());
            entry.setType((String) logEntry.getOrDefault("type", "FRONTEND"));
            entry.setExecutionId((String) logEntry.get("executionId"));
            entry.setOperation((String) logEntry.get("operation"));
            entry.setInput(logEntry.get("data"));
            entry.setError((String) logEntry.get("error"));

            logEntry(entry);
        } catch (Exception e) {
            logger.error("保存前端日志失败", e);
        }
    }

    public static class LogEntry {
        private LocalDateTime timestamp;
        private String traceId;
        private String type;
        private String executionId;
        private String nodeId;
        private String nodeType;
        private String agentId;
        private String operation;
        private Object input;
        private Object output;
        private Boolean success;
        private String error;

        // Getters and Setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getTraceId() { return traceId; }
        public void setTraceId(String traceId) { this.traceId = traceId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getExecutionId() { return executionId; }
        public void setExecutionId(String executionId) { this.executionId = executionId; }
        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public String getNodeType() { return nodeType; }
        public void setNodeType(String nodeType) { this.nodeType = nodeType; }
        public String getAgentId() { return agentId; }
        public void setAgentId(String agentId) { this.agentId = agentId; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public Object getInput() { return input; }
        public void setInput(Object input) { this.input = input; }
        public Object getOutput() { return output; }
        public void setOutput(Object output) { this.output = output; }
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}