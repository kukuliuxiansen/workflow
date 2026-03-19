# 智能分解节点设计方案

## 文档信息

| 项目名称 | Workflow Engine - 智能分解节点设计 |
|---------|------------------------------------|
| 版本 | 1.0.0 |
| 编写日期 | 2026-03-20 |
| 状态 | 设计方案 |

---

## 一、概述

### 1.1 设计背景

传统工作流引擎采用预定义的节点和连线，适合固定流程的场景。但对于开放式任务（如"实现一个登录功能"），无法预先定义所有步骤。

Claude Code 采用 ReAct 循环（Reasoning + Acting）模式，通过 Agent 实时决策、动态执行，实现了强大的任务处理能力。

本方案借鉴 Claude Code 的设计思想，在工作流引擎中引入 **智能分解节点**，实现任务的动态分解和执行。

### 1.2 核心理念

**传统工作流 vs 智能分解**

```
传统工作流（预定义）：
┌─────┐   ┌─────┐   ┌─────┐   ┌─────┐
│开始 │→→→│节点A│→→→│节点B│→→→│结束 │
└─────┘   └─────┘   └─────┘   └─────┘
          固定路径，人工设计

智能分解（动态生成）：
┌─────┐   ┌──────────────────────────────┐   ┌─────┐
│开始 │→→→│        智能分解节点            │→→→│结束 │
└─────┘   │                              │   └─────┘
          │  Agent实时决定：              │
          │  - 是否需要分解？             │
          │  - 分解成什么？               │
          │  - 先做哪个？                 │
          │  - 如何执行？                 │
          └──────────────────────────────┘
               动态路径，Agent决策
```

### 1.3 设计目标

1. **动态分解** - Agent 根据任务复杂度自主决定是否分解
2. **递归处理** - 子任务可以继续分解，直到原子任务
3. **子Agent协作** - 可委托专门的子Agent处理特定任务
4. **上下文隔离** - 子Agent独立上下文，防止信息爆炸
5. **可控执行** - 设置迭代上限和深度上限，防止失控

---

## 二、Claude Code 原理分析

### 2.1 核心架构

Claude Code 本质是一个 **ReAct 循环**（Reasoning + Acting）：

```
┌─────────────────────────────────────────────────────────────┐
│                    Claude Code 主循环                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│    ┌──────────┐     ┌──────────┐     ┌──────────┐          │
│    │  思考    │ --> │   行动   │ --> │  观察    │          │
│    │(Reasoning)│     │(Acting)  │     │(Observe) │          │
│    └──────────┘     └──────────┘     └──────────┘          │
│         ↑                                    │              │
│         └────────────────────────────────────┘              │
│                    循环直到完成                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Tool Use 机制

Claude Code 通过工具调用来执行具体操作：

```
用户输入: "帮我实现一个登录功能"
                │
                ▼
┌─────────────────────────────────────────────────────────────┐
│                      Claude 模型                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  System Prompt:                                             │
│  "你是一个编程助手，可以使用以下工具：                         │
│   - Read: 读取文件                                           │
│   - Edit: 编辑文件                                           │
│   - Bash: 执行命令                                           │
│   - Glob: 搜索文件                                           │
│   - Agent: 启动子Agent处理复杂任务                            │
│   ..."                                                       │
│                                                             │
│  上下文: 当前目录结构、git状态、已读取的文件内容...            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                │
                ▼
        生成 Tool Call
```

### 2.3 任务拆解核心：Agent Tool

Claude Code 的关键设计是 **Agent Tool**，可以启动子Agent：

```json
{
  "name": "Agent",
  "description": "启动一个子Agent来处理复杂任务",
  "parameters": {
    "prompt": "子任务描述",
    "subagent_type": "general-purpose | Explore | Plan | ..."
  }
}
```

**执行示例**：

```
主Agent收到: "实现登录功能"

主Agent思考:
"这个任务比较大，我需要：
 1. 先探索代码库结构
 2. 规划实现方案
 3. 编写代码
 4. 测试验证"

主Agent调用工具:
┌────────────────────────────────────────────┐
│ Agent(subagent_type="Explore",             │
│       prompt="探索项目结构，找到认证相关代码") │
└────────────────────────────────────────────┘
         │
         ▼
┌────────────────────────────────────────────┐
│           子Agent (Explore类型)             │
│   执行探索，返回项目结构信息                  │
└────────────────────────────────────────────┘
         │
         ▼
主Agent继续执行...
```

### 2.4 子Agent类型

| 类型 | 职责 | 特点 |
|------|------|------|
| `general-purpose` | 通用任务 | 可使用所有工具 |
| `Explore` | 代码探索 | 快速搜索，只读操作 |
| `Plan` | 规划方案 | 分析架构，输出计划 |
| `claude-code-guide` | 使用帮助 | 回答使用问题 |

### 2.5 上下文管理

```
Turn 1: 用户输入 + System Prompt
        ↓
Turn 2: + Tool Result (探索结果)
        ↓
Turn 3: + Tool Result (规划结果) + 已读取的文件内容
        ↓
Turn 4: + Tool Result (编辑结果) + 更多文件内容
        ↓
        ... 上下文越来越大
        ↓
        触发压缩机制
```

**关键设计**：
- 每个子Agent有**独立的上下文**
- 子Agent完成后，只返回**摘要结果**给主Agent
- 避免主上下文爆炸

### 2.6 核心原理总结

```
┌─────────────────────────────────────────────────────────────┐
│                  Claude Code 任务拆解原理                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 单一主循环                                               │
│     主 Agent 持续运行，不断思考-行动-观察                     │
│                                                             │
│  2. Agent Tool 委托                                          │
│     遇到复杂子任务时，启动子 Agent 处理                       │
│                                                             │
│  3. 上下文隔离                                               │
│     子 Agent 独立上下文，完成后只返回摘要                      │
│                                                             │
│  4. 流式决策                                                 │
│     不预设工作流，每一步根据上下文动态决定                     │
│                                                             │
│  5. 工具驱动                                                 │
│     所有能力通过工具暴露，Agent 自主选择使用                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、整体架构设计

