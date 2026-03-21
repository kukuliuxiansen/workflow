# SmartDecompose 执行流程设计

## 一、整体流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SmartDecompose 执行流程                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  输入: NodeExecutionContext                                                 │
│  ├── taskDescription: "开发一个登录网站"                                    │
│  ├── projectPath: "/Users/demo/projects/login-app"                         │
│  └── nodeConfig: {maxRetries: 5, ...}                                       │
│                                                                             │
│                         ┌─────────────────┐                                 │
│                         │   初始化阶段    │                                 │
│                         └────────┬────────┘                                 │
│                                  │                                          │
│                                  ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         主执行循环                                   │   │
│  │                                                                     │   │
│  │   while (taskQueue不为空) {                                         │   │
│  │       task = taskQueue.poll()                                       │   │
│  │       response = makeDecision(task)                                 │   │
│  │                                                                     │   │
│  │       if (response.decision == "split") {                           │   │
│  │           taskQueue.addAll(response.tasks)                          │   │
│  │       } else {                                                      │   │
│  │           reviewResult = review(task, response.result)              │   │
│  │           if (!reviewResult.approved) {                             │   │
│  │               retry or manual                                       │   │
│  │           }                                                         │   │
│  │       }                                                             │   │
│  │   }                                                                 │   │
│  │                                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                  │                                          │
│                                  ▼                                          │
│                         ┌─────────────────┐                                 │
│                         │   收尾阶段      │                                 │
│                         └─────────────────┘                                 │
│                                  │                                          │
│                                  ▼                                          │
│  输出: NodeResult                                                          │
│  ├── success: true/false                                                    │
│  └── data: {completedTasks, failedTasks, ...}                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、详细流程

