# SmartDecompose 测试方案

## 一、测试策略

### 1.1 测试层次

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              测试金字塔                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                          ┌─────────────┐                                    │
│                          │  E2E 测试   │  完整工作流执行                    │
│                          └─────────────┘                                    │
│                       ┌───────────────────┐                                 │
│                       │   集成测试        │  OpenClaw 调用、数据库交互       │
│                       └───────────────────┘                                 │
│                    ┌─────────────────────────┐                              │
│                    │       单元测试          │  提示词构建、响应解析、逻辑   │
│                    └─────────────────────────┘                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 测试分类

| 类型 | 说明 | Mock 策略 |
|-----|------|----------|
| 单元测试 | 测试单个类/方法 | Mock 所有外部依赖 |
| 集成测试 | 测试模块间交互 | Mock OpenClaw，使用真实数据库 |
| E2E 测试 | 完整流程测试 | 使用真实 OpenClaw |

---

## 二、单元测试

### 2.1 PromptBuilder 测试

```java
@ExtendWith(MockitoExtension.class)
class PromptBuilderTest {

    @Mock
    private PromptTemplateRepository templateRepository;

    @InjectMocks
    private PromptBuilder promptBuilder;

    @Test
    @DisplayName("构建决策提示词 - 成功")
    void testBuildDecisionPrompt_success() {
        // Given
        DecomposeContext context = createTestContext();
        SubTask task = SubTask.builder()
            .id("TASK_001")
            .description("创建一个登录页面")
            .build();

        PromptTemplate template = new PromptTemplate();
        template.setContent("项目路径: {{projectPath}}\n任务: {{taskDescription}}");
        when(templateRepository.findByTypeAndIsDefaultTrue("decision"))
            .thenReturn(Optional.of(template));

        context.setDecisionTemplate(template);

        // When
        String prompt = promptBuilder.buildDecisionPrompt(context, task);

        // Then
        assertThat(prompt).contains("/Users/test/project");
        assertThat(prompt).contains("创建一个登录页面");
    }

    @Test
    @DisplayName("构建审核提示词 - 包含问题列表")
    void testBuildReviewPrompt_withIssues() {
        // Given
        DecomposeContext context = createTestContext();
        SubTask task = SubTask.builder()
            .id("TASK_001")
            .description("创建登录页面")
            .criteria("页面包含用户名密码输入框")
            .build();

        String execResult = "已创建 login.html";
        List<String> issues = List.of("缺少密码输入框", "缺少提交按钮");

        PromptTemplate template = new PromptTemplate();
        template.setContent("任务: {{taskDescription}}\n问题: {{previousIssues}}");
        context.setReviewTemplate(template);

        // When
        String prompt = promptBuilder.buildReviewPrompt(context, task, execResult, issues);

        // Then
        assertThat(prompt).contains("缺少密码输入框");
        assertThat(prompt).contains("缺少提交按钮");
    }

    private DecomposeContext createTestContext() {
        DecomposeContext context = new DecomposeContext();
        context.setExecutionId("test-exec-001");
        context.setProjectPath("/Users/test/project");
        return context;
    }
}
```

### 2.2 ResponseParser 测试

