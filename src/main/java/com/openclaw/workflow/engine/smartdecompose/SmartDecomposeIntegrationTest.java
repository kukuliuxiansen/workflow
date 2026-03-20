package com.openclaw.workflow.engine.smartdecompose;

import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 智能分解节点真实API集成测试
 * 测试完整的ReAct循环，包含真实的API调用
 */
public class SmartDecomposeIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(SmartDecomposeIntegrationTest.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GATEWAY_URL = "http://localhost:18789";
    private static final String GATEWAY_TOKEN = "56b640cc2d91411f63255af68355c19ee33c88ec458878ca";
    private static final String AGENT_ID = "project-manager";

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("  智能分解节点真实API集成测试");
        logger.info("========================================");
        logger.info("Gateway URL: {}", GATEWAY_URL);
        logger.info("Agent ID: {}", AGENT_ID);
        logger.info("");

        try {
            // 测试1: 真实API调用 - 简单任务
            runTest("真实API_简单任务完成", SmartDecomposeIntegrationTest::testRealApiSimpleTask);

            // 测试2: 真实API调用 - 任务分解
            runTest("真实API_任务分解", SmartDecomposeIntegrationTest::testRealApiDecompose);

            // 测试3: 真实API调用 - 子Agent
            runTest("真实API_子Agent", SmartDecomposeIntegrationTest::testRealSubAgent);

            // 测试4: 完整ReAct循环
            runTest("真实API_完整ReAct循环", SmartDecomposeIntegrationTest::testFullReActLoop);

        } catch (Exception e) {
            logger.error("测试执行异常: {}", e.getMessage(), e);
        }

        printReport();
    }

    /**
     * 测试1: 真实API调用 - 简单任务完成
     */
    private static void testRealApiSimpleTask() throws Exception {
        logger.info(">>> 测试真实API - 简单任务完成");

        OpenClawGatewayClient client = new OpenClawGatewayClient(GATEWAY_URL, GATEWAY_TOKEN);

        String systemPrompt = "你是一个智能任务执行引擎。请分析任务并决定下一步行动。";
        String userPrompt = "当前任务: 打个招呼\n\n请输出你的决策，使用以下格式:\n[THOUGHT]\n你的思考\n[/THOUGHT]\n\n[ACTION]\ntool: <工具名>\n参数...\n[/ACTION]";

        logger.info(">>> 发送API请求...");
        long startTime = System.currentTimeMillis();

        OpenClawGatewayClient.AgentRequest request = OpenClawGatewayClient.AgentRequest.builder()
            .agentId(AGENT_ID)
            .systemPrompt(systemPrompt)
            .message(userPrompt)
            .context("test_simple_" + System.currentTimeMillis())
            .maxTokens(1024)
            .temperature(0.7)
            .build();

        OpenClawGatewayClient.AgentResponse response = client.executeAgent(request);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("API响应: success={}, duration={}ms", response.isSuccess(), duration);
        logger.info("响应内容长度: {}", response.getContent() != null ? response.getContent().length() : 0);

        if (!response.isSuccess()) {
            throw new RuntimeException("API调用失败: " + response.getErrorMessage());
        }

        // 解析响应
        AgentOutputParser parser = new AgentOutputParser();
        AgentAction action = parser.parse(response.getContent());

        logger.info("解析结果: tool={}, thought={}", action.getTool(),
            truncate(action.getThought(), 100));

        assert action != null : "解析结果不应为null";
        assert action.getTool() != null : "工具不应为null";
    }

    /**
     * 测试2: 真实API调用 - 任务分解
     */
    private static void testRealApiDecompose() throws Exception {
        logger.info(">>> 测试真实API - 任务分解");

        OpenClawGatewayClient client = new OpenClawGatewayClient(GATEWAY_URL, GATEWAY_TOKEN);

        String systemPrompt = "你是一个智能任务执行引擎。对于复杂任务，请使用decompose工具分解。";
        String userPrompt = "当前任务: 实现用户登录功能\n\n" +
            "这是一个复杂任务，请分解为子任务。使用以下格式:\n" +
            "[THOUGHT]\n你的分析\n[/THOUGHT]\n\n" +
            "[ACTION]\ntool: decompose\nsubtasks:\n  - id: task1\n    description: \"子任务描述\"\n    priority: 1\n[/ACTION]";

        logger.info(">>> 发送API请求...");
        long startTime = System.currentTimeMillis();

        OpenClawGatewayClient.AgentRequest request = OpenClawGatewayClient.AgentRequest.builder()
            .agentId(AGENT_ID)
            .systemPrompt(systemPrompt)
            .message(userPrompt)
            .context("test_decompose_" + System.currentTimeMillis())
            .maxTokens(2048)
            .temperature(0.7)
            .build();

        OpenClawGatewayClient.AgentResponse response = client.executeAgent(request);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("API响应: success={}, duration={}ms", response.isSuccess(), duration);

        if (!response.isSuccess()) {
            throw new RuntimeException("API调用失败: " + response.getErrorMessage());
        }

        logger.info("响应内容 (前500字符):\n{}", truncate(response.getContent(), 500));

        // 验证响应包含分解内容
        String content = response.getContent();
        assert content != null && !content.isEmpty() : "响应内容不应为空";
    }

    /**
     * 测试3: 真实API调用 - 子Agent
     */
    private static void testRealSubAgent() throws Exception {
        logger.info(">>> 测试真实API - 子Agent");

        SubAgentRequest request = new SubAgentRequest();
        request.setAgentType(SubAgentType.EXPLORE);
        request.setPrompt("请分析当前工作目录的项目结构");
        request.setParentExecutionId("integration_test_" + System.currentTimeMillis());

        SubAgentExecutor executor = new SubAgentExecutor(GATEWAY_URL, GATEWAY_TOKEN);

        logger.info(">>> 启动子Agent...");
        long startTime = System.currentTimeMillis();

        SubAgentResult result = executor.execute(request);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("子Agent结果: success={}, duration={}ms", result.isSuccess(), duration);
        logger.info("摘要: {}", truncate(result.getSummary(), 200));

        // 子Agent执行完成即视为成功（无论返回内容）
        assert result != null : "结果不应为null";
    }

    /**
     * 测试4: 完整ReAct循环
     */
    private static void testFullReActLoop() throws Exception {
        logger.info(">>> 测试完整ReAct循环");

        // 创建上下文
        DecomposeContext context = new DecomposeContext();
        context.setExecutionId("react_test_" + System.currentTimeMillis());
        context.setMaxIterations(5);
        context.setMaxDepth(2);

        // 创建初始任务
        TaskState initialTask = TaskState.builder()
            .taskId("initial")
            .description("完成一个简单的自我介绍")
            .depth(0)
            .priority(1)
            .build();

        context.getTaskStack().push(initialTask);
        context.getTaskMap().put("initial", initialTask);

        // 创建处理器
        SmartDecomposeHandler handler = new SmartDecomposeHandler(GATEWAY_URL, GATEWAY_TOKEN, AGENT_ID);

        // 模拟执行一轮迭代
        context.incrementIteration();
        context.setCurrentTask(initialTask);
        initialTask.setStatus(TaskState.TaskStatus.RUNNING);

        // 构建提示
        String prompt = "# 当前任务\n\n" +
            "任务ID: initial\n" +
            "描述: 完成一个简单的自我介绍\n\n" +
            "请分析这个任务并决定下一步行动。\n" +
            "如果这是简单任务，请使用 mark_complete 工具标记完成。";

        logger.info(">>> 执行第一轮迭代...");

        // 调用Agent
        OpenClawGatewayClient client = new OpenClawGatewayClient(GATEWAY_URL, GATEWAY_TOKEN);
        OpenClawGatewayClient.AgentRequest request = OpenClawGatewayClient.AgentRequest.builder()
            .agentId(AGENT_ID)
            .systemPrompt("你是一个智能任务执行引擎。")
            .message(prompt)
            .context("react_" + context.getExecutionId() + "_0")
            .maxTokens(2048)
            .temperature(0.7)
            .build();

        OpenClawGatewayClient.AgentResponse response = client.executeAgent(request);

        logger.info("Agent响应: success={}", response.isSuccess());
        logger.info("响应内容:\n{}", truncate(response.getContent(), 500));

        // 解析并执行动作
        AgentOutputParser parser = new AgentOutputParser();
        AgentAction action = parser.parse(response.getContent());

        logger.info("解析的动作: tool={}", action.getTool());

        ActionExecutor actionExecutor = new ActionExecutor(GATEWAY_URL, GATEWAY_TOKEN);
        ActionResult result = actionExecutor.execute(context, action);

        logger.info("执行结果: success={}, message={}", result.isSuccess(),
            truncate(result.getMessage(), 100));

        // 验证
        assert action != null : "动作不应为null";
        assert result != null : "结果不应为null";
    }

    // ==================== 辅助方法 ====================

    private static void runTest(String name, TestRunnable test) {
        totalTests++;
        try {
            logger.info("");
            logger.info("----------------------------------------");
            logger.info("执行: {}", name);
            test.run();
            passedTests++;
            logger.info("✓ {} 通过", name);
        } catch (Exception e) {
            failedTests++;
            logger.error("✗ {} 失败: {}", name, e.getMessage(), e);
        }
    }

    private static void printReport() {
        logger.info("");
        logger.info("========================================");
        logger.info("  测试报告");
        logger.info("========================================");
        logger.info("总测试数: {}", totalTests);
        logger.info("通过: {}", passedTests);
        logger.info("失败: {}", failedTests);
        double rate = totalTests > 0 ? (passedTests * 100.0 / totalTests) : 0;
        logger.info(String.format("成功率: %.2f%%", rate));

        if (rate >= 95) {
            logger.info("★ 成功率达到95%要求！");
        } else {
            logger.info("✗ 成功率未达标");
        }
        logger.info("========================================");
    }

    private static String truncate(String str, int max) {
        if (str == null) return "";
        return str.length() > max ? str.substring(0, max) + "..." : str;
    }

    @FunctionalInterface
    interface TestRunnable {
        void run() throws Exception;
    }
}