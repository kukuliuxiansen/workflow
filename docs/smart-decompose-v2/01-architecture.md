# SmartDecompose V2 架构设计

## 一、设计背景

### V1 的问题

V1 版本的 SmartDecompose 试图自己实现工具（如 `RunCommandActionHandler`），但这些工具是空壳实现——代码被注释掉，没有真正执行能力。这导致工作流根本无法完成任务。

### V2 的定位

**SmartDecompose = 项目经理**

它不执行任何具体操作，只做三件事：
1. 判断任务是否需要拆分
2. 委托 OpenClaw 执行
3. 审核执行结果

OpenClaw 已经有完整的工具链（读文件、写文件、执行命令、搜索代码），SmartDecompose 只需要正确地"发号施令"。

---

## 二、核心概念

### 2.1 角色分工

```
SmartDecompose（项目经理）     OpenClaw（执行者）
─────────────────────────     ─────────────────
• 分解任务                     • 读取文件
• 生成提示词                   • 写入文件
• 委托执行                     • 执行命令
• 审核结果                     • 搜索代码
• 处理重试                     • 调用API
```

### 2.2 两个核心提示词

系统只有两种提示词模板：

| 类型 | 用途 | 输出 |
|-----|------|------|
| 决策提示词 | 判断任务：执行还是拆分 | `{decision, thought, result/tasks}` |
| 审核提示词 | 判断结果：通过还是拒绝 | `{status, thought, issues, suggestion}` |

### 2.3 执行边界

**8分钟原则**：如果 OpenClaw 执行一个任务超过 8 分钟，说明任务太复杂，应该拆分。这是经验值，基于 OpenClaw 的实际能力边界。

---

## 三、整体架构

### 3.1 组件清单

```
SmartDecomposeHandler     入口，接收工作流引擎调用
    │
    └── DecomposeOrchestrator    编排器，控制主循环
            │
            ├── PromptBuilder        构建提示词
            ├── OpenClawClient       调用 OpenClaw
            ├── ResponseParser       解析 JSON 响应
            └── StatePersister       持久化执行状态
```

### 3.2 执行流程

```
while (任务队列不为空) {
    取出任务 → 构建决策提示词 → 调用 OpenClaw → 解析响应

    if (decision == "split") {
        子任务入队
        continue
    }

    // decision == "execute"
    构建审核提示词 → 调用 OpenClaw → 解析响应

    if (status == "APPROVED") {
        任务完成
    } else {
        重试（最多5次）→ 失败则人工介入
    }
}
```

### 3.3 为什么必须串行

OpenClaw 只支持单会话模式。如果并行调用多个任务，每个任务会创建独立会话，导致上下文丢失——前一个任务创建的文件，后一个任务看不到。

因此子任务必须串行执行，共享同一个会话。

---

## 四、提示词系统设计

### 4.1 存储策略

| 内容 | 存储位置 | 原因 |
|-----|---------|------|
| 提示词模板 | 数据库 `prompt_template` 表 | 不同场景需要不同模板，需灵活调整 |
| JSON 输出格式 | 代码中硬编码 | 固定不变，硬编码更可靠，避免数据库字段变更导致解析失败 |
| 动态参数 | 代码中生成 | 项目路径、任务描述、上下文等，运行时才能确定 |

### 4.2 决策提示词

**目的**：让 OpenClaw 判断任务是直接执行还是需要拆分。

**模板存储**：数据库 `prompt_template` 表，`type = 'decision'`

**模板示例**（核心部分）：

```markdown
# 角色
你是一个任务决策专家。

# 项目信息
- 项目路径: {{projectPath}}
- 技术栈: {{techStack}}

# 当前任务
{{taskDescription}}

# 已完成的任务
{{completedTasksInfo}}

# 决策要求
分析当前任务：
1. 如果任务简单，可以在8分钟内完成 → 直接执行
2. 如果任务复杂，需要多个步骤 → 拆分为子任务

# 输出格式
必须严格输出以下 JSON 格式，不要输出其他内容：

如果选择执行：
```json
{
  "decision": "execute",
  "thought": "你的分析过程",
  "result": "执行结果描述"
}
```

如果选择拆分：
```json
{
  "decision": "split",
  "thought": "你的分析过程",
  "tasks": [
    {"id": "TASK_001", "description": "子任务描述", "estimatedMinutes": 3},
    {"id": "TASK_002", "description": "子任务描述", "estimatedMinutes": 5}
  ]
}
```
```