```java
class ResponseParserTest {

    private ResponseParser parser = new ResponseParser();

    @Test
    @DisplayName("解析决策响应 - execute")
    void testParseDecision_execute() {
        String rawResponse = """
            ```json
            {
              "decision": "execute",
              "thought": "简单任务，直接执行",
              "result": "已创建文件"
            }
            ```
            """;

        DecisionResponse response = parser.parseDecision(rawResponse);

        assertThat(response.getDecision()).isEqualTo("execute");
        assertThat(response.getThought()).isEqualTo("简单任务，直接执行");
        assertThat(response.getResult()).isEqualTo("已创建文件");
    }

    @Test
    @DisplayName("解析决策响应 - split")
    void testParseDecision_split() {
        String rawResponse = """
            {
              "decision": "split",
              "thought": "任务复杂，需要拆分",
              "tasks": [
                {
                  "id": "TASK_001",
                  "description": "创建项目结构",
                  "criteria": "pom.xml 存在",
                  "estimatedMinutes": 2
                }
              ]
            }
            """;

        DecisionResponse response = parser.parseDecision(rawResponse);

        assertThat(response.getDecision()).isEqualTo("split");
        assertThat(response.getTasks()).hasSize(1);
        assertThat(response.getTasks().get(0).getId()).isEqualTo("TASK_001");
    }

    @Test
    @DisplayName("解析审核响应 - APPROVED")
    void testParseReview_approved() {
        String rawResponse = """
            {
              "status": "APPROVED",
              "thought": "检查通过",
              "summary": "文件创建成功"
            }
            """;

        ReviewResponse response = parser.parseReview(rawResponse);

        assertThat(response.isApproved()).isTrue();
        assertThat(response.getSummary()).isEqualTo("文件创建成功");
    }

    @Test
    @DisplayName("解析审核响应 - REJECTED")
    void testParseReview_rejected() {
        String rawResponse = """
            {
              "status": "REJECTED",
              "thought": "检查失败",
              "issues": ["文件不存在", "代码有误"],
              "suggestion": "请重新创建"
            }
            """;

        ReviewResponse response = parser.parseReview(rawResponse);

        assertThat(response.isRejected()).isTrue();
        assertThat(response.getIssues()).containsExactly("文件不存在", "代码有误");
        assertThat(response.getSuggestion()).isEqualTo("请重新创建");
    }

    @Test
    @DisplayName("解析响应 - 无 JSON 块抛出异常")
    void testParse_noJson_throwsException() {
        String rawResponse = "这是普通文本，没有 JSON";

        assertThrows(ResponseParseException.class, () -> {
            parser.parseDecision(rawResponse);
        });
    }

    @Test
    @DisplayName("解析响应 - 从 markdown 代码块提取")
    void testParse_fromMarkdownCodeBlock() {
        String rawResponse = """
            根据分析，我决定直接执行：

            ```json
            {
              "decision": "execute",
              "thought": "简单任务",
              "result": "完成"
            }
            ```

            以上是我的决策。
            """;

        DecisionResponse response = parser.parseDecision(rawResponse);

        assertThat(response.getDecision()).isEqualTo("execute");
    }
}
```

### 2.3 DecomposeOrchestrator 测试

