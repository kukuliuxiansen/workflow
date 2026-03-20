package com.openclaw.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 日志格式化和文件读取工具
 */
public class LogFormatter {

    private static final Logger logger = LoggerFactory.getLogger(LogFormatter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static String formatLogLine(OperationLogService.LogEntry entry) {
        try {
            Map<String, Object> logMap = new LinkedHashMap<>();
            logMap.put("timestamp", entry.getTimestamp().format(FORMATTER));
            logMap.put("traceId", entry.getTraceId());
            logMap.put("type", entry.getType());
            logMap.put("executionId", entry.getExecutionId());

            if (entry.getNodeId() != null) logMap.put("nodeId", entry.getNodeId());
            if (entry.getNodeType() != null) logMap.put("nodeType", entry.getNodeType());
            if (entry.getAgentId() != null) logMap.put("agentId", entry.getAgentId());
            if (entry.getOperation() != null) logMap.put("operation", entry.getOperation());
            if (entry.getInput() != null) logMap.put("input", entry.getInput());
            if (entry.getOutput() != null) logMap.put("output", entry.getOutput());
            if (entry.getError() != null) logMap.put("error", entry.getError());
            if (entry.getSuccess() != null) logMap.put("success", entry.getSuccess());

            return objectMapper.writeValueAsString(logMap);
        } catch (Exception e) {
            logger.error("格式化日志失败", e);
            return entry.toString();
        }
    }

    public static Map<String, Object> parseLogLine(String line) {
        try {
            Map<String, Object> logMap = objectMapper.readValue(line, Map.class);
            Map<String, Object> result = new LinkedHashMap<>();

            result.put("timestamp", logMap.get("timestamp"));
            result.put("traceId", logMap.get("traceId"));
            result.put("type", logMap.get("type"));
            result.put("executionId", logMap.get("executionId"));

            // 根据类型提取特定字段
            String type = (String) logMap.get("type");
            if ("NODE".equals(type)) {
                result.put("nodeId", logMap.get("nodeId"));
                result.put("nodeType", logMap.get("nodeType"));
                result.put("operation", logMap.get("operation"));
            } else if ("AI".equals(type)) {
                result.put("agentId", logMap.get("agentId"));
            }

            result.put("input", logMap.get("input"));
            result.put("output", logMap.get("output"));
            result.put("error", logMap.get("error"));

            return result;
        } catch (Exception e) {
            logger.warn("解析日志行失败: {}", line);
            return null;
        }
    }

    public static List<Map<String, Object>> readLogsFromFile(String date, int limit) {
        List<Map<String, Object>> logs = new ArrayList<>();

        try {
            String logDir = "logs";
            String logFile = logDir + "/operation.log";

            if (date != null && !date.isEmpty()) {
                logFile = logDir + "/operation-" + date + ".log";
            }

            Path path = Paths.get(logFile);
            if (!Files.exists(path)) {
                return logs;
            }

            // 从文件末尾读取
            List<String> lines = new ArrayList<>();
            try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
                long fileLength = raf.length();
                long position = fileLength - 1;
                StringBuilder lineBuilder = new StringBuilder();

                while (position >= 0 && lines.size() < limit) {
                    raf.seek(position);
                    char c = (char) raf.read();

                    if (c == '\n') {
                        if (lineBuilder.length() > 0) {
                            lines.add(lineBuilder.reverse().toString());
                            lineBuilder = new StringBuilder();
                        }
                    } else {
                        lineBuilder.append(c);
                    }
                    position--;
                }

                if (lineBuilder.length() > 0 && lines.size() < limit) {
                    lines.add(lineBuilder.reverse().toString());
                }
            }

            // 解析日志
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                Map<String, Object> parsed = parseLogLine(line);
                if (parsed != null) {
                    logs.add(parsed);
                }
            }

        } catch (Exception e) {
            logger.error("读取日志文件失败", e);
        }

        return logs;
    }

    public static Object truncateIfNeeded(Object obj, int maxLen) {
        if (obj == null) return null;

        try {
            String json = obj instanceof String ? (String) obj : objectMapper.writeValueAsString(obj);
            if (json.length() > maxLen) {
                return json.substring(0, maxLen) + "...(truncated)";
            }
            return obj;
        } catch (Exception e) {
            return obj.toString();
        }
    }
}