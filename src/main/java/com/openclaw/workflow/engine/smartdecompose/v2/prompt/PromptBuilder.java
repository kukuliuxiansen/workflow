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
 * 模板使用 {{key}} 占位符，支持条件块 {{#if key}}...{{/if}}。
 */
@Component
public class PromptBuilder {

    /**
     * 构建决策提示词
     *
     * @param context 执行上下文
     * @param task    当前任务
     * @return 完整的决策提示词
     */
    public String buildDecisionPrompt(DecomposeContext context, SubTask task) {
        String template = context.getDecisionTemplateContent();
        if (template == null || template.isEmpty()) {
            throw new IllegalStateException("决策模板内容为空");
        }

        Map<String, String> params = new HashMap<>();
        params.put("projectPath", nvl(context.getProjectPath(), ""));
        params.put("techStack", nvl(context.getTechStack(), ""));
        params.put("taskDescription", nvl(task.getDescription(), ""));
        params.put("completedTasks", context.getCompletedTasksInfo());

        return render(template, params);
    }

    /**
     * 构建审核提示词
     *
     * @param context         执行上下文
     * @param task            当前任务
     * @param executionResult 执行结果
     * @param previousIssues  之前发现的问题列表
     * @return 完整的审核提示词
     */
    public String buildReviewPrompt(DecomposeContext context, SubTask task,
                                    String executionResult, List<String> previousIssues) {
        String template = context.getReviewTemplateContent();
        if (template == null || template.isEmpty()) {
            throw new IllegalStateException("审核模板内容为空");
        }

        Map<String, String> params = new HashMap<>();
        params.put("taskDescription", nvl(task.getDescription(), ""));
        params.put("criteria", nvl(task.getCriteria(), "无特殊要求"));
        params.put("executionResult", nvl(executionResult, ""));
        params.put("projectPath", nvl(context.getProjectPath(), ""));
        params.put("previousIssues", formatIssues(previousIssues));

        return render(template, params);
    }

    /**
     * 构建重试提示词
     *
     * @param context 执行上下文
     * @param task    当前任务
     * @param issues  审核发现的问题列表
     * @return 完整的重试提示词
     */
    public String buildRetryPrompt(DecomposeContext context, SubTask task, List<String> issues) {
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
     *
     * 替换 {{key}} 占位符，处理条件块
     */
    private String render(String template, Map<String, String> params) {
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return processConditionals(result, params);
    }

    /**
     * 处理条件块
     *
     * 支持 {{#if key}}...{{else}}...{{/if}} 语法
     */
    private String processConditionals(String template, Map<String, String> params) {
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

            String value = params.get(varName);
            String replacement = (value != null && !value.isEmpty()) ? ifContent : elseContent;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 格式化问题列表
     */
    private String formatIssues(List<String> issues) {
        if (issues == null || issues.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < issues.size(); i++) {
            sb.append(i + 1).append(". ").append(issues.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 空值处理
     */
    private String nvl(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
}