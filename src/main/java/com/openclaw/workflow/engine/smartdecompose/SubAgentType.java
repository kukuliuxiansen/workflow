package com.openclaw.workflow.engine.smartdecompose;

/**
 * 子Agent类型定义
 */
public enum SubAgentType {

    /**
     * 探索Agent - 快速搜索和分析代码库
     */
    EXPLORE(
        "explore",
        "快速探索代码库，搜索文件和内容",
        5,
        "用于了解项目结构、查找特定代码、分析依赖关系"
    ),

    /**
     * 规划Agent - 分析架构，制定计划
     */
    PLAN(
        "plan",
        "分析需求，制定实现计划和架构方案",
        10,
        "用于复杂任务的架构设计和实施规划"
    ),

    /**
     * 执行Agent - 执行具体编码任务
     */
    EXECUTE(
        "execute",
        "执行编码、配置、部署等具体任务",
        30,
        "用于实现具体功能的代码编写"
    ),

    /**
     * 测试Agent - 运行测试并分析结果
     */
    TEST(
        "test",
        "运行测试，分析失败原因，尝试修复",
        15,
        "用于测试执行、问题诊断和修复"
    ),

    /**
     * 审查Agent - 代码审查和质量检查
     */
    REVIEW(
        "review",
        "审查代码质量，检查安全性和最佳实践",
        10,
        "用于代码评审、安全检查、规范验证"
    ),

    /**
     * 文档Agent - 生成和维护文档
     */
    DOCUMENT(
        "document",
        "生成和维护项目文档",
        10,
        "用于编写README、API文档、注释等"
    ),

    /**
     * 通用Agent - 处理一般任务
     */
    GENERAL_PURPOSE(
        "general-purpose",
        "处理通用任务",
        30,
        "用于一般性任务处理"
    );

    private final String name;
    private final String description;
    private final int timeoutMinutes;
    private final String usageGuide;

    SubAgentType(String name, String description, int timeoutMinutes, String usageGuide) {
        this.name = name;
        this.description = description;
        this.timeoutMinutes = timeoutMinutes;
        this.usageGuide = usageGuide;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getTimeoutMinutes() { return timeoutMinutes; }
    public String getUsageGuide() { return usageGuide; }

    /**
     * 根据名称获取类型
     */
    public static SubAgentType fromName(String name) {
        if (name == null) return GENERAL_PURPOSE;
        for (SubAgentType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return GENERAL_PURPOSE;
    }
}