**代码中的渲染逻辑**：

```java
public class PromptBuilder {

    private final PromptTemplateRepository templateRepository;

    public String buildDecisionPrompt(SubTask task, DecomposeContext context) {
        // 1. 从数据库加载模板
        PromptTemplate template = templateRepository
            .findByTypeAndIsDefaultTrue("decision")
            .orElseThrow(() -> new IllegalStateException("未找到决策模板"));

        // 2. 准备参数
        Map<String, Object> params = new HashMap<>();
        params.put("projectPath", context.getProjectPath());
        params.put("techStack", context.getTechStack());
        params.put("taskDescription", task.getDescription());
        params.put("completedTasksInfo", formatCompletedTasks(context.getCompletedTasks()));

        // 3. 渲染模板（简单的 {{key}} 替换）
        return render(template.getContent(), params);
    }

    private String render(String template, Map<String, Object> params) {
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}",
                                    String.valueOf(entry.getValue()));
        }
        return result;
    }

    private String formatCompletedTasks(List<SubTask> tasks) {
        if (tasks.isEmpty()) {
            return "无";
        }
        return tasks.stream()
            .map(t -> "- " + t.getDescription() + ": " + t.getExecutionResult())
            .collect(Collectors.joining("\n"));
    }
}
```

**JSON 输出格式（硬编码在代码中）**：

```java
public class DecisionResponse {

    private String decision;      // "execute" 或 "split"
    private String thought;       // 分析过程
    private String result;        // decision=execute 时的执行结果
    private List<SubTaskDef> tasks;  // decision=split 时的子任务列表

    public static class SubTaskDef {
        private String id;
        private String description;
        private Integer estimatedMinutes;
    }

    // 判断方法
    public boolean isExecute() {
        return "execute".equals(decision);
    }

    public boolean isSplit() {
        return "split".equals(decision);
    }
}
```

### 4.3 审核提示词

**目的**：让 OpenClaw 审核执行结果是否符合预期。

**模板存储**：数据库 `prompt_template` 表，`type = 'review'`

**模板示例**：

```markdown
# 角色
你是一个代码审核专家。

# 项目信息
- 项目路径: {{projectPath}}

# 任务要求
任务描述: {{taskDescription}}
验收标准: {{criteria}}

# 执行结果
{{executionResult}}

# 审核要求
检查执行结果是否满足任务要求：
1. 文件是否正确创建/修改
2. 代码逻辑是否正确
3. 是否满足验收标准

# 输出格式
必须严格输出以下 JSON 格式：

如果通过：
```json
{
  "status": "APPROVED",
  "thought": "审核分析",
  "summary": "结果摘要"
}
```

如果拒绝：
```json
{
  "status": "REJECTED",
  "thought": "审核分析",
  "issues": ["问题1", "问题2"],
  "suggestion": "修复建议"
}
```
```

**代码中的渲染逻辑**：

```java
public String buildReviewPrompt(SubTask task, String executionResult, DecomposeContext context) {
    PromptTemplate template = templateRepository
        .findByTypeAndIsDefaultTrue("review")
        .orElseThrow(() -> new IllegalStateException("未找到审核模板"));

    Map<String, Object> params = new HashMap<>();
    params.put("projectPath", context.getProjectPath());
    params.put("taskDescription", task.getDescription());
    params.put("criteria", task.getCriteria() != null ? task.getCriteria() : "无特殊要求");
    params.put("executionResult", executionResult);

    return render(template.getContent(), params);
}
```

**JSON 输出格式（硬编码在代码中）**：