### 3.1 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                      智能分解节点架构                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    ReAct 执行循环                        │   │
│  │  ┌──────┐    ┌──────┐    ┌──────┐    ┌──────┐          │   │
│  │  │ 思考 │ ─→ │ 行动 │ ─→ │ 观察 │ ─→ │ 判断 │          │   │
│  │  └──────┘    └──────┘    └──────┘    └──────┘          │   │
│  │       ↑                                   │              │   │
│  │       └───────────────────────────────────┘              │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                      工具集                              │   │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐           │   │
│  │  │子Agent │ │文件操作│ │命令执行│ │环境感知│           │   │
│  │  └────────┘ └────────┘ └────────┘ └────────┘           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    状态管理                              │   │
│  │  - 任务栈（待处理任务）                                   │   │
│  │  - 完成记录（已完成任务）                                 │   │
│  │  - 上下文缓存（关键信息）                                 │   │
│  │  - 执行历史（决策轨迹）                                   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 执行模型示例

```
输入任务: "实现用户登录功能"
                │
                ▼
┌─────────────────────────────────────────────────────────────┐
│                    第1轮循环                                 │
├─────────────────────────────────────────────────────────────┤
│ 思考: 任务较大，需要分解                                      │
│ 行动: decompose("实现登录功能")                               │
│ 观察: 得到子任务列表                                          │
│       - [1] 设计登录页面UI                                    │
│       - [2] 实现后端认证API                                   │
│       - [3] 设计用户数据表                                    │
│ 判断: 继续处理子任务                                          │
└─────────────────────────────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────────────────┐
│                    第2轮循环                                 │
├─────────────────────────────────────────────────────────────┤
│ 思考: 选择子任务[1]，评估复杂度                                │
│ 行动: assess(子任务[1])                                       │
│ 观察: 复杂度中等，需要先了解现有组件                           │
│ 判断: 启动探索子Agent                                         │
└─────────────────────────────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────────────────┐
│                 启动子Agent (Explore)                        │
├─────────────────────────────────────────────────────────────┤
│ 任务: 探索现有UI组件和样式规范                                 │
│ 结果: 返回组件库信息、样式规范摘要                             │
└─────────────────────────────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────────────────┐
│                    第3轮循环                                 │
├─────────────────────────────────────────────────────────────┤
│ 思考: 有了组件信息，开始实现登录页面                           │
│ 行动: write_file("login.html", content)                      │
│ 观察: 文件创建成功                                            │
│ 判断: 子任务[1]完成，继续下一个                                │
└─────────────────────────────────────────────────────────────┘
                │
                ▼
              ... 循环继续 ...
                │
                ▼
┌─────────────────────────────────────────────────────────────┐
│                    第N轮循环                                 │
├─────────────────────────────────────────────────────────────┤
│ 思考: 所有子任务已完成                                        │
│ 行动: mark_complete(result)                                  │
│ 观察: 任务标记为完成                                          │
│ 判断: 退出循环                                                │
└─────────────────────────────────────────────────────────────┘
                │
                ▼
              返回最终结果
```

---

## 四、核心组件设计

### 4.1 任务状态模型

```java
/**
 * 任务状态
 */
public class TaskState {
    private String taskId;              // 任务ID
    private String description;         // 任务描述
    private TaskStatus status;          // PENDING/RUNNING/COMPLETED/FAILED
    private int depth;                  // 递归深度
    private int priority;               // 优先级
    private List<TaskState> subtasks;   // 子任务列表
    private TaskState parent;           // 父任务引用
    private Object result;              // 执行结果
    private List<String> dependencies;  // 依赖任务ID
    private Date createTime;            // 创建时间
    private Date completeTime;          // 完成时间
}

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    PENDING,        // 待处理
    RUNNING,        // 执行中
    BLOCKED,        // 被阻塞（等待依赖）
    COMPLETED,      // 已完成
    FAILED,         // 失败
    DECOMPOSED      // 已分解为子任务
}
```

### 4.2 执行上下文

```java
/**
 * 智能分解执行上下文
 */
public class DecomposeContext {

    // ============ 任务管理 ============
    private Stack<TaskState> taskStack;         // 任务栈（待处理）
    private List<TaskState> completedTasks;     // 已完成任务列表
    private TaskState currentTask;              // 当前正在执行的任务
    private Map<String, TaskState> taskMap;     // 任务ID -> 任务映射

    // ============ 循环控制 ============
    private int iterationCount;                 // 当前迭代次数
    private int maxIterations;                  // 最大迭代次数（默认100）
    private int maxDepth;                        // 最大递归深度（默认5）

    // ============ 状态追踪 ============
    private List<DecisionRecord> decisionHistory; // 决策历史
    private Map<String, Object> contextCache;   // 上下文缓存（存储关键信息）
    private List<ToolCallRecord> toolCalls;     // 工具调用记录

    // ============ 输出收集 ============
    private StringBuilder outputBuffer;         // 输出缓冲
    private List<Artifact> artifacts;           // 生成的产物（文件等）

    // ============ 执行状态 ============
    private DecomposeStatus status;             // RUNNING/COMPLETED/FAILED/TIMEOUT
    private String errorMessage;                // 错误信息
}

/**
 * 决策记录
 */
public class DecisionRecord {
    private int iteration;              // 迭代序号
    private String taskId;              // 当前任务ID
    private String thought;             // 思考内容
    private String action;              // 执行的动作
    private ActionResult result;        // 执行结果
    private long timestamp;             // 时间戳
}

/**
 * 工具调用记录
 */
public class ToolCallRecord {
    private String toolName;            // 工具名称
    private Map<String, Object> parameters; // 参数
    private Object result;              // 返回结果
    private long durationMs;            // 耗时
    private boolean success;            // 是否成功
}

/**
 * 产物记录
 */
public class Artifact {
    private String type;                // file/command/output
    private String path;                // 文件路径（如果是文件）
    private String content;             // 内容摘要
    private Date createTime;            // 创建时间
}
```

### 4.3 工具定义