```java
@ExtendWith(MockitoExtension.class)
class DecomposeOrchestratorTest {

    @Mock
    private PromptBuilder promptBuilder;

    @Mock
    private OpenClawClient openClawClient;

    @Mock
    private ResponseParser responseParser;

    @Mock
    private DecisionHistoryRepository decisionHistoryRepository;

    @InjectMocks
    private DecomposeOrchestrator orchestrator;

    @Test
    @DisplayName("执行简单任务 - 直接执行并审核通过")
    void testRun_simpleTask_executeAndApprove() {
        // Given
        DecomposeContext context = createTestContext();
        SubTask task = SubTask.builder()
            .id("TASK_ROOT")
            .description("创建 Hello.java")
            .build();
        context.getTaskQueue().add(task);

        DecisionResponse decision = new DecisionResponse();
        decision.setDecision("execute");
        decision.setResult("已创建 Hello.java");

        ReviewResponse review = new ReviewResponse();
        review.setStatus("APPROVED");
        review.setSummary("文件创建成功");

        when(promptBuilder.buildDecisionPrompt(any(), any())).thenReturn("prompt");
        when(openClawClient.execute(any())).thenReturn("raw-response");
        when(responseParser.parseDecision(any())).thenReturn(decision);
        when(responseParser.parseReview(any())).thenReturn(review);
        when(promptBuilder.buildReviewPrompt(any(), any(), any(), any())).thenReturn("review-prompt");

        // When
        orchestrator.run(context);

        // Then
        assertThat(context.getStatus()).isEqualTo(DecomposeStatus.COMPLETED);
        assertThat(context.getCompletedTasks()).hasSize(1);
    }

    @Test
    @DisplayName("执行复杂任务 - 拆分后执行")
    void testRun_complexTask_splitAndExecute() {
        // Given
        DecomposeContext context = createTestContext();
        SubTask rootTask = SubTask.builder()
            .id("TASK_ROOT")
            .description("开发登录功能")
            .build();
        context.getTaskQueue().add(rootTask);

        // 第一次决策：拆分
        DecisionResponse splitDecision = new DecisionResponse();
        splitDecision.setDecision("split");
        splitDecision.setTasks(List.of(
            SubTask.builder().id("TASK_001").description("创建Controller").build(),
            SubTask.builder().id("TASK_002").description("创建页面").build()
        ));

        // 第二、三次决策：执行
        DecisionResponse execDecision = new DecisionResponse();
        execDecision.setDecision("execute");
        execDecision.setResult("完成");

        ReviewResponse approvedReview = new ReviewResponse();
        approvedReview.setStatus("APPROVED");

        when(promptBuilder.buildDecisionPrompt(any(), any())).thenReturn("prompt");
        when(openClawClient.execute(any())).thenReturn("response");
        when(responseParser.parseDecision(any()))
            .thenReturn(splitDecision)  // 第一次：拆分
            .thenReturn(execDecision)   // 第二次：执行
            .thenReturn(execDecision);  // 第三次：执行
        when(responseParser.parseReview(any())).thenReturn(approvedReview);
        when(promptBuilder.buildReviewPrompt(any(), any(), any(), any())).thenReturn("review-prompt");

        // When
        orchestrator.run(context);

        // Then
        assertThat(context.getStatus()).isEqualTo(DecomposeStatus.COMPLETED);
        assertThat(context.getCompletedTasks()).hasSize(2); // 2个子任务
        assertThat(context.getIterationCount()).isEqualTo(3); // 1次拆分 + 2次执行
    }

    @Test
    @DisplayName("审核拒绝后重试成功")
    void testRun_reviewRejected_retrySuccess() {
        // Given
        DecomposeContext context = createTestContext();
        context.setMaxRetries(3);
        SubTask task = SubTask.builder()
            .id("TASK_ROOT")
            .description("创建文件")
            .build();
        context.getTaskQueue().add(task);

        DecisionResponse decision = new DecisionResponse();
        decision.setDecision("execute");
        decision.setResult("已创建文件");

        ReviewResponse rejectedReview = new ReviewResponse();
        rejectedReview.setStatus("REJECTED");
        rejectedReview.setIssues(List.of("文件不存在"));

        ReviewResponse approvedReview = new ReviewResponse();
        approvedReview.setStatus("APPROVED");

        when(promptBuilder.buildDecisionPrompt(any(), any())).thenReturn("prompt");
        when(promptBuilder.buildReviewPrompt(any(), any(), any(), any())).thenReturn("review-prompt");
        when(promptBuilder.buildRetryPrompt(any(), any(), any())).thenReturn("retry-prompt");
        when(openClawClient.execute(any())).thenReturn("response");
        when(responseParser.parseDecision(any())).thenReturn(decision);
        when(responseParser.parseReview(any()))
            .thenReturn(rejectedReview)  // 第一次审核：拒绝
            .thenReturn(approvedReview); // 第二次审核：通过

        // When
        orchestrator.run(context);

        // Then
        assertThat(context.getStatus()).isEqualTo(DecomposeStatus.COMPLETED);
        verify(openClawClient, times(2)).execute(any()); // 执行1次 + 重试1次
    }

    @Test
    @DisplayName("超过最大迭代次数")
    void testRun_exceedMaxIterations() {
        // Given
        DecomposeContext context = createTestContext();
        context.setMaxIterations(2);
        SubTask task = SubTask.builder()
            .id("TASK_ROOT")
            .description("任务")
            .build();
        context.getTaskQueue().add(task);

        DecisionResponse splitDecision = new DecisionResponse();
        splitDecision.setDecision("split");
        splitDecision.setTasks(List.of(
            SubTask.builder().id("TASK_001").description("子任务1").build(),
            SubTask.builder().id("TASK_002").description("子任务2").build()
        ));

        when(promptBuilder.buildDecisionPrompt(any(), any())).thenReturn("prompt");
        when(openClawClient.execute(any())).thenReturn("response");
        when(responseParser.parseDecision(any())).thenReturn(splitDecision);

        // When
        orchestrator.run(context);

        // Then
        assertThat(context.getStatus()).isEqualTo(DecomposeStatus.ITERATION_EXCEEDED);
    }

    private DecomposeContext createTestContext() {
        DecomposeContext context = new DecomposeContext();
        context.setExecutionId("test-exec-001");
        context.setWorkflowId("test-wf-001");
        context.setNodeId("node-001");
        context.setProjectPath("/Users/test/project");
        context.setMaxRetries(5);
        context.setMaxIterations(50);
        context.setStatus(DecomposeStatus.RUNNING);
        return context;
    }
}
```

---

## 三、集成测试

### 3.1 测试配置