```java
public class ReviewResponse {

    private String status;         // "APPROVED" 或 "REJECTED"
    private String thought;        // 审核分析
    private String summary;        // status=APPROVED 时的摘要
    private List<String> issues;   // status=REJECTED 时的问题列表
    private String suggestion;     // status=REJECTED 时的修复建议

    public boolean isApproved() {
        return "APPROVED".equals(status);
    }

    public boolean isRejected() {
        return "REJECTED".equals(status);
    }
}
```

### 4.4 重试提示词

当审核拒绝后，需要重新执行。重试时会附加失败原因：

```java
public String buildRetryPrompt(SubTask task, String executionResult,
                                List<String> issues, DecomposeContext context) {
    // 复用决策提示词，但附加失败信息
    String basePrompt = buildDecisionPrompt(task, context);

    StringBuilder retryPrompt = new StringBuilder(basePrompt);
    retryPrompt.append("\n\n# 上次执行失败\n");
    retryPrompt.append("执行结果：\n").append(executionResult).append("\n\n");
    retryPrompt.append("问题列表：\n");
    for (String issue : issues) {
        retryPrompt.append("- ").append(issue).append("\n");
    }
    retryPrompt.append("\n请修复上述问题后重新执行。");

    return retryPrompt.toString();
}
```

---

## 五、OpenClaw 调用机制

### 5.1 调用接口

SmartDecompose 通过 OpenClaw Gateway 调用 OpenClaw。Gateway 提供 HTTP API：

```java
public class OpenClawClient {

    private final String gatewayUrl;
    private final String token;
    private final int timeout = 480000;  // 8分钟

    /**
     * 发送提示词到 OpenClaw
     * @param prompt 完整的提示词
     * @return OpenClaw 的原始文本响应
     */
    public String execute(String prompt) {
        Map<String, Object> request = new HashMap<>();
        request.put("agentId", "smart-decompose");
        request.put("message", prompt);
        request.put("timeout", timeout);

        // 如果需要复用会话，传入 sessionId
        if (currentSessionId != null) {
            request.put("sessionId", currentSessionId);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            gatewayUrl + "/api/agent/execute",
            entity,
            Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new OpenClawException("调用失败: " + response.getStatusCode());
        }

        Map<String, Object> body = response.getBody();
        if (body == null || !"success".equals(body.get("status"))) {
            String error = body != null ? String.valueOf(body.get("error")) : "未知错误";
            throw new OpenClawException("执行失败: " + error);
        }

        // 保存会话ID用于后续调用
        if (body.containsKey("sessionId")) {
            this.currentSessionId = (String) body.get("sessionId");
        }

        return (String) body.get("content");
    }
}
```

### 5.2 响应解析

OpenClaw 返回纯文本，可能是：
1. 纯 JSON
2. Markdown 包裹的 JSON（```json ... ```）
3. 带有其他文字说明的 JSON

需要提取其中的 JSON 部分：

