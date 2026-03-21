# SmartDecompose 配置与扩展

## 一、节点配置

### 1.1 配置参数

| 参数 | 类型 | 默认值 | 说明 |
|-----|------|-------|------|
| maxRetries | int | 5 | 审核拒绝后的最大重试次数 |
| maxIterations | int | 50 | 最大迭代次数（防止死循环） |
| requireManualReview | boolean | true | 重试失败后是否触发人工审核 |
| decisionTemplateId | string | null | 指定决策提示词模板ID（可选） |
| reviewTemplateId | string | null | 指定审核提示词模板ID（可选） |

### 1.2 配置示例

```json
{
  "maxRetries": 5,
  "maxIterations": 50,
  "requireManualReview": true
}
```

### 1.3 配置类

```java
public class SmartDecomposeConfig {

    private int maxRetries = 5;
    private int maxIterations = 50;
    private boolean requireManualReview = true;
    private String decisionTemplateId;
    private String reviewTemplateId;

    // 从 JSON 字符串解析
    public static SmartDecomposeConfig fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new SmartDecomposeConfig();
        }
        try {
            return new ObjectMapper().readValue(json, SmartDecomposeConfig.class);
        } catch (Exception e) {
            return new SmartDecomposeConfig();
        }
    }

    // Getters, Setters...
}
```

---

## 二、提示词模板配置

### 2.1 默认模板初始化

系统启动时自动初始化默认模板：

```java
@Component
public class PromptTemplateInitializer implements ApplicationRunner {

    @Autowired
    private PromptTemplateRepository templateRepository;

    @Override
    public void run(ApplicationArguments args) {
        initDecisionTemplate();
        initReviewTemplate();
        initRetryTemplate();
    }

    private void initDecisionTemplate() {
        if (!templateRepository.findByType("decision").isEmpty()) {
            return; // 已存在
        }

        PromptTemplate template = new PromptTemplate();
        template.setId("tpl_decision_default");
        template.setType("decision");
        template.setName("默认决策模板");
        template.setContent(loadResource("templates/decision-template.md"));
        template.setDefault(true);

        templateRepository.save(template);
    }

    private String loadResource(String path) {
        try {
            return IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(path),
                StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            throw new RuntimeException("加载模板失败: " + path, e);
        }
    }
}
```

### 2.2 模板文件位置

```
src/main/resources/templates/
├── decision-template.md    # 决策提示词模板
├── review-template.md      # 审核提示词模板
└── retry-template.md       # 重试提示词模板
```

### 2.3 通过 API 管理模板

**获取模板列表**
```
GET /api/prompt-templates?type=decision
```

**创建新模板**
```
POST /api/prompt-templates
Content-Type: application/json

{
  "type": "decision",
  "name": "代码开发专用模板",
  "content": "模板内容...",
  "isDefault": false
}
```

**更新模板**
```
PUT /api/prompt-templates/{id}
Content-Type: application/json

{
  "content": "更新后的模板内容..."
}
```

**设置默认模板**
```
POST /api/prompt-templates/{id}/set-default
```

**预览模板渲染**
```
POST /api/prompt-templates/preview
Content-Type: application/json

{
  "template": "项目路径: {{projectPath}}\n任务: {{taskDescription}}",
  "params": {
    "projectPath": "/Users/demo/project",
    "taskDescription": "创建登录功能"
  }
}
```

---

## 三、人工审核配置

### 3.1 人工审核回调 API

```
POST /api/executions/{executionId}/manual-review/{reviewId}
Content-Type: application/json

{
  "action": "approve",  // approve 或 reject
  "comment": "同意通过",
  "reviewer": "user_001"
}
```

### 3.2 回调处理

```java
@RestController
@RequestMapping("/api/executions")
public class ManualReviewController {

    @Autowired
    private ManualReviewRepository manualReviewRepository;

    @Autowired
    private DecomposeOrchestrator orchestrator;

    @Autowired
    private ExecutionStateRepository stateRepository;

    @PostMapping("/{executionId}/manual-review/{reviewId}")
    public Map<String, Object> handleManualReview(
            @PathVariable String executionId,
            @PathVariable String reviewId,
            @RequestBody ManualReviewRequest request) {

        // 1. 加载审核记录
        ManualReviewRecord record = manualReviewRepository.findById(reviewId)
            .orElseThrow(() -> new NotFoundException("审核记录不存在"));

        if (record.getStatus() != ManualReviewStatus.WAITING) {
            throw new IllegalStateException("审核记录已处理");
        }

        // 2. 更新审核记录
        record.setStatus("approve".equals(request.getAction())
            ? ManualReviewStatus.APPROVED
            : ManualReviewStatus.REJECTED);
        record.setReviewer(request.getReviewer());
        record.setReviewComment(request.getComment());
        record.setReviewedAt(LocalDateTime.now());
        manualReviewRepository.save(record);

        // 3. 恢复执行
        if ("approve".equals(request.getAction())) {
            DecomposeContext context = stateRepository.load(executionId);
            context.setStatus(DecomposeStatus.RUNNING);
            orchestrator.run(context);
        }

        return Map.of(
            "success", true,
            "status", record.getStatus().name()
        );
    }
}
```