```java
/**
 * 智能分解节点可用工具
 */
public enum DecomposeTool {

    /**
     * 分解任务 - 将任务拆分为子任务
     */
    DECOMPOSE(
        "decompose",
        "将当前任务分解为多个子任务",
        Arrays.asList("subtasks"),
        "用于复杂任务，需要拆分为更小的可执行单元"
    ),

    /**
     * 执行原子任务 - 直接执行简单任务
     */
    EXECUTE(
        "execute",
        "直接执行原子任务（简单、单一的操作）",
        Arrays.asList("action_type", "parameters"),
        "用于简单任务，预估耗时<5分钟"
    ),

    /**
     * 启动子Agent - 委托给专门的子Agent
     */
    SPAWN_AGENT(
        "spawn_agent",
        "启动子Agent处理专门任务",
        Arrays.asList("agent_type", "prompt", "context"),
        "用于需要专门技能的任务，如代码探索、架构规划"
    ),

    /**
     * 标记完成 - 标记当前任务完成
     */
    MARK_COMPLETE(
        "mark_complete",
        "标记当前任务为完成状态",
        Arrays.asList("result", "summary"),
        "任务执行完成时调用"
    ),

    /**
     * 请求帮助 - 遇到问题时请求用户输入
     */
    ASK_USER(
        "ask_user",
        "请求用户提供更多信息或做出决策",
        Arrays.asList("question", "options"),
        "遇到不确定的情况时使用"
    ),

    /**
     * 读取上下文 - 获取项目环境信息
     */
    READ_CONTEXT(
        "read_context",
        "读取项目文件或环境信息",
        Arrays.asList("target", "filters"),
        "需要了解项目状态时使用"
    ),

    /**
     * 写入产物 - 创建或修改文件
     */
    WRITE_ARTIFACT(
        "write_artifact",
        "创建或修改项目文件",
        Arrays.asList("path", "content", "mode"),
        "输出代码、配置等产物时使用"
    ),

    /**
     * 执行命令 - 运行shell命令
     */
    RUN_COMMAND(
        "run_command",
        "执行shell命令",
        Arrays.asList("command", "timeout", "cwd"),
        "需要运行测试、安装依赖等时使用"
    ),

    /**
     * 标记失败 - 标记任务失败
     */
    MARK_FAILED(
        "mark_failed",
        "标记当前任务失败",
        Arrays.asList("reason", "error_details"),
        "无法继续执行时使用"
    );

    private final String name;
    private final String description;
    private final List<String> requiredParameters;
    private final String usageGuide;
}
```

### 4.4 动作解析模型

```java
/**
 * Agent动作（从Agent输出解析）
 */
public class AgentAction {
    private DecomposeTool tool;                 // 工具类型
    private Map<String, Object> parameters;     // 参数
    private String thought;                     // 思考过程
    private int confidence;                     // 置信度 0-100
}

/**
 * 动作执行结果
 */
public class ActionResult {
    private boolean success;            // 是否成功
    private String message;             // 结果消息
    private Object data;                // 返回数据
    private List<String> nextHints;     // 建议的下一步
    private boolean shouldContinue;     // 是否应继续循环
}
```

---

## 五、Agent提示设计

### 5.1 系统提示模板

```markdown
# 智能任务执行系统

你是一个智能任务执行引擎，负责完成用户交给你的任务。

## 当前状态

- 迭代次数: {{iteration}} / {{maxIterations}}
- 递归深度: {{depth}} / {{maxDepth}}
- 待处理任务数: {{pendingTaskCount}}
- 已完成任务数: {{completedTaskCount}}

## 当前任务

{{#if currentTask}}
任务ID: {{currentTask.id}}
描述: {{currentTask.description}}
深度: {{currentTask.depth}}
{{else}}
当前没有正在执行的任务。
{{/if}}

## 可用工具

{{#each tools}}
### {{@index + 1}}. `{{this.name}}`
{{this.description}}

参数:
{{#each this.requiredParameters}}
- `{{this}}`: 必填
{{/each}}

使用场景: {{this.usageGuide}}
{{/each}}

## 决策框架

每次行动前，按以下步骤思考：

### Step 1: 评估当前任务

检查任务复杂度：
- 任务是否涉及多个独立模块？
- 是否需要多种不同技能？
- 预估耗时是否超过10分钟？

### Step 2: 选择策略

根据评估结果选择：

| 情况 | 建议工具 |
|------|---------|
| 复杂任务，需要拆分 | `decompose` |
| 简单任务，可直接执行 | `execute` |
| 需要专门技能 | `spawn_agent` |
| 需要了解项目状态 | `read_context` |
| 遇到不确定情况 | `ask_user` |
| 任务完成 | `mark_complete` |
| 无法继续 | `mark_failed` |

### Step 3: 执行并观察

执行选择的工具，观察结果，决定下一步。

## 输出格式

每轮必须按以下格式输出：

```
[THOUGHT]
分析当前任务状态、评估复杂度、决定下一步行动。
[/THOUGHT]

[ACTION]
tool: <工具名称>
parameters:
  param1: value1
  param2: value2
[/ACTION]
```

## 注意事项

1. **避免无限循环** - 确保每一步都在推进任务进度
2. **合理分解** - 子任务应该是独立、可执行的单元
3. **善用子Agent** - 专业的事情交给专业的Agent
4. **及时汇报** - 完成后调用 `mark_complete` 结束任务
5. **求助用户** - 不确定时使用 `ask_user` 询问
```

### 5.2 任务分解提示

```markdown
## 任务分解指南

当使用 `decompose` 工具时，遵循以下原则：

### 分解原则

1. **MECE原则**
   - 子任务之间相互独立（Mutually Exclusive）
   - 子任务完全覆盖父任务（Collectively Exhaustive）

2. **合理粒度**
   - 每个子任务预估耗时 5-15 分钟
   - 单一职责，一个任务做一件事

3. **明确依赖**
   - 标注子任务之间的依赖关系
   - 有依赖的任务在被依赖任务完成后执行

4. **优先级**
   - 给出建议的执行顺序
   - 高优先级任务优先处理

### 输出格式

```
[ACTION]
tool: decompose
parameters:
  subtasks:
    - id: task_1
      description: "创建登录页面HTML结构"
      priority: 1
      dependencies: []
    - id: task_2
      description: "添加登录表单样式"
      priority: 2
      dependencies: [task_1]
    - id: task_3
      description: "实现表单验证逻辑"
      priority: 2
      dependencies: [task_1]
[/ACTION]
```

### 分解示例

输入任务：实现用户登录功能

```
[ACTION]
tool: decompose
parameters:
  subtasks:
    - id: ui_design
      description: "设计并实现登录页面UI"
      priority: 1
      dependencies: []
    - id: api_auth
      description: "实现后端认证API接口"
      priority: 1
      dependencies: []
    - id: db_schema
      description: "设计用户数据表结构"
      priority: 1
      dependencies: []
    - id: integration
      description: "前后端集成联调"
      priority: 2
      dependencies: [ui_design, api_auth, db_schema]
    - id: testing
      description: "编写测试用例并验证"
      priority: 3
      dependencies: [integration]
