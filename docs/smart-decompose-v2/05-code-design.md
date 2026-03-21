# SmartDecompose 代码实现方案

## 一、文件结构

```
src/main/java/com/openclaw/workflow/engine/smartdecompose/
├── SmartDecomposeHandler.java          # 入口处理器
├── DecomposeOrchestrator.java          # 编排器
├── model/
│   ├── DecomposeContext.java           # 执行上下文
│   ├── SubTask.java                    # 子任务
│   ├── DecisionResponse.java           # 决策响应
│   ├── ReviewResponse.java             # 审核响应
│   └── DecomposeStatus.java            # 状态枚举
├── prompt/
│   ├── PromptBuilder.java              # 提示词构建器
│   └── PromptOutputFormat.java         # 输出格式定义(硬编码)
├── client/
│   ├── OpenClawClient.java             # OpenClaw调用客户端
│   └── ResponseParser.java             # 响应解析器
├── repository/
│   ├── PromptTemplateRepository.java   # 模板仓储
│   ├── ExecutionStateRepository.java   # 状态仓储
│   └── DecisionHistoryRepository.java  # 决策历史仓储
└── config/
    └── SmartDecomposeConfig.java       # 节点配置
```

---

## 二、核心类设计

### 2.1 SmartDecomposeHandler

```java
package com.openclaw.workflow.engine.smartdecompose;

import com.openclaw.workflow.engine.handler.BaseNodeHandler;
import com.openclaw.workflow.engine.model.NodeExecutionContext;
import com.openclaw.workflow.engine.model.NodeResult;
import com.openclaw.workflow.entity.WorkflowNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SmartDecompose 节点处理器 - 入口类
 */
@Component
public class SmartDecomposeHandler extends BaseNodeHandler {

    @Autowired
    private DecomposeOrchestrator orchestrator;

    @Autowired
    private PromptTemplateRepository promptTemplateRepository;

    @Autowired
    private ExecutionStateRepository executionStateRepository;

    @Override
    public NodeResult execute(NodeExecutionContext ctx) throws Exception {
        logger.info("SmartDecompose 开始执行: executionId={}", ctx.getExecutionId());

        // 1. 初始化上下文
        DecomposeContext context = initializeContext(ctx);

        // 2. 加载提示词模板
        loadTemplates(context);

        // 3. 执行编排
        orchestrator.run(context);

        // 4. 构建结果
        return buildResult(context);
    }

    private DecomposeContext initializeContext(NodeExecutionContext ctx) {
        DecomposeContext context = new DecomposeContext();
        context.setExecutionId(ctx.getExecutionId());
        context.setWorkflowId(ctx.getWorkflowId());
        context.setNodeId(ctx.getNode().getId());
        context.setProjectPath(ctx.getProjectPath());
        context.setStatus(DecomposeStatus.RUNNING);

        // 解析节点配置
        SmartDecomposeConfig config = SmartDecomposeConfig.fromJson(
            ctx.getNode().getConfig()
        );
        context.setMaxRetries(config.getMaxRetries());
        context.setMaxIterations(config.getMaxIterations());
        context.setRequireManualReview(config.isRequireManualReview());

        // 创建初始任务
        SubTask initialTask = SubTask.builder()
            .id("TASK_ROOT_" + System.currentTimeMillis())
            .description(ctx.getTaskDescription())
            .depth(0)
            .build();
        context.getTaskQueue().add(initialTask);

        return context;
    }

    private void loadTemplates(DecomposeContext context) {
        context.setDecisionTemplate(
            promptTemplateRepository.findByTypeAndIsDefaultTrue("decision")
                .orElseThrow(() -> new IllegalStateException("未找到决策提示词模板"))
        );
        context.setReviewTemplate(
            promptTemplateRepository.findByTypeAndIsDefaultTrue("review")
                .orElseThrow(() -> new IllegalStateException("未找到审核提示词模板"))
        );
    }

    private NodeResult buildResult(DecomposeContext context) {
        if (context.getStatus() == DecomposeStatus.COMPLETED) {
            return NodeResult.success(Map.of(
                "status", "COMPLETED",
                "iterations", context.getIterationCount(),
                "completedTasks", context.getCompletedTasks().size(),
                "failedTasks", context.getFailedTasks().size()
            ));
        } else if (context.getStatus() == DecomposeStatus.WAITING_MANUAL_REVIEW) {
            return NodeResult.waiting("等待人工审核", Map.of(
                "manualReviewId", context.getManualReviewId()
            ));
        } else {
            return NodeResult.failed(context.getErrorMessage());
        }
    }

    @Override
    public List<String> validate(WorkflowNode node) {
        List<String> errors = new ArrayList<>();
        // 验证配置
        try {
            SmartDecomposeConfig config = SmartDecomposeConfig.fromJson(node.getConfig());
            if (config.getMaxRetries() < 1 || config.getMaxRetries() > 20) {
                errors.add("maxRetries 必须在 1-20 之间");
            }
            if (config.getMaxIterations() < 1 || config.getMaxIterations() > 100) {
                errors.add("maxIterations 必须在 1-100 之间");
            }
        } catch (Exception e) {
            errors.add("配置格式错误: " + e.getMessage());
        }
        return errors;
    }
}
```

