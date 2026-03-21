# SmartDecompose 工具系统设计文档

## 文档信息

| 项目名称 | Workflow Engine - SmartDecompose 工具系统 |
|---------|------------------------------------------|
| 版本 | 2.0.0 |
| 编写日期 | 2026-03-21 |
| 状态 | 设计方案 |

---

## 一、问题诊断

### 1.1 现有架构分析

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         当前执行链路                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  SmartDecomposeHandler.execute()                                        │
│         │                                                               │
│         ▼                                                               │
│  DecomposePromptBuilder.buildPrompt()  ───► 生成提示词（注入工具描述）    │
│         │                                                               │
│         ▼                                                               │
│  OpenClawGatewayClient.executeAgent()  ───► 调用远程LLM                 │
│         │                                                               │
│         ▼                                                               │
│  AgentOutputParser.parse()             ───► 解析LLM返回文本             │
│         │                                                               │
│         ▼                                                               │
│  ActionExecutor.execute()              ───► 分发到ActionHandler         │
│         │                                                               │
│         ▼                                                               │
│  RunCommandActionHandler.handle()      ───► ⚠️ 假实现，只log不执行      │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 问题根源

| 文件 | 行号 | 问题代码 | 影响 |
|-----|------|---------|-----|
| `RunCommandActionHandler.java` | 31-33 | `// Process process = Runtime.getRuntime().exec(command);` 被注释 | 命令不执行 |
| `WriteArtifactActionHandler.java` | 31 | `context.getArtifacts().add(artifact)` | 只存内存，不写文件 |
| `ReadContextActionHandler.java` | 11-25 | `context.getContextCache().get(key)` | 只读缓存，不读文件 |
| `DecomposePromptBuilder.java` | - | 未注入 `projectPath` | LLM 不知道项目路径 |

**核心结论**：工具层是假的，整个链路能跑通但无法产生实际效果。

---

## 二、完整执行链路设计

