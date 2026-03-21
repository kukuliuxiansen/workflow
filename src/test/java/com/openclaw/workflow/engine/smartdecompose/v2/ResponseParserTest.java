package com.openclaw.workflow.engine.smartdecompose.v2;

import com.openclaw.workflow.engine.smartdecompose.v2.client.ResponseParser;
import com.openclaw.workflow.engine.smartdecompose.v2.client.ResponseParseException;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecisionResponse;
import com.openclaw.workflow.engine.smartdecompose.v2.model.ReviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResponseParser 单元测试
 */
class ResponseParserTest {

    private ResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new ResponseParser();
    }

    // ==================== 决策响应解析测试 ====================

    @Test
    void testParseDecision_Execute() {
        String response = "{\n" +
            "  \"decision\": \"execute\",\n" +
            "  \"thought\": \"任务足够简单，可以直接执行\",\n" +
            "  \"result\": \"执行结果描述\"\n" +
            "}";

        DecisionResponse decision = parser.parseDecision(response);

        assertTrue(decision.isExecute());
        assertFalse(decision.isSplit());
        assertEquals("execute", decision.getDecision());
        assertEquals("任务足够简单，可以直接执行", decision.getThought());
        assertEquals("执行结果描述", decision.getResult());
    }

    @Test
    void testParseDecision_Split() {
        String response = "{\n" +
            "  \"decision\": \"split\",\n" +
            "  \"thought\": \"任务太复杂，需要拆分\",\n" +
            "  \"tasks\": [\n" +
            "    {\"id\": \"TASK_001\", \"description\": \"子任务1\", \"estimatedMinutes\": 5},\n" +
            "    {\"id\": \"TASK_002\", \"description\": \"子任务2\", \"estimatedMinutes\": 3}\n" +
            "  ]\n" +
            "}";

        DecisionResponse decision = parser.parseDecision(response);

        assertTrue(decision.isSplit());
        assertFalse(decision.isExecute());
        assertEquals("split", decision.getDecision());
        assertEquals(2, decision.getTasks().size());
        assertEquals("TASK_001", decision.getTasks().get(0).getId());
        assertEquals("子任务1", decision.getTasks().get(0).getDescription());
    }

    @Test
    void testParseDecision_MarkdownJsonBlock() {
        String response = "```json\n" +
            "{\n" +
            "  \"decision\": \"execute\",\n" +
            "  \"thought\": \"测试Markdown包裹\",\n" +
            "  \"result\": \"结果\"\n" +
            "}\n" +
            "```";

        DecisionResponse decision = parser.parseDecision(response);

        assertTrue(decision.isExecute());
        assertEquals("测试Markdown包裹", decision.getThought());
    }

    @Test
    void testParseDecision_MarkdownCodeBlock() {
        String response = "```\n" +
            "{\n" +
            "  \"decision\": \"execute\",\n" +
            "  \"thought\": \"测试普通代码块\",\n" +
            "  \"result\": \"结果\"\n" +
            "}\n" +
            "```";

        DecisionResponse decision = parser.parseDecision(response);

        assertTrue(decision.isExecute());
        assertEquals("测试普通代码块", decision.getThought());
    }

    @Test
    void testParseDecision_EmbeddedInText() {
        String response = "好的，我来分析这个任务：\n\n" +
            "{\n" +
            "  \"decision\": \"execute\",\n" +
            "  \"thought\": \"嵌入在文本中的JSON\",\n" +
            "  \"result\": \"结果\"\n" +
            "}\n\n" +
            "以上是我的分析。";

        DecisionResponse decision = parser.parseDecision(response);

        assertTrue(decision.isExecute());
        assertEquals("嵌入在文本中的JSON", decision.getThought());
    }

    @Test
    void testParseDecision_MissingDecision() {
        String response = "{\"thought\": \"缺少decision字段\"}";

        assertThrows(ResponseParseException.class, () -> parser.parseDecision(response));
    }

    @Test
    void testParseDecision_MissingThought() {
        String response = "{\"decision\": \"execute\"}";

        assertThrows(ResponseParseException.class, () -> parser.parseDecision(response));
    }

    @Test
    void testParseDecision_InvalidDecision() {
        String response = "{\"decision\": \"invalid\", \"thought\": \"无效决策\"}";

        assertThrows(ResponseParseException.class, () -> parser.parseDecision(response));
    }

    @Test
    void testParseDecision_SplitWithoutTasks() {
        String response = "{\"decision\": \"split\", \"thought\": \"没有子任务\"}";

        assertThrows(ResponseParseException.class, () -> parser.parseDecision(response));
    }

    // ==================== 审核响应解析测试 ====================

    @Test
    void testParseReview_Approved() {
        String response = "{\n" +
            "  \"status\": \"APPROVED\",\n" +
            "  \"thought\": \"任务完成良好\",\n" +
            "  \"summary\": \"执行成功，符合要求\"\n" +
            "}";

        ReviewResponse review = parser.parseReview(response);

        assertTrue(review.isApproved());
        assertFalse(review.isRejected());
        assertEquals("APPROVED", review.getStatus());
        assertEquals("任务完成良好", review.getThought());
        assertEquals("执行成功，符合要求", review.getSummary());
    }

    @Test
    void testParseReview_Rejected() {
        String response = "{\n" +
            "  \"status\": \"REJECTED\",\n" +
            "  \"thought\": \"存在以下问题\",\n" +
            "  \"issues\": [\"问题1\", \"问题2\"],\n" +
            "  \"suggestion\": \"修改建议\"\n" +
            "}";

        ReviewResponse review = parser.parseReview(response);

        assertTrue(review.isRejected());
        assertFalse(review.isApproved());
        assertEquals("REJECTED", review.getStatus());
        assertEquals(2, review.getIssues().size());
        assertEquals("修改建议", review.getSuggestion());
    }

    @Test
    void testParseReview_MarkdownJsonBlock() {
        String response = "```json\n" +
            "{\n" +
            "  \"status\": \"APPROVED\",\n" +
            "  \"thought\": \"审核通过\",\n" +
            "  \"summary\": \"完成\"\n" +
            "}\n" +
            "```";

        ReviewResponse review = parser.parseReview(response);

        assertTrue(review.isApproved());
    }

    @Test
    void testParseReview_MissingStatus() {
        String response = "{\"thought\": \"缺少status字段\"}";

        assertThrows(ResponseParseException.class, () -> parser.parseReview(response));
    }

    @Test
    void testParseReview_MissingThought() {
        String response = "{\"status\": \"APPROVED\"}";

        assertThrows(ResponseParseException.class, () -> parser.parseReview(response));
    }

    @Test
    void testParseReview_RejectedWithoutIssues() {
        String response = "{\"status\": \"REJECTED\", \"thought\": \"没有问题列表\"}";

        assertThrows(ResponseParseException.class, () -> parser.parseReview(response));
    }

    // ==================== 边界情况测试 ====================

    @Test
    void testParseDecision_EmptyResponse() {
        assertThrows(ResponseParseException.class, () -> parser.parseDecision(""));
    }

    @Test
    void testParseDecision_NullResponse() {
        assertThrows(ResponseParseException.class, () -> parser.parseDecision(null));
    }

    @Test
    void testParseDecision_NestedJson() {
        String response = "{\n" +
            "  \"decision\": \"execute\",\n" +
            "  \"thought\": \"嵌套JSON测试 {key: value}\",\n" +
            "  \"result\": \"结果 {nested: true}\"\n" +
            "}";

        DecisionResponse decision = parser.parseDecision(response);

        assertTrue(decision.isExecute());
        assertTrue(decision.getThought().contains("{key: value}"));
    }

    @Test
    void testParseDecision_SubTaskDef_toSubTask() {
        String response = "{\n" +
            "  \"decision\": \"split\",\n" +
            "  \"thought\": \"拆分测试\",\n" +
            "  \"tasks\": [\n" +
            "    {\"id\": \"T1\", \"description\": \"任务\", \"criteria\": \"标准\", \"estimatedMinutes\": 10}\n" +
            "  ]\n" +
            "}";

        DecisionResponse decision = parser.parseDecision(response);
        DecisionResponse.SubTaskDef subTaskDef = decision.getTasks().get(0);
        com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask subTask = subTaskDef.toSubTask();

        assertEquals("T1", subTask.getId());
        assertEquals("任务", subTask.getDescription());
        assertEquals("标准", subTask.getCriteria());
        assertEquals(10, subTask.getEstimatedMinutes());
    }
}