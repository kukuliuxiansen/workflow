package com.openclaw.workflow.engine.smartdecompose;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent输出解析器
 * 解析Agent返回的文本，提取思考和动作
 */
public class AgentOutputParser {

    private static final Logger logger = LoggerFactory.getLogger(AgentOutputParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern THOUGHT_PATTERN = Pattern.compile(
        "\\[THOUGHT\\](.*?)\\[/THOUGHT\\]", Pattern.DOTALL
    );

    private static final Pattern ACTION_PATTERN = Pattern.compile(
        "\\[ACTION\\](.*?)\\[/ACTION\\]", Pattern.DOTALL
    );

    private static final Pattern NODE_DECISION_PATTERN = Pattern.compile(
        "\\[NODE_DECISION\\](.*?)\\[/NODE_DECISION\\]", Pattern.DOTALL
    );

    /**
     * 解析Agent输出
     */
    public AgentAction parse(String agentOutput) {
        if (agentOutput == null || agentOutput.trim().isEmpty()) {
            return createDefaultAction("输出为空");
        }

        AgentAction action = new AgentAction();

        // 1. 解析思考
        Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(agentOutput);
        if (thoughtMatcher.find()) {
            action.setThought(thoughtMatcher.group(1).trim());
        }

        // 2. 解析动作
        Matcher actionMatcher = ACTION_PATTERN.matcher(agentOutput);
        if (actionMatcher.find()) {
            String actionBlock = actionMatcher.group(1).trim();
            parseActionBlock(action, actionBlock);
        } else {
            // 尝试解析 NODE_DECISION 格式
            Matcher decisionMatcher = NODE_DECISION_PATTERN.matcher(agentOutput);
            if (decisionMatcher.find()) {
                parseNodeDecision(action, decisionMatcher.group(1).trim());
            } else {
                // 没有找到明确动作，从内容推断
                return inferFromOutput(agentOutput);
            }
        }

        return action;
    }

    /**
     * 解析动作块
     */
    private void parseActionBlock(AgentAction action, String block) {
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
        // 匹配 key: value 格式
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
                        subtasks.add((Map<String, Object>) item);
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

    /**
     * 解析 NODE_DECISION 格式
     */
    private void parseNodeDecision(AgentAction action, String decisionBlock) {
        Map<String, Object> parameters = new HashMap<>();

        // 解析 node_ids
        Pattern nodeIdsPattern = Pattern.compile("node_ids:\\s*(.+?)(?=\\n|$)");
        Matcher nodeIdsMatcher = nodeIdsPattern.matcher(decisionBlock);
        if (nodeIdsMatcher.find()) {
            String nodeIds = nodeIdsMatcher.group(1).trim();
            parameters.put("node_ids", nodeIds);
        }

        // 解析 reason
        Pattern reasonPattern = Pattern.compile("reason:\\s*(.+?)(?=\\n|$)");
        Matcher reasonMatcher = reasonPattern.matcher(decisionBlock);
        if (reasonMatcher.find()) {
            parameters.put("reason", reasonMatcher.group(1).trim());
        }

        // 解析 tool_name
        Pattern toolNamePattern = Pattern.compile("tool:\\s*(.+?)(?=\\n|$)");
        Matcher toolNameMatcher = toolNamePattern.matcher(decisionBlock);
        if (toolNameMatcher.find()) {
            String toolName = toolNameMatcher.group(1).trim();
            DecomposeTool tool = DecomposeTool.fromName(toolName);
            if (tool != null) {
                action.setTool(tool);
            }
        }

        // 如果没有指定工具，根据内容推断
        if (action.getTool() == null) {
            String nodeIds = (String) parameters.get("node_ids");
            if (nodeIds != null) {
                if (nodeIds.contains("continue")) {
                    action.setTool(DecomposeTool.CONTINUE);
                } else if (nodeIds.contains("complete") || nodeIds.contains("finish")) {
                    action.setTool(DecomposeTool.MARK_COMPLETE);
                } else if (nodeIds.contains("fail")) {
                    action.setTool(DecomposeTool.MARK_FAILED);
                } else {
                    action.setTool(DecomposeTool.DECOMPOSE);
                    // node_ids 作为子任务ID
                    List<Map<String, Object>> subtasks = new ArrayList<>();
                    for (String nodeId : nodeIds.split("[,\\s]+")) {
                        if (!nodeId.isEmpty()) {
                            Map<String, Object> task = new HashMap<>();
                            task.put("id", nodeId);
                            task.put("description", nodeId);
                            task.put("priority", 5);
                            task.put("dependencies", Collections.emptyList());
                            subtasks.add(task);
                        }
                    }
                    parameters.put("subtasks", subtasks);
                }
            }
        }

        action.setParameters(parameters);
    }

    /**
     * 从输出推断动作（当没有明确动作块时）
     */
    private AgentAction inferFromOutput(String output) {
        AgentAction action = new AgentAction();
        Map<String, Object> parameters = new HashMap<>();

        String lowerOutput = output.toLowerCase();

        // 根据关键词推断
        if (lowerOutput.contains("完成") || lowerOutput.contains("成功") || lowerOutput.contains("finished")) {
            action.setTool(DecomposeTool.MARK_COMPLETE);
            parameters.put("result", output);
            parameters.put("summary", extractSummary(output));
        } else if (lowerOutput.contains("失败") || lowerOutput.contains("错误") || lowerOutput.contains("error")) {
            action.setTool(DecomposeTool.MARK_FAILED);
            parameters.put("reason", output);
        } else if (lowerOutput.contains("分解") || lowerOutput.contains("子任务") || lowerOutput.contains("subtask")) {
            action.setTool(DecomposeTool.DECOMPOSE);
            // 尝试从文本提取任务列表
            parameters.put("subtasks", extractTasksFromText(output));
        } else if (lowerOutput.contains("继续") || lowerOutput.contains("continue")) {
            action.setTool(DecomposeTool.CONTINUE);
        } else {
            // 默认继续
            action.setTool(DecomposeTool.CONTINUE);
        }

        action.setThought(output.length() > 200 ? output.substring(0, 200) + "..." : output);
        action.setParameters(parameters);

        logger.info("推断动作: {} from output", action.getTool());
        return action;
    }

    /**
     * 提取摘要
     */
    private String extractSummary(String output) {
        // 取前100个字符作为摘要
        String cleaned = output.replaceAll("\\s+", " ").trim();
        return cleaned.length() > 100 ? cleaned.substring(0, 100) + "..." : cleaned;
    }

    /**
     * 从文本提取任务列表
     */
    private List<Map<String, Object>> extractTasksFromText(String text) {
        List<Map<String, Object>> tasks = new ArrayList<>();

        // 匹配数字编号的任务
        Pattern numberedTask = Pattern.compile("(\\d+)\\.\\s*(.+?)(?=\\d+\\.|$)", Pattern.DOTALL);
        Matcher matcher = numberedTask.matcher(text);

        while (matcher.find()) {
            Map<String, Object> task = new HashMap<>();
            task.put("id", "task_" + matcher.group(1));
            task.put("description", matcher.group(2).trim());
            task.put("priority", Integer.parseInt(matcher.group(1)));
            task.put("dependencies", Collections.emptyList());
            tasks.add(task);
        }

        // 匹配破折号列表
        if (tasks.isEmpty()) {
            Pattern dashTask = Pattern.compile("-\\s*(.+?)(?=-|$)", Pattern.DOTALL);
            matcher = dashTask.matcher(text);
            int num = 1;
            while (matcher.find()) {
                Map<String, Object> task = new HashMap<>();
                task.put("id", "task_" + num);
                task.put("description", matcher.group(1).trim());
                task.put("priority", num);
                task.put("dependencies", Collections.emptyList());
                tasks.add(task);
                num++;
            }
        }

        return tasks;
    }

    /**
     * 创建默认动作
     */
    private AgentAction createDefaultAction(String reason) {
        AgentAction action = new AgentAction();
        action.setTool(DecomposeTool.CONTINUE);
        action.setThought(reason);
        action.setParameters(new HashMap<>());
        return action;
    }
}