---

## 四、扩展点

### 4.1 自定义决策处理器

```java
/**
 * 自定义决策处理器接口
 */
public interface DecisionHandler {

    /**
     * 处理决策
     * @return true 表示已处理，false 表示使用默认处理
     */
    boolean handle(DecomposeContext context, SubTask task, DecisionResponse decision);
}

/**
 * 注册自定义处理器
 */
@Component
public class CustomDecisionHandlerRegistry {

    private Map<String, DecisionHandler> handlers = new HashMap<>();

    public void register(String type, DecisionHandler handler) {
        handlers.put(type, handler);
    }

    public DecisionHandler getHandler(String type) {
        return handlers.get(type);
    }
}
```

### 4.2 自定义审核策略

```java
/**
 * 审核策略接口
 */
public interface ReviewStrategy {

    /**
     * 判断是否需要人工审核
     */
    boolean requireManualReview(SubTask task, ReviewResponse review);

    /**
     * 生成重试提示词
     */
    String buildRetryPrompt(SubTask task, List<String> issues);
}

/**
 * 默认审核策略
 */
@Component
public class DefaultReviewStrategy implements ReviewStrategy {

    @Override
    public boolean requireManualReview(SubTask task, ReviewResponse review) {
        // 默认：重试次数超过限制时需要人工审核
        return true;
    }

    @Override
    public String buildRetryPrompt(SubTask task, List<String> issues) {
        return "请解决以下问题后重新执行：\n" +
            issues.stream().map(s -> "- " + s).collect(Collectors.joining("\n"));
    }
}
```

### 4.3 任务拆分拦截器

```java
/**
 * 任务拆分拦截器
 */
public interface TaskSplitInterceptor {

    /**
     * 拆分前调用
     * @return false 可阻止拆分
     */
    default boolean beforeSplit(SubTask parentTask, List<SubTask> subTasks) {
        return true;
    }

    /**
     * 拆分后调用
     */
    default void afterSplit(SubTask parentTask, List<SubTask> subTasks) {}
}

/**
 * 示例：限制拆分深度
 */
@Component
public class MaxDepthInterceptor implements TaskSplitInterceptor {

    private static final int MAX_DEPTH = 5;

    @Override
    public boolean beforeSplit(SubTask parentTask, List<SubTask> subTasks) {
        if (parentTask.getDepth() >= MAX_DEPTH) {
            throw new IllegalStateException("超过最大拆分深度: " + MAX_DEPTH);
        }
        return true;
    }
}
```

---

## 五、监控与日志

### 5.1 执行日志

```
[INFO] SmartDecompose 开始执行: executionId=exec_001
[INFO] 处理任务: id=TASK_ROOT, description=开发登录网站
[INFO] 决策结果: decision=split, thought=任务复杂，需要拆分
[INFO] 任务拆分为 3 个子任务
[INFO] 处理任务: id=TASK_001, description=创建项目结构
[INFO] 决策结果: decision=execute
[INFO] 审核通过: 项目结构创建成功
[INFO] 处理任务: id=TASK_002, description=实现登录Controller
[WARN] 审核拒绝 (第1次): [文件不存在, 缺少POST接口]
[INFO] 审核通过: 登录Controller创建成功
[INFO] SmartDecompose 执行完成: iterations=5, completedTasks=3
```

### 5.2 决策历史查询

```
GET /api/executions/{executionId}/decisions
```

响应：
```json
{
  "executionId": "exec_001",
  "decisions": [
    {
      "iteration": 1,
      "taskId": "TASK_ROOT",
      "decision": "split",
      "thought": "任务复杂，需要拆分",
      "createdAt": "2026-03-21T10:00:00"
    },
    {
      "iteration": 2,
      "taskId": "TASK_001",
      "decision": "execute",
      "thought": "简单任务，直接执行",
      "createdAt": "2026-03-21T10:01:00"
    }
  ]
}
```

### 5.3 执行状态查询

```
GET /api/executions/{executionId}/decompose-status
```

响应：
```json
{
  "executionId": "exec_001",
  "status": "RUNNING",
  "iterationCount": 3,
  "maxIterations": 50,
  "queueSize": 2,
  "completedCount": 1,
  "failedCount": 0,
  "currentTask": {
    "id": "TASK_002",
    "description": "实现登录Controller"
  }
}
```

---

## 六、性能配置

### 6.1 OpenClaw 调用配置

```yaml
# application.yml
openclaw:
  gateway:
    url: http://localhost:18789
    token: your-token
  agent:
    id: smart-decompose
  timeout:
    decision: 480000    # 决策超时 8分钟
    review: 60000       # 审核超时 1分钟
  retry:
    max-attempts: 3     # 调用失败重试次数
    backoff: 1000       # 重试间隔
```

### 6.2 缓存配置

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(100)
        );
        return cacheManager;
    }
}

// 提示词模板缓存
@Cacheable(value = "prompt-templates", key = "#type")
public PromptTemplate findDefaultByType(String type) {
    return templateRepository.findByTypeAndIsDefaultTrue(type)
        .orElseThrow(() -> new IllegalStateException("模板不存在"));
}
```