### 2.2 DecomposeOrchestrator

```java
package com.openclaw.workflow.engine.smartdecompose;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 分解编排器 - 核心执行逻辑
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
    private DecisionHistoryRepository decisionHistoryRepository;

    @Autowired
    private ManualReviewRepository manualReviewRepository;

    @Autowired
    private ExecutionStateRepository executionStateRepository;

    public void run(DecomposeContext context) {

        while (!context.getTaskQueue().isEmpty()) {

            // 检查迭代次数
            if (context.getIterationCount() >= context.getMaxIterations()) {
                context.setStatus(DecomposeStatus.ITERATION_EXCEEDED);
                context.setErrorMessage("超过最大迭代次数");
                break;
            }

            // 取出任务
            SubTask currentTask = context.getTaskQueue().poll();
            context.setCurrentTask(currentTask);
            context.incrementIteration();

            logger.info("处理任务: id={}, description={}",
                currentTask.getId(), currentTask.getDescription());

            try {
                // 决策阶段
                DecisionResponse decision = makeDecision(context, currentTask);

                // 根据决策分支
                if (decision.isSplit()) {
                    handleSplit(context, currentTask, decision);
                } else {
                    handleExecute(context, currentTask, decision);
                }

                // 保存状态
                saveState(context);

            } catch (Exception e) {
                logger.error("任务执行异常: {}", e.getMessage(), e);
                context.addFailedTask(currentTask);
                context.setErrorMessage(e.getMessage());
                break;
            }
        }

        // 完成
        if (context.getStatus() == DecomposeStatus.RUNNING) {
            context.setStatus(DecomposeStatus.COMPLETED);
        }
    }

    /**
     * 决策阶段
     */
    private DecisionResponse makeDecision(DecomposeContext context, SubTask task) {
        // 构建提示词
        String prompt = promptBuilder.buildDecisionPrompt(context, task);

        // 调用 OpenClaw
        String rawResponse = openClawClient.execute(prompt);

        // 解析响应
        DecisionResponse response = responseParser.parseDecision(rawResponse);

        // 记录决策
        recordDecision(context, task, response);

        return response;
    }

    /**
     * 处理拆分决策
     */
    private void handleSplit(DecomposeContext context, SubTask parentTask, DecisionResponse decision) {
        logger.info("任务拆分为 {} 个子任务", decision.getTasks().size());

        for (SubTask subTask : decision.getTasks()) {
            subTask.setDepth(parentTask.getDepth() + 1);
            subTask.setParentTaskId(parentTask.getId());
            context.getTaskQueue().add(subTask);
        }
    }

    /**
     * 处理执行决策
     */
    private void handleExecute(DecomposeContext context, SubTask task, DecisionResponse decision) {
        // 审核阶段
        boolean approved = reviewAndRetry(context, task, decision.getResult());

        if (approved) {
            task.setStatus(SubTaskStatus.COMPLETED);
            context.getCompletedTasks().add(task);
            logger.info("任务完成: {}", task.getId());
        } else {
            task.setStatus(SubTaskStatus.FAILED);
            context.addFailedTask(task);
        }
    }

    /**
     * 审核与重试
     */
    private boolean reviewAndRetry(DecomposeContext context, SubTask task, String execResult) {
        int retryCount = 0;
        List<String> previousIssues = new ArrayList<>();

        while (retryCount <= context.getMaxRetries()) {

            // 构建审核提示词
            String reviewPrompt = promptBuilder.buildReviewPrompt(
                context, task, execResult, previousIssues
            );

            // 调用审核
            String rawReview = openClawClient.review(reviewPrompt);
            ReviewResponse review = responseParser.parseReview(rawReview);

            if (review.isApproved()) {
                logger.info("审核通过: {}", review.getSummary());
                return true;
            }

            // 审核拒绝
            previousIssues = review.getIssues();
            retryCount++;
            logger.warn("审核拒绝 (第{}次): {}", retryCount, review.getIssues());

            if (retryCount > context.getMaxRetries()) {
                // 超过重试次数
                if (context.isRequireManualReview()) {
                    return triggerManualReview(context, task, execResult, review);
                }
                return false;
            }

            // 重新执行
            String retryPrompt = promptBuilder.buildRetryPrompt(context, task, previousIssues);
            execResult = openClawClient.execute(retryPrompt);
        }

        return false;
    }

    /**
     * 触发人工审核
     */
    private boolean triggerManualReview(DecomposeContext context, SubTask task,
                                         String execResult, ReviewResponse review) {
        // 创建人工审核记录
        ManualReviewRecord record = new ManualReviewRecord();
        record.setId(UUID.randomUUID().toString());
        record.setExecutionId(context.getExecutionId());
        record.setTaskId(task.getId());
        record.setTaskDescription(task.getDescription());
        record.setExecutionResult(execResult);
        record.setReviewIssues(toJson(review.getIssues()));
        record.setStatus(ManualReviewStatus.WAITING);
        record.setCreatedAt(LocalDateTime.now());

        manualReviewRepository.save(record);

        // 更新上下文
        context.setStatus(DecomposeStatus.WAITING_MANUAL_REVIEW);
        context.setManualReviewId(record.getId());

        logger.warn("触发人工审核: manualReviewId={}", record.getId());

        return false;
    }

    private void recordDecision(DecomposeContext context, SubTask task, DecisionResponse decision) {
        DecisionHistory history = new DecisionHistory();
        history.setId(UUID.randomUUID().toString());
        history.setExecutionId(context.getExecutionId());
        history.setIteration(context.getIterationCount());
        history.setTaskId(task.getId());
        history.setDecision(decision.getDecision());
        history.setThought(decision.getThought());
        history.setCreatedAt(LocalDateTime.now());

        decisionHistoryRepository.save(history);
    }

    private void saveState(DecomposeContext context) {
        // 持久化状态
        executionStateRepository.save(toEntity(context));
    }
}
```

