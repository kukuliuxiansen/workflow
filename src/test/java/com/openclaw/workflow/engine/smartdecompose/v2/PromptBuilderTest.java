package com.openclaw.workflow.engine.smartdecompose.v2;

import com.openclaw.workflow.engine.smartdecompose.v2.model.DecomposeContext;
import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;
import com.openclaw.workflow.engine.smartdecompose.v2.prompt.PromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PromptBuilder 单元测试
 */
class PromptBuilderTest {

    private PromptBuilder promptBuilder;
    private DecomposeContext context;
    private SubTask task;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder();
        context = new DecomposeContext();
        context.setExecutionId("test-exec-001");
        context.setProjectPath("/test/project");
        context.setTechStack("Java, Spring Boot");

        task = SubTask.builder()
            .id("TASK_001")
            .description("实现用户登录功能")
            .criteria("必须支持用户名密码登录")
            .estimatedMinutes(10)
            .build();
    }

    @Test
    void testBuildDecisionPrompt_Basic() {
        String template = "项目路径: {{projectPath}}\n" +
            "技术栈: {{techStack}}\n" +
            "任务: {{taskDescription}}\n" +
            "已完成任务: {{completedTasks}}";

        context.setDecisionTemplateContent(template);

        String prompt = promptBuilder.buildDecisionPrompt(context, task);

        assertTrue(prompt.contains("/test/project"));
        assertTrue(prompt.contains("Java, Spring Boot"));
        assertTrue(prompt.contains("实现用户登录功能"));
        assertTrue(prompt.contains("暂无"));
    }

    @Test
    void testBuildDecisionPrompt_WithCompletedTasks() {
        String template = "{{completedTasks}}";
        context.setDecisionTemplateContent(template);

        SubTask completed = SubTask.builder()
            .id("TASK_000")
            .description("已完成任务")
            .build();
        completed.setExecutionResult("成功");
        context.addCompletedTask(completed);

        String prompt = promptBuilder.buildDecisionPrompt(context, task);

        assertTrue(prompt.contains("已完成任务"));
        assertTrue(prompt.contains("成功"));
    }

    @Test
    void testBuildDecisionPrompt_ConditionalBlock() {
        String template = "任务: {{taskDescription}}\n" +
            "{{#if techStack}}技术栈: {{techStack}}{{/if}}";

        context.setDecisionTemplateContent(template);

        String prompt = promptBuilder.buildDecisionPrompt(context, task);

        assertTrue(prompt.contains("技术栈: Java, Spring Boot"));
    }

    @Test
    void testBuildDecisionPrompt_ConditionalBlock_Empty() {
        String template = "任务: {{taskDescription}}\n" +
            "{{#if techStack}}技术栈: {{techStack}}{{else}}无技术栈{{/if}}";

        context.setDecisionTemplateContent(template);
        context.setTechStack("");

        String prompt = promptBuilder.buildDecisionPrompt(context, task);

        assertTrue(prompt.contains("无技术栈"));
        assertFalse(prompt.contains("技术栈:"));
    }

    @Test
    void testBuildDecisionPrompt_MissingTemplate() {
        context.setDecisionTemplateContent(null);

        assertThrows(IllegalStateException.class, () -> promptBuilder.buildDecisionPrompt(context, task));
    }

    @Test
    void testBuildReviewPrompt_Basic() {
        String template = "任务: {{taskDescription}}\n" +
            "验收标准: {{criteria}}\n" +
            "执行结果: {{executionResult}}\n" +
            "执行思路: {{executionThought}}\n" +
            "之前的问题: {{previousIssues}}";

        context.setReviewTemplateContent(template);

        String prompt = promptBuilder.buildReviewPrompt(context, task, "登录成功", "分析过程", Arrays.asList("问题1", "问题2"));

        assertTrue(prompt.contains("实现用户登录功能"));
        assertTrue(prompt.contains("必须支持用户名密码登录"));
        assertTrue(prompt.contains("登录成功"));
        assertTrue(prompt.contains("分析过程"));
        assertTrue(prompt.contains("问题1"));
        assertTrue(prompt.contains("问题2"));
    }

    @Test
    void testBuildReviewPrompt_EmptyPreviousIssues() {
        String template = "问题: {{previousIssues}}";
        context.setReviewTemplateContent(template);

        String prompt = promptBuilder.buildReviewPrompt(context, task, "结果", "分析", Collections.emptyList());

        assertFalse(prompt.contains("1."));
    }

    @Test
    void testBuildReviewPrompt_MissingTemplate() {
        context.setReviewTemplateContent(null);

        assertThrows(IllegalStateException.class,
            () -> promptBuilder.buildReviewPrompt(context, task, "结果", "分析", Collections.emptyList()));
    }

    @Test
    void testBuildRetryPrompt() {
        String decisionTemplate = "任务: {{taskDescription}}";
        context.setDecisionTemplateContent(decisionTemplate);

        String retryPrompt = promptBuilder.buildRetryPrompt(context, task, Arrays.asList("问题A", "问题B"));

        assertTrue(retryPrompt.contains("实现用户登录功能"));
        assertTrue(retryPrompt.contains("上次执行失败"));
        assertTrue(retryPrompt.contains("问题A"));
        assertTrue(retryPrompt.contains("问题B"));
    }

    @Test
    void testRender_MultiplePlaceholders() {
        String template = "{{a}} + {{b}} = {{c}}";
        context.setDecisionTemplateContent(template);

        String prompt = promptBuilder.buildDecisionPrompt(context, task);

        // 占位符没有被参数匹配时保持原样
        assertTrue(prompt.contains("{{a}}"));
    }

    @Test
    void testNullValueHandling() {
        String template = "路径: {{projectPath}}";
        context.setDecisionTemplateContent(template);
        context.setProjectPath(null);

        String prompt = promptBuilder.buildDecisionPrompt(context, task);

        assertTrue(prompt.contains("路径: "));
    }
}