### 2.1 链路总览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              完整执行链路                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐                                                        │
│  │ 1. 初始化阶段    │                                                        │
│  └────────┬────────┘                                                        │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ DecomposePromptBuilder.buildPrompt()                                │   │
│  │                                                                     │   │
│  │ 输入: DecomposeContext (含 projectPath, taskDescription)            │   │
│  │ 输出: 完整提示词字符串                                               │   │
│  │                                                                     │   │
│  │ 生成内容:                                                           │   │
│  │ ├── # 当前状态 (迭代次数、任务栈深度)                                │   │
│  │ ├── # 当前任务 (任务ID、描述)                                        │   │
│  │ ├── # 项目信息 (projectPath)          ◄── 新增                       │   │
│  │ ├── # 可用工具 (工具名+描述+参数)      ◄── 扩展                       │   │
│  │ ├── # 决策框架 (输出格式说明)                                       │   │
│  │ └── # 最近历史 (最近3次决策)                                        │   │
│  └────────┬────────────────────────────────────────────────────────────┘   │
│           ▼                                                                 │
│  ┌─────────────────┐                                                        │
│  │ 2. LLM决策阶段  │                                                        │
│  └────────┬────────┘                                                        │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ OpenClawGatewayClient.executeAgent()                                │   │
│  │                                                                     │   │
│  │ 调用远程LLM，传入:                                                  │   │
│  │ - agentId: "project-manager"                                        │   │
│  │ - systemPrompt: DecomposePromptBuilder.buildSystemPrompt()          │   │
│  │ - message: 上一步生成的提示词                                        │   │
│  │                                                                     │   │
│  │ LLM返回:                                                            │   │
│  │ [THOUGHT]                                                           │   │
│  │ 需要查看项目的目录结构来了解代码组织...                               │   │
│  │ [/THOUGHT]                                                          │   │
│  │                                                                     │   │
│  │ [ACTION]                                                            │   │
│  │ tool: run_command                                                   │   │
│  │ command: ls -la /Users/demo/project                                 │   │
│  │ [/ACTION]                                                           │   │
│  └────────┬────────────────────────────────────────────────────────────┘   │
│           ▼                                                                 │
│  ┌─────────────────┐                                                        │
│  │ 3. 解析识别阶段 │                                                        │
│  └────────┬────────┘                                                        │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ AgentOutputParser.parse()                                           │   │
│  │                                                                     │   │
│  │ 解析流程:                                                           │   │
│  │                                                                     │   │
│  │ 步骤1: 提取 [THOUGHT]...[/THOUGHT] 块                               │   │
│  │        → 正则: \[THOUGHT\](.*?)\[/THOUGHT\]                         │   │
│  │        → 结果: "需要查看项目的目录结构来了解代码组织..."              │   │
│  │        → 存入: AgentAction.thought                                  │   │
│  │                                                                     │   │
│  │ 步骤2: 提取 [ACTION]...[/ACTION] 块                                 │   │
│  │        → 正则: \[ACTION\](.*?)\[/ACTION\]                           │   │
│  │        → 结果: "tool: run_command\ncommand: ls -la ..."             │   │
│  │                                                                     │   │
│  │ 步骤3: 解析 ACTION 块内容                                           │   │
│  │        → 提取 tool: 正则 tool:\s*(\w+)                              │   │
│  │        → 结果: "run_command"                                        │   │
│  │        → 映射: DecomposeTool.RUN_COMMAND                            │   │
│  │                                                                     │   │
│  │ 步骤4: 提取参数                                                     │   │
│  │        → 正则: (\w+):\s*(.+?)(?=\n\w+:|$)                           │   │
│  │        → 结果: {command: "ls -la /Users/demo/project"}              │   │
│  │                                                                     │   │
│  │ 输出: AgentAction 对象                                              │   │
│  │   - tool: DecomposeTool.RUN_COMMAND                                 │   │
│  │   - thought: "需要查看项目的目录结构..."                             │   │
│  │   - parameters: {command: "ls -la ..."}                             │   │
│  └────────┬────────────────────────────────────────────────────────────┘   │
│           ▼                                                                 │
│  ┌─────────────────┐                                                        │
│  │ 4. 执行验证阶段 │                                                        │
│  └────────┬────────┘                                                        │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ ToolExecutor.execute()  ◄── 新增组件                                │   │
│  │                                                                     │   │
│  │ 步骤1: 参数验证                                                     │   │
│  │        → 检查必要参数是否存在                                       │   │
│  │        → 检查路径是否在 projectPath 范围内                           │   │
│  │        → 检查命令是否在白名单内                                     │   │
│  │                                                                     │   │
│  │ 步骤2: 获取工具实例                                                 │   │
│  │        → ToolRegistry.getTool("run_command")                        │   │
│  │        → 返回: BashTool 实例                                        │   │
│  │                                                                     │   │
│  │ 步骤3: 执行工具                                                     │   │
│  │        → BashTool.execute(params, toolContext)                      │   │
│  │        → 实际执行: ProcessBuilder + 超时控制                        │   │
│  │                                                                     │   │
│  │ 步骤4: 收集结果                                                     │   │
│  │        → ToolResult {success, output, error}                        │   │
│  └────────┬────────────────────────────────────────────────────────────┘   │
│           ▼                                                                 │
│  ┌─────────────────┐                                                        │
│  │ 5. 结果反馈阶段 │                                                        │
│  └────────┬────────┘                                                        │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ 上下文更新 + 下一轮准备                                              │   │
│  │                                                                     │   │
│  │ 步骤1: 更新 DecomposeContext                                        │   │
│  │        → outputBuffer.append(执行结果)                              │   │
│  │        → toolCalls.add(工具调用记录)                                │   │
│  │        → contextCache.put(关键信息)                                 │   │
│  │                                                                     │   │
│  │ 步骤2: 记录决策历史                                                 │   │
│  │        → decisionHistory.add(DecisionRecord)                        │   │
│  │                                                                     │   │
│  │ 步骤3: 判断是否继续                                                 │   │
│  │        → 检查 iterationCount < maxIterations                        │   │
│  │        → 检查 status == RUNNING                                     │   │
│  │                                                                     │   │
│  │ 步骤4: 构建下一轮提示词                                             │   │
│  │        → 包含上一轮执行结果作为观察                                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 决策者是谁？

