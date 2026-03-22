package com.openclaw.workflow.engine.smartdecompose.v2.prompt;

import com.openclaw.workflow.engine.smartdecompose.v2.model.DecomposeContext;
import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示词构建器
 *
 * 从模板构建最终提示词，注入动态参数。
 * 模板使用 {{key}} 占位符，支持条件块 {{#if key}}...{{/if}} 和循环 {{#each list}}...{{/each}}。
 */
@Component
public class PromptBuilder {

    /**
     * 构建决策提示词
     */
    public String buildDecisionPrompt(DecomposeContext context, SubTask task) {
        String template = context.getDecisionTemplateContent();
        if (template == null || template.isEmpty()) {
            throw new IllegalStateException("决策模板内容为空");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("projectPath", nvl(context.getProjectPath(), ""));
        params.put("techStack", nvl(context.getTechStack(), ""));
        params.put("taskDescription", nvl(task.getDescription(), ""));
        params.put("completedTasks", context.getCompletedTasksInfo());

        // 场景参数
        params.put("decisionThreshold", context.getDecisionThresholdMinutes());
        params.put("outputFormatExecute", nvl(context.getOutputFormatExecute(), getDefaultOutputFormatExecute()));
        params.put("outputFormatSplit", nvl(context.getOutputFormatSplit(), getDefaultOutputFormatSplit()));

        return render(template, params);
    }

    /**
     * 默认 execute 输出格式
     */
    private String getDefaultOutputFormatExecute() {
        return "{\n" +
            "  \"decision\": \"execute\",\n" +
            "  \"thought\": \"你的分析过程\",\n" +
            "  \"result\": \"执行后的结果描述\"\n" +
            "}";
    }

    /**
     * 默认 split 输出格式
     */
    private String getDefaultOutputFormatSplit() {
        return "{\n" +
            "  \"decision\": \"split\",\n" +
            "  \"thought\": \"你的分析过程\",\n" +
            "  \"tasks\": [\n" +
            "    {\n" +
            "      \"id\": \"TASK_001\",\n" +
            "      \"description\": \"子任务描述\",\n" +
            "      \"criteria\": \"验收标准\",\n" +
            "      \"estimatedMinutes\": 3\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    /**
     * 构建审核提示词
     */
    public String buildReviewPrompt(DecomposeContext context, SubTask task,
                                    String executionResult, List<String> previousIssues) {
        String template = context.getReviewTemplateContent();
        if (template == null || template.isEmpty()) {
            throw new IllegalStateException("审核模板内容为空");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("taskDescription", nvl(task.getDescription(), ""));
        params.put("criteria", nvl(task.getCriteria(), "无特殊要求"));
        params.put("executionResult", nvl(executionResult, ""));
        params.put("projectPath", nvl(context.getProjectPath(), ""));
        params.put("previousIssues", previousIssues);

        return render(template, params);
    }

    /**
     * 构建重试提示词
     */
    public String buildRetryPrompt(DecomposeContext context, SubTask task, List<String> issues) {
        String template = context.getRetryTemplateContent();
        if (template == null || template.isEmpty()) {
            // 回退：使用决策模板 + 问题追加
            return buildFallbackRetryPrompt(context, task, issues);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("taskDescription", nvl(task.getDescription(), ""));
        params.put("criteria", nvl(task.getCriteria(), "无特殊要求"));
        params.put("projectPath", nvl(context.getProjectPath(), ""));
        params.put("previousIssues", issues);

        return render(template, params);
    }

    /**
     * 回退方案：使用决策模板 + 问题追加
     */
    private String buildFallbackRetryPrompt(DecomposeContext context, SubTask task, List<String> issues) {
        String basePrompt = buildDecisionPrompt(context, task);

        StringBuilder retryPrompt = new StringBuilder(basePrompt);
        retryPrompt.append("\n\n# 上次执行失败\n");
        retryPrompt.append("审核发现以下问题：\n");
        for (String issue : issues) {
            retryPrompt.append("- ").append(issue).append("\n");
        }
        retryPrompt.append("\n请解决上述问题后重新执行。");

        return retryPrompt.toString();
    }

    /**
     * 渲染模板
     */
    private String render(String template, Map<String, Object> params) {
        String result = template;

        // 1. 替换简单占位符 {{key}}
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
            result = result.replace(placeholder, value);
        }

        // 2. 处理循环块 {{#each list}}...{{/each}}
        result = processLoops(result, params);

        // 3. 处理条件块 {{#if key}}...{{/if}}
        result = processConditionals(result, params);

        return result;
    }

    /**
     * 处理循环块
     *
     * 支持 {{#each list}}...{{this}}...{{@index}}...{{/each}} 语法
     */
    @SuppressWarnings("unchecked")
    private String processLoops(String template, Map<String, Object> params) {
        Pattern pattern = Pattern.compile(
            "\\{\\{#each (\\w+)\\}\\}(.*?)\\{\\{/each\\}\\}",
            Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String loopContent = matcher.group(2);

            Object value = params.get(varName);
            String replacement = "";

            if (value instanceof List) {
                List<String> list = (List<String>) value;
                StringBuilder loopResult = new StringBuilder();
                for (int i = 0; i < list.size(); i++) {
                    String item = list.get(i) != null ? list.get(i) : "";
                    String itemContent = loopContent
                        .replace("{{this}}", item)
                        .replace("{{@index}}", String.valueOf(i + 1));
                    loopResult.append(itemContent);
                }
                replacement = loopResult.toString();
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 处理条件块
     *
     * 支持 {{#if key}}...{{else}}...{{/if}} 语法
     */
    private String processConditionals(String template, Map<String, Object> params) {
        Pattern pattern = Pattern.compile(
            "\\{\\{#if (\\w+)\\}\\}(.*?)(?:\\{\\{else\\}\\}(.*?))?\\{\\{/if\\}\\}",
            Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String ifContent = matcher.group(2);
            String elseContent = matcher.group(3) != null ? matcher.group(3) : "";

            Object value = params.get(varName);
            boolean isTruthy = isTruthy(value);
            String replacement = isTruthy ? ifContent : elseContent;

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 判断值是否为真
     */
    private boolean isTruthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return !((String) value).isEmpty();
        }
        if (value instanceof List) {
            return !((List<?>) value).isEmpty();
        }
        return true;
    }

    /**
     * 空值处理
     */
    private String nvl(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
}