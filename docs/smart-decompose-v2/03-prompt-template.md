# SmartDecompose 提示词模板设计

## 一、设计原则

### 1.1 存储策略

| 内容 | 存储位置 | 原因 |
|-----|---------|------|
| 提示词模板主体 | 数据库 | 需要灵活调整，不同场景不同模板 |
| JSON 输出格式定义 | 代码中 | 固定不变，硬编码更可靠 |
| 动态参数注入 | 代码中运行时生成 | 项目路径、任务描述等动态内容 |

### 1.2 模板类型

| 模板类型 | 用途 | 调用时机 |
|---------|------|---------|
| decision | 决策提示词 | 每次取出任务时 |
| review | 审核提示词 | 每次执行完成后 |
| retry | 重试提示词 | 审核拒绝后重新执行 |

---

## 二、数据库表设计

### 2.1 prompt_template 表

```sql
CREATE TABLE prompt_template (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,              -- decision / review / retry
    name TEXT NOT NULL,              -- 模板名称
    content TEXT NOT NULL,           -- 模板内容（带占位符）
    variables TEXT,                  -- 变量定义 JSON
    is_default INTEGER DEFAULT 0,    -- 是否默认模板
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 示例数据
INSERT INTO prompt_template (id, type, name, content, is_default) VALUES
('tpl_decision_default', 'decision', '默认决策模板', '...', 1),
('tpl_review_default', 'review', '默认审核模板', '...', 1),
('tpl_retry_default', 'retry', '默认重试模板', '...', 1);
```

### 2.2 变量定义格式

```json
{
  "variables": [
    {"name": "projectPath", "required": true, "description": "项目路径"},
    {"name": "taskDescription", "required": true, "description": "任务描述"},
    {"name": "completedTasks", "required": false, "description": "已完成任务信息"},
    {"name": "criteria", "required": false, "description": "验收标准"},
    {"name": "executionResult", "required": false, "description": "执行结果"},
    {"name": "previousIssues", "required": false, "description": "之前发现的问题"}
  ]
}
```

---

## 三、决策提示词模板

### 3.1 模板内容

```
你是一个智能任务执行引擎。请分析并处理以下任务。

═══════════════════════════════════════════════════════════════════════════

【项目信息】

项目路径: {{projectPath}}
{{#if techStack}}技术栈: {{techStack}}{{/if}}

【当前任务】

{{taskDescription}}

【已完成的前置任务】

{{#if completedTasks}}
{{completedTasks}}
{{else}}
（暂无，这是第一个任务）
{{/if}}

═══════════════════════════════════════════════════════════════════════════

【决策规则】

1. 评估任务复杂度：
   - 如果预估可以在 8 分钟内完成，选择 execute
   - 如果任务包含多个独立步骤，选择 split

2. 判断依据：
   - 需要创建多个文件 → split
   - 需要修改多个模块 → split
   - 需要验证多个功能点 → split
   - 单一文件创建/修改 → execute
   - 单一功能验证 → execute

═══════════════════════════════════════════════════════════════════════════

【输出格式】

请严格按照以下 JSON 格式输出，不要输出其他内容：

选择 execute（直接执行）时：
```json
{
  "decision": "execute",
  "thought": "你的分析过程",
  "result": "执行后的结果描述，包括做了什么操作、创建了什么文件等"
}
```

选择 split（拆分任务）时：
```json
{
  "decision": "split",
  "thought": "你的分析过程",
  "tasks": [
    {
      "id": "TASK_001",
      "description": "子任务描述",
      "criteria": "验收标准：完成时应该达到什么状态",
      "estimatedMinutes": 3
    }
  ]
}
```

═══════════════════════════════════════════════════════════════════════════

请开始分析和处理任务，直接输出 JSON 结果。
```

### 3.2 代码中的 JSON 格式定义（硬编码）

```java
public class DecisionOutputFormat {

    // 执行格式
    public static final String EXECUTE_FORMAT = """
        {
          "decision": "execute",
          "thought": "你的分析过程",
          "result": "执行后的结果描述"
        }
        """;

    // 拆分格式
    public static final String SPLIT_FORMAT = """
        {
          "decision": "split",
          "thought": "你的分析过程",
          "tasks": [
            {
              "id": "TASK_XXX",
              "description": "子任务描述",
              "criteria": "验收标准",
              "estimatedMinutes": 数字
            }
          ]
        }
        """;

    // 用于验证响应的字段
    public static final List<String> REQUIRED_FIELDS = List.of("decision", "thought");
    public static final List<String> VALID_DECISIONS = List.of("execute", "split");
}
```

