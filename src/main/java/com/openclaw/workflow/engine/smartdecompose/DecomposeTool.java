package com.openclaw.workflow.engine.smartdecompose;

import java.util.Arrays;
import java.util.List;

/**
 * 智能分解节点可用工具
 */
public enum DecomposeTool {

    /**
     * 分解任务 - 将任务拆分为子任务
     */
    DECOMPOSE(
        "decompose",
        "将当前任务分解为多个子任务",
        Arrays.asList("subtasks"),
        "用于复杂任务，需要拆分为更小的可执行单元"
    ),

    /**
     * 执行原子任务 - 直接执行简单任务
     */
    EXECUTE(
        "execute",
        "直接执行原子任务（简单、单一的操作）",
        Arrays.asList("action_type", "parameters"),
        "用于简单任务，预估耗时<5分钟"
    ),

    /**
     * 启动子Agent - 委托给专门的子Agent
     */
    SPAWN_AGENT(
        "spawn_agent",
        "启动子Agent处理专门任务",
        Arrays.asList("agent_type", "prompt"),
        "用于需要专门技能的任务，如代码探索、架构规划"
    ),

    /**
     * 标记完成 - 标记当前任务完成
     */
    MARK_COMPLETE(
        "mark_complete",
        "标记当前任务为完成状态",
        Arrays.asList("result", "summary"),
        "任务执行完成时调用"
    ),

    /**
     * 标记失败 - 标记任务失败
     */
    MARK_FAILED(
        "mark_failed",
        "标记当前任务失败",
        Arrays.asList("reason"),
        "无法继续执行时使用"
    ),

    /**
     * 读取上下文 - 获取项目环境信息
     */
    READ_CONTEXT(
        "read_context",
        "读取项目文件或环境信息",
        Arrays.asList("target"),
        "需要了解项目状态时使用"
    ),

    /**
     * 写入产物 - 创建或修改文件
     */
    WRITE_ARTIFACT(
        "write_artifact",
        "创建或修改项目文件",
        Arrays.asList("path", "content"),
        "输出代码、配置等产物时使用"
    ),

    /**
     * 执行命令 - 运行shell命令
     */
    RUN_COMMAND(
        "run_command",
        "执行shell命令",
        Arrays.asList("command"),
        "需要运行测试、安装依赖等时使用"
    ),

    /**
     * 继续执行 - 保持当前状态继续下一轮
     */
    CONTINUE(
        "continue",
        "继续执行下一轮迭代",
        Arrays.asList(),
        "需要更多迭代时使用"
    );

    private final String name;
    private final String description;
    private final List<String> requiredParameters;
    private final String usageGuide;

    DecomposeTool(String name, String description, List<String> requiredParameters, String usageGuide) {
        this.name = name;
        this.description = description;
        this.requiredParameters = requiredParameters;
        this.usageGuide = usageGuide;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getRequiredParameters() { return requiredParameters; }
    public String getUsageGuide() { return usageGuide; }

    public static DecomposeTool fromName(String name) {
        if (name == null) return null;
        for (DecomposeTool tool : values()) {
            if (tool.name.equalsIgnoreCase(name)) {
                return tool;
            }
        }
        return null;
    }
}