```
┌─────────────────────────────────────────────────────────────────┐
│                        决策者分析                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  决策者: 远程LLM (通过 OpenClawGatewayClient 调用)              │
│                                                                 │
│  决策依据:                                                       │
│  1. 提示词中的任务描述 (taskDescription)                        │
│  2. 提示词中的可用工具列表                                       │
│  3. 提示词中的项目路径 (projectPath)                            │
│  4. 历史决策记录 (最近3轮)                                       │
│  5. 上一轮工具执行结果                                          │
│                                                                 │
│  决策输出格式:                                                   │
│  [THOUGHT]                                                      │
│  <分析过程>                                                     │
│  [/THOUGHT]                                                     │
│                                                                 │
│  [ACTION]                                                       │
│  tool: <工具名>                                                 │
│  <参数名>: <参数值>                                             │
│  [/ACTION]                                                      │
│                                                                 │
│  决策逻辑 (LLM内部):                                            │
│  - 理解当前任务目标                                              │
│  - 评估可用工具的能力                                           │
│  - 选择最合适的工具                                             │
│  - 构造工具所需参数                                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 如何识别决策？

```
┌─────────────────────────────────────────────────────────────────┐
│                      决策识别机制                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  识别层: AgentOutputParser                                      │
│                                                                 │
│  识别规则:                                                       │
│                                                                 │
│  规则1: 识别 THOUGHT 块                                         │
│  ────────────────────────────                                   │
│  正则表达式: \[THOUGHT\](.*?)\[/THOUGHT\]                       │
│  匹配模式: Pattern.DOTALL (跨行匹配)                            │
│  提取内容: 块内所有文本                                          │
│  存储位置: AgentAction.thought                                  │
│                                                                 │
│  规则2: 识别 ACTION 块                                          │
│  ────────────────────────────                                   │
│  正则表达式: \[ACTION\](.*?)\[/ACTION\]                         │
│  匹配模式: Pattern.DOTALL                                       │
│  提取内容: 块内所有文本                                          │
│  后续处理: 交给 ActionBlockParser 解析                          │
│                                                                 │
│  规则3: 识别工具名称                                             │
│  ────────────────────────────                                   │
│  正则表达式: tool:\s*(\w+)                                      │
│  匹配范围: ACTION 块内容                                         │
│  映射逻辑: DecomposeTool.fromName(toolName)                     │
│                                                                 │
│  规则4: 识别参数                                                 │
│  ────────────────────────────                                   │
│  正则表达式: (\w+):\s*(.+?)(?=\n\w+:|$)                         │
│  匹配范围: ACTION 块内容                                         │
│  排除字段: tool, subtasks, parameters                           │
│  存储格式: Map<String, Object>                                  │
│                                                                 │
│  兜底机制:                                                       │
│  ────────────────────────────                                   │
│  如果没有找到 ACTION 块:                                        │
│  1. 尝试匹配 [NODE_DECISION] 格式                               │
│  2. 调用 OutputInferrer.infer() 推断意图                        │
│  3. 返回默认 CONTINUE 动作                                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 三、工具协议详细设计

### 3.1 工具调用协议

#### 3.1.1 LLM输出格式

LLM 必须按以下格式输出工具调用：

```
[THOUGHT]
分析当前情况，说明选择该工具的原因。
可以是多行文本。
[/THOUGHT]

[ACTION]
tool: <工具名称>
<参数1>: <值1>
<参数2>: <值2>
...
[/ACTION]
```

#### 3.1.2 各工具的ACTION格式

**run_command (执行命令)**
```
[ACTION]
tool: run_command
command: ls -la /Users/demo/project
timeout: 30000
[/ACTION]
```

**read_file (读取文件)**
```
[ACTION]
tool: read_file
path: /Users/demo/project/src/main.java
limit: 100
[/ACTION]
```

**write_file (写入文件)**
```
[ACTION]
tool: write_file
path: /Users/demo/project/output.txt
content: |
  第一行内容
  第二行内容
[/ACTION]
```

**edit_file (编辑文件)**
```
[ACTION]
tool: edit_file
path: /Users/demo/project/config.json
old_string: "version": "1.0"
new_string: "version": "2.0"
[/ACTION]
```

**search_files (搜索文件)**
```
[ACTION]
tool: search_files
pattern: **/*.java
path: /Users/demo/project
[/ACTION]
```

