package com.openclaw.workflow.engine.smartdecompose.v2.prompt;

import java.util.Arrays;
import java.util.List;

/**
 * 提示词输出格式常量
 *
 * 硬编码在代码中，定义 OpenClaw 应该返回的 JSON 格式。
 * 这些格式与 ResponseParser 的解析逻辑强绑定，不可配置。
 */
public final class PromptOutputFormat {

    private PromptOutputFormat() {
        // 私有构造方法，禁止实例化
    }

    // ===================== 决策输出格式 =====================

    /**
     * execute 决策的 JSON 格式示例
     * 用于提示词中展示期望的输出格式
     */
    public static final String DECISION_EXECUTE_FORMAT =
        "{\n" +
        "  \"decision\": \"execute\",\n" +
        "  \"thought\": \"你的分析过程\",\n" +
        "  \"result\": \"执行结果描述\"\n" +
        "}";

    /**
     * split 决策的 JSON 格式示例
     * 用于提示词中展示期望的输出格式
     */
    public static final String DECISION_SPLIT_FORMAT =
        "{\n" +
        "  \"decision\": \"split\",\n" +
        "  \"thought\": \"你的分析过程\",\n" +
        "  \"tasks\": [\n" +
        "    {\n" +
        "      \"id\": \"TASK_XXX\",\n" +
        "      \"description\": \"子任务描述\",\n" +
        "      \"criteria\": \"验收标准\",\n" +
        "      \"estimatedMinutes\": 3\n" +
        "    }\n" +
        "  ]\n" +
        "}";

    /**
     * 决策响应必填字段
     */
    public static final List<String> DECISION_REQUIRED_FIELDS = Arrays.asList(
        "decision", "thought"
    );

    /**
     * 有效的决策值
     */
    public static final List<String> VALID_DECISIONS = Arrays.asList(
        "execute", "split"
    );

    // ===================== 审核输出格式 =====================

    /**
     * APPROVED 审核的 JSON 格式示例
     */
    public static final String REVIEW_APPROVED_FORMAT =
        "{\n" +
        "  \"status\": \"APPROVED\",\n" +
        "  \"thought\": \"审核分析过程\",\n" +
        "  \"summary\": \"任务完成情况总结\"\n" +
        "}";

    /**
     * REJECTED 审核的 JSON 格式示例
     */
    public static final String REVIEW_REJECTED_FORMAT =
        "{\n" +
        "  \"status\": \"REJECTED\",\n" +
        "  \"thought\": \"审核分析过程\",\n" +
        "  \"issues\": [\"问题1\", \"问题2\"],\n" +
        "  \"suggestion\": \"修改建议\"\n" +
        "}";

    /**
     * 审核响应必填字段
     */
    public static final List<String> REVIEW_REQUIRED_FIELDS = Arrays.asList(
        "status", "thought"
    );

    /**
     * 有效的审核状态值
     */
    public static final List<String> VALID_REVIEW_STATUSES = Arrays.asList(
        "APPROVED", "REJECTED"
    );
}