---

## 四、审核提示词模板

### 4.1 模板内容

```
你是一个严格的代码审核员。请审核以下任务执行结果。

═══════════════════════════════════════════════════════════════════════════

【原始任务】

{{taskDescription}}

【验收标准】

{{#if criteria}}
{{criteria}}
{{else}}
（无明确验收标准，请判断任务是否合理完成）
{{/if}}

【执行者报告】

{{executionResult}}

【项目路径】

{{projectPath}}

{{#if previousIssues}}
【之前发现的问题】

{{#each previousIssues}}
- {{this}}
{{/each}}

请确保这些问题已经解决。
{{/if}}

═══════════════════════════════════════════════════════════════════════════

【审核要求】

1. 验证执行者是否真的完成了任务
   - 如果提到创建了文件，请检查文件是否存在
   - 如果提到修改了代码，请检查修改是否正确

2. 验证是否符合验收标准
   - 逐条对照验收标准检查

3. 检查是否有明显问题
   - 语法错误
   - 逻辑错误
   - 遗漏的功能

═══════════════════════════════════════════════════════════════════════════

【输出格式】

请严格按照以下 JSON 格式输出：

审核通过：
```json
{
  "status": "APPROVED",
  "thought": "审核分析过程",
  "summary": "任务完成情况总结"
}
```

审核拒绝：
```json
{
  "status": "REJECTED",
  "thought": "审核分析过程",
  "issues": [
    "发现的问题1",
    "发现的问题2"
  ],
  "suggestion": "修改建议"
}
```

═══════════════════════════════════════════════════════════════════════════

请开始审核，直接输出 JSON 结果。
```

### 4.2 代码中的 JSON 格式定义

```java
public class ReviewOutputFormat {

    public static final String APPROVED_FORMAT = """
        {
          "status": "APPROVED",
          "thought": "审核分析过程",
          "summary": "任务完成情况总结"
        }
        """;

    public static final String REJECTED_FORMAT = """
        {
          "status": "REJECTED",
          "thought": "审核分析过程",
          "issues": ["问题1", "问题2"],
          "suggestion": "修改建议"
        }
        """;

    public static final List<String> REQUIRED_FIELDS = List.of("status", "thought");
    public static final List<String> VALID_STATUSES = List.of("APPROVED", "REJECTED");
}
```

---

## 五、重试提示词模板

### 5.1 模板内容

```
你需要重新执行任务，并解决审核中发现的问题。

═══════════════════════════════════════════════════════════════════════════

【原始任务】

{{taskDescription}}

【验收标准】

{{criteria}}

【之前执行的问题】

审核员发现了以下问题：

{{#each previousIssues}}
{{@index}}. {{this}}
{{/each}}

【项目路径】

{{projectPath}}

═══════════════════════════════════════════════════════════════════════════

【要求】

1. 重新执行任务
2. 确保解决上述所有问题
3. 完成后说明你做了哪些修改

═══════════════════════════════════════════════════════════════════════════

【输出格式】

请输出 JSON：
```json
{
  "result": "执行结果，说明解决了哪些问题"
}
```
```

---

## 六、模板渲染器

### 6.1 PromptBuilder 实现

