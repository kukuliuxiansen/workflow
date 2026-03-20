package com.openclaw.workflow.engine.smartdecompose;

/**
 * 智能分解异常类型
 */
public enum DecomposeExceptionType {

    // 执行限制异常
    MAX_ITERATION_EXCEEDED("超过最大迭代次数"),
    MAX_DEPTH_EXCEEDED("超过最大递归深度"),

    // Agent异常
    AGENT_EXECUTION_FAILED("Agent执行失败"),
    AGENT_TIMEOUT("Agent执行超时"),
    AGENT_OUTPUT_PARSE_ERROR("Agent输出解析失败"),

    // 任务异常
    TASK_DEPENDENCY_CYCLE("任务依赖循环"),
    TASK_EXECUTION_FAILED("任务执行失败"),

    // 工具异常
    TOOL_NOT_FOUND("工具不存在"),
    TOOL_PARAMETER_MISSING("工具参数缺失"),
    TOOL_EXECUTION_ERROR("工具执行错误"),

    // 子Agent异常
    SUBAGENT_FAILED("子Agent执行失败"),
    SUBAGENT_TIMEOUT("子Agent执行超时"),

    // 用户交互异常
    USER_INPUT_TIMEOUT("等待用户输入超时"),
    USER_INPUT_INVALID("用户输入无效");

    private final String message;

    DecomposeExceptionType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}