package com.openclaw.workflow.engine.smartdecompose;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Action块解析器
 * 负责解析 [ACTION] 格式的输出
 */
public class ActionBlockParser {

    private static final Logger logger = LoggerFactory.getLogger(ActionBlockParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析动作块
     */
    public void parse(AgentAction action, String block) {
        Map<String, Object> parameters = new HashMap<>();

        // 解析工具名称
        Pattern toolPattern = Pattern.compile("tool:\\s*(\\w+)");
        Matcher toolMatcher = toolPattern.matcher(block);
        if (toolMatcher.find()) {
            String toolName = toolMatcher.group(1);
            DecomposeTool tool = DecomposeTool.fromName(toolName);
            if (tool != null) {
                action.setTool(tool);
            } else {
                logger.warn("未知的工具名称: {}", toolName);
                action.setTool(DecomposeTool.CONTINUE);
            }
        } else {
            action.setTool(DecomposeTool.CONTINUE);
        }

        // 解析简单参数
        parseSimpleParameters(block, parameters);

        // 解析子任务列表
        if (block.contains("subtasks:")) {
            List<Map<String, Object>> subtasks = parseSubtasks(block);
            parameters.put("subtasks", subtasks);
        }

        action.setParameters(parameters);
    }

    /**
     * 解析简单参数（单行值）
     */
    private void parseSimpleParameters(String block, Map<String, Object> parameters) {
        Pattern paramPattern = Pattern.compile("(\\w+):\\s*(.+?)(?=\\n\\w+:|$)", Pattern.MULTILINE);
        Matcher matcher = paramPattern.matcher(block);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2).trim();

            // 跳过特殊字段
            if (key.equals("tool") || key.equals("subtasks") || key.equals("parameters")) {
                continue;
            }

            // 清理引号
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }

            parameters.put(key, value);
        }
    }

    /**
     * 解析子任务列表
     */
    private List<Map<String, Object>> parseSubtasks(String block) {
        List<Map<String, Object>> subtasks = new ArrayList<>();

        // 匹配 YAML 风格的子任务定义
        Pattern taskPattern = Pattern.compile(
            "-\\s*(?:id:\\s*)?(\\S+)\\s*" +
            "(?:description:|desc:)\\s*[\"']?([^\"'\\n]+)[\"']?\\s*" +
            "(?:priority:\\s*(\\d+))?\\s*" +
            "(?:dependencies:\\s*\\[([^\\]]*)\\])?",
            Pattern.DOTALL
        );

        Matcher matcher = taskPattern.matcher(block);
        while (matcher.find()) {
            Map<String, Object> task = new HashMap<>();
            task.put("id", matcher.group(1).trim());
            task.put("description", matcher.group(2).trim());

            String priority = matcher.group(3);
            task.put("priority", priority != null ? Integer.parseInt(priority) : 5);

            String deps = matcher.group(4);
            if (deps != null && !deps.trim().isEmpty()) {
                List<String> depList = new ArrayList<>();
                for (String dep : deps.split(",")) {
                    depList.add(dep.trim());
                }
                task.put("dependencies", depList);
            } else {
                task.put("dependencies", Collections.emptyList());
            }

            subtasks.add(task);
        }

        // 如果上面的模式没有匹配到，尝试更宽松的解析
        if (subtasks.isEmpty()) {
            subtasks = parseSubtasksLoose(block);
        }

        logger.info("解析到 {} 个子任务", subtasks.size());
        return subtasks;
    }

    /**
     * 宽松的子任务解析
     */
    private List<Map<String, Object>> parseSubtasksLoose(String block) {
        List<Map<String, Object>> subtasks = new ArrayList<>();

        // 匹配 JSON 格式
        if (block.contains("[") && block.contains("]")) {
            try {
                int start = block.indexOf("[");
                int end = block.lastIndexOf("]") + 1;
                String jsonArray = block.substring(start, end);
                List<?> list = objectMapper.readValue(jsonArray, List.class);
                for (Object item : list) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> mapItem = (Map<String, Object>) item;
                        subtasks.add(mapItem);
                    }
                }
            } catch (Exception e) {
                logger.debug("JSON解析失败，尝试其他方式: {}", e.getMessage());
            }
        }

        // 匹配带引号的描述
        if (subtasks.isEmpty()) {
            Pattern descPattern = Pattern.compile("-\\s*[\"']([^\"']+)[\"']");
            Matcher matcher = descPattern.matcher(block);
            int taskNum = 1;
            while (matcher.find()) {
                Map<String, Object> task = new HashMap<>();
                task.put("id", "task_" + taskNum++);
                task.put("description", matcher.group(1));
                task.put("priority", 5);
                task.put("dependencies", Collections.emptyList());
                subtasks.add(task);
            }
        }

        return subtasks;
    }
}