```java
@Component
public class PromptBuilder {

    private final PromptTemplateRepository templateRepository;

    /**
     * 构建决策提示词
     */
    public String buildDecisionPrompt(DecomposeContext ctx, SubTask task) {
        PromptTemplate template = templateRepository.findDefaultByType("decision");

        Map<String, Object> params = new HashMap<>();
        params.put("projectPath", ctx.getProjectPath());
        params.put("techStack", ctx.getTechStack());
        params.put("taskDescription", task.getDescription());
        params.put("completedTasks", formatCompletedTasks(ctx.getCompletedTasks()));

        return renderTemplate(template.getContent(), params);
    }

    /**
     * 构建审核提示词
     */
    public String buildReviewPrompt(DecomposeContext ctx, SubTask task,
                                     String executionResult, List<String> previousIssues) {
        PromptTemplate template = templateRepository.findDefaultByType("review");

        Map<String, Object> params = new HashMap<>();
        params.put("taskDescription", task.getDescription());
        params.put("criteria", task.getCriteria());
        params.put("executionResult", executionResult);
        params.put("projectPath", ctx.getProjectPath());
        params.put("previousIssues", previousIssues);

        return renderTemplate(template.getContent(), params);
    }

    /**
     * 构建重试提示词
     */
    public String buildRetryPrompt(DecomposeContext ctx, SubTask task, List<String> issues) {
        PromptTemplate template = templateRepository.findDefaultByType("retry");

        Map<String, Object> params = new HashMap<>();
        params.put("taskDescription", task.getDescription());
        params.put("criteria", task.getCriteria());
        params.put("previousIssues", issues);
        params.put("projectPath", ctx.getProjectPath());

        return renderTemplate(template.getContent(), params);
    }

    /**
     * 渲染模板
     */
    private String renderTemplate(String template, Map<String, Object> params) {
        // 使用简单的占位符替换
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }

        // 处理条件块 {{#if ...}}...{{/if}}
        result = processConditionals(result, params);

        // 处理循环 {{#each ...}}...{{/each}}
        result = processLoops(result, params);

        return result;
    }

    /**
     * 格式化已完成任务信息
     */
    private String formatCompletedTasks(List<SubTask> completedTasks) {
        if (completedTasks == null || completedTasks.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < completedTasks.size(); i++) {
            SubTask task = completedTasks.get(i);
            sb.append(i + 1).append(". ").append(task.getDescription());
            if (task.getResult() != null) {
                sb.append(" ✓");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
```

---

## 七、模板管理 API

### 7.1 CRUD 接口

```java
@RestController
@RequestMapping("/api/prompt-templates")
public class PromptTemplateController {

    /**
     * 获取模板列表
     */
    @GetMapping
    public List<PromptTemplate> list(@RequestParam(required = false) String type) {
        if (type != null) {
            return templateRepository.findByType(type);
        }
        return templateRepository.findAll();
    }

    /**
     * 获取单个模板
     */
    @GetMapping("/{id}")
    public PromptTemplate get(@PathVariable String id) {
        return templateRepository.findById(id);
    }

    /**
     * 创建模板
     */
    @PostMapping
    public PromptTemplate create(@RequestBody PromptTemplate template) {
        validateTemplate(template);
        return templateRepository.save(template);
    }

    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    public PromptTemplate update(@PathVariable String id, @RequestBody PromptTemplate template) {
        template.setId(id);
        validateTemplate(template);
        return templateRepository.save(template);
    }

    /**
     * 设置默认模板
     */
    @PostMapping("/{id}/set-default")
    public void setDefault(@PathVariable String id) {
        PromptTemplate template = templateRepository.findById(id);
        // 清除同类型的其他默认
        templateRepository.clearDefaultByType(template.getType());
        // 设置当前为默认
        template.setDefault(true);
        templateRepository.save(template);
    }

    /**
     * 预览渲染结果
     */
    @PostMapping("/preview")
    public String preview(@RequestBody PreviewRequest request) {
        return promptBuilder.renderTemplate(request.getTemplate(), request.getParams());
    }
}
```

### 7.2 预览请求格式

```json
{
  "template": "你收到一个任务：{{taskDescription}}...",
  "params": {
    "taskDescription": "创建一个登录页面",
    "projectPath": "/Users/demo/project"
  }
}
```

---

## 八、模板版本管理

### 8.1 版本表设计

```sql
CREATE TABLE prompt_template_version (
    id TEXT PRIMARY KEY,
    template_id TEXT NOT NULL,
    version INTEGER NOT NULL,
    content TEXT NOT NULL,
    change_note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by TEXT,
    FOREIGN KEY (template_id) REFERENCES prompt_template(id)
);

-- 每次更新模板时，自动创建版本记录
```

### 8.2 版本回滚

```java
@PostMapping("/{id}/rollback/{version}")
public PromptTemplate rollback(@PathVariable String id, @PathVariable int version) {
    PromptTemplateVersion oldVersion = versionRepository.findByTemplateIdAndVersion(id, version);
    PromptTemplate template = templateRepository.findById(id);
    template.setContent(oldVersion.getContent());
    return templateRepository.save(template);
}
```