**search_content (搜索内容)**
```
[ACTION]
tool: search_content
pattern: public class
file_pattern: *.java
[/ACTION]
```

**spawn_agent (委托子Agent)**
```
[ACTION]
tool: spawn_agent
agent_type: explore
prompt: 查找项目中所有的Controller类
[/ACTION]
```

**mark_complete (标记完成)**
```
[ACTION]
tool: mark_complete
summary: 任务已完成，生成了配置文件
result: {"files": ["config.json"]}
[/ACTION]
```

**mark_failed (标记失败)**
```
[ACTION]
tool: mark_failed
reason: 无法找到指定的配置文件
[/ACTION]
```

### 3.2 解析器代码设计

```java
/**
 * ACTION块解析器 - 核心解析逻辑
 */
public class ActionBlockParser {

    // 工具名称正则
    private static final Pattern TOOL_PATTERN = Pattern.compile("tool:\\s*(\\w+)");

    // 参数正则（匹配 key: value 格式）
    private static final Pattern PARAM_PATTERN = Pattern.compile(
        "(\\w+):\\s*(.+?)(?=\\n\\w+:|$)",
        Pattern.MULTILINE
    );

    // 多行内容正则（匹配 | 后的多行文本）
    private static final Pattern MULTILINE_PATTERN = Pattern.compile(
        "(\\w+):\\s*\\|\\s*\\n((?:\\s{2,}.+\\n?)+)"
    );

    /**
     * 解析ACTION块
     * @param action 输出对象
     * @param block ACTION块内容（不含[ACTION]标签）
     */
    public void parse(AgentAction action, String block) {
        Map<String, Object> parameters = new HashMap<>();

        // 1. 提取工具名称
        Matcher toolMatcher = TOOL_PATTERN.matcher(block);
        if (toolMatcher.find()) {
            String toolName = toolMatcher.group(1);
            DecomposeTool tool = DecomposeTool.fromName(toolName);
            action.setTool(tool != null ? tool : DecomposeTool.CONTINUE);
        }

        // 2. 提取简单参数
        Matcher paramMatcher = PARAM_PATTERN.matcher(block);
        while (paramMatcher.find()) {
            String key = paramMatcher.group(1);
            String value = paramMatcher.group(2).trim();

            // 跳过工具名
            if (!"tool".equals(key)) {
                // 去除引号
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                parameters.put(key, value);
            }
        }

        // 3. 提取多行参数（如content: |）
        Matcher multilineMatcher = MULTILINE_PATTERN.matcher(block);
        while (multilineMatcher.find()) {
            String key = multilineMatcher.group(1);
            String value = multilineMatcher.group(2);
            // 去除行首缩进
            value = value.lines()
                .map(line -> line.replaceFirst("^\\s{2,}", ""))
                .collect(Collectors.joining("\n"));
            parameters.put(key, value);
        }

        action.setParameters(parameters);
    }
}
```

### 3.3 工具注册表设计

```java
/**
 * 工具注册表
 */
public class ToolRegistry {

    private final Map<String, Tool> tools = new HashMap<>();

    public ToolRegistry(String projectPath) {
        // 注册所有工具
        register(new ReadFileTool(projectPath));
        register(new WriteFileTool(projectPath));
        register(new EditFileTool(projectPath));
        register(new BashTool(projectPath));
        register(new GlobTool(projectPath));
        register(new GrepTool(projectPath));
        register(new AgentTool());  // 子Agent委托
    }

    private void register(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    public Tool getTool(String name) {
        return tools.get(name);
    }

    /**
     * 获取所有工具描述（注入到Prompt）
     */
    public String getToolsDescription() {
        StringBuilder sb = new StringBuilder();
        for (Tool tool : tools.values()) {
            sb.append("### ").append(tool.getName()).append("\n");
            sb.append(tool.getDescription()).append("\n");
            sb.append("参数:\n");
            for (ToolParameter param : tool.getParameters()) {
                sb.append("- ").append(param.getName());
                if (param.isRequired()) {
                    sb.append(" (必需)");
                }
                sb.append(": ").append(param.getDescription()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
```

---

## 四、工具实现规范

### 4.1 工具接口定义