```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
@AutoConfigureMockMvc
abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected OpenClawClient openClawClient;

    @Autowired
    protected PromptTemplateRepository promptTemplateRepository;

    @Autowired
    protected ExecutionStateRepository executionStateRepository;

    @BeforeEach
    void setUp() {
        // 清理数据
        executionStateRepository.deleteAll();

        // 初始化模板
        if (promptTemplateRepository.count() == 0) {
            initTemplates();
        }
    }

    private void initTemplates() {
        PromptTemplate decisionTemplate = new PromptTemplate();
        decisionTemplate.setId("tpl_decision_test");
        decisionTemplate.setType("decision");
        decisionTemplate.setName("测试决策模板");
        decisionTemplate.setContent(loadTemplate("decision-template.md"));
        decisionTemplate.setDefault(true);
        promptTemplateRepository.save(decisionTemplate);

        PromptTemplate reviewTemplate = new PromptTemplate();
        reviewTemplate.setId("tpl_review_test");
        reviewTemplate.setType("review");
        reviewTemplate.setName("测试审核模板");
        reviewTemplate.setContent(loadTemplate("review-template.md"));
        reviewTemplate.setDefault(true);
        promptTemplateRepository.save(reviewTemplate);
    }
}
```

### 3.2 SmartDecomposeHandler 集成测试

```java
class SmartDecomposeHandlerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SmartDecomposeHandler handler;

    @Test
    @DisplayName("完整流程 - 简单任务执行成功")
    void testExecute_simpleTask_success() throws Exception {
        // Given
        NodeExecutionContext ctx = createNodeContext("创建一个 Hello.java 文件");

        // Mock OpenClaw 响应
        DecisionResponse decision = new DecisionResponse();
        decision.setDecision("execute");
        decision.setThought("简单任务，直接执行");
        decision.setResult("已创建 Hello.java 文件");

        ReviewResponse review = new ReviewResponse();
        review.setStatus("APPROVED");
        review.setSummary("文件创建成功");

        when(openClawClient.execute(any())).thenReturn(toJson(decision));
        when(openClawClient.review(any())).thenReturn(toJson(review));

        // When
        NodeResult result = handler.execute(ctx);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsKey("completedTasks");
    }

    @Test
    @DisplayName("完整流程 - 复杂任务拆分执行")
    void testExecute_complexTask_splitAndExecute() throws Exception {
        // Given
        NodeExecutionContext ctx = createNodeContext("开发一个登录网站");

        // 第一次：拆分
        DecisionResponse splitResponse = new DecisionResponse();
        splitResponse.setDecision("split");
        splitResponse.setTasks(List.of(
            SubTask.builder().id("TASK_001").description("创建Controller").build(),
            SubTask.builder().id("TASK_002").description("创建页面").build()
        ));

        // 后续：执行
        DecisionResponse execResponse = new DecisionResponse();
        execResponse.setDecision("execute");
        execResponse.setResult("完成");

        ReviewResponse approvedReview = new ReviewResponse();
        approvedReview.setStatus("APPROVED");

        when(openClawClient.execute(any()))
            .thenReturn(toJson(splitResponse))   // 第一次：拆分
            .thenReturn(toJson(execResponse))    // 第二次：执行
            .thenReturn(toJson(execResponse));   // 第三次：执行
        when(openClawClient.review(any())).thenReturn(toJson(approvedReview));

        // When
        NodeResult result = handler.execute(ctx);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().get("completedTasks")).isEqualTo(2);
    }

    @Test
    @DisplayName("审核拒绝 - 重试成功")
    void testExecute_reviewRejected_retrySuccess() throws Exception {
        // Given
        NodeExecutionContext ctx = createNodeContext("创建配置文件");
        ctx.getNode().setConfig("{\"maxRetries\": 3}");

        DecisionResponse decision = new DecisionResponse();
        decision.setDecision("execute");
        decision.setResult("已创建文件");

        ReviewResponse rejectedReview = new ReviewResponse();
        rejectedReview.setStatus("REJECTED");
        rejectedReview.setIssues(List.of("文件路径错误"));

        ReviewResponse approvedReview = new ReviewResponse();
        approvedReview.setStatus("APPROVED");

        when(openClawClient.execute(any())).thenReturn(toJson(decision));
        when(openClawClient.review(any()))
            .thenReturn(toJson(rejectedReview))  // 第一次审核：拒绝
            .thenReturn(toJson(approvedReview)); // 第二次审核：通过

        // When
        NodeResult result = handler.execute(ctx);

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(openClawClient, times(2)).execute(any()); // 执行 + 重试
    }

    private NodeExecutionContext createNodeContext(String taskDescription) {
        WorkflowNode node = new WorkflowNode();
        node.setId("node-001");
        node.setType("smart_decompose");
        node.setConfig("{\"maxRetries\": 5, \"maxIterations\": 50}");

        NodeExecutionContext ctx = new NodeExecutionContext();
        ctx.setExecutionId("test-exec-" + System.currentTimeMillis());
        ctx.setWorkflowId("test-wf-001");
        ctx.setNode(node);
        ctx.setTaskDescription(taskDescription);
        ctx.setProjectPath("/Users/test/project");

        return ctx;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

### 3.3 提示词模板 API 测试

```java
class PromptTemplateApiTest extends BaseIntegrationTest {