```java
public class ResponseParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    public DecisionResponse parseDecision(String content) {
        String json = extractJson(content);
        try {
            DecisionResponse response = mapper.readValue(json, DecisionResponse.class);
            validateDecisionResponse(response);
            return response;
        } catch (JsonProcessingException e) {
            throw new ResponseParseException("决策响应解析失败: " + e.getMessage(), e);
        }
    }

    public ReviewResponse parseReview(String content) {
        String json = extractJson(content);
        try {
            ReviewResponse response = mapper.readValue(json, ReviewResponse.class);
            validateReviewResponse(response);
            return response;
        } catch (JsonProcessingException e) {
            throw new ResponseParseException("审核响应解析失败: " + e.getMessage(), e);
        }
    }

    private String extractJson(String content) {
        if (content == null || content.isEmpty()) {
            throw new ResponseParseException("响应内容为空");
        }

        // 尝试匹配 ```json ... ```
        Pattern jsonBlock = Pattern.compile("```json\\s*\\n([\\s\\S]*?)\\n```");
        Matcher matcher = jsonBlock.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 尝试匹配 ``` ... ```
        Pattern codeBlock = Pattern.compile("```\\s*\\n([\\s\\S]*?)\\n```");
        matcher = codeBlock.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 尝试直接解析整个内容
        content = content.trim();
        if (content.startsWith("{")) {
            // 找到匹配的 }
            int depth = 0;
            int end = -1;
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i + 1;
                        break;
                    }
                }
            }
            if (end > 0) {
                return content.substring(0, end);
            }
        }

        throw new ResponseParseException("无法从响应中提取 JSON: " +
            content.substring(0, Math.min(200, content.length())));
    }

    private void validateDecisionResponse(DecisionResponse response) {
        if (response.getDecision() == null) {
            throw new ResponseParseException("决策响应缺少 decision 字段");
        }
        if (!"execute".equals(response.getDecision()) &&
            !"split".equals(response.getDecision())) {
            throw new ResponseParseException("decision 必须是 execute 或 split");
        }
        if (response.isSplit() &&
            (response.getTasks() == null || response.getTasks().isEmpty())) {
            throw new ResponseParseException("拆分决策必须包含子任务列表");
        }
    }

    private void validateReviewResponse(ReviewResponse response) {
        if (response.getStatus() == null) {
            throw new ResponseParseException("审核响应缺少 status 字段");
        }
        if (!"APPROVED".equals(response.getStatus()) &&
            !"REJECTED".equals(response.getStatus())) {
            throw new ResponseParseException("status 必须是 APPROVED 或 REJECTED");
        }
        if (response.isRejected() &&
            (response.getIssues() == null || response.getIssues().isEmpty())) {
            throw new ResponseParseException("拒绝审核必须包含问题列表");
        }
    }
}
```

### 5.3 会话管理

OpenClaw 支持会话模式，同一会话内的多次调用共享上下文：

```java
public class DecomposeOrchestrator {

    private final OpenClawClient openClawClient;
    private final PromptBuilder promptBuilder;
    private final ResponseParser responseParser;

    // 每个执行有一个独立的 OpenClawClient 实例，保持会话
    public void run(DecomposeContext context) {
        // 初始化会话
        openClawClient.startSession();

        try {
            Queue<SubTask> taskQueue = context.getTaskQueue();

            while (!taskQueue.isEmpty() &&
                   context.getIterationCount() < context.getMaxIterations()) {

                context.incrementIteration();

                SubTask currentTask = taskQueue.poll();
                context.setCurrentTask(currentTask);

                // 决策
                String decisionPrompt = promptBuilder.buildDecisionPrompt(currentTask, context);
                String decisionResponse = openClawClient.execute(decisionPrompt);
                DecisionResponse decision = responseParser.parseDecision(decisionResponse);

                // 记录决策历史
                saveDecisionHistory(context, currentTask, decision);

                if (decision.isSplit()) {
                    // 子任务入队（放在队首，深度优先）
                    List<SubTask> subTasks = convertToSubTasks(decision.getTasks(), currentTask);
                    for (int i = subTasks.size() - 1; i >= 0; i--) {
                        taskQueue.addFirst(subTasks.get(i));
                    }
                    continue;
                }

                // 执行完成，进行审核
                String reviewPrompt = promptBuilder.buildReviewPrompt(
                    currentTask, decision.getResult(), context);
                String reviewResponse = openClawClient.execute(reviewPrompt);
                ReviewResponse review = responseParser.parseReview(reviewResponse);

                if (review.isApproved()) {
                    // 审核通过
                    currentTask.setStatus(SubTaskStatus.COMPLETED);
                    currentTask.setExecutionResult(decision.getResult());
                    context.addCompletedTask(currentTask);
                } else {
                    // 审核拒绝，处理重试
                    handleRejection(context, currentTask, decision.getResult(), review);
                }

                // 持久化状态
                statePersister.save(context);
            }

        } finally {
            openClawClient.endSession();
        }
    }
}
```

### 5.4 错误处理