### 2.3 PromptBuilder

```java
package com.openclaw.workflow.engine.smartdecompose.prompt;

import com.openclaw.workflow.engine.smartdecompose.model.DecomposeContext;
import com.openclaw.workflow.engine.smartdecompose.model.SubTask;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 提示词构建器
 */
@Component
public class PromptBuilder {

    /**
     * 构建决策提示词
     */
    public String buildDecisionPrompt(DecomposeContext context, SubTask task) {
        String template = context.getDecisionTemplate().getContent();

        Map<String, String> params = Map.of(
            "projectPath", nvl(context.getProjectPath(), ""),
            "techStack", nvl(context.getTechStack(), ""),
            "taskDescription", nvl(task.getDescription(), ""),
            "completedTasks", formatCompletedTasks(context.getCompletedTasks())
        );

        return render(template, params);
    }

    /**
     * 构建审核提示词
     */
    public String buildReviewPrompt(DecomposeContext context, SubTask task,
                                     String executionResult, List<String> previousIssues) {
        String template = context.getReviewTemplate().getContent();

        Map<String, String> params = Map.of(
            "taskDescription", nvl(task.getDescription(), ""),
            "criteria", nvl(task.getCriteria(), ""),
            "executionResult", nvl(executionResult, ""),
            "projectPath", nvl(context.getProjectPath(), ""),
            "previousIssues", formatIssues(previousIssues)
        );

        return render(template, params);
    }

    /**
     * 构建重试提示词
     */
    public String buildRetryPrompt(DecomposeContext context, SubTask task, List<String> issues) {
        return buildReviewPrompt(context, task, "", issues);
    }

    /**
     * 渲染模板
     */
    private String render(String template, Map<String, String> params) {
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        // 处理条件块 {{#if ...}}...{{/if}}
        result = processConditionals(result, params);

        return result;
    }

    /**
     * 处理条件块
     */
    private String processConditionals(String template, Map<String, String> params) {
        // {{#if var}}...{{else}}...{{/if}}
        Pattern pattern = Pattern.compile(
            "\\{\\{#if (\\w+)\\}\\}(.*?)(?:\\{\\{else\\}\\}(.*?))?\\{\\{/if\\}\\}",
            Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String ifContent = matcher.group(2);
            String elseContent = matcher.group(3) != null ? matcher.group(3) : "";

            String value = params.get(varName);
            String replacement = (value != null && !value.isEmpty()) ? ifContent : elseContent;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String formatCompletedTasks(List<SubTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tasks.size(); i++) {
            sb.append(i + 1).append(". ").append(tasks.get(i).getDescription()).append("\n");
        }
        return sb.toString();
    }

    private String formatIssues(List<String> issues) {
        if (issues == null || issues.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < issues.size(); i++) {
            sb.append(i + 1).append(". ").append(issues.get(i)).append("\n");
        }
        return sb.toString();
    }

    private String nvl(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
}
```

### 2.4 OpenClawClient