```java
/**
 * 工具接口
 */
public interface Tool {

    /**
     * 工具名称（与DecomposeTool枚举对应）
     */
    String getName();

    /**
     * 工具描述（注入到Prompt给LLM看）
     */
    String getDescription();

    /**
     * 参数定义
     */
    List<ToolParameter> getParameters();

    /**
     * 执行工具
     * @param parameters 从ACTION块解析的参数
     * @param context 工具执行上下文
     * @return 执行结果
     */
    ToolResult execute(Map<String, Object> parameters, ToolExecutionContext context);
}

/**
 * 工具参数定义
 */
public class ToolParameter {
    private String name;
    private String description;
    private boolean required;
    private String defaultValue;
    private Class<?> type;

    // builder pattern...
}

/**
 * 工具执行结果
 */
public class ToolResult {
    private boolean success;
    private String output;      // 返回给LLM的内容
    private String error;       // 错误信息
    private Object data;        // 结构化数据

    public static ToolResult success(String output) {
        ToolResult result = new ToolResult();
        result.success = true;
        result.output = output;
        return result;
    }

    public static ToolResult error(String error) {
        ToolResult result = new ToolResult();
        result.success = false;
        result.error = error;
        return result;
    }
}

/**
 * 工具执行上下文
 */
public class ToolExecutionContext {
    private String projectPath;      // 项目根目录（安全边界）
    private String workingDir;       // 当前工作目录
    private int timeoutMs;           // 默认超时
    private Map<String, Object> variables;  // 工作流变量
    private ArtifactCollector artifacts;    // 产物收集器
}
```

### 4.2 各工具实现要点

| 工具 | 实现类 | 核心实现 | 安全控制 |
|-----|-------|---------|---------|
| read_file | ReadFileTool | `Files.readString()` + 分页 | 路径必须在 projectPath 内 |
| write_file | WriteFileTool | `Files.writeString()` + 自动创建目录 | 路径必须在 projectPath 内 |
| edit_file | EditFileTool | 读取→定位→替换→写回 | 路径必须在 projectPath 内 |
| run_command | BashTool | `ProcessBuilder` + 超时线程 | 命令白名单检查 |
| search_files | GlobTool | `Files.walk()` + glob匹配 | 搜索范围限制在 projectPath |
| search_content | GrepTool | `Files.lines()` + 正则匹配 | 文件大小限制 |
| spawn_agent | AgentTool | 调用 SubAgentExecutor | 递归深度限制 |

### 4.3 BashTool 实现示例

```java
public class BashTool implements Tool {

    private final String projectPath;
    private final Set<String> allowedCommands = Set.of(
        "ls", "cat", "find", "grep", "git", "npm", "mvn", "gradle",
        "java", "python", "node", "pytest", "jest"
    );

    private final Set<String> forbiddenPatterns = Set.of(
        "rm -rf", "sudo", "chmod 777", "> /dev/", "mkfs"
    );

    public BashTool(String projectPath) {
        this.projectPath = projectPath;
    }

    @Override
    public String getName() { return "run_command"; }

    @Override
    public String getDescription() {
        return "执行shell命令。命令在工作目录中运行，有超时限制。";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return List.of(
            ToolParameter.required("command", "要执行的命令", String.class),
            ToolParameter.optional("timeout", "超时时间(毫秒)", 60000, Integer.class)
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolExecutionContext ctx) {
        String command = (String) params.get("command");
        int timeout = params.containsKey("timeout")
            ? ((Number) params.get("timeout")).intValue()
            : 60000;

        // 1. 安全检查
        if (!isCommandAllowed(command)) {
            return ToolResult.error("命令不允许执行: " + command);
        }

        // 2. 执行命令
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.directory(new File(ctx.getWorkingDir()));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 等待完成（带超时）
            boolean finished = process.waitFor(timeout, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ToolResult.error("命令执行超时");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return ToolResult.error("命令返回非零: " + exitCode + "\n" + output);
            }

            return ToolResult.success(output.toString());

        } catch (Exception e) {
            return ToolResult.error("执行失败: " + e.getMessage());
        }
    }

    private boolean isCommandAllowed(String command) {
        String lower = command.toLowerCase();

        // 检查禁止模式
        for (String pattern : forbiddenPatterns) {
            if (lower.contains(pattern)) {
                return false;
            }
        }

        // 检查命令是否在白名单
        String cmd = command.split("\\s+")[0];
        return allowedCommands.contains(cmd);
    }
}
```