```java
public class OpenClawException extends RuntimeException {
    private final ErrorCode code;

    public enum ErrorCode {
        CONNECTION_FAILED,    // 网络连接失败
        TIMEOUT,              // 执行超时
        INVALID_RESPONSE,     // 响应格式错误
        EXECUTION_ERROR       // OpenClaw 内部错误
    }
}

// 在 OpenClawClient 中的错误处理
public String execute(String prompt) {
    try {
        // ... HTTP 调用
    } catch (ResourceAccessException e) {
        throw new OpenClawException(ErrorCode.CONNECTION_FAILED,
            "无法连接到 OpenClaw Gateway: " + gatewayUrl, e);
    } catch (HttpClientErrorException e) {
        if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            throw new OpenClawException(ErrorCode.EXECUTION_ERROR,
                "认证失败，请检查 token", e);
        }
        throw new OpenClawException(ErrorCode.EXECUTION_ERROR,
            "请求错误: " + e.getMessage(), e);
    } catch (HttpServerErrorException e) {
        if (e.getStatusCode() == HttpStatus.GATEWAY_TIMEOUT) {
            throw new OpenClawException(ErrorCode.TIMEOUT,
                "执行超时（超过 " + timeout + "ms）", e);
        }
        throw new OpenClawException(ErrorCode.EXECUTION_ERROR,
            "服务器错误: " + e.getMessage(), e);
    }
}
```

---

## 六、执行状态机

### 6.1 状态定义

```java
public enum DecomposeStatus {
    RUNNING,                // 执行中
    COMPLETED,              // 全部完成
    FAILED,                 // 执行失败（不可恢复）
    WAITING_MANUAL_REVIEW,  // 等待人工审核
    ITERATION_EXCEEDED      // 超过最大迭代次数
}
```

### 6.2 任务状态

```java
public enum SubTaskStatus {
    PENDING,     // 等待执行
    RUNNING,     // 执行中
    COMPLETED,   // 已完成
    FAILED       // 失败（重试耗尽）
}
```

### 6.3 状态流转

```
初始: 创建根任务 → 状态=RUNNING

循环:
    取任务 → 状态=PENDING → RUNNING

    决策=split → 创建子任务 → 任务状态=PENDING（等待后续处理）

    决策=execute → 审核
        审核通过 → 任务状态=COMPLETED
        审核拒绝 → 重试
            重试成功 → 任务状态=COMPLETED
            重试耗尽 → 触发人工审核 → 状态=WAITING_MANUAL_REVIEW

    人工审核
        通过 → 恢复执行 → 状态=RUNNING
        拒绝 → 任务状态=FAILED

终止条件:
    队列空 → 状态=COMPLETED
    迭代超限 → 状态=ITERATION_EXCEEDED
    不可恢复错误 → 状态=FAILED
```

### 6.4 上下文持久化

每次迭代后保存状态，支持中断恢复：

```java
@Component
public class DecomposeStatePersister {

    private final DecomposeExecutionStateRepository repository;
    private final ObjectMapper objectMapper;

    public void save(DecomposeContext context) {
        DecomposeExecutionState state = new DecomposeExecutionState();
        state.setExecutionId(context.getExecutionId());
        state.setStatus(context.getStatus().name());
        state.setIterationCount(context.getIterationCount());
        state.setTaskQueue(serializeQueue(context.getTaskQueue()));
        state.setCompletedTasks(serializeList(context.getCompletedTasks()));
        state.setFailedTasks(serializeList(context.getFailedTasks()));
        state.setUpdatedAt(LocalDateTime.now());

        repository.save(state);
    }

    public DecomposeContext load(String executionId) {
        DecomposeExecutionState state = repository.findByExecutionId(executionId)
            .orElseThrow(() -> new NotFoundException("执行状态不存在"));

        DecomposeContext context = new DecomposeContext();
        context.setExecutionId(state.getExecutionId());
        context.setStatus(DecomposeStatus.valueOf(state.getStatus()));
        context.setIterationCount(state.getIterationCount());
        context.setTaskQueue(deserializeQueue(state.getTaskQueue()));
        context.setCompletedTasks(deserializeList(state.getCompletedTasks()));
        context.setFailedTasks(deserializeList(state.getFailedTasks()));

        return context;
    }

    private String serializeQueue(Queue<SubTask> queue) {
        return objectMapper.writeValueAsString(new ArrayList<>(queue));
    }

    private Queue<SubTask> deserializeQueue(String json) {
        List<SubTask> list = objectMapper.readValue(json,
            new TypeReference<List<SubTask>>() {});
        return new LinkedList<>(list);
    }
}
```