[/ACTION]
```
```

### 5.3 原子任务判断标准

```markdown
## 原子任务判断标准

以下情况任务为**原子任务**，应使用 `execute` 直接执行：

### 原子任务特征

1. **单一明确操作**
   - 创建单个文件
   - 编写单个函数
   - 修改特定配置项
   - 执行单个命令

2. **预估耗时 < 5分钟**
   - 简单配置修改
   - 小范围代码调整
   - 快速验证

3. **无子依赖**
   - 不需要先完成其他任务
   - 所有必要信息已具备

4. **单一技能领域**
   - 只涉及一种技术栈
   - 不需要多领域协作

### 非原子任务特征（需要分解）

1. **多模块** - 涉及多个独立模块
2. **多技能** - 涉及多个技术栈
3. **多阶段** - 有明确的阶段性里程碑
4. **预估耗时 > 15分钟**
5. **有协作需求** - 需要多人或多Agent协作
```

---

## 六、子Agent系统设计

### 6.1 子Agent类型定义

```java
/**
 * 子Agent类型定义
 */
public enum SubAgentType {

    /**
     * 探索Agent - 快速搜索和分析代码库
     */
    EXPLORE(
        "explore",
        "快速探索代码库，搜索文件和内容",
        Arrays.asList("Glob", "Grep", "Read"),
        Duration.ofMinutes(5),
        "用于了解项目结构、查找特定代码、分析依赖关系"
    ),

    /**
     * 规划Agent - 分析架构，制定计划
     */
    PLAN(
        "plan",
        "分析需求，制定实现计划和架构方案",
        Arrays.asList("Glob", "Grep", "Read", "WebSearch"),
        Duration.ofMinutes(10),
        "用于复杂任务的架构设计和实施规划"
    ),

    /**
     * 执行Agent - 执行具体编码任务
     */
    EXECUTE(
        "execute",
        "执行编码、配置、部署等具体任务",
        Arrays.asList("Read", "Edit", "Write", "Bash"),
        Duration.ofMinutes(30),
        "用于实现具体功能的代码编写"
    ),

    /**
     * 测试Agent - 运行测试并分析结果
     */
    TEST(
        "test",
        "运行测试，分析失败原因，尝试修复",
        Arrays.asList("Bash", "Read", "Edit"),
        Duration.ofMinutes(15),
        "用于测试执行、问题诊断和修复"
    ),

    /**
     * 审查Agent - 代码审查和质量检查
     */
    REVIEW(
        "review",
        "审查代码质量，检查安全性和最佳实践",
        Arrays.asList("Read", "Grep"),
        Duration.ofMinutes(10),
        "用于代码评审、安全检查、规范验证"
    ),

    /**
     * 文档Agent - 生成和维护文档
     */
    DOCUMENT(
        "document",
        "生成和维护项目文档",
        Arrays.asList("Read", "Write", "Glob"),
        Duration.ofMinutes(10),
        "用于编写README、API文档、注释等"
    );

    private final String type;
    private final String description;
    private final List<String> allowedTools;
    private final Duration timeout;
    private final String usageGuide;
}
```

### 6.2 子Agent执行流程

```
主Agent上下文
       │
       │ spawn_agent(explore, "找到认证相关代码")
       ▼
┌─────────────────────────────────────────────┐
│           子Agent (Explore)                  │
│                                             │
│  独立上下文:                                 │
│  - 只读权限                                  │
│  - 工具: Glob, Grep, Read                   │
│  - 超时: 5分钟                               │
│                                             │
│  执行过程:                                   │
│  1. Glob("**/*auth*")                       │
│  2. Grep("login", "password")               │
│  3. Read(src/auth/login.js)                 │
│  ...                                        │
│                                             │
│  输出:                                       │
│  {                                          │
│    "found_files": [...],                    │
│    "summary": "发现3个认证相关文件...",       │
│    "recommendations": [...]                 │
│  }                                          │
└─────────────────────────────────────────────┘
       │
       │ 返回摘要结果（不是全部内容）
       ▼
主Agent继续执行...
```

### 6.3 子Agent系统提示

```markdown
# 子Agent系统提示模板

你是一个专门的 {{agent_type}} Agent。

## 职责

{{description}}

## 可用工具

你只能使用以下工具：
{{#each allowed_tools}}
- {{this}}
{{/each}}

## 限制

- 超时时间: {{timeout_minutes}} 分钟
- 你必须在这个时间内完成任务并返回结果
- 只返回摘要信息，不要返回大量原始数据

## 输出格式

完成任务后，返回JSON格式的结果：

```json
{
  "status": "success | failed | timeout",
  "summary": "任务完成摘要，简明扼要",
  "outputs": {
    "key1": "value1",
    "key2": "value2"
  },
  "recommendations": [
    "建议1",
    "建议2"
  ]
}
```

## 当前任务

{{prompt}}
```

### 6.4 子Agent执行器

```java
/**
 * 子Agent执行器
 */
public class SubAgentExecutor {

    private final OpenClawClient openClawClient;
    private final int maxConcurrentAgents = 3;

    /**
     * 执行子Agent
     */
    public SubAgentResult execute(SubAgentRequest request) {
        SubAgentType agentType = request.getAgentType();

        // 1. 构建子Agent上下文
        SubAgentContext context = buildContext(request);

        // 2. 生成系统提示
        String systemPrompt = generateSystemPrompt(agentType);

        // 3. 执行Agent
        AgentExecutionResult result = openClawClient.execute(
            AgentRequest.builder()
                .systemPrompt(systemPrompt)
                .userPrompt(request.getPrompt())
                .context(context)
                .timeout(agentType.getTimeout())
                .build()
        );

        // 4. 解析结果
        return parseResult(result);
    }

    /**
     * 并行执行多个子Agent
     */
    public List<SubAgentResult> executeParallel(List<SubAgentRequest> requests) {
        // 限制并发数
        List<List<SubAgentRequest>> batches = partition(requests, maxConcurrentAgents);

        List<SubAgentResult> results = new ArrayList<>();
        for (List<SubAgentRequest> batch : batches) {
            List<CompletableFuture<SubAgentResult>> futures = batch.stream()
                .map(req -> CompletableFuture.supplyAsync(() -> execute(req)))
                .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            for (CompletableFuture<SubAgentResult> future : futures) {
                results.add(future.join());
            }
        }

        return results;
    }
}
```