---

## 五、Prompt注入设计

### 5.1 修改 DecomposePromptBuilder

```java
public class DecomposePromptBuilder {

    private final ToolRegistry toolRegistry;

    public DecomposePromptBuilder(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public static String buildPrompt(DecomposeContext ctx, TaskState task) {
        StringBuilder prompt = new StringBuilder();

        appendSystemStatus(prompt, ctx, task);
        appendProjectInfo(prompt, ctx);      // 新增
        appendCurrentTask(prompt, task);
        appendAvailableTools(prompt);        // 改用 toolRegistry
        appendDecisionFramework(prompt);
        appendRecentHistory(prompt, ctx);
        appendObservation(prompt, ctx);      // 新增：上一轮执行结果

        return prompt.toString();
    }

    /**
     * 新增：注入项目信息
     */
    private static void appendProjectInfo(StringBuilder prompt, DecomposeContext ctx) {
        prompt.append("# 项目信息\n\n");
        prompt.append("项目路径: ").append(ctx.getProjectPath()).append("\n");
        prompt.append("工作目录: ").append(ctx.getProjectPath()).append("\n\n");
    }

    /**
     * 修改：从ToolRegistry获取工具描述
     */
    private void appendAvailableTools(StringBuilder prompt) {
        prompt.append("# 可用工具\n\n");
        prompt.append(toolRegistry.getToolsDescription());
    }

    /**
     * 新增：注入上一轮观察结果
     */
    private static void appendObservation(StringBuilder prompt, DecomposeContext ctx) {
        List<ToolCallRecord> calls = ctx.getToolCalls();
        if (!calls.isEmpty()) {
            ToolCallRecord lastCall = calls.get(calls.size() - 1);
            prompt.append("# 上一步执行结果\n\n");
            prompt.append("工具: ").append(lastCall.getToolName()).append("\n");
            prompt.append("状态: ").append(lastCall.isSuccess() ? "成功" : "失败").append("\n");
            prompt.append("输出:\n```\n").append(lastCall.getResult()).append("\n```\n\n");
        }
    }
}
```

### 5.2 SystemPrompt 设计

```java
public static String buildSystemPrompt() {
    return """
        你是一个智能任务执行引擎，负责执行复杂任务。

        ## 你的能力

        1. 你可以执行shell命令（run_command）
        2. 你可以读取文件（read_file）
        3. 你可以写入文件（write_file）
        4. 你可以编辑文件（edit_file）
        5. 你可以搜索文件（search_files）
        6. 你可以搜索内容（search_content）
        7. 你可以委托子任务给专门的Agent（spawn_agent）

        ## 工作方式

        你通过 ReAct 循环工作：
        1. 思考(THOUGHT): 分析当前状态，决定下一步
        2. 行动(ACTION): 选择工具，提供参数
        3. 观察: 系统执行工具，返回结果
        4. 循环: 基于观察继续思考

        ## 输出格式

        每次回复必须包含：

        [THOUGHT]
        你的分析过程...
        [/THOUGHT]

        [ACTION]
        tool: <工具名>
        参数...
        [/ACTION]

        ## 注意事项

        1. 所有文件路径必须基于项目路径
        2. 完成任务后使用 mark_complete
        3. 无法继续时使用 mark_failed
        4. 一次只调用一个工具
        """;
}
```

---

## 六、完整调用序列图

