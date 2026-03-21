package com.openclaw.workflow.engine.smartdecompose.v2;

import com.openclaw.workflow.engine.smartdecompose.v2.client.OpenClawClient;
import com.openclaw.workflow.engine.smartdecompose.v2.client.ResponseParser;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecomposeContext;
import com.openclaw.workflow.engine.smartdecompose.v2.model.DecisionResponse;
import com.openclaw.workflow.engine.smartdecompose.v2.model.SubTask;
import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.DecomposeStatus;
import com.openclaw.workflow.engine.smartdecompose.v2.model.enums.SubTaskStatus;
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

    /**
     * 执行主循环
     */
    public void run(DecomposeContext context) {
        logger.info("开始执行: executionId={}", context.getExecutionId());

        try {
            openClawClient.startSession();

            while (!context.getTaskQueue().isEmpty()) {
                if (context.getIterationCount() >= context.getMaxIterations()) {
                    context.setStatus(DecomposeStatus.ITERATION_EXCEEDED);
                    context.setErrorMessage("超过最大迭代次数: " + context.getMaxIterations());
                    logger.warn("迭代超限: {}", context.getMaxIterations());
                    break;
                }

                SubTask currentTask = context.getTaskQueue().poll();
                if (currentTask == null) {
                    break;
                }

                context.setCurrentTask(currentTask);
                context.incrementIteration();
                currentTask.setStatus(SubTaskStatus.RUNNING);

                logger.info("处理任务: id={}, description={}", currentTask.getId(), currentTask.getDescription());

                try {
                    DecisionResponse decision = makeDecision(context, currentTask);

                    if (decision.isSplit()) {
                        handleSplit(context, currentTask, decision);
                    } else {
                        handleExecute(context, currentTask, decision);
                    }

                    statePersister.save(context);

                } catch (Exception e) {
                    logger.error("任务执行异常: {}", e.getMessage(), e);
                    currentTask.setStatus(SubTaskStatus.FAILED);
                    context.addFailedTask(currentTask);
                    context.setErrorMessage(e.getMessage());
                }
            }

            if (context.getStatus() == DecomposeStatus.RUNNING) {
                context.setStatus(DecomposeStatus.COMPLETED);
                logger.info("执行完成: completedTasks={}, failedTasks={}",
                    context.getCompletedTasks().size(), context.getFailedTasks().size());
            }

        } finally {
            openClawClient.endSession();
        }
    }

    /**
     * 决策阶段
     */
    private DecisionResponse makeDecision(DecomposeContext context, SubTask task) {
        logger.debug("决策阶段: taskId={}", task.getId());

        String prompt = promptBuilder.buildDecisionPrompt(context, task);
        String rawResponse = openClawClient.execute(prompt);
        DecisionResponse response = responseParser.parseDecision(rawResponse);

        logger.info("决策结果: decision={}, thought={}",
            response.getDecision(),
            response.getThought() != null ? response.getThought().substring(0, Math.min(50, response.getThought().length())) : "");

        decisionRecorder.record(context, task, response);

        return response;
    }

    /**
     * 处理拆分决策
     */
    private void handleSplit(DecomposeContext context, SubTask task, DecisionResponse decision) {
        logger.info("任务拆分为 {} 个子任务", decision.getTasks().size());

        List<SubTask> subTasks = new ArrayList<>();
        for (DecisionResponse.SubTaskDef def : decision.getTasks()) {
            SubTask subTask = def.toSubTask();
            subTask.setParentTaskId(task.getId());
            subTask.setDepth(task.getDepth() + 1);
            subTasks.add(subTask);
        }

        for (int i = subTasks.size() - 1; i >= 0; i--) {
            context.getTaskQueue().addFirst(subTasks.get(i));
        }

        logger.debug("子任务入队完成，当前队列长度: {}", context.getTaskQueue().size());
    }

    /**
     * 处理执行决策
     */
    private void handleExecute(DecomposeContext context, SubTask task, DecisionResponse decision) {
        String executionResult = decision.getResult();
        boolean approved = reviewProcessor.reviewAndRetry(context, task, executionResult);

        if (approved) {
            task.setStatus(SubTaskStatus.COMPLETED);
            task.setExecutionResult(executionResult);
            context.addCompletedTask(task);
            logger.info("任务完成: {}", task.getId());
        } else {
            task.setStatus(SubTaskStatus.FAILED);
            context.addFailedTask(task);
            logger.warn("任务失败: {}", task.getId());
        }
    }
}