### 2.1 初始化阶段

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              初始化阶段                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  输入: NodeExecutionContext                                                 │
│                                                                             │
│  Step 1: 创建 DecomposeContext                                              │
│  ───────────────────────────────────                                        │
│  DecomposeContext context = new DecomposeContext();                        │
│  context.setExecutionId(ctx.getExecutionId());                             │
│  context.setWorkflowId(ctx.getWorkflowId());                                │
│  context.setNodeId(ctx.getNode().getId());                                  │
│  context.setProjectPath(ctx.getProjectPath());                              │
│  context.setGlobalPrompt(ctx.getGlobalPrompt());                            │
│                                                                             │
│  Step 2: 解析节点配置                                                       │
│  ─────────────────────────                                                  │
│  NodeConfig config = parseConfig(ctx.getNode().getConfig());               │
│  context.setMaxRetries(config.getMaxRetries() ?: 5);                        │
│  context.setMaxIterations(config.getMaxIterations() ?: 50);                 │
│  context.setRequireManualReview(config.isRequireManualReview() ?: true);   │
│                                                                             │
│  Step 3: 创建初始任务                                                       │
│  ─────────────────────────────                                              │
│  SubTask initialTask = SubTask.builder()                                    │
│      .id("TASK_ROOT_" + timestamp())                                        │
│      .description(ctx.getTaskDescription())                                 │
│      .depth(0)                                                              │
│      .build();                                                              │
│  context.getTaskQueue().add(initialTask);                                   │
│                                                                             │
│  Step 4: 加载提示词模板                                                     │
│  ───────────────────────────────                                            │
│  PromptTemplate decisionTemplate = promptTemplateRepo.findByType("decision");│
│  PromptTemplate reviewTemplate = promptTemplateRepo.findByType("review");   │
│  context.setDecisionTemplate(decisionTemplate);                             │
│  context.setReviewTemplate(reviewTemplate);                                 │
│                                                                             │
│  输出: DecomposeContext (已初始化)                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.2 主执行循环

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              主执行循环                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  while (!context.getTaskQueue().isEmpty()) {                                │
│                                                                             │
│      // ───────────────────────────────────────────────────────────────     │
│      // 检查迭代次数                                                       │
│      // ───────────────────────────────────────────────────────────────     │
│      if (context.getIterationCount() >= context.getMaxIterations()) {       │
│          context.setStatus(ITERATION_EXCEEDED);                             │
│          break;                                                             │
│      }                                                                      │
│                                                                             │
│      // ───────────────────────────────────────────────────────────────     │
│      // 取出任务                                                           │
│      // ───────────────────────────────────────────────────────────────     │
│      SubTask currentTask = context.getTaskQueue().poll();                   │
│      context.setCurrentTask(currentTask);                                   │
│      context.incrementIteration();                                          │
│                                                                             │
│      // ───────────────────────────────────────────────────────────────     │
│      // 决策阶段                                                           │
│      // ───────────────────────────────────────────────────────────────     │
│      DecisionResponse decision = makeDecision(context, currentTask);        │
│      recordDecision(context, decision);                                     │
│                                                                             │
│      // ───────────────────────────────────────────────────────────────     │
│      // 根据决策分支                                                       │
│      // ───────────────────────────────────────────────────────────────     │
│      if ("split".equals(decision.getDecision())) {                          │
│          // 子任务入队                                                     │
│          for (SubTask subTask : decision.getTasks()) {                      │
│              subTask.setDepth(currentTask.getDepth() + 1);                  │
│              context.getTaskQueue().add(subTask);                           │
│          }                                                                  │
│          log.info("任务拆分为 {} 个子任务", decision.getTasks().size());    │
│                                                                             │
│      } else if ("execute".equals(decision.getDecision())) {                 │
│          // 审核阶段                                                       │
│          boolean success = reviewAndHandle(context, currentTask, decision); │
│          if (!success) {                                                    │
│              // 审核失败且无法恢复                                         │
│              break;                                                         │
│          }                                                                  │
│          context.getCompletedTasks().add(currentTask);                      │
│      }                                                                      │
│  }                                                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.3 决策阶段

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              决策阶段                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  DecisionResponse makeDecision(DecomposeContext ctx, SubTask task)          │
│                                                                             │
│  Step 1: 构建决策提示词                                                     │
│  ───────────────────────────────                                            │
│  String prompt = PromptBuilder.buildDecisionPrompt(                         │
│      template: ctx.getDecisionTemplate(),                                   │
│      projectPath: ctx.getProjectPath(),                                     │
│      taskDescription: task.getDescription(),                                │
│      completedTasks: ctx.getCompletedTasksInfo()                            │
│  );                                                                         │
│                                                                             │
│  Step 2: 调用 OpenClaw                                                      │
│  ─────────────────────                                                      │
│  String rawResponse = openClawClient.execute(prompt);                       │
│                                                                             │
│  Step 3: 解析响应                                                           │
│  ─────────────────                                                          │
│  DecisionResponse response;                                                 │
│  try {                                                                      │
│      response = ResponseParser.parseDecision(rawResponse);                  │
│  } catch (JsonParseException e) {                                           │
│      // JSON 解析失败，尝试提取                                             │
│      response = ResponseParser.extractDecision(rawResponse);                │
│  }                                                                          │
│                                                                             │
│  Step 4: 验证响应                                                           │
│  ─────────────────                                                          │
│  if (!isValidDecision(response)) {                                          │
│      // 无效响应，重试或报错                                                │
│      throw new DecisionException("无效的决策响应");                         │
│  }                                                                          │
│                                                                             │
│  Step 5: 记录日志                                                           │
│  ─────────────────                                                          │
│  log.info("决策结果: decision={}, thought={}",                              │
│      response.getDecision(),                                                │
│      response.getThought()                                                  │
│  );                                                                         │
│                                                                             │
│  return response;                                                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.4 审核阶段

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              审核阶段                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  boolean reviewAndHandle(DecomposeContext ctx, SubTask task,                │
│                          DecisionResponse decision)                         │
│                                                                             │
│  int retryCount = 0;                                                        │
│  String executionResult = decision.getResult();                             │
│  List<String> previousIssues = new ArrayList<>();                           │
│                                                                             │
│  while (retryCount <= ctx.getMaxRetries()) {                                │
│                                                                             │
│      // ───────────────────────────────────────────────────────────────     │
│      // 构建审核提示词                                                     │
│      // ───────────────────────────────────────────────────────────────     │
│      String reviewPrompt = PromptBuilder.buildReviewPrompt(                 │
│          template: ctx.getReviewTemplate(),                                 │
│          taskDescription: task.getDescription(),                            │
│          criteria: task.getCriteria(),                                      │
│          executionResult: executionResult,                                  │
│          projectPath: ctx.getProjectPath(),                                 │
│          previousIssues: previousIssues                                     │
│      );                                                                     │
│                                                                             │
│      // ───────────────────────────────────────────────────────────────     │
│      // 调用审核 OpenClaw                                                  │
│      // ───────────────────────────────────────────────────────────────     │
│      String rawReview = openClawClient.review(reviewPrompt);                │
│      ReviewResponse review = ResponseParser.parseReview(rawReview);         │
│                                                                             │
│      // ───────────────────────────────────────────────────────────────     │
│      // 处理审核结果                                                       │
│      // ───────────────────────────────────────────────────────────────     │
│      if ("APPROVED".equals(review.getStatus())) {                           │
│          log.info("审核通过: {}", review.getSummary());                     │
│          return true;                                                       │
│      }                                                                      │
│                                                                             │
│      // ───────────────────────────────────────────────────────────────     │
│      // 审核拒绝                                                           │
│      // ───────────────────────────────────────────────────────────────     │
│      previousIssues = review.getIssues();                                   │
│      retryCount++;                                                          │
│                                                                             │
│      log.warn("审核拒绝 (第{}次): {}", retryCount, review.getIssues());     │
│                                                                             │
│      if (retryCount > ctx.getMaxRetries()) {                                │
│          // 超过重试次数                                                   │
│          if (ctx.isRequireManualReview()) {                                 │
│              return manualIntervention(ctx, task, executionResult, review); │
│          } else {                                                           │
│              context.addFailedTask(task, review);                           │
│              return false;                                                  │
│          }                                                                  │
│      }                                                                      │
│                                                                             │
│      // ───────────────────────────────────────────────────────────────     │
│      // 重新执行                                                           │
│      // ───────────────────────────────────────────────────────────────     │
│      String retryPrompt = PromptBuilder.buildRetryPrompt(                   │
│          task: task,                                                        │
│          issues: previousIssues,                                            │
│          projectPath: ctx.getProjectPath()                                  │
│      );                                                                     │
│      executionResult = openClawClient.execute(retryPrompt);                 │
│  }                                                                          │
│                                                                             │
│  return false;                                                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.5 人工介入

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              人工介入                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  boolean manualIntervention(DecomposeContext ctx, SubTask task,             │
│                             String execResult, ReviewResponse review)       │
│                                                                             │
│  Step 1: 创建人工审核记录                                                   │
│  ───────────────────────────────                                            │
│  ManualReviewRecord record = ManualReviewRecord.builder()                   │
│      .executionId(ctx.getExecutionId())                                     │
│      .taskId(task.getId())                                                  │
│      .taskDescription(task.getDescription())                                │
│      .executionResult(execResult)                                           │
│      .reviewIssues(review.getIssues())                                      │
│      .status(WAITING)                                                       │
│      .createTime(now())                                                     │
│      .build();                                                              │
│  manualReviewRepo.save(record);                                             │
│                                                                             │
│  Step 2: 更新上下文状态                                                     │
│  ─────────────────────────────                                              │
│  ctx.setStatus(WAITING_MANUAL_REVIEW);                                      │
│  ctx.setManualReviewId(record.getId());                                     │
│  saveState(ctx);  // 持久化状态                                             │
│                                                                             │
│  Step 3: 返回暂停状态                                                       │
│  ─────────────────────────────                                              │
│  // 工作流引擎会暂停此节点，等待人工回调                                    │
│  return NodeResult.waiting(                                                 │
│      "等待人工审核",                                                        │
│      Map.of("manualReviewId", record.getId())                               │
│  );                                                                         │
│                                                                             │
│  // ─────────────────────────────────────────────────────────────────────   │
│  // 人工审核回调 (单独的 API 调用)                                          │
│  // ─────────────────────────────────────────────────────────────────────   │
│                                                                             │
│  void handleManualReviewCallback(String reviewId, ManualReviewAction action)│
│                                                                             │
│  // 1. 加载审核记录                                                         │
│  ManualReviewRecord record = manualReviewRepo.findById(reviewId);           │
│                                                                             │
│  // 2. 加载上下文                                                           │
│  DecomposeContext ctx = loadState(record.getExecutionId());                 │
│                                                                             │
│  // 3. 处理用户决策                                                         │
│  if (action == APPROVE) {                                                   │
│      ctx.getCompletedTasks().add(ctx.getCurrentTask());                     │
│      ctx.setStatus(RUNNING);                                                │
│      // 恢复执行                                                           │
│      continueExecution(ctx);                                                │
│  } else {                                                                   │
│      ctx.setStatus(FAILED);                                                 │
│      ctx.setErrorMessage("人工审核拒绝");                                    │
│      // 返回失败                                                           │
│      return NodeResult.failed("人工审核拒绝");                              │
│  }                                                                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 三、状态流转

