package com.openclaw.workflow.engine.smartdecompose;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import com.openclaw.workflow.engine.handler.BaseNodeHandler;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.entity.DecisionHistory;
import com.openclaw.workflow.entity.DecisionHistoryId;
import com.openclaw.workflow.entity.SmartDecomposeState;
import com.openclaw.workflow.entity.WorkflowNode;
import com.openclaw.workflow.repository.DecisionHistoryRepository;
import com.openclaw.workflow.repository.SmartDecomposeStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 智能分解节点处理器
 * 实现ReAct循环（Reasoning + Acting）模式
 */
public class SmartDecomposeHandler extends BaseNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(SmartDecomposeHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AgentOutputParser outputParser;
    private final ActionExecutor actionExecutor;
    private final String gatewayUrl;
    private final String gatewayToken;
    private final String agentId;

    // 数据库持久化（通过setter注入）
    private SmartDecomposeStateRepository stateRepository;
    private DecisionHistoryRepository decisionHistoryRepository;

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

    // Setter注入Repository
    public void setStateRepository(SmartDecomposeStateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    public void setDecisionHistoryRepository(DecisionHistoryRepository decisionHistoryRepository) {
        this.decisionHistoryRepository = decisionHistoryRepository;
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) throws Exception {
        logger.info("========== 智能分解节点开始执行 ==========");
        logger.info("ExecutionId: {}, NodeId: {}", ctx.getExecutionId(), ctx.getNode().getId());

        // 1. 初始化上下文
        DecomposeContext context = initializeContext(ctx);

        // 2. 创建初始任务
        TaskState initialTask = createInitialTask(ctx);
        context.getTaskStack().push(initialTask);
        context.getTaskMap().put(initialTask.getTaskId(), initialTask);

        logger.info("初始任务: {}", initialTask.getDescription());

        // 3. 主循环
        while (!isFinished(context)) {
            // 3.1 检查循环限制
            if (context.isIterationExceeded()) {
                logger.warn("达到最大迭代次数: {}", context.getMaxIterations());
                context.setStatus(DecomposeContext.DecomposeStatus.TIMEOUT);
                context.setErrorMessage("超过最大迭代次数: " + context.getMaxIterations());
                break;
            }

            // 3.2 获取下一个任务
            TaskState currentTask = getNextTask(context);
            if (currentTask == null) {
                logger.info("任务栈为空，执行完成");
                context.setStatus(DecomposeContext.DecomposeStatus.COMPLETED);
                break;
            }

            context.setCurrentTask(currentTask);
            currentTask.setStatus(TaskState.TaskStatus.RUNNING);

            logger.info("---------- 迭代 {} ----------", context.getIterationCount() + 1);
            logger.info("当前任务: {} (depth={}, priority={})",
                currentTask.getDescription(), currentTask.getDepth(), currentTask.getPriority());

            try {
                // 3.3 构建Agent提示
                String prompt = buildPrompt(context, currentTask);

                // 3.4 执行Agent
                String agentOutput = executeAgent(ctx, prompt, context.getIterationCount());

                // 3.5 解析Agent输出
                AgentAction action = outputParser.parse(agentOutput);
                logger.info("Agent决策: tool={}, thought={}",
                    action.getTool(), truncate(action.getThought(), 100));

                // 3.6 执行动作
                ActionResult actionResult = actionExecutor.execute(context, action);
                logger.info("动作执行结果: success={}, message={}",
                    actionResult.isSuccess(), truncate(actionResult.getMessage(), 100));

                // 3.7 更新状态
                updateContext(context, action, actionResult);

                // 3.8 记录决策
                recordDecision(context, currentTask, action, actionResult);

            } catch (Exception e) {
                logger.error("迭代 {} 执行异常: {}", context.getIterationCount() + 1, e.getMessage(), e);
                // 记录错误但继续执行
                ActionResult errorResult = ActionResult.error(e.getMessage());
                recordDecision(context, currentTask,
                    createErrorAction(e.getMessage()), errorResult);
            }

            context.incrementIteration();
        }

        // 4. 构建最终结果
        // 保存最终状态到数据库
        saveState(context);
        return buildFinalResult(context);
    }

    /**
     * 保存状态到数据库
     */
    private void saveState(DecomposeContext context) {
        if (stateRepository == null) return;

        try {
            SmartDecomposeState state = new SmartDecomposeState();
            state.setId(context.getExecutionId() + "_" + context.getNodeId());
            state.setExecutionId(context.getExecutionId());
            state.setNodeId(context.getNodeId());
            state.setStatus(context.getStatus().name());
            state.setErrorMessage(context.getErrorMessage());
            state.setCurrentIteration(context.getIterationCount());
            state.setMaxIterations(context.getMaxIterations());
            state.setCurrentDepth(context.getCurrentTask() != null ? context.getCurrentTask().getDepth() : 0);
            state.setMaxDepth(context.getMaxDepth());
            state.setCurrentTaskId(context.getCurrentTask() != null ? context.getCurrentTask().getTaskId() : null);

            // 序列化任务栈
            if (!context.getTaskStack().isEmpty()) {
                state.setTaskStack(objectMapper.writeValueAsString(context.getTaskStack()));
            }

            // 序列化已完成任务
            if (!context.getCompletedTasks().isEmpty()) {
                state.setCompletedTasks(objectMapper.writeValueAsString(context.getCompletedTasks()));
            }

            // 序列化任务映射
            if (!context.getTaskMap().isEmpty()) {
                state.setTaskMap(objectMapper.writeValueAsString(context.getTaskMap()));
            }

            // 序列化决策历史
            if (!context.getDecisionHistory().isEmpty()) {
                state.setDecisionHistory(objectMapper.writeValueAsString(context.getDecisionHistory()));
            }

            state.setUpdatedAt(LocalDateTime.now());
            if (state.getCreatedAt() == null) {
                state.setCreatedAt(LocalDateTime.now());
            }

            stateRepository.save(state);
            logger.debug("状态已保存到数据库: executionId={}, status={}", context.getExecutionId(), state.getStatus());
        } catch (Exception e) {
            logger.warn("保存状态到数据库失败: {}", e.getMessage());
        }
    }

    /**
     * 初始化执行上下文
     */
    private DecomposeContext initializeContext(NodeExecutionContext ctx) {
        DecomposeContext context = new DecomposeContext();
        context.setExecutionId(ctx.getExecutionId());
        context.setWorkflowId(ctx.getWorkflowId());
        context.setNodeId(ctx.getNode().getId());
        context.setProjectPath(ctx.getProjectPath());
        context.setGlobalPrompt(ctx.getGlobalPrompt());

        // 从节点配置读取参数
        WorkflowNode node = ctx.getNode();
        String configStr = node.getConfig();
        if (configStr != null && !configStr.isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = objectMapper.readValue(configStr, Map.class);
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

        logger.info("上下文初始化: maxIterations={}, maxDepth={}",
            context.getMaxIterations(), context.getMaxDepth());

        return context;
    }

    /**
     * 创建初始任务
     */
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

    /**
     * 判断是否完成
     */
    private boolean isFinished(DecomposeContext context) {
        return context.getTaskStack().isEmpty() && context.getCurrentTask() == null;
    }

    /**
     * 获取下一个任务（考虑优先级和依赖）
     */
    private TaskState getNextTask(DecomposeContext context) {
        Stack<TaskState> stack = context.getTaskStack();

        // 遍历栈找到可执行的任务
        List<TaskState> blocked = new ArrayList<>();
        TaskState selected = null;

        while (!stack.isEmpty()) {
            TaskState task = stack.pop();

            // 检查依赖是否满足
            if (hasUnsatisfiedDependencies(task, context)) {
                blocked.add(task);
                continue;
            }

            selected = task;
            break;
        }

        // 将被阻塞的任务放回栈
        for (int i = blocked.size() - 1; i >= 0; i--) {
            stack.push(blocked.get(i));
        }

        return selected;
    }

    /**
     * 检查依赖是否满足
     */
    private boolean hasUnsatisfiedDependencies(TaskState task, DecomposeContext context) {
        if (task.getDependencies() == null || task.getDependencies().isEmpty()) {
            return false;
        }

        for (String depId : task.getDependencies()) {
            TaskState depTask = context.getTaskMap().get(depId);
            if (depTask == null || depTask.getStatus() != TaskState.TaskStatus.COMPLETED) {
                return true;
            }
        }

        return false;
    }

    /**
     * 构建Agent提示
     */
    private String buildPrompt(DecomposeContext context, TaskState currentTask) {
        StringBuilder prompt = new StringBuilder();

        // 系统状态
        prompt.append("# 当前状态\n\n");
        prompt.append("- 迭代次数: ").append(context.getIterationCount())
              .append(" / ").append(context.getMaxIterations()).append("\n");
        prompt.append("- 递归深度: ").append(currentTask.getDepth())
              .append(" / ").append(context.getMaxDepth()).append("\n");
        prompt.append("- 待处理任务数: ").append(context.getTaskStack().size()).append("\n");
        prompt.append("- 已完成任务数: ").append(context.getCompletedTasks().size()).append("\n\n");

        // 当前任务
        prompt.append("# 当前任务\n\n");
        prompt.append("任务ID: ").append(currentTask.getTaskId()).append("\n");
        prompt.append("描述: ").append(currentTask.getDescription()).append("\n");
        prompt.append("深度: ").append(currentTask.getDepth()).append("\n\n");

        // 可用工具
        prompt.append("# 可用工具\n\n");
        for (DecomposeTool tool : DecomposeTool.values()) {
            prompt.append("- **").append(tool.getName()).append("**: ")
                  .append(tool.getDescription()).append("\n");
            prompt.append("  - 使用场景: ").append(tool.getUsageGuide()).append("\n");
            prompt.append("  - 必需参数: ").append(tool.getRequiredParameters()).append("\n\n");
        }

        // 决策框架
        prompt.append("# 决策框架\n\n");
        prompt.append("1. **评估任务复杂度**: 判断任务是否需要分解\n");
        prompt.append("   - 复杂任务（多模块、多技能、预估>10分钟）→ 使用 `decompose`\n");
        prompt.append("   - 简单任务（单一操作、预估<5分钟）→ 使用 `execute`\n");
        prompt.append("   - 需要专门技能 → 使用 `spawn_agent`\n\n");

        prompt.append("2. **输出格式**:\n");
        prompt.append("```\n");
        prompt.append("[THOUGHT]\n");
        prompt.append("分析当前任务状态、评估复杂度、决定下一步行动。\n");
        prompt.append("[/THOUGHT]\n\n");
        prompt.append("[ACTION]\n");
        prompt.append("tool: <工具名称>\n");
        prompt.append("<参数名>: <参数值>\n");
        prompt.append("[/ACTION]\n");
        prompt.append("```\n\n");

        prompt.append("3. **注意事项**:\n");
        prompt.append("- 任务完成后调用 `mark_complete`\n");
        prompt.append("- 无法继续时调用 `mark_failed`\n");
        prompt.append("- 需要更多信息时使用 `read_context`\n\n");

        // 最近的决策历史
        List<DecomposeContext.DecisionRecord> history = context.getDecisionHistory();
        if (!history.isEmpty()) {
            prompt.append("# 最近决策\n\n");
            int start = Math.max(0, history.size() - 3);
            for (int i = start; i < history.size(); i++) {
                DecomposeContext.DecisionRecord record = history.get(i);
                prompt.append("- **迭代 ").append(record.getIteration()).append("**: ")
                      .append(record.getAction()).append(" -> ")
                      .append(record.getResult() != null && record.getResult().isSuccess() ? "成功" : "失败")
                      .append("\n");
            }
            prompt.append("\n");
        }

        return prompt.toString();
    }

    /**
     * 执行Agent
     */
    private String executeAgent(NodeExecutionContext ctx, String prompt, int iteration) throws Exception {
        logger.info(">>> 调用Agent API (迭代 {})", iteration + 1);
        logger.debug("Agent Prompt:\n{}", truncate(prompt, 1000));

        OpenClawGatewayClient client = new OpenClawGatewayClient(gatewayUrl, gatewayToken);

        // 构建系统提示
        String systemPrompt = buildSystemPrompt();

        // 构建请求
        OpenClawGatewayClient.AgentRequest request = OpenClawGatewayClient.AgentRequest.builder()
            .agentId(agentId)
            .systemPrompt(systemPrompt)
            .message(prompt)
            .context("decompose_" + ctx.getExecutionId() + "_" + iteration)
            .maxTokens(4096)
            .temperature(0.7)
            .build();

        // 记录请求日志
        logger.info("API Request: agentId={}, context={}", agentId, request.getContext());

        long startTime = System.currentTimeMillis();
        OpenClawGatewayClient.AgentResponse response = client.executeAgent(request);
        long duration = System.currentTimeMillis() - startTime;

        // 记录响应日志
        logger.info("API Response: success={}, duration={}ms, tokens={}, contentLength={}",
            response.isSuccess(), duration, response.getTotalTokens(),
            response.getContent() != null ? response.getContent().length() : 0);

        if (!response.isSuccess()) {
            throw new RuntimeException("Agent执行失败: " + response.getErrorMessage());
        }

        String content = response.getContent();
        logger.info("Agent输出 (前500字符):\n{}", truncate(content, 500));

        return content;
    }

    /**
     * 构建系统提示
     */
    private String buildSystemPrompt() {
        return "你是一个智能任务执行引擎，负责分析和分解复杂任务。\n\n" +
            "你的职责是：\n" +
            "1. 分析任务复杂度，判断是否需要分解\n" +
            "2. 对于复杂任务，分解为更小的子任务\n" +
            "3. 对于简单任务，直接执行或标记完成\n\n" +
            "输出格式要求：\n" +
            "[THOUGHT]\n" +
            "思考过程...\n" +
            "[/THOUGHT]\n\n" +
            "[ACTION]\n" +
            "tool: <工具名>\n" +
            "参数...\n" +
            "[/ACTION]\n";
    }

    /**
     * 更新上下文状态
     */
    private void updateContext(DecomposeContext context, AgentAction action, ActionResult result) {
        // 更新输出缓冲
        context.getOutputBuffer().append(result.getMessage()).append("\n");

        // 如果不需要继续，清空当前任务
        if (!result.isShouldContinue() && context.getCurrentTask() != null) {
            TaskState current = context.getCurrentTask();
            if (current.getStatus() == TaskState.TaskStatus.COMPLETED ||
                current.getStatus() == TaskState.TaskStatus.FAILED) {
                context.setCurrentTask(null);
            }
        }
    }

    /**
     * 记录决策
     */
    private void recordDecision(DecomposeContext context, TaskState task,
                                AgentAction action, ActionResult result) {
        // 记录到内存
        DecomposeContext.DecisionRecord record = new DecomposeContext.DecisionRecord();
        record.setIteration(context.getIterationCount());
        record.setTaskId(task != null ? task.getTaskId() : "unknown");
        record.setThought(action != null ? action.getThought() : "");
        record.setAction(action != null && action.getTool() != null ? action.getTool().getName() : "unknown");
        record.setResult(result);
        record.setTimestamp(System.currentTimeMillis());

        context.getDecisionHistory().add(record);

        // 持久化到数据库
        if (decisionHistoryRepository != null) {
            try {
                DecisionHistory dh = new DecisionHistory();
                dh.setId(context.getExecutionId() + "_" + context.getNodeId());
                dh.setIteration(context.getIterationCount());
                dh.setExecutionId(context.getExecutionId());
                dh.setNodeId(context.getNodeId());
                dh.setTaskId(task != null ? task.getTaskId() : "unknown");
                dh.setThought(action != null ? action.getThought() : "");
                dh.setAction(action != null && action.getTool() != null ? action.getTool().getName() : "unknown");
                dh.setResultStatus(result != null && result.isSuccess() ? "SUCCESS" : "FAILED");
                dh.setResultMessage(result != null ? result.getMessage() : "");
                dh.setTimestamp(LocalDateTime.now());

                // 序列化参数
                if (action != null && action.getParameters() != null) {
                    dh.setActionParameters(objectMapper.writeValueAsString(action.getParameters()));
                }

                decisionHistoryRepository.save(dh);
            } catch (Exception e) {
                logger.warn("保存决策历史失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 创建错误动作
     */
    private AgentAction createErrorAction(String errorMessage) {
        AgentAction action = new AgentAction();
        action.setTool(DecomposeTool.CONTINUE);
        action.setThought("执行出错: " + errorMessage);
        action.setParameters(new HashMap<>());
        return action;
    }

    /**
     * 构建最终结果
     */
    private NodeResult buildFinalResult(DecomposeContext context) {
        logger.info("========== 智能分解节点执行结束 ==========");
        logger.info("状态: {}, 迭代次数: {}, 完成任务: {}",
            context.getStatus(), context.getIterationCount(), context.getCompletedTasks().size());

        Map<String, Object> result = new HashMap<>();
        result.put("status", context.getStatus().name());
        result.put("iterations", context.getIterationCount());
        result.put("completedTasks", context.getCompletedTasks().size());

        // 收集完成的任务结果
        List<Map<String, Object>> taskResults = new ArrayList<>();
        for (TaskState task : context.getCompletedTasks()) {
            Map<String, Object> taskResult = new HashMap<>();
            taskResult.put("id", task.getTaskId());
            taskResult.put("description", task.getDescription());
            taskResult.put("result", task.getResult());
            taskResults.add(taskResult);
        }
        result.put("taskResults", taskResults);

        // 收集生成的产物
        if (!context.getArtifacts().isEmpty()) {
            result.put("artifacts", context.getArtifacts());
        }

        // 决策历史摘要
        List<String> decisionSummary = new ArrayList<>();
        for (DecomposeContext.DecisionRecord record : context.getDecisionHistory()) {
            decisionSummary.add(String.format("迭代%d: %s -> %s",
                record.getIteration(), record.getAction(),
                record.getResult() != null && record.getResult().isSuccess() ? "成功" : "失败"));
        }
        result.put("decisionSummary", decisionSummary);

        if (context.getStatus() == DecomposeContext.DecomposeStatus.COMPLETED) {
            return NodeResult.success(result);
        } else {
            NodeResult nodeResult = NodeResult.failed(result, context.getErrorMessage());
            return nodeResult;
        }
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }

    @Override
    public List<String> validate(WorkflowNode node) {
        List<String> errors = new ArrayList<>();

        String configStr = node.getConfig();
        if (configStr == null || configStr.isEmpty()) {
            return errors;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = objectMapper.readValue(configStr, Map.class);

            // 验证maxIterations
            if (config.containsKey("maxIterations")) {
                int maxIter = ((Number) config.get("maxIterations")).intValue();
                if (maxIter < 1 || maxIter > 1000) {
                    errors.add("maxIterations必须在1-1000之间");
                }
            }

            // 验证maxDepth
            if (config.containsKey("maxDepth")) {
                int maxDepth = ((Number) config.get("maxDepth")).intValue();
                if (maxDepth < 1 || maxDepth > 10) {
                    errors.add("maxDepth必须在1-10之间");
                }
            }
        } catch (Exception e) {
            errors.add("配置格式错误: " + e.getMessage());
        }

        return errors;
    }
}