```
┌─────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Handler │    │ PromptBuilder│    │ LLM Gateway  │    │ OutputParser │    │ ToolExecutor │
└────┬────┘    └──────┬───────┘    └──────┬───────┘    └──────┬───────┘    └──────┬───────┘
     │                │                   │                   │                   │
     │  execute(ctx)  │                   │                   │                   │
     │───────────────>│                   │                   │                   │
     │                │                   │                   │                   │
     │                │ buildPrompt()     │                   │                   │
     │                │──────────────────>│                   │                   │
     │                │                   │                   │                   │
     │                │                   │ executeAgent()    │                   │
     │                │                   │──────────────────>│                   │
     │                │                   │                   │                   │
     │                │                   │                   │ LLM Response      │
     │                │                   │<──────────────────│                   │
     │                │                   │                   │                   │
     │                │                   │ parse(response)   │                   │
     │                │                   │──────────────────>│                   │
     │                │                   │                   │                   │
     │                │                   │                   │ AgentAction       │
     │                │                   │<──────────────────│                   │
     │                │                   │                   │                   │
     │                │                   │                   │                   │
     │<───────────────│───────────────────│───────────────────│                   │
     │                │                   │                   │                   │
     │                │                   │                   │                   │
     │ execute(action)│                   │                   │                   │
     │───────────────────────────────────────────────────────────────────────────>│
     │                │                   │                   │                   │
     │                │                   │                   │                   │
     │                │                   │                   │                   │
     │                │                   │                   │  getTool(name)    │
     │                │                   │                   │<──────────────────│
     │                │                   │                   │                   │
     │                │                   │                   │  Tool instance    │
     │                │                   │                   │──────────────────>│
     │                │                   │                   │                   │
     │                │                   │                   │                   │
     │                │                   │                   │  execute(params)  │
     │                │                   │                   │──────────────────>│
     │                │                   │                   │                   │
     │                │                   │                   │  ToolResult       │
     │                │                   │                   │<──────────────────│
     │                │                   │                   │                   │
     │ ToolResult     │                   │                   │                   │
     │<───────────────────────────────────────────────────────────────────────────│
     │                │                   │                   │                   │
     │ updateContext  │                   │                   │                   │
     │───────────────>│                   │                   │                   │
     │                │                   │                   │                   │
```

---

## 七、文件清单

### 7.1 新建文件

| 文件路径 | 行数估算 | 职责 |
|---------|---------|------|
| `smartdecompose/tool/Tool.java` | ~30 | 工具接口 |
| `smartdecompose/tool/ToolResult.java` | ~40 | 工具结果 |
| `smartdecompose/tool/ToolParameter.java` | ~30 | 参数定义 |
| `smartdecompose/tool/ToolRegistry.java` | ~50 | 工具注册 |
| `smartdecompose/tool/ToolExecutor.java` | ~80 | 工具执行器 |
| `smartdecompose/tool/ToolExecutionContext.java` | ~40 | 执行上下文 |
| `smartdecompose/tool/impl/ReadFileTool.java` | ~80 | 文件读取 |
| `smartdecompose/tool/impl/WriteFileTool.java` | ~70 | 文件写入 |
| `smartdecompose/tool/impl/EditFileTool.java` | ~90 | 文件编辑 |
| `smartdecompose/tool/impl/BashTool.java` | ~120 | 命令执行 |
| `smartdecompose/tool/impl/GlobTool.java` | ~60 | 文件搜索 |
| `smartdecompose/tool/impl/GrepTool.java` | ~80 | 内容搜索 |
| `smartdecompose/tool/impl/AgentTool.java` | ~50 | 子Agent委托 |

### 7.2 需修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `SmartDecomposeHandler.java` | 引入 ToolExecutor 替代 ActionExecutor |
| `DecomposePromptBuilder.java` | 注入 projectPath、工具描述、观察结果 |
| `DecomposeTool.java` | 新增工具枚举值 |
| `DecomposeContext.java` | 添加 workingDir 字段 |

---

## 八、实施步骤

| 阶段 | 任务 | 依赖 |
|-----|------|-----|
| Phase 1 | 创建 Tool 接口体系 | 无 |
| Phase 2 | 实现 ReadFileTool, WriteFileTool | Phase 1 |
| Phase 3 | 实现 EditFileTool, BashTool | Phase 1 |
| Phase 4 | 实现 GlobTool, GrepTool | Phase 1 |
| Phase 5 | 实现 AgentTool | Phase 1 |
| Phase 6 | 创建 ToolRegistry, ToolExecutor | Phase 1-5 |
| Phase 7 | 修改 DecomposePromptBuilder | Phase 6 |
| Phase 8 | 修改 SmartDecomposeHandler | Phase 6-7 |
| Phase 9 | 集成测试 | Phase 8 |