---

## 七、执行流程详解

### 7.1 主执行循环

```java
/**
 * 智能分解节点处理器
 */
public class SmartDecomposeHandler extends AbstractNodeHandler {

    private final ActionExecutor actionExecutor;
    private final AgentOutputParser outputParser;

    @Override
    public NodeResult execute(NodeExecutionContext ctx) {
        // 1. 初始化上下文
        DecomposeContext context = initializeContext(ctx);

        // 2. 创建初始任务
        TaskState initialTask = createInitialTask(ctx);
        context.getTaskStack().push(initialTask);

        // 3. 主循环
        while (!isFinished(context)) {
            // 3.1 检查循环限制
            if (context.getIterationCount() >= context.getMaxIterations()) {
                context.setStatus(DecomposeStatus.TIMEOUT);
                break;
            }

            // 3.2 获取下一个任务
            TaskState currentTask = getNextTask(context);
            if (currentTask == null) {
                context.setStatus(DecomposeStatus.COMPLETED);
                break;
            }

            context.setCurrentTask(currentTask);
            currentTask.setStatus(TaskStatus.RUNNING);

            // 3.3 构建Agent提示
            String prompt = buildPrompt(context, currentTask);

            // 3.4 执行Agent
            AgentResult agentResult = executeAgent(ctx, prompt);

            // 3.5 解析Agent输出
            AgentAction action = outputParser.parse(agentResult.getOutput());

            // 3.6 执行动作
            ActionResult actionResult = actionExecutor.execute(context, action);

            // 3.7 更新状态
            updateContext(context, action, actionResult);

            // 3.8 记录决策
            recordDecision(context, currentTask, action, actionResult);

            context.incrementIteration();
        }

        // 4. 构建最终结果
        return buildFinalResult(context);
    }

    /**
     * 判断是否完成
     */
    private boolean isFinished(DecomposeContext context) {
        // 任务栈为空且无运行中任务
        return context.getTaskStack().isEmpty()
            && context.getCurrentTask() == null;
    }

    /**
     * 获取下一个任务（考虑优先级和依赖）
     */
    private TaskState getNextTask(DecomposeContext context) {
        Stack<TaskState> stack = context.getTaskStack();

        while (!stack.isEmpty()) {
            TaskState task = stack.pop();

            // 检查依赖是否满足
            if (hasUnsatisfiedDependencies(task, context)) {
                // 放回栈底，稍后处理
                pushToBottom(stack, task);
                continue;
            }

            return task;
        }

        return null;
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
            if (depTask == null || depTask.getStatus() != TaskStatus.COMPLETED) {
                return true;
            }
        }

        return false;
    }
}
```

### 7.2 动作执行器

```java
/**
 * 动作执行器
 */
public class ActionExecutor {

    private final SubAgentExecutor subAgentExecutor;
    private final FileSystemManager fileSystemManager;
    private final CommandExecutor commandExecutor;

    public ActionResult execute(DecomposeContext context, AgentAction action) {
        switch (action.getTool()) {
            case DECOMPOSE:
                return executeDecompose(context, action);
            case EXECUTE:
                return executeDirect(context, action);
            case SPAWN_AGENT:
                return executeSpawnAgent(context, action);
            case MARK_COMPLETE:
                return executeMarkComplete(context, action);
            case MARK_FAILED:
                return executeMarkFailed(context, action);
            case ASK_USER:
                return executeAskUser(context, action);
            case READ_CONTEXT:
                return executeReadContext(context, action);
            case WRITE_ARTIFACT:
                return executeWriteArtifact(context, action);
            case RUN_COMMAND:
                return executeRunCommand(context, action);
            default:
                return ActionResult.error("Unknown tool: " + action.getTool());
        }
    }

    /**
     * 执行分解动作
     */
    private ActionResult executeDecompose(DecomposeContext context, AgentAction action) {
        TaskState currentTask = context.getCurrentTask();
        List<Map<String, Object>> subtaskDefs = (List<Map<String, Object>>) action.getParameters().get("subtasks");

        // 检查深度限制
        if (currentTask.getDepth() >= context.getMaxDepth()) {
            return ActionResult.error("已达到最大递归深度，无法继续分解");
        }

        // 创建子任务状态
        List<TaskState> subtaskStates = new ArrayList<>();
        for (Map<String, Object> def : subtaskDefs) {
            TaskState subtask = TaskState.builder()
                .taskId(generateTaskId())
                .description((String) def.get("description"))
                .status(TaskStatus.PENDING)
                .depth(currentTask.getDepth() + 1)
                .priority((Integer) def.getOrDefault("priority", 5))
                .dependencies((List<String>) def.get("dependencies"))
                .parent(currentTask)
                .build();

            subtaskStates.add(subtask);
            context.getTaskMap().put(subtask.getTaskId(), subtask);
        }

        // 更新当前任务状态
        currentTask.setStatus(TaskStatus.DECOMPOSED);
        currentTask.setSubtasks(subtaskStates);

        // 按优先级将子任务压入栈
        subtaskStates.stream()
            .sorted(Comparator.comparingInt(TaskState::getPriority).reversed())
            .forEach(task -> context.getTaskStack().push(task));

        return ActionResult.success(
            "任务已分解为 " + subtaskStates.size() + " 个子任务",
            null,
            true
        );
    }

    /**
     * 执行启动子Agent动作
     */
    private ActionResult executeSpawnAgent(DecomposeContext context, AgentAction action) {
        Map<String, Object> params = action.getParameters();
        String agentType = (String) params.get("agent_type");
        String prompt = (String) params.get("prompt");
        Map<String, Object> subContext = (Map<String, Object>) params.get("context");

        // 创建子Agent请求
        SubAgentRequest request = SubAgentRequest.builder()
            .agentType(SubAgentType.valueOf(agentType.toUpperCase()))
            .prompt(prompt)
            .context(subContext)
            .parentExecutionId(context.getExecutionId())
            .build();

        // 执行子Agent
        SubAgentResult result = subAgentExecutor.execute(request);

        // 将结果合并到上下文缓存
        if (result.getOutputs() != null) {
            context.getContextCache().putAll(result.getOutputs());
        }

        // 记录工具调用
        context.getToolCalls().add(ToolCallRecord.builder()
            .toolName("spawn_agent:" + agentType)
            .parameters(params)
            .result(result)
            .success(result.isSuccess())
            .build());

        if (result.isSuccess()) {
            return ActionResult.success(result.getSummary(), result.getOutputs(), true);
        } else {
            return ActionResult.error("子Agent执行失败: " + result.getErrorMessage());
        }
    }

    /**
     * 执行标记完成动作
     */
    private ActionResult executeMarkComplete(DecomposeContext context, AgentAction action) {
        TaskState currentTask = context.getCurrentTask();
        Map<String, Object> params = action.getParameters();

        // 更新任务状态
        currentTask.setStatus(TaskStatus.COMPLETED);
        currentTask.setResult(params.get("result"));
        currentTask.setCompleteTime(new Date());

        // 添加到已完成列表
        context.getCompletedTasks().add(currentTask);

        // 清空当前任务
        context.setCurrentTask(null);

        // 如果有父任务，检查父任务是否可以继续
        if (currentTask.getParent() != null) {
            TaskState parent = currentTask.getParent();
            boolean allSubtasksComplete = parent.getSubtasks().stream()
                .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);

            if (allSubtasksComplete) {
                // 所有子任务完成，父任务也标记为完成
                parent.setStatus(TaskStatus.COMPLETED);
                context.getCompletedTasks().add(parent);
            }
        }

        return ActionResult.success(
            "任务完成: " + currentTask.getDescription(),
            currentTask.getResult(),
            true
        );
    }

    /**
     * 执行请求用户帮助动作
     */
    private ActionResult executeAskUser(DecomposeContext context, AgentAction action) {
        Map<String, Object> params = action.getParameters();
        String question = (String) params.get("question");
        List<String> options = (List<String>) params.get("options");

        // 暂停执行，等待用户输入
        context.setStatus(DecomposeStatus.WAITING_USER_INPUT);

        // 通过WebSocket或飞书通知用户
        notifyUser(context.getExecutionId(), question, options);

        // 返回等待状态
        return ActionResult.waiting(
            "等待用户输入: " + question,
            "user_input"
        );
    }
}
```