    @Test
    @DisplayName("获取模板列表")
    void testListTemplates() throws Exception {
        mockMvc.perform(get("/api/prompt-templates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("创建新模板")
    void testCreateTemplate() throws Exception {
        String body = """
            {
              "type": "decision",
              "name": "测试模板",
              "content": "项目: {{projectPath}}",
              "isDefault": false
            }
            """;

        mockMvc.perform(post("/api/prompt-templates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("测试模板"));
    }

    @Test
    @DisplayName("预览模板渲染")
    void testPreviewTemplate() throws Exception {
        String body = """
            {
              "template": "项目路径: {{projectPath}}, 任务: {{taskDescription}}",
              "params": {
                "projectPath": "/Users/test/project",
                "taskDescription": "创建登录功能"
              }
            }
            """;

        mockMvc.perform(post("/api/prompt-templates/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/Users/test/project")))
            .andExpect(content().string(containsString("创建登录功能")));
    }
}
```

---

## 四、E2E 测试

### 4.1 测试场景设计

| 场景 | 输入任务 | 预期行为 | 验证点 |
|-----|---------|---------|-------|
| E2E-001 | 创建一个 Hello.java | 直接执行 | 文件存在 |
| E2E-002 | 开发登录网站 | 拆分执行 | 多个文件存在 |
| E2E-003 | 创建配置文件（故意失败） | 审核拒绝 → 重试 → 人工介入 | 人工审核记录 |
| E2E-004 | 超大任务 | 多层拆分 | 拆分深度合理 |

### 4.2 E2E 测试用例

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SmartDecomposeE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("E2E-001: 简单任务直接执行")
    void testE2E_001_simpleTask() {
        // Given
        String taskDescription = "在项目根目录创建一个 Hello.java 文件，包含 main 方法";
        String projectPath = System.getProperty("java.io.tmpdir") + "/test-project-" + System.currentTimeMillis();

        StartExecutionRequest request = new StartExecutionRequest();
        request.setInput(Map.of("task_description", taskDescription));
        request.setTaskConfig(Map.of("projectPath", projectPath));

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/workflows/{workflowId}/executions",
            request,
            Map.class,
            "test-workflow-id"
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String executionId = (String) response.getBody().get("executionId");

        // 等待执行完成
        await().atMost(2, TimeUnit.MINUTES).until(() -> {
            ResponseEntity<Map> status = restTemplate.getForEntity(
                "/api/executions/{executionId}",
                Map.class,
                executionId
            );
            return "COMPLETED".equals(status.getBody().get("status"));
        });

        // 验证文件存在
        assertThat(new File(projectPath, "Hello.java")).exists();
    }

    @Test
    @DisplayName("E2E-002: 复杂任务拆分执行")
    void testE2E_002_complexTask() {
        // Given
        String taskDescription = "开发一个简单的登录网站，包含登录页面和登录接口";
        String projectPath = System.getProperty("java.io.tmpdir") + "/test-login-" + System.currentTimeMillis();

        // When & Then
        // ... 类似 E2E-001
        // 验证：多个文件存在
    }

    @Test
    @DisplayName("E2E-003: 审核拒绝后人工介入")
    void testE2E_003_manualIntervention() throws InterruptedException {
        // Given: 配置一个会失败的模板
        // When: 执行任务
        // Then: 等待人工审核

        // 模拟人工审核
        ManualReviewRequest reviewRequest = new ManualReviewRequest();
        reviewRequest.setAction("approve");
        reviewRequest.setComment("人工通过");
        reviewRequest.setReviewer("tester");

        restTemplate.postForEntity(
            "/api/executions/{executionId}/manual-review/{reviewId}",
            reviewRequest,
            Map.class,
            executionId,
            reviewId
        );

        // 验证执行完成
    }
}
```

---

## 五、测试数据

### 5.1 决策响应 Mock 数据

```json
{
  "simple_execute": {
    "decision": "execute",
    "thought": "这是一个简单的单一文件创建任务，预计2分钟可完成",
    "result": "已在 /Users/test/project 创建 Hello.java 文件，包含标准的 main 方法"
  },

  "simple_split": {
    "decision": "split",
    "thought": "任务涉及多个独立模块，需要拆分执行",
    "tasks": [
      {
        "id": "TASK_001",
        "description": "创建 Spring Boot 项目结构",
        "criteria": "pom.xml 和主类文件存在",
        "estimatedMinutes": 2
      },
      {
        "id": "TASK_002",
        "description": "实现登录 Controller",
        "criteria": "LoginController.java 存在，包含 GET/POST 接口",
        "estimatedMinutes": 3
      },
      {
        "id": "TASK_003",
        "description": "创建登录页面",
        "criteria": "login.html 存在，包含用户名密码表单",
        "estimatedMinutes": 2
      }
    ]
  },

  "deep_split": {
    "decision": "split",
    "thought": "这是一个复杂系统，需要多层拆分",
    "tasks": [
      {
        "id": "TASK_BACKEND",
        "description": "开发后端 API",
        "criteria": "后端接口可用",
        "estimatedMinutes": 5
      },
      {
        "id": "TASK_FRONTEND",
        "description": "开发前端页面",
        "criteria": "前端页面可访问",
        "estimatedMinutes": 5
      }
    ]
  }
}
```

### 5.2 审核响应 Mock 数据

```json
{
  "approved": {
    "status": "APPROVED",
    "thought": "检查了项目目录，文件确实存在，代码结构正确",
    "summary": "任务完成，文件创建成功"
  },

  "rejected_missing_file": {
    "status": "REJECTED",
    "thought": "执行者声称创建了文件，但实际检查发现文件不存在",
    "issues": [
      "声称创建的文件 Hello.java 不存在",
      "没有实际写入文件内容"
    ],
    "suggestion": "请使用正确的文件写入方法创建文件"
  },

  "rejected_code_error": {
    "status": "REJECTED",
    "thought": "代码存在语法错误",
    "issues": [
      "第5行缺少分号",
      "类名与文件名不匹配"
    ],
    "suggestion": "请修正语法错误后重新提交"
  }
}
```

---

## 六、测试命令

### 6.1 运行所有测试

```bash
# 运行所有测试
mvn test

# 只运行单元测试
mvn test -Dtest="*Test"

# 只运行集成测试
mvn test -Dtest="*IntegrationTest"

# 只运行 E2E 测试
mvn test -Dtest="*E2ETest"

# 运行指定测试类
mvn test -Dtest="PromptBuilderTest"

# 运行指定测试方法
mvn test -Dtest="PromptBuilderTest#testBuildDecisionPrompt_success"
```

### 6.2 测试覆盖率

```bash
# 生成覆盖率报告
mvn jacoco:report

# 查看报告
open target/site/jacoco/index.html
```

### 6.3 测试验证清单

```bash
#!/bin/bash
# run-all-tests.sh

echo "=== 运行单元测试 ==="
mvn test -Dtest="*Test" -q

echo "=== 运行集成测试 ==="
mvn test -Dtest="*IntegrationTest" -q

echo "=== 运行 E2E 测试 ==="
mvn test -Dtest="*E2ETest" -q

echo "=== 生成覆盖率报告 ==="
mvn jacoco:report -q

echo "=== 测试完成 ==="
```

---

## 七、测试报告模板

### 7.1 每日测试报告

```
## SmartDecompose 测试报告

**日期**: 2026-03-21
**版本**: v2.0.0

### 测试概览

| 类型 | 总数 | 通过 | 失败 | 跳过 |
|-----|------|------|------|------|
| 单元测试 | 25 | 25 | 0 | 0 |
| 集成测试 | 8 | 8 | 0 | 0 |
| E2E 测试 | 4 | 3 | 1 | 0 |

### 覆盖率

| 模块 | 行覆盖率 | 分支覆盖率 |
|-----|---------|-----------|
| smartdecompose | 85% | 78% |
| prompt | 92% | 88% |
| client | 75% | 65% |

### 失败用例

1. E2E-003: 人工介入测试失败
   - 原因: 超时
   - 解决方案: 增加等待时间

### 建议

- 增加 client 模块的测试覆盖率
- 补充边界条件测试用例
```