### 3.1 任务状态

```
┌─────────────┐     取出任务      ┌─────────────┐     执行完成     ┌─────────────┐
│   PENDING   │ ───────────────> │   RUNNING   │ ───────────────> │  COMPLETED  │
└─────────────┘                   └─────────────┘                   └─────────────┘
                                        │
                                        │ 审核拒绝且重试失败
                                        │
                                        ▼
                                  ┌─────────────┐
                                  │   FAILED    │
                                  └─────────────┘
```

### 3.2 上下文状态

```
┌─────────────┐
│   RUNNING   │ ◄─────────────────────────────────────────┐
└──────┬──────┘                                               │
       │                                                      │
       ├──────── 所有任务完成 ────────> ┌─────────────┐      │
       │                               │  COMPLETED  │      │
       │                               └─────────────┘      │
       │                                                      │
       ├──────── 迭代次数超限 ────────> ┌─────────────┐      │
       │                               │ITERATION_EXCEEDED│  │
       │                               └─────────────┘      │
       │                                                      │
       ├──────── 人工审核等待 ────────> ┌─────────────┐      │
       │                               │WAITING_MANUAL│──────┘
       │                               │   _REVIEW   │  人工通过后恢复
       │                               └─────────────┘
       │
       └──────── 严重错误 ────────> ┌─────────────┐
                                    │   FAILED    │
                                    └─────────────┘
```