### 7.3 Agent输出解析器

```java
/**
 * Agent输出解析器
 */
public class AgentOutputParser {

    private static final Pattern THOUGHT_PATTERN = Pattern.compile(
        "\\[THOUGHT\\](.*?)\\[/THOUGHT\\]", Pattern.DOTALL
    );

    private static final Pattern ACTION_PATTERN = Pattern.compile(
        "\\[ACTION\\](.*?)\\[/ACTION\\]", Pattern.DOTALL
    );

    /**
     * 解析Agent输出
     */
    public AgentAction parse(String agentOutput) {
        AgentAction action = new AgentAction();

        // 1. 解析思考
        Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(agentOutput);
        if (thoughtMatcher.find()) {
            action.setThought(thoughtMatcher.group(1).trim());
        }

        // 2. 解析动作
        Matcher actionMatcher = ACTION_PATTERN.matcher(agentOutput);
        if (actionMatcher.find()) {
            String actionBlock = actionMatcher.group(1).trim();
            parseActionBlock(action, actionBlock);
        } else {
            // 没有找到动作块，尝试推断
            return inferFromOutput(agentOutput);
        }

        return action;
    }

    /**
     * 解析动作块
     */
    private void parseActionBlock(AgentAction action, String block) {
        // 解析工具名称
        Pattern toolPattern = Pattern.compile("tool:\\s*(\\w+)");
        Matcher toolMatcher = toolPattern.matcher(block);
        if (toolMatcher.find()) {
            String toolName = toolMatcher.group(1);
            action.setTool(DecomposeTool.valueOf(toolName.toUpperCase()));
        }

        // 解析参数
        Map<String, Object> parameters = new HashMap<>();

        // 简单参数（单行值）
        Pattern simpleParamPattern = Pattern.compile("(\\w+):\\s*(.+?)(?=\\n\\w+:|$)");
        Matcher simpleParamMatcher = simpleParamPattern.matcher(block);
        while (simpleParamMatcher.find()) {
            String key = simpleParamMatcher.group(1);
            String value = simpleParamMatcher.group(2).trim();
            if (!key.equals("tool") && !key.equals("parameters") && !key.equals("subtasks")) {
                parameters.put(key, parseValue(value));
            }
        }

        // 解析subtasks（YAML格式）
        if (block.contains("subtasks:")) {
            List<Map<String, Object>> subtasks = parseSubtasks(block);
            parameters.put("subtasks", subtasks);
        }

        action.setParameters(parameters);
    }

    /**
     * 解析子任务列表
     */
    private List<Map<String, Object>> parseSubtasks(String block) {
        List<Map<String, Object>> subtasks = new ArrayList<>();

        // 使用正则解析YAML风格的子任务定义
        Pattern taskPattern = Pattern.compile(
            "- id:\\s*(\\S+)\\s*" +
            "description:\\s*\"([^\"]+)\"\\s*" +
            "priority:\\s*(\\d+)\\s*" +
            "dependencies:\\s*\\[([^\\]]*)\\]",
            Pattern.DOTALL
        );

        Matcher matcher = taskPattern.matcher(block);
        while (matcher.find()) {
            Map<String, Object> task = new HashMap<>();
            task.put("id", matcher.group(1));
            task.put("description", matcher.group(2));
            task.put("priority", Integer.parseInt(matcher.group(3)));

            String deps = matcher.group(4).trim();
            if (!deps.isEmpty()) {
                task.put("dependencies", Arrays.asList(deps.split("\\s*,\\s*")));
            } else {
                task.put("dependencies", Collections.emptyList());
            }

            subtasks.add(task);
        }

        return subtasks;
    }

    /**
     * 从输出推断动作（当没有明确动作块时）
     */
    private AgentAction inferFromOutput(String output) {
        // 简单的推断逻辑
        if (output.contains("完成") || output.contains("成功")) {
            AgentAction action = new AgentAction();
            action.setTool(DecomposeTool.MARK_COMPLETE);
            action.setThought(output);
            action.setParameters(Map.of("result", output, "summary", "任务完成"));
            return action;
        }

        // 默认继续执行
        AgentAction action = new AgentAction();
        action.setTool(DecomposeTool.READ_CONTEXT);
        action.setThought("无法解析明确动作，尝试获取更多上下文");
        action.setParameters(Map.of("target", "current_state"));
        return action;
    }
}
```

---

## 八、数据库设计

