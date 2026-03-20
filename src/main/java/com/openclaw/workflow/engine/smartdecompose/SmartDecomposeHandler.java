package com.openclaw.workflow.engine.smartdecompose;

import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import com.openclaw.workflow.engine.handler.BaseNodeHandler;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.entity.DecisionHistory;
import com.openclaw.workflow.entity.DecisionHistoryId;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.repository.DecisionHistoryRepository;
import com.openclaw.workflow.repository.SmartDecomposeStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 智能分解节点处理器 - 主类
 */
public class SmartDecomposeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(SmartDecomposeHandler.class);

    private final AgentOutputParser outputParser;
    private final ActionExecutor actionExecutor;
    private final String gatewayUrl;
    private final String gatewayToken;
    private final String agentId;

    private SmartDecomposeStateRepository stateRepository;
    private DecisionHistoryRepository decisionHistoryRepository;
    private DecomposeStateManager stateManager;

    public SmartDecomposeHandler(String gatewayUrl, String gatewayToken, String agentId) {
        this.gatewayUrl = gatewayUrl;
        this.gatewayToken = gatewayToken;
        this.agentId = agentId;
        this.outputParser = new AgentOutputParser();
        this.actionExecutor = new ActionExecutor(gatewayUrl, gatewayToken);
    }

    public SmartDecomposeHandler() {
        this("http://localhost:18789",
             "56b640cc2d91411f63255af68355c19ee33c88ec458878ca",
             "project-manager");
    }

    public void setStateRepository(SmartDecomposeStateRepository stateRepository) {
        this.stateRepository = stateRepository;
        this.stateManager = new DecomposeStateManager(stateRepository);
    }

    public void setDecisionHistoryRepository(DecisionHistoryRepository decisionHistoryRepository) {
        this.decisionHistoryRepository = decisionHistoryRepository;
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) throws Exception {
        logger.info("========== 智能分解节点开始执行 ==========");

        DecomposeContext context = initializeContext(ctx);
        TaskState initialTask = createInitialTask(ctx);
        context.getTaskStack().push(initialTask);
        context.getTaskMap().put(initialTask.getTaskId(), initialTask);

        while (!isFinished(context)) {
            if (context.isIterationExceeded()) {
                context.setStatus(DecomposeContext.DecomposeStatus.TIMEOUT);
                break;
            }

            TaskState currentTask = getNextTask(context);
            if (currentTask == null) {
                context.setStatus(DecomposeContext.DecomposeStatus.COMPLETED);
                break;
            }

            context.setCurrentTask(currentTask);
            currentTask.setStatus(TaskState.TaskStatus.RUNNING);

            try {
                String prompt = DecomposePromptBuilder.buildPrompt(context, currentTask);
                String agentOutput = executeAgent(ctx, prompt, context.getIterationCount());
                AgentAction action = outputParser.parse(agentOutput);
                ActionResult result = actionExecutor.execute(context, action);

                updateContext(context, action, result);
                recordDecision(context, currentTask, action, result);

            } catch (Exception e) {
                logger.error("迭代 {} 执行异常: {}", context.getIterationCount() + 1, e.getMessage());
                recordDecision(context, currentTask, createErrorAction(e.getMessage()), ActionResult.error(e.getMessage()));
            }

            context.incrementIteration();
        }

        if (stateManager != null) {
            stateManager.saveState(context);
        }
        return buildFinalResult(context);
    }

    private DecomposeContext initializeContext(NodeExecutionContext ctx) {
        DecomposeContext context = new DecomposeContext();
        context.setExecutionId(ctx.getExecutionId());
        context.setWorkflowId(ctx.getWorkflowId());
        context.setNodeId(ctx.getNode().getId());
        context.setProjectPath(ctx.getProjectPath());
        context.setGlobalPrompt(ctx.getGlobalPrompt());

        WorkflowNode node = ctx.getNode();
        if (node.getConfig() != null && !node.getConfig().isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(node.getConfig(), Map.class);
                if (config.containsKey("maxIterations")) {
                    context.setMaxIterations(((Number) config.get("maxIterations")).intValue());
                }
                if (config.containsKey("maxDepth")) {
                    context.setMaxDepth(((Number) config.get("maxDepth")).intValue());
                }
            } catch (Exception e) {
                logger.warn("解析节点配置失败: {}", e.getMessage());
            }
        }
        return context;
    }

    private TaskState createInitialTask(NodeExecutionContext ctx) {
        String description = ctx.getTaskDescription();
        if (description == null || description.isEmpty()) {
            description = "执行智能分解任务";
        }
        return TaskState.builder()
            .taskId("task_root_" + System.currentTimeMillis())
            .description(description)
            .status(TaskState.TaskStatus.PENDING)
            .depth(0)
            .priority(1)
            .dependencies(new ArrayList<>())
            .build();
    }

    private boolean isFinished(DecomposeContext context) {
        return context.getStatus() != DecomposeContext.DecomposeStatus.RUNNING;
    }

    private TaskState getNextTask(DecomposeContext context) {
        while (!context.getTaskStack().isEmpty()) {
            TaskState task = context.getTaskStack().pop();
            if (task.getStatus() == TaskState.TaskStatus.PENDING) {
                return task;
            }
        }
        return null;
    }

    private String executeAgent(NodeExecutionContext ctx, String prompt, int iteration) throws Exception {
        OpenClawGatewayClient client = new OpenClawGatewayClient(gatewayUrl, gatewayToken);

        OpenClawGatewayClient.AgentRequest request = OpenClawGatewayClient.AgentRequest.builder()
            .agentId(agentId)
            .systemPrompt(DecomposePromptBuilder.buildSystemPrompt())
            .message(prompt)
            .context("decompose_" + ctx.getExecutionId() + "_" + iteration)
            .build();

        OpenClawGatewayClient.AgentResponse response = client.executeAgent(request);

        if (!response.isSuccess()) {
            throw new RuntimeException("Agent执行失败: " + response.getErrorMessage());
        }
        return response.getContent();
    }

    private void updateContext(DecomposeContext context, AgentAction action, ActionResult result) {
        context.getOutputBuffer().append(result.getMessage()).append("\n");

        if (!result.isShouldContinue() && context.getCurrentTask() != null) {
            TaskState current = context.getCurrentTask();
            if (current.getStatus() == TaskState.TaskStatus.COMPLETED ||
                current.getStatus() == TaskState.TaskStatus.FAILED) {
                context.setCurrentTask(null);
            }
        }
    }

    private void recordDecision(DecomposeContext context, TaskState task, AgentAction action, ActionResult result) {
        DecomposeContext.DecisionRecord record = new DecomposeContext.DecisionRecord();
        record.setIteration(context.getIterationCount());
        record.setTaskId(task.getTaskId());
        record.setAction(action.getTool().getName());
        record.setResult(result);
        context.getDecisionHistory().add(record);

        if (decisionHistoryRepository != null) {
            try {
                DecisionHistory history = new DecisionHistory();
                history.setId(new DecisionHistoryId(context.getExecutionId(), context.getIterationCount()));
                history.setTaskId(task.getTaskId());
                history.setAction(action.getTool().getName());
                history.setSuccess(result.isSuccess());
                history.setMessage(result.getMessage());
                decisionHistoryRepository.save(history);
            } catch (Exception e) {
                logger.warn("保存决策历史失败: {}", e.getMessage());
            }
        }
    }

    private AgentAction createErrorAction(String errorMessage) {
        AgentAction action = new AgentAction();
        action.setTool(DecomposeTool.CONTINUE);
        action.setThought("执行出错: " + errorMessage);
        action.setParameters(new HashMap<>());
        return action;
    }

    private NodeResult buildFinalResult(DecomposeContext context) {
        logger.info("智能分解执行结束: status={}, iterations={}, tasks={}",
            context.getStatus(), context.getIterationCount(), context.getCompletedTasks().size());

        Map<String, Object> result = new HashMap<>();
        result.put("status", context.getStatus().name());
        result.put("iterations", context.getIterationCount());
        result.put("completedTasks", context.getCompletedTasks().size());

        List<Map<String, Object>> taskResults = new ArrayList<>();
        for (TaskState task : context.getCompletedTasks()) {
            Map<String, Object> taskResult = new HashMap<>();
            taskResult.put("id", task.getTaskId());
            taskResult.put("description", task.getDescription());
            taskResult.put("result", task.getResult());
            taskResults.add(taskResult);
        }
        result.put("taskResults", taskResults);

        if (context.getStatus() == DecomposeContext.DecomposeStatus.COMPLETED) {
            return NodeResult.success(result);
        } else {
            return NodeResult.failed(result, context.getErrorMessage());
        }
    }

    @Override
    public List<String> validate(WorkflowNode node) {
        List<String> errors = new ArrayList<>();
        if (node.getConfig() == null || node.getConfig().isEmpty()) {
            return errors;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(node.getConfig(), Map.class);
            if (config.containsKey("maxIterations")) {
                int maxIter = ((Number) config.get("maxIterations")).intValue();
                if (maxIter < 1 || maxIter > 1000) {
                    errors.add("maxIterations必须在1-1000之间");
                }
            }
        } catch (Exception e) {
            errors.add("配置格式错误: " + e.getMessage());
        }
        return errors;
    }
}