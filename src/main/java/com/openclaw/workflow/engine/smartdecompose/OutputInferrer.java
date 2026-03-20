package com.openclaw.workflow.engine.smartdecompose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 输出推断器
 * 当没有明确动作块时，从非结构化输出推断动作
 */
public class OutputInferrer {

    private static final Logger logger = LoggerFactory.getLogger(OutputInferrer.class);

    /**
     * 从输出推断动作
     */
    public AgentAction infer(String output) {
        AgentAction action = new AgentAction();
        Map<String, Object> parameters = new HashMap<>();

        String lowerOutput = output.toLowerCase();

        // 根据关键词推断
        if (containsCompletionKeywords(lowerOutput)) {
            action.setTool(DecomposeTool.MARK_COMPLETE);
            parameters.put("result", output);
            parameters.put("summary", extractSummary(output));
        } else if (containsFailureKeywords(lowerOutput)) {
            action.setTool(DecomposeTool.MARK_FAILED);
            parameters.put("reason", output);
        } else if (containsDecomposeKeywords(lowerOutput)) {
            action.setTool(DecomposeTool.DECOMPOSE);
            parameters.put("subtasks", extractTasksFromText(output));
        } else if (containsContinueKeywords(lowerOutput)) {
            action.setTool(DecomposeTool.CONTINUE);
        } else {
            action.setTool(DecomposeTool.CONTINUE);
        }

        action.setThought(truncateThought(output));
        action.setParameters(parameters);

        logger.info("推断动作: {} from output", action.getTool());
        return action;
    }

    /**
     * 检查是否包含完成关键词
     */
    private boolean containsCompletionKeywords(String lowerOutput) {
        return lowerOutput.contains("完成") || lowerOutput.contains("成功") || lowerOutput.contains("finished");
    }

    /**
     * 检查是否包含失败关键词
     */
    private boolean containsFailureKeywords(String lowerOutput) {
        return lowerOutput.contains("失败") || lowerOutput.contains("错误") || lowerOutput.contains("error");
    }

    /**
     * 检查是否包含分解关键词
     */
    private boolean containsDecomposeKeywords(String lowerOutput) {
        return lowerOutput.contains("分解") || lowerOutput.contains("子任务") || lowerOutput.contains("subtask");
    }

    /**
     * 检查是否包含继续关键词
     */
    private boolean containsContinueKeywords(String lowerOutput) {
        return lowerOutput.contains("继续") || lowerOutput.contains("continue");
    }

    /**
     * 提取摘要
     */
    private String extractSummary(String output) {
        String cleaned = output.replaceAll("\\s+", " ").trim();
        return cleaned.length() > 100 ? cleaned.substring(0, 100) + "..." : cleaned;
    }

    /**
     * 截断思考内容
     */
    private String truncateThought(String output) {
        return output.length() > 200 ? output.substring(0, 200) + "..." : output;
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
            tasks = extractDashTasks(text);
        }

        return tasks;
    }

    /**
     * 从破折号列表提取任务
     */
    private List<Map<String, Object>> extractDashTasks(String text) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        Pattern dashTask = Pattern.compile("-\\s*(.+?)(?=-|$)", Pattern.DOTALL);
        Matcher matcher = dashTask.matcher(text);
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

        return tasks;
    }
}