### 8.1 智能分解执行状态表

```sql
CREATE TABLE smart_decompose_state (
    id TEXT PRIMARY KEY,
    execution_id TEXT NOT NULL,
    node_id TEXT NOT NULL,

    -- 任务栈（JSON序列化）
    task_stack TEXT,

    -- 已完成任务
    completed_tasks TEXT,

    -- 任务映射
    task_map TEXT,

    -- 循环状态
    current_iteration INTEGER DEFAULT 0,
    max_iterations INTEGER DEFAULT 100,
    current_depth INTEGER DEFAULT 0,
    max_depth INTEGER DEFAULT 5,

    -- 当前任务ID
    current_task_id TEXT,

    -- 决策历史
    decision_history TEXT,

    -- 上下文缓存
    context_cache TEXT,

    -- 工具调用记录
    tool_calls TEXT,

    -- 产物列表
    artifacts TEXT,

    -- 执行状态
    status TEXT DEFAULT 'RUNNING',
    error_message TEXT,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_decompose_execution ON smart_decompose_state(execution_id);
```

### 8.2 子Agent执行记录表

```sql
CREATE TABLE subagent_execution (
    id TEXT PRIMARY KEY,
    parent_execution_id TEXT NOT NULL,
    parent_node_id TEXT NOT NULL,

    agent_type TEXT NOT NULL,
    prompt TEXT,

    -- 执行状态
    status TEXT DEFAULT 'PENDING',

    -- 结果
    result_summary TEXT,
    result_outputs TEXT,
    error_message TEXT,

    -- 耗时
    start_time DATETIME,
    end_time DATETIME,
    duration_ms INTEGER,

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subagent_parent ON subagent_execution(parent_execution_id);
```

### 8.3 决策历史表

```sql
CREATE TABLE decision_history (
    id TEXT PRIMARY KEY,
    execution_id TEXT NOT NULL,
    node_id TEXT NOT NULL,

    iteration INTEGER NOT NULL,
    task_id TEXT,

    thought TEXT,
    action TEXT,
    action_parameters TEXT,

    result_status TEXT,
    result_message TEXT,

    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_decision_execution ON decision_history(execution_id);
```

---

## 九、API设计

### 9.1 智能分解节点API

```
POST /api/executions/{executionId}/smart-decompose/{nodeId}/start

启动智能分解节点执行

Request:
{
    "initialTask": "实现用户登录功能",
    "maxIterations": 100,
    "maxDepth": 5,
    "context": {
        "projectPath": "/path/to/project"
    }
}

Response:
{
    "success": true,
    "data": {
        "executionId": "exec_xxx",
        "nodeId": "node_decompose_1",
        "status": "RUNNING"
    }
}
```

### 9.2 获取执行状态API

```
GET /api/executions/{executionId}/smart-decompose/{nodeId}/status

获取智能分解执行状态

Response:
{
    "success": true,
    "data": {
        "status": "RUNNING",
        "iteration": 15,
        "maxIterations": 100,
        "currentDepth": 2,
        "maxDepth": 5,
        "currentTask": {
            "id": "task_8",
            "description": "实现登录表单验证",
            "depth": 2,
            "status": "RUNNING"
        },
        "pendingTasks": 3,
        "completedTasks": 5,
        "recentDecisions": [
            {
                "iteration": 14,
                "thought": "开始实现表单验证...",
                "action": "execute",
                "result": "success"
            }
        ]
    }
}
```

### 9.3 用户输入回调API

```
POST /api/executions/{executionId}/smart-decompose/{nodeId}/user-input

用户输入回调

Request:
{
    "input": "选择方案A，使用JWT认证",
    "option": "option_a"
}

Response:
{
    "success": true,
    "data": {
        "status": "RUNNING",
        "message": "用户输入已接收，继续执行"
    }
}
```

### 9.4 子Agent结果回调API

```
POST /api/executions/{executionId}/subagent/{subagentId}/result

子Agent执行结果回调

Request:
{
    "status": "success",
    "summary": "发现3个认证相关文件",
    "outputs": {
        "files": ["auth.js", "login.js", "user.js"],
        "recommendations": ["使用现有的auth模块"]
    }
}

Response:
{
    "success": true
}
```

---

## 十、异常处理

### 10.1 异常类型定义

```java
/**
 * 智能分解异常类型
 */
public enum DecomposeExceptionType {

    // 执行限制异常
    MAX_ITERATION_EXCEEDED("超过最大迭代次数"),
    MAX_DEPTH_EXCEEDED("超过最大递归深度"),

    // Agent异常
    AGENT_EXECUTION_FAILED("Agent执行失败"),
    AGENT_TIMEOUT("Agent执行超时"),
    AGENT_OUTPUT_PARSE_ERROR("Agent输出解析失败"),

    // 任务异常
    TASK_DEPENDENCY_CYCLE("任务依赖循环"),
    TASK_EXECUTION_FAILED("任务执行失败"),

    // 工具异常
    TOOL_NOT_FOUND("工具不存在"),
    TOOL_PARAMETER_MISSING("工具参数缺失"),
    TOOL_EXECUTION_ERROR("工具执行错误"),

    // 子Agent异常
    SUBAGENT_FAILED("子Agent执行失败"),
    SUBAGENT_TIMEOUT("子Agent执行超时"),

    // 用户交互异常
    USER_INPUT_TIMEOUT("等待用户输入超时"),
    USER_INPUT_INVALID("用户输入无效")
}
```

### 10.2 异常处理策略

```java
/**
 * 异常处理器
 */
public class DecomposeExceptionHandler {

    public ActionResult handle(Exception e, DecomposeContext context) {
        // 记录异常
        log.error("智能分解执行异常", e);

        // 根据异常类型处理
        if (e instanceof MaxIterationExceededException) {
            context.setStatus(DecomposeStatus.TIMEOUT);
            return ActionResult.error("执行超时：超过最大迭代次数 " + context.getMaxIterations());
        }

        if (e instanceof AgentExecutionException) {
            // Agent执行失败，尝试重试或使用备用策略
            return handleAgentFailure((AgentExecutionException) e, context);
        }

        if (e instanceof SubAgentException) {
            // 子Agent失败，记录并继续
            return handleSubAgentFailure((SubAgentException) e, context);
        }

        // 默认处理
        context.setStatus(DecomposeStatus.FAILED);
        context.setErrorMessage(e.getMessage());
        return ActionResult.error(e.getMessage());
    }

    private ActionResult handleAgentFailure(AgentExecutionException e, DecomposeContext context) {
        // 尝试使用默认动作
        AgentAction fallbackAction = getDefaultAction(context);
        return actionExecutor.execute(context, fallbackAction);
    }

    private ActionResult handleSubAgentFailure(SubAgentException e, DecomposeContext context) {
        // 记录失败，尝试继续执行
        context.getToolCalls().add(ToolCallRecord.builder()
            .toolName("subagent:" + e.getAgentType())
            .success(false)
            .result(e.getMessage())
            .build());

        // 返回错误但继续执行
        return ActionResult.error("子Agent失败: " + e.getMessage(), null, true);
    }
}
```

