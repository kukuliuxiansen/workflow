package com.openclaw.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 操作日志服务
 * 记录所有节点操作、AI调用等关键操作的输入输出
 */
@Service
public class OperationLogService {

    private static final Logger OPERATION_LOGGER = LoggerFactory.getLogger("OPERATION_LOG");
    private static final Logger logger = LoggerFactory.getLogger(OperationLogService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 内存中保存最近的日志（用于前端展示）
    private final Deque<LogEntry> recentLogs = new ConcurrentLinkedDeque<>();
    private static final int MAX_RECENT_LOGS = 500;

    /**
     * 记录节点操作日志
     */
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
            entry.setInput(truncateIfNeeded(input));
            entry.setOutput(truncateIfNeeded(output));

            logEntry(entry);
        } catch (Exception e) {
            logger.error("记录节点操作日志失败", e);
        }
    }

    /**
     * 记录AI调用日志
     */
    public void logAICall(String executionId, String agentId, String prompt,
                          Object responsePayload, boolean success, String error) {
        try {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(LocalDateTime.now());
            entry.setType("AI");
            entry.setExecutionId(executionId);
            entry.setAgentId(agentId);
            entry.setInput(truncateIfNeeded(prompt));
            entry.setOutput(truncateIfNeeded(responsePayload));
            entry.setSuccess(success);
            entry.setError(error);

            logEntry(entry);
        } catch (Exception e) {
            logger.error("记录AI调用日志失败", e);
        }
    }

    /**
     * 记录工作流执行日志
     */
    public void logWorkflowExecution(String executionId, String workflowId,
                                      String workflowName, String status, String message) {
        try {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(LocalDateTime.now());
            entry.setType("WORKFLOW");
            entry.setExecutionId(executionId);
            entry.setWorkflowId(workflowId);
            entry.setWorkflowName(workflowName);
            entry.setOperation(status);
            entry.setMessage(message);

            logEntry(entry);
        } catch (Exception e) {
            logger.error("记录工作流执行日志失败", e);
        }
    }

    /**
     * 记录API调用日志
     */
    public void logApiCall(String executionId, String nodeId, String url, String method,
                           Object requestBody, Object responseBody, int statusCode) {
        try {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(LocalDateTime.now());
            entry.setType("API");
            entry.setExecutionId(executionId);
            entry.setNodeId(nodeId);
            entry.setOperation(method + " " + url);
            entry.setInput(truncateIfNeeded(requestBody));
            entry.setOutput(truncateIfNeeded(responseBody));
            entry.setStatusCode(statusCode);

            logEntry(entry);
        } catch (Exception e) {
            logger.error("记录API调用日志失败", e);
        }
    }

    /**
     * 记录通用操作日志
     */
    public void logOperation(String type, String operation, Object input, Object output, String message) {
        try {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(LocalDateTime.now());
            entry.setType(type);
            entry.setOperation(operation);
            entry.setInput(truncateIfNeeded(input));
            entry.setOutput(truncateIfNeeded(output));
            entry.setMessage(message);

            logEntry(entry);
        } catch (Exception e) {
            logger.error("记录操作日志失败", e);
        }
    }

    /**
     * 记录错误日志
     */
    public void logError(String type, String operation, String error, Object context) {
        try {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(LocalDateTime.now());
            entry.setType("ERROR");
            entry.setOperation(operation);
            entry.setError(error);
            entry.setInput(truncateIfNeeded(context));
            entry.setSuccess(false);

            logEntry(entry);
        } catch (Exception e) {
            logger.error("记录错误日志失败", e);
        }
    }

    private void logEntry(LogEntry entry) {
        // 写入文件
        String logLine = formatLogLine(entry);
        OPERATION_LOGGER.info(logLine);

        // 保存到内存
        recentLogs.addFirst(entry);
        while (recentLogs.size() > MAX_RECENT_LOGS) {
            recentLogs.removeLast();
        }
    }

    private String formatLogLine(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(entry.getType()).append("|");
        sb.append(entry.getExecutionId() != null ? entry.getExecutionId() : "-").append("|");
        sb.append(entry.getNodeId() != null ? entry.getNodeId() : "-").append("|");
        sb.append(entry.getOperation() != null ? entry.getOperation() : "-").append("|");
        sb.append(entry.isSuccess() ? "SUCCESS" : "FAIL").append("|");
        if (entry.getInput() != null) {
            sb.append("IN:").append(entry.getInput()).append("|");
        }
        if (entry.getOutput() != null) {
            sb.append("OUT:").append(entry.getOutput());
        }
        if (entry.getError() != null) {
            sb.append("ERR:").append(entry.getError());
        }
        return sb.toString();
    }

    private Object truncateIfNeeded(Object obj) {
        if (obj == null) return null;

        try {
            String json = obj instanceof String ? (String) obj : objectMapper.writeValueAsString(obj);
            // 限制长度，AI输出可能很长
            if (json.length() > 5000) {
                return json.substring(0, 5000) + "...(truncated)";
            }
            return obj;
        } catch (Exception e) {
            return obj.toString();
        }
    }

    /**
     * 获取最近的日志
     */
    public List<LogEntry> getRecentLogs(int limit) {
        List<LogEntry> result = new ArrayList<>();
        int count = 0;
        for (LogEntry entry : recentLogs) {
            result.add(entry);
            if (++count >= limit) break;
        }
        return result;
    }

    /**
     * 获取指定执行ID的日志
     */
    public List<LogEntry> getLogsByExecutionId(String executionId) {
        List<LogEntry> result = new ArrayList<>();
        for (LogEntry entry : recentLogs) {
            if (executionId.equals(entry.getExecutionId())) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * 从文件读取日志
     */
    public List<Map<String, Object>> readLogFromFile(String date, int limit) {
        List<Map<String, Object>> logs = new ArrayList<>();

        String logFileName;
        if (date == null || date.isEmpty()) {
            logFileName = "operation.log";
        } else {
            logFileName = "operation." + date + ".log";
        }

        Path logPath = Paths.get("logs", logFileName);
        if (!Files.exists(logPath)) {
            return logs;
        }

        try (BufferedReader reader = Files.newBufferedReader(logPath)) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < limit) {
                Map<String, Object> parsed = parseLogLine(line);
                if (parsed != null) {
                    logs.add(parsed);
                    count++;
                }
            }
        } catch (IOException e) {
            logger.error("读取日志文件失败: {}", e.getMessage());
        }

        Collections.reverse(logs); // 最新的在前面
        return logs;
    }

    private Map<String, Object> parseLogLine(String line) {
        try {
            String[] parts = line.split("\\|", 7);
            if (parts.length < 5) return null;

            Map<String, Object> log = new LinkedHashMap<>();
            log.put("timestamp", parts[0]);
            log.put("type", parts[1]);
            log.put("executionId", "-".equals(parts[2]) ? null : parts[2]);
            log.put("nodeId", "-".equals(parts[3]) ? null : parts[3]);
            log.put("operation", "-".equals(parts[4]) ? null : parts[4]);
            log.put("success", "SUCCESS".equals(parts[5]));

            if (parts.length > 6) {
                String rest = parts[6];
                // 解析IN/OUT/ERR
                if (rest.contains("OUT:")) {
                    int idx = rest.indexOf("OUT:");
                    if (rest.contains("IN:")) {
                        log.put("input", rest.substring(3, idx));
                    }
                    log.put("output", rest.substring(idx + 4));
                } else if (rest.contains("IN:")) {
                    log.put("input", rest.substring(3));
                }
                if (rest.contains("ERR:")) {
                    int idx = rest.indexOf("ERR:");
                    log.put("error", rest.substring(idx + 4));
                }
            }

            return log;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 清空内存中的日志
     */
    public void clearRecentLogs() {
        recentLogs.clear();
    }

    /**
     * 日志条目
     */
    public static class LogEntry {
        private LocalDateTime timestamp;
        private String type; // NODE, AI, API, WORKFLOW, ERROR
        private String executionId;
        private String workflowId;
        private String workflowName;
        private String nodeId;
        private String nodeType;
        private String agentId;
        private String operation;
        private Object input;
        private Object output;
        private boolean success = true;
        private String error;
        private String message;
        private Integer statusCode;

        // Getters and Setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getExecutionId() { return executionId; }
        public void setExecutionId(String executionId) { this.executionId = executionId; }
        public String getWorkflowId() { return workflowId; }
        public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
        public String getWorkflowName() { return workflowName; }
        public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
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
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Integer getStatusCode() { return statusCode; }
        public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    }
}