---

## 七、与工作流引擎集成

### 7.1 节点配置

SmartDecompose 作为一种节点类型，配置示例：

```json
{
  "id": "node_001",
  "type": "smart_decompose",
  "name": "智能分解执行",
  "config": {
    "maxRetries": 5,
    "maxIterations": 50,
    "requireManualReview": true,
    "decisionTemplateId": "tpl_decision_code",
    "reviewTemplateId": "tpl_review_strict"
  }
}
```

### 7.2 节点处理器

```java
@Component
public class SmartDecomposeHandler extends BaseNodeHandler {

    @Autowired
    private DecomposeOrchestrator orchestrator;

    @Autowired
    private PromptTemplateRepository templateRepository;

    @Override
    public String getNodeType() {
        return "smart_decompose";
    }

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        // 1. 解析配置
        SmartDecomposeConfig config = SmartDecomposeConfig.fromJson(
            ctx.getNode().getConfig());

        // 2. 初始化上下文
        DecomposeContext context = new DecomposeContext();
        context.setExecutionId(ctx.getExecutionId());
        context.setWorkflowId(ctx.getWorkflowId());
        context.setNodeId(ctx.getNode().getId());
        context.setProjectPath(ctx.getProjectPath());
        context.setTechStack(ctx.getTechStack());
        context.setMaxRetries(config.getMaxRetries());
        context.setMaxIterations(config.getMaxIterations());
        context.setRequireManualReview(config.isRequireManualReview());

        // 3. 加载提示词模板
        if (config.getDecisionTemplateId() != null) {
            context.setDecisionTemplate(
                templateRepository.findById(config.getDecisionTemplateId()).orElse(null));
        }
        if (config.getReviewTemplateId() != null) {
            context.setReviewTemplate(
                templateRepository.findById(config.getReviewTemplateId()).orElse(null));
        }

        // 4. 创建根任务
        SubTask rootTask = SubTask.builder()
            .id("TASK_ROOT")
            .description(ctx.getInputData().getTaskDescription())
            .depth(0)
            .build();
        context.getTaskQueue().add(rootTask);

        // 5. 执行编排
        orchestrator.run(context);

        // 6. 返回结果
        return NodeResult.builder()
            .status(context.getStatus() == DecomposeStatus.COMPLETED
                ? NodeResult.Status.SUCCESS
                : NodeResult.Status.FAILED)
            .output(Map.of(
                "completedTasks", context.getCompletedTasks().size(),
                "failedTasks", context.getFailedTasks().size(),
                "iterations", context.getIterationCount()
            ))
            .build();
    }
}
```

### 7.3 上下游数据传递

**上游输入**：
- `taskDescription`：任务描述（必需）
- `projectPath`：项目路径
- `techStack`：技术栈信息
- `globalPrompt`：全局提示

**下游输出**：
- `completedTasks`：已完成任务列表
- `failedTasks`：失败任务列表
- `executionSummary`：执行摘要

---

## 八、扩展能力

### 8.1 提示词模板扩展

通过数据库配置不同模板：
- 不同场景使用不同模板（代码开发、文档编写、测试验证）
- 模板支持版本管理
- 支持模板预览和测试

### 8.2 审核策略扩展

可配置不同的审核严格程度：
- `宽松模式`：只检查文件是否存在
- `标准模式`：检查基本功能
- `严格模式`：检查代码规范、边界情况

### 8.3 拦截器扩展

在关键节点插入自定义逻辑：
- 任务拆分前：检查拆分深度
- 执行前：记录日志、权限检查
- 审核后：发送通知