---

## 十一、与其他节点的关系

### 11.1 组合使用示例

```
简单场景：
[开始] → [智能分解] → [结束]

混合场景：
[开始] → [条件判断] ─┬→ [智能分解] → [结束]
                     └→ [固定流程] → [结束]

嵌套场景：
[开始] → [并行] ─┬→ [智能分解A] ─┬→ [合并] → [结束]
                  └→ [智能分解B] ─┘

条件+分解：
[开始] → [智能分解] → [条件判断] ─┬→ [智能分解细化] → [结束]
                                  └→ [直接执行] → [结束]
```

### 11.2 节点配置示例

```json
{
    "id": "node_smart_decompose_1",
    "type": "smart_decompose",
    "name": "智能任务分解",
    "config": {
        "maxIterations": 100,
        "maxDepth": 5,
        "initialTask": "{{input.task_description}}",
        "enabledTools": [
            "decompose",
            "execute",
            "spawn_agent",
            "mark_complete",
            "ask_user",
            "read_context",
            "write_artifact",
            "run_command"
        ],
        "enabledSubAgents": ["explore", "plan", "execute", "test", "review"],
        "context": {
            "projectPath": "{{input.project_path}}",
            "globalPrompt": "{{input.global_prompt}}"
        }
    },
    "on_complete": "node_end",
    "on_fail": "node_error_handler"
}
```

---

## 十二、实施计划

### 12.1 开发阶段

| 阶段 | 内容 | 预计工期 |
|------|------|---------|
| **Phase 1** | 核心执行循环 + 任务状态管理 | 3天 |
| **Phase 2** | Agent输出解析 + 动作执行器 | 2天 |
| **Phase 3** | 子Agent系统 | 3天 |
| **Phase 4** | 数据库持久化 + API接口 | 2天 |
| **Phase 5** | 异常处理 + 用户交互 | 2天 |
| **Phase 6** | 测试 + 优化 | 3天 |

### 12.2 依赖关系

```
Phase 1 (核心循环)
    │
    ├──→ Phase 2 (动作执行)
    │
    └──→ Phase 3 (子Agent)
              │
              └──→ Phase 4 (持久化)
                        │
                        └──→ Phase 5 (异常处理)
                                  │
                                  └──→ Phase 6 (测试)
```

### 12.3 验收标准

- [ ] 支持任务动态分解和递归执行
- [ ] 支持最多5层递归深度
- [ ] 支持最多100次迭代
- [ ] 支持至少5种子Agent类型
- [ ] 子Agent执行结果正确返回给主Agent
- [ ] 异常情况下能正确恢复或优雅终止
- [ ] 所有状态正确持久化到数据库

---

## 十三、附录

### 13.1 完整执行示例

**输入任务**: "实现用户登录功能"

**执行过程**:

```
=== 第1轮 ===
[THOUGHT]
当前任务：实现用户登录功能
这是一个复杂的任务，涉及前端、后端、数据库等多个方面。
需要分解为更小的子任务。
[/THOUGHT]

[ACTION]
tool: decompose
parameters:
  subtasks:
    - id: task_ui
      description: "设计并实现登录页面UI"
      priority: 1
      dependencies: []
    - id: task_api
      description: "实现后端认证API"
      priority: 1
      dependencies: []
    - id: task_db
      description: "设计用户数据表"
      priority: 1
      dependencies: []
    - id: task_integration
      description: "前后端集成联调"
      priority: 2
      dependencies: [task_ui, task_api, task_db]
[/ACTION]

结果: 成功分解为4个子任务

=== 第2轮 ===
[THOUGHT]
开始处理第一个子任务：设计并实现登录页面UI
需要先了解现有的UI组件库和样式规范
[/THOUGHT]

[ACTION]
tool: spawn_agent
parameters:
  agent_type: explore
  prompt: "探索项目中的UI组件库和样式规范，找到可用于登录页面的组件"
  context: {}
[/ACTION]

结果: 子Agent返回组件库信息

=== 第3轮 ===
[THOUGHT]
已经了解了组件库，现在可以创建登录页面
这是一个原子任务，直接执行
[/THOUGHT]

[ACTION]
tool: write_artifact
parameters:
  path: "src/pages/login.html"
  content: "<html>...</html>"
  mode: create
[/ACTION]

结果: 文件创建成功

=== 第4轮 ===
[THOUGHT]
登录页面UI已完成，标记任务完成
[/THOUGHT]

[ACTION]
tool: mark_complete
parameters:
  result: "登录页面已创建"
  summary: "创建了src/pages/login.html，包含登录表单"
[/ACTION]

结果: task_ui 标记为完成

... 继续处理其他任务 ...

=== 第N轮 ===
[THOUGHT]
所有子任务已完成，主任务完成
[/THOUGHT]

[ACTION]
tool: mark_complete
parameters:
  result: "用户登录功能实现完成"
  summary: "完成了UI页面、后端API、数据库设计和集成联调"
[/ACTION]

结果: 任务全部完成
```

### 13.2 术语表

| 术语 | 英文 | 解释 |
|------|------|------|
| ReAct | Reasoning + Acting | 思考-行动循环模式 |
| 智能分解 | Smart Decompose | 动态将复杂任务分解为子任务 |
| 子Agent | Sub-Agent | 专门处理特定类型任务的Agent |
| 原子任务 | Atomic Task | 不可再分的最小任务单元 |
| 任务栈 | Task Stack | 存储待处理任务的数据结构 |
| 上下文隔离 | Context Isolation | 子Agent独立上下文的设计 |
| 决策历史 | Decision History | 记录每一步决策的日志 |
| 产物 | Artifact | 执行过程中生成的文件或输出 |