package com.openclaw.workflow.engine.smartdecompose.v2.template;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模板变量定义
 *
 * 定义不同模板类型可用的变量
 */
public class TemplateVariable {

    private final String name;
    private final String displayName;
    private final TemplateType templateType;

    public TemplateVariable(String name, String displayName, TemplateType templateType) {
        this.name = name;
        this.displayName = displayName;
        this.templateType = templateType;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public TemplateType getTemplateType() { return templateType; }

    /**
     * 模板类型
     */
    public enum TemplateType {
        DECISION("decision", "决策模板"),
        REVIEW("review", "审核模板"),
        RETRY("retry", "重试模板");

        private final String code;
        private final String displayName;

        TemplateType(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        public String getCode() { return code; }
        public String getDisplayName() { return displayName; }
    }

    // ==================== 决策模板变量 ====================
    public static final TemplateVariable PROJECT_PATH = new TemplateVariable("projectPath", "项目路径", TemplateType.DECISION);
    public static final TemplateVariable TECH_STACK = new TemplateVariable("techStack", "技术栈", TemplateType.DECISION);
    public static final TemplateVariable TASK_DESCRIPTION = new TemplateVariable("taskDescription", "任务描述", TemplateType.DECISION);
    public static final TemplateVariable TASK_CRITERIA = new TemplateVariable("taskCriteria", "验收标准", TemplateType.DECISION);
    public static final TemplateVariable COMPLETED_TASKS = new TemplateVariable("completedTasks", "已完成任务", TemplateType.DECISION);

    // ==================== 审核模板变量 ====================
    public static final TemplateVariable REVIEW_TASK_DESCRIPTION = new TemplateVariable("taskDescription", "任务描述", TemplateType.REVIEW);
    public static final TemplateVariable REVIEW_CRITERIA = new TemplateVariable("criteria", "验收标准", TemplateType.REVIEW);
    public static final TemplateVariable EXECUTION_RESULT = new TemplateVariable("executionResult", "执行结果", TemplateType.REVIEW);
    public static final TemplateVariable EXECUTION_THOUGHT = new TemplateVariable("executionThought", "执行者分析", TemplateType.REVIEW);
    public static final TemplateVariable REVIEW_PROJECT_PATH = new TemplateVariable("projectPath", "项目路径", TemplateType.REVIEW);
    public static final TemplateVariable PREVIOUS_ISSUES = new TemplateVariable("previousIssues", "之前的问题", TemplateType.REVIEW);

    // ==================== 重试模板变量 ====================
    public static final TemplateVariable RETRY_TASK_DESCRIPTION = new TemplateVariable("taskDescription", "任务描述", TemplateType.RETRY);
    public static final TemplateVariable RETRY_CRITERIA = new TemplateVariable("criteria", "验收标准", TemplateType.RETRY);
    public static final TemplateVariable ISSUES = new TemplateVariable("issues", "问题列表", TemplateType.RETRY);

    /**
     * 所有变量列表
     */
    private static final List<TemplateVariable> ALL_VARIABLES = Arrays.asList(
            PROJECT_PATH, TECH_STACK, TASK_DESCRIPTION, TASK_CRITERIA, COMPLETED_TASKS,
            REVIEW_TASK_DESCRIPTION, REVIEW_CRITERIA, EXECUTION_RESULT, EXECUTION_THOUGHT, REVIEW_PROJECT_PATH, PREVIOUS_ISSUES,
            RETRY_TASK_DESCRIPTION, RETRY_CRITERIA, ISSUES
    );

    /**
     * 获取指定类型的变量列表
     */
    public static List<TemplateVariable> getByType(TemplateType type) {
        return ALL_VARIABLES.stream()
                .filter(v -> v.getTemplateType() == type)
                .collect(Collectors.toList());
    }

    /**
     * 获取变量信息的Map形式（用于API返回）
     */
    public static List<VariableInfo> getVariableInfoList(TemplateType type) {
        return getByType(type).stream()
                .map(v -> new VariableInfo(v.getName(), v.getDisplayName()))
                .collect(Collectors.toList());
    }

    /**
     * 变量信息DTO
     */
    public static class VariableInfo {
        private String name;
        @JsonProperty("displayName")
        private String displayName;

        public VariableInfo(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }
}