---

## 四、异常处理

### 4.1 异常类型

| 异常 | 场景 | 处理方式 |
|-----|------|---------|
| `OpenClawException` | OpenClaw 调用失败 | 记录日志，重试 |
| `JsonParseException` | JSON 解析失败 | 尝试提取，或默认处理 |
| `DecisionException` | 决策响应无效 | 使用默认决策 |
| `ReviewRejectedException` | 审核拒绝 | 重试或人工介入 |
| `IterationExceededException` | 迭代次数超限 | 标记失败，返回 |

### 4.2 重试策略

```java
// OpenClaw 调用重试
@Retryable(
    value = OpenClawException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public String executeWithRetry(String prompt) {
    return openClawClient.execute(prompt);
}

// JSON 解析失败后的降级处理
public DecisionResponse parseDecisionWithFallback(String rawResponse) {
    try {
        return ResponseParser.parseDecision(rawResponse);
    } catch (JsonParseException e) {
        log.warn("JSON解析失败，尝试提取: {}", e.getMessage());
        return ResponseParser.extractDecision(rawResponse);
    }
}
```

---

## 五、日志记录

### 5.1 关键日志点

| 阶段 | 日志级别 | 内容 |
|-----|---------|------|
| 任务开始 | INFO | executionId, taskId, description |
| 决策结果 | INFO | decision, thought |
| 拆分结果 | INFO | 子任务数量, 子任务ID列表 |
| 审核开始 | DEBUG | taskId, executionResult摘要 |
| 审核通过 | INFO | taskId, summary |
| 审核拒绝 | WARN | retryCount, issues |
| 人工介入 | WARN | manualReviewId, taskId |
| 任务完成 | INFO | completedTasks数量, 总耗时 |
| 异常 | ERROR | 异常类型, 堆栈 |

### 5.2 决策历史记录

```java
void recordDecision(DecomposeContext ctx, SubTask task, DecisionResponse decision) {
    DecisionHistory history = DecisionHistory.builder()
        .executionId(ctx.getExecutionId())
        .iteration(ctx.getIterationCount())
        .taskId(task.getId())
        .decision(decision.getDecision())
        .thought(decision.getThought())
        .timestamp(now())
        .build();
    decisionHistoryRepo.save(history);
}
```