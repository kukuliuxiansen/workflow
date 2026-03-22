package com.openclaw.workflow.engine.smartdecompose.v2;

import com.openclaw.workflow.engine.smartdecompose.v2.client.OpenClawClient;
import com.openclaw.workflow.engine.smartdecompose.v2.client.ResponseParser;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecomposeContext;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecisionResponse;
import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;
import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.DecomposeStatus;
import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.SubTaskStatus;
import com.openclaw.workflow.engine.smartdecompose.v2.extension.DecisionHandler;
import com.openclaw.workflow.engine.smartdecompose.v2.extension.ExtensionRegistry;
import com.openclaw.workflow.engine.smartdecompose.v2.extension.TaskSplitInterceptor;
import com.openclaw.workflow.engine.smartdecompose.v2.persistence.DecisionRecorder;
import com.openclaw.workflow.engine.smartdecompose.v2.persistence.StatePersister;
import com.openclaw.workflow.engine.smartdecompose.v2.prompt.PromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 分解编排器
 *
 * SmartDecompose 的核心执行逻辑。
 * 控制主循环，协调决策、执行、审核、重试。
 */
@Component
public class DecomposeOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(DecomposeOrchestrator.class);

    @Autowired
    private PromptBuilder promptBuilder;

    @Autowired
    private OpenClawClient openClawClient;

    @Autowired
    private ResponseParser responseParser;

    @Autowired
    private StatePersister statePersister;

    @Autowired
    private DecisionRecorder decisionRecorder;

    @Autowired
    private ReviewProcessor reviewProcessor;

    @Autowired
    private ExtensionRegistry extensionRegistry;

    /**
     * 执行主循环
     *
     * 入参: DecomposeContext context
     * 出参: void（结果保存在 context 中）
     */
    public void run(DecomposeContext context) {
        logger.info("========================================");
        logger.info("[ORCHESTRATOR] ===== 开始执行 =====");
        logger.info("[ORCHESTRATOR] 入参 - executionId: {}", context.getExecutionId());
        logger.info("[ORCHESTRATOR] 入参 - workflowId: {}", context.getWorkflowId());
        logger.info("[ORCHESTRATOR] 入参 - nodeId: {}", context.getNodeId());
        logger.info("[ORCHESTRATOR] 入参 - projectPath: {}", context.getProjectPath());
        logger.info("[ORCHESTRATOR] 入参 - techStack: {}", context.getTechStack());
        logger.info("[ORCHESTRATOR] 入参 - maxRetries: {}", context.getMaxRetries());
        logger.info("[ORCHESTRATOR] 入参 - maxIterations: {}", context.getMaxIterations());
        logger.info("[ORCHESTRATOR] 入参 - requireManualReview: {}", context.isRequireManualReview());
        logger.info("[ORCHESTRATOR] 入参 - taskQueue初始大小: {}", context.getTaskQueue().size());
        logger.info("========================================");

        try {
            logger.info("[ORCHESTRATOR] 启动 OpenClaw 会话...");
            openClawClient.startSession();

            // 恢复会话时设置已有的 sessionId
            if (context.getOpenClawSessionId() != null) {
                openClawClient.setSessionId(context.getOpenClawSessionId());
                logger.info("[ORCHESTRATOR] 恢复 OpenClaw 会话: {}", context.getOpenClawSessionId());
            }

            while (!context.getTaskQueue().isEmpty()) {
                logger.info("----------------------------------------");
                logger.info("[ORCHESTRATOR] 迭代 #{}, 队列剩余: {}",
                    context.getIterationCount() + 1, context.getTaskQueue().size());

                if (context.getIterationCount() >= context.getMaxIterations()) {
                    context.setStatus(DecomposeStatus.ITERATION_EXCEEDED);
                    context.setErrorMessage("超过最大迭代次数: " + context.getMaxIterations());
                    logger.warn("[ORCHESTRATOR] 迭代超限: {}", context.getMaxIterations());
                    break;
                }

                // 检查任务总数限制
                if (context.getTotalTaskCount() >= context.getMaxTotalTasks()) {
                    context.setStatus(DecomposeStatus.ITERATION_EXCEEDED);
                    context.setErrorMessage("超过最大任务总数: " + context.getMaxTotalTasks());
                    logger.warn("[ORCHESTRATOR] 任务总数超限: {}, 当前: {}", context.getMaxTotalTasks(), context.getTotalTaskCount());
                    break;
                }

                SubTask currentTask = context.getTaskQueue().poll();
                if (currentTask == null) {
                    logger.warn("[ORCHESTRATOR] 队列为空，退出循环");
                    break;
                }

                context.setCurrentTask(currentTask);
                context.incrementIteration();
                currentTask.setStatus(SubTaskStatus.RUNNING);

                // 保存：任务开始
                statePersister.save(context);

                logger.info("[ORCHESTRATOR] 取出任务: id={}, depth={}, description={}",
                    currentTask.getId(), currentTask.getDepth(),
                    truncate(currentTask.getDescription(), 100));

                try {
                    DecisionResponse decision = makeDecision(context, currentTask);

                    if (decision.isSplit()) {
                        handleSplit(context, currentTask, decision);
                    } else {
                        handleExecute(context, currentTask, decision);
                    }

                    logger.info("[ORCHESTRATOR] 保存执行状态...");
                    statePersister.save(context);

                } catch (Exception e) {
                    logger.error("[ORCHESTRATOR] 任务执行异常: {}", e.getMessage(), e);
                    currentTask.setStatus(SubTaskStatus.FAILED);
                    context.addFailedTask(currentTask);
                    context.setErrorMessage(e.getMessage());
                }
                logger.info("----------------------------------------");
            }

            if (context.getStatus() == DecomposeStatus.RUNNING) {
                context.setStatus(DecomposeStatus.COMPLETED);
                logger.info("[ORCHESTRATOR] 所有任务处理完成");
            }

            // 保存会话ID以便恢复
            context.setOpenClawSessionId(openClawClient.getSessionId());
            logger.info("[ORCHESTRATOR] OpenClaw 会话ID: {}", context.getOpenClawSessionId());

        } finally {
            logger.info("[ORCHESTRATOR] 结束 OpenClaw 会话");
            openClawClient.endSession();
        }

        logger.info("========================================");
        logger.info("[ORCHESTRATOR] ===== 执行结束 =====");
        logger.info("[ORCHESTRATOR] 出参 - status: {}", context.getStatus());
        logger.info("[ORCHESTRATOR] 出参 - iterationCount: {}", context.getIterationCount());
        logger.info("[ORCHESTRATOR] 出参 - completedTasks: {}", context.getCompletedTasks().size());
        logger.info("[ORCHESTRATOR] 出参 - failedTasks: {}", context.getFailedTasks().size());
        logger.info("[ORCHESTRATOR] 出参 - errorMessage: {}", context.getErrorMessage());
        logger.info("========================================");
    }

    /**
     * 设置 OpenClaw 会话ID（用于恢复执行）
     *
     * @param sessionId 会话ID
     */
    public void setOpenClawSessionId(String sessionId) {
        openClawClient.setSessionId(sessionId);
    }

    /**
     * 决策阶段
     */
    private DecisionResponse makeDecision(DecomposeContext context, SubTask task) {
        logger.info("[DECISION] ===== 决策阶段开始 =====");
        logger.info("[DECISION] taskId: {}", task.getId());
        logger.info("[DECISION] taskDescription: {}", truncate(task.getDescription(), 200));

        String prompt = promptBuilder.buildDecisionPrompt(context, task);
        logger.info("[DECISION] 提示词长度: {} 字符", prompt.length());
        logger.debug("[DECISION] 提示词内容:\n{}", truncate(prompt, 1000));

        logger.info("[DECISION] 调用 OpenClaw...");
        long startTime = System.currentTimeMillis();
        String rawResponse = openClawClient.execute(prompt);
        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("[DECISION] OpenClaw 响应耗时: {}ms", elapsed);
        logger.info("[DECISION] OpenClaw 原始响应长度: {} 字符", rawResponse != null ? rawResponse.length() : 0);
        logger.info("[DECISION] OpenClaw 原始响应:\n{}", rawResponse);

        // 立即保存 sessionId（OpenClaw调用后可能已更新）
        context.setOpenClawSessionId(openClawClient.getSessionId());
        logger.info("[DECISION] 保存 sessionId: {}", context.getOpenClawSessionId());
        statePersister.save(context);

        DecisionResponse response = responseParser.parseDecision(rawResponse);
        logger.info("[DECISION] 解析结果: decision={}, thought={}",
            response.getDecision(),
            truncate(response.getThought(), 100));

        if (response.isSplit()) {
            logger.info("[DECISION] 拆分任务数: {}", response.getTasks() != null ? response.getTasks().size() : 0);
            if (response.getTasks() != null) {
                for (int i = 0; i < response.getTasks().size(); i++) {
                    DecisionResponse.SubTaskDef subTask = response.getTasks().get(i);
                    logger.info("[DECISION]   子任务[{}]: id={}, desc={}",
                        i, subTask.getId(), truncate(subTask.getDescription(), 100));
                }
            }
        } else {
            logger.info("[DECISION] 执行结果: {}", truncate(response.getResult(), 200));
        }

        decisionRecorder.record(context, task, response);

        // 调用自定义决策处理器
        List<DecisionHandler> handlers = extensionRegistry.getDecisionHandlers();
        logger.info("[DECISION] 注册的决策处理器数: {}", handlers.size());
        for (DecisionHandler handler : handlers) {
            if (handler.handle(context, task, response)) {
                logger.info("[DECISION] 决策处理器 {} 已处理", handler.getName());
                break;
            }
        }

        logger.info("[DECISION] ===== 决策阶段结束 =====");
        return response;
    }

    /**
     * 处理拆分决策
     */
    private void handleSplit(DecomposeContext context, SubTask task, DecisionResponse decision) {
        logger.info("[SPLIT] ===== 拆分阶段开始 =====");
        logger.info("[SPLIT] 父任务: id={}, depth={}", task.getId(), task.getDepth());
        logger.info("[SPLIT] 子任务数: {}", decision.getTasks().size());

        List<SubTask> subTasks = new ArrayList<>();
        for (DecisionResponse.SubTaskDef def : decision.getTasks()) {
            SubTask subTask = def.toSubTask();
            subTask.setParentTaskId(task.getId());
            subTask.setDepth(task.getDepth() + 1);
            subTasks.add(subTask);
            logger.info("[SPLIT] 创建子任务: id={}, depth={}, desc={}",
                subTask.getId(), subTask.getDepth(), truncate(subTask.getDescription(), 100));
        }

        // 调用拆分前拦截器
        List<TaskSplitInterceptor> interceptors = extensionRegistry.getSplitInterceptors();
        logger.info("[SPLIT] 拦截器数: {}", interceptors.size());
        for (TaskSplitInterceptor interceptor : interceptors) {
            logger.info("[SPLIT] 调用拦截器 beforeSplit: {}", interceptor.getName());
            if (!interceptor.beforeSplit(task, subTasks)) {
                logger.warn("[SPLIT] 拦截器 {} 阻止了拆分", interceptor.getName());
                return;
            }
        }

        // 深度优先：将子任务逆序入队，保证顺序执行
        logger.info("[SPLIT] 子任务入队（深度优先，逆序）...");
        for (int i = subTasks.size() - 1; i >= 0; i--) {
            context.getTaskQueue().addFirst(subTasks.get(i));
        }

        // 调用拆分后拦截器
        for (TaskSplitInterceptor interceptor : interceptors) {
            logger.info("[SPLIT] 调用拦截器 afterSplit: {}", interceptor.getName());
            interceptor.afterSplit(task, subTasks);
        }

        logger.info("[SPLIT] 当前队列长度: {}", context.getTaskQueue().size());
        logger.info("[SPLIT] ===== 拆分阶段结束 =====");
    }

    /**
     * 处理执行决策
     */
    private void handleExecute(DecomposeContext context, SubTask task, DecisionResponse decision) {
        logger.info("[EXECUTE] ===== 执行阶段开始 =====");
        logger.info("[EXECUTE] taskId: {}", task.getId());
        logger.info("[EXECUTE] 执行分析(来自决策): {}", truncate(decision.getThought(), 200));

        // 执行阶段也可能返回 split（如测试发现BUG需要创建修复任务）
        if (decision.isSplit()) {
            logger.info("[EXECUTE] 执行结果需要拆分任务（如发现BUG）");
            handleSplit(context, task, decision);
            logger.info("[EXECUTE] ===== 执行阶段结束 =====");
            return;
        }

        logger.info("[EXECUTE] 执行结果(来自决策): {}", truncate(decision.getResult(), 200));

        String executionResult = decision.getResult();
        String executionThought = decision.getThought();
        logger.info("[EXECUTE] 调用审核处理器...");
        boolean approved = reviewProcessor.reviewAndRetry(context, task, executionResult, executionThought);

        if (approved) {
            task.setStatus(SubTaskStatus.COMPLETED);
            task.setExecutionResult(executionResult);
            context.addCompletedTask(task);
            logger.info("[EXECUTE] 任务完成: taskId={}", task.getId());
        } else {
            task.setStatus(SubTaskStatus.FAILED);
            context.addFailedTask(task);
            logger.warn("[EXECUTE] 任务失败: taskId={}", task.getId());
        }
        logger.info("[EXECUTE] ===== 执行阶段结束 =====");
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "null";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...";
    }
}