package com.openclaw.workflow.controller;

import com.openclaw.workflow.engine.smartdecompose.v2.DecomposeOrchestrator;
import com.openclaw.workflow.engine.smartdecompose.v2.client.OpenClawClient;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecomposeContext;
import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;
import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.DecomposeStatus;
import com.openclaw.workflow.entity.PromptTemplate;
import com.openclaw.workflow.repository.PromptTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SmartDecompose 集成测试控制器
 *
 * 用于自测 SmartDecompose 流程，真实调用 OpenClaw。
 * 包含详细的日志输出，方便排查问题。
 */
@RestController
@RequestMapping("/api/test/smart-decompose")
public class SmartDecomposeTestController {

    private static final Logger logger = LoggerFactory.getLogger(SmartDecomposeTestController.class);

    @Autowired
    private DecomposeOrchestrator orchestrator;

    @Autowired
    private PromptTemplateRepository promptTemplateRepository;

    @Autowired
    private OpenClawClient openClawClient;

    /**
     * 集成测试：实现登录注册功能
     *
     * GET /api/test/smart-decompose/login-register?projectPath=/path/to/project
     */
    @GetMapping("/login-register")
    public ResponseEntity<Map<String, Object>> testLoginRegister(
            @RequestParam(required = false, defaultValue = "/tmp/test-project") String projectPath) {

        String executionId = "test_" + System.currentTimeMillis();
        logger.info("========== SmartDecompose 集成测试开始 ==========");
        logger.info("[TEST] executionId: {}", executionId);
        logger.info("[TEST] projectPath: {}", projectPath);
        logger.info("[TEST] 任务: 实现一个完善的登录注册功能");

        try {
            // 1. 初始化上下文
            logger.info("[STEP-1] 初始化上下文...");
            DecomposeContext context = createTestContext(executionId, projectPath);
            logContextDetails(context);

            // 2. 加载模板
            logger.info("[STEP-2] 加载提示词模板...");
            loadTemplates(context);
            logTemplateDetails(context);

            // 3. 创建根任务
            logger.info("[STEP-3] 创建根任务...");
            SubTask rootTask = SubTask.builder()
                .id("TASK_ROOT")
                .description("实现一个完善的登录注册功能，包括：用户注册、用户登录、密码加密、会话管理")
                .depth(0)
                .build();
            context.getTaskQueue().add(rootTask);
            logger.info("[TEST] 根任务已创建: id={}, description={}", rootTask.getId(), rootTask.getDescription());

            // 4. 执行编排
            logger.info("[STEP-4] 开始执行编排（将真实调用 OpenClaw）...");
            logger.info("========================================");
            orchestrator.run(context);
            logger.info("========================================");
            logger.info("[STEP-4] 编排执行完成");

            // 5. 输出结果
            logger.info("[STEP-5] 输出执行结果...");
            logExecutionResult(context);

            // 6. 构建返回
            Map<String, Object> result = buildTestResult(context, executionId);
            logger.info("========== SmartDecompose 集成测试结束 ==========");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("[ERROR] 测试执行异常: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("executionId", executionId);
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 简单测试：只调用一次 OpenClaw
     *
     * GET /api/test/smart-decompose/simple?prompt=xxx
     */
    @GetMapping("/simple")
    public ResponseEntity<Map<String, Object>> testSimpleCall(
            @RequestParam(required = false, defaultValue = "请说 hello") String prompt) {

        String executionId = "simple_" + System.currentTimeMillis();
        logger.info("========== OpenClaw 简单调用测试 ==========");
        logger.info("[TEST] executionId: {}", executionId);
        logger.info("[TEST] prompt: {}", prompt);

        try {
            logger.info("[STEP-1] 开始 OpenClaw 会话...");
            openClawClient.startSession();

            logger.info("[STEP-2] 调用 OpenClaw...");
            long startTime = System.currentTimeMillis();
            String response = openClawClient.execute(prompt);
            long elapsed = System.currentTimeMillis() - startTime;

            logger.info("[STEP-3] OpenClaw 响应 (耗时 {}ms):", elapsed);
            logger.info("[RESPONSE] {}", response);

            logger.info("[STEP-4] 结束会话...");
            openClawClient.endSession();

            String sessionId = openClawClient.getSessionId();
            logger.info("[TEST] sessionId: {}", sessionId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("executionId", executionId);
            result.put("prompt", prompt);
            result.put("response", response);
            result.put("elapsedMs", elapsed);
            result.put("sessionId", sessionId);

            logger.info("========== 测试完成 ==========");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("[ERROR] 简单调用测试失败: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            errorResult.put("executionId", executionId);
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 测试决策流程：发送决策提示词并解析响应
     */
    @GetMapping("/decision")
    public ResponseEntity<Map<String, Object>> testDecision(
            @RequestParam(required = false, defaultValue = "/tmp/test-project") String projectPath,
            @RequestParam(required = false, defaultValue = "创建一个 Hello World 程序") String taskDescription) {

        String executionId = "decision_" + System.currentTimeMillis();
        logger.info("========== 决策流程测试 ==========");
        logger.info("[TEST] executionId: {}", executionId);
        logger.info("[TEST] taskDescription: {}", taskDescription);

        try {
            // 加载决策模板
            PromptTemplate decisionTemplate = promptTemplateRepository.findByTypeAndIsDefaultTrue("decision")
                .orElseThrow(() -> new IllegalStateException("未找到决策模板"));
            logger.info("[TEST] 决策模板ID: {}", decisionTemplate.getId());

            // 构建提示词
            String templateContent = decisionTemplate.getContent();
            String prompt = templateContent
                .replace("{{projectPath}}", projectPath)
                .replace("{{taskDescription}}", taskDescription)
                .replace("{{completedTasks}}", "暂无");

            logger.info("[STEP-1] 构建的决策提示词:");
            logger.info("[PROMPT-START] {}", prompt);
            logger.info("[PROMPT-END]");

            // 调用 OpenClaw
            logger.info("[STEP-2] 调用 OpenClaw...");
            openClawClient.startSession();
            long startTime = System.currentTimeMillis();
            String response = openClawClient.execute(prompt);
            long elapsed = System.currentTimeMillis() - startTime;

            logger.info("[STEP-3] OpenClaw 响应 (耗时 {}ms):", elapsed);
            logger.info("[RESPONSE] {}", response);

            openClawClient.endSession();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("executionId", executionId);
            result.put("taskDescription", taskDescription);
            result.put("prompt", prompt);
            result.put("response", response);
            result.put("elapsedMs", elapsed);

            logger.info("========== 测试完成 ==========");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("[ERROR] 决策测试失败: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    // ==================== 私有方法 ====================

    private DecomposeContext createTestContext(String executionId, String projectPath) {
        DecomposeContext context = new DecomposeContext();
        context.setExecutionId(executionId);
        context.setWorkflowId("test-workflow");
        context.setNodeId("test-node-smart-decompose");
        context.setProjectPath(projectPath);
        context.setTechStack("Java Spring Boot");
        context.setStatus(DecomposeStatus.RUNNING);
        context.setMaxRetries(3);
        context.setMaxIterations(20);
        context.setRequireManualReview(false);  // 测试时关闭人工审核
        return context;
    }

    private void loadTemplates(DecomposeContext context) {
        PromptTemplate decisionTemplate = promptTemplateRepository.findByTypeAndIsDefaultTrue("decision")
            .orElseThrow(() -> new IllegalStateException("未找到决策模板"));
        context.setDecisionTemplateContent(decisionTemplate.getContent());
        logger.info("[TEMPLATE] 决策模板已加载: id={}, contentLength={}",
            decisionTemplate.getId(), decisionTemplate.getContent().length());

        PromptTemplate reviewTemplate = promptTemplateRepository.findByTypeAndIsDefaultTrue("review")
            .orElseThrow(() -> new IllegalStateException("未找到审核模板"));
        context.setReviewTemplateContent(reviewTemplate.getContent());
        logger.info("[TEMPLATE] 审核模板已加载: id={}, contentLength={}",
            reviewTemplate.getId(), reviewTemplate.getContent().length());

        PromptTemplate retryTemplate = promptTemplateRepository.findByTypeAndIsDefaultTrue("retry")
            .orElseThrow(() -> new IllegalStateException("未找到重试模板"));
        context.setRetryTemplateContent(retryTemplate.getContent());
        logger.info("[TEMPLATE] 重试模板已加载: id={}, contentLength={}",
            retryTemplate.getId(), retryTemplate.getContent().length());
    }

    private void logContextDetails(DecomposeContext context) {
        logger.info("[CONTEXT] executionId: {}", context.getExecutionId());
        logger.info("[CONTEXT] workflowId: {}", context.getWorkflowId());
        logger.info("[CONTEXT] nodeId: {}", context.getNodeId());
        logger.info("[CONTEXT] projectPath: {}", context.getProjectPath());
        logger.info("[CONTEXT] techStack: {}", context.getTechStack());
        logger.info("[CONTEXT] maxRetries: {}", context.getMaxRetries());
        logger.info("[CONTEXT] maxIterations: {}", context.getMaxIterations());
        logger.info("[CONTEXT] requireManualReview: {}", context.isRequireManualReview());
        logger.info("[CONTEXT] status: {}", context.getStatus());
    }

    private void logTemplateDetails(DecomposeContext context) {
        String decisionContent = context.getDecisionTemplateContent();
        String reviewContent = context.getReviewTemplateContent();
        logger.info("[TEMPLATE] 决策模板长度: {}", decisionContent != null ? decisionContent.length() : 0);
        logger.info("[TEMPLATE] 审核模板长度: {}", reviewContent != null ? reviewContent.length() : 0);
    }

    private void logExecutionResult(DecomposeContext context) {
        logger.info("[RESULT] 最终状态: {}", context.getStatus());
        logger.info("[RESULT] 迭代次数: {}", context.getIterationCount());
        logger.info("[RESULT] 已完成任务数: {}", context.getCompletedTasks().size());
        logger.info("[RESULT] 失败任务数: {}", context.getFailedTasks().size());
        logger.info("[RESULT] 错误信息: {}", context.getErrorMessage());

        if (!context.getCompletedTasks().isEmpty()) {
            logger.info("[RESULT] 已完成任务列表:");
            for (int i = 0; i < context.getCompletedTasks().size(); i++) {
                SubTask task = context.getCompletedTasks().get(i);
                logger.info("  [{}] id={}, description={}",
                    i + 1, task.getId(), truncate(task.getDescription(), 100));
            }
        }

        if (!context.getFailedTasks().isEmpty()) {
            logger.info("[RESULT] 失败任务列表:");
            for (int i = 0; i < context.getFailedTasks().size(); i++) {
                SubTask task = context.getFailedTasks().get(i);
                logger.info("  [{}] id={}, description={}",
                    i + 1, task.getId(), truncate(task.getDescription(), 100));
            }
        }

        logger.info("[RESULT] OpenClaw会话ID: {}", context.getOpenClawSessionId());
    }

    private Map<String, Object> buildTestResult(DecomposeContext context, String executionId) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", context.getStatus() == DecomposeStatus.COMPLETED);
        result.put("executionId", executionId);
        result.put("status", context.getStatus().name());
        result.put("iterations", context.getIterationCount());
        result.put("completedTasks", context.getCompletedTasks().size());
        result.put("failedTasks", context.getFailedTasks().size());
        result.put("errorMessage", context.getErrorMessage());
        result.put("openClawSessionId", context.getOpenClawSessionId());

        // 添加任务详情
        java.util.List<Map<String, Object>> completedTaskDetails = new java.util.ArrayList<>();
        for (SubTask task : context.getCompletedTasks()) {
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("id", task.getId());
            taskInfo.put("description", task.getDescription());
            taskInfo.put("executionResult", truncate(task.getExecutionResult(), 500));
            completedTaskDetails.add(taskInfo);
        }
        result.put("completedTaskDetails", completedTaskDetails);

        return result;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...";
    }
}