```java
package com.openclaw.workflow.engine.smartdecompose.client;

import com.openclaw.workflow.engine.connector.AgentRequest;
import com.openclaw.workflow.engine.connector.AgentResponse;
import com.openclaw.workflow.engine.connector.OpenClawGatewayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OpenClaw 客户端
 */
@Component
public class OpenClawClient {

    @Value("${openclaw.gateway.url}")
    private String gatewayUrl;

    @Value("${openclaw.gateway.token}")
    private String token;

    @Value("${openclaw.agent.id:smart-decompose}")
    private String agentId;

    private OpenClawGatewayClient gatewayClient;

    /**
     * 执行决策/任务
     */
    public String execute(String prompt) {
        return callAgent(prompt, "decision");
    }

    /**
     * 执行审核
     */
    public String review(String prompt) {
        return callAgent(prompt, "review");
    }

    private String callAgent(String prompt, String context) {
        if (gatewayClient == null) {
            gatewayClient = new OpenClawGatewayClient(gatewayUrl, token);
        }

        AgentRequest request = AgentRequest.builder()
            .agentId(agentId)
            .message(prompt)
            .context(context + "_" + System.currentTimeMillis())
            .build();

        AgentResponse response = gatewayClient.executeAgent(request);

        if (!response.isSuccess()) {
            throw new OpenClawException("OpenClaw 调用失败: " + response.getErrorMessage());
        }

        return response.getContent();
    }
}
```

### 2.5 ResponseParser

```java
package com.openclaw.workflow.engine.smartdecompose.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclaw.workflow.engine.smartdecompose.model.DecisionResponse;
import com.openclaw.workflow.engine.smartdecompose.model.ReviewResponse;
import com.openclaw.workflow.engine.smartdecompose.model.SubTask;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 响应解析器
 */
@Component
public class ResponseParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Pattern JSON_PATTERN = Pattern.compile(
        "\\{[\\s\\S]*\\}", Pattern.MULTILINE
    );

    /**
     * 解析决策响应
     */
    public DecisionResponse parseDecision(String rawResponse) {
        String json = extractJson(rawResponse);

        try {
            return mapper.readValue(json, DecisionResponse.class);
        } catch (Exception e) {
            throw new ResponseParseException("解析决策响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析审核响应
     */
    public ReviewResponse parseReview(String rawResponse) {
        String json = extractJson(rawResponse);

        try {
            return mapper.readValue(json, ReviewResponse.class);
        } catch (Exception e) {
            throw new ResponseParseException("解析审核响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从响应中提取 JSON
     */
    private String extractJson(String response) {
        // 处理 markdown 代码块
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }

        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }

        // 直接查找 JSON 对象
        Matcher matcher = JSON_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group();
        }

        throw new ResponseParseException("无法从响应中提取 JSON: " + response);
    }
}
```

### 2.6 PromptOutputFormat (硬编码部分)

```java
package com.openclaw.workflow.engine.smartdecompose.prompt;

/**
 * 提示词输出格式定义
 * 这部分硬编码在代码中，不可配置
 */
public final class PromptOutputFormat {

    private PromptOutputFormat() {}

    // ===================== 决策输出格式 =====================

    public static final String DECISION_EXECUTE_FORMAT = """
        {
          "decision": "execute",
          "thought": "你的分析过程",
          "result": "执行结果描述"
        }
        """;

    public static final String DECISION_SPLIT_FORMAT = """
        {
          "decision": "split",
          "thought": "你的分析过程",
          "tasks": [
            {
              "id": "TASK_XXX",
              "description": "子任务描述",
              "criteria": "验收标准",
              "estimatedMinutes": 3
            }
          ]
        }
        """;

    // 决策必填字段
    public static final List<String> DECISION_REQUIRED_FIELDS = List.of(
        "decision", "thought"
    );

    // 有效的决策值
    public static final List<String> VALID_DECISIONS = List.of(
        "execute", "split"
    );

    // ===================== 审核输出格式 =====================

    public static final String REVIEW_APPROVED_FORMAT = """
        {
          "status": "APPROVED",
          "thought": "审核分析过程",
          "summary": "任务完成情况总结"
        }
        """;

    public static final String REVIEW_REJECTED_FORMAT = """
        {
          "status": "REJECTED",
          "thought": "审核分析过程",
          "issues": ["问题1", "问题2"],
          "suggestion": "修改建议"
        }
        """;

    // 审核必填字段
    public static final List<String> REVIEW_REQUIRED_FIELDS = List.of(
        "status", "thought"
    );

    // 有效的审核状态
    public static final List<String> VALID_REVIEW_STATUSES = List.of(
        "APPROVED", "REJECTED"
    );
}
```