# Agent决策节点实现方案

## 文档信息

| 项目名称 | Workflow Engine - Agent决策节点实现 |
|---------|-------------------------------------|
| 版本 | 1.0.0 |
| 编写日期 | 2026-03-20 |
| 状态 | 设计方案 |

---

## 一、概述

### 1.1 背景

在工作流引擎中，Agent只能返回文本内容。如何让Agent的输出能够驱动工作流的流转，是实现条件判断、并行执行、循环执行等控制节点的核心问题。

### 1.2 解决思路

**核心机制**：在Agent执行时，注入下游节点信息，要求Agent按协议格式返回节点选择。

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   注入提示    │ --> │  Agent执行   │ --> │  解析决策    │
│  (下游节点)   │     │  (返回文本)   │     │  (节点ID)    │
└──────────────┘     └──────────────┘     └──────────────┘
```

### 1.3 适用节点

| 节点类型 | 决策方式 | 是否需要Agent |
|---------|---------|--------------|
| 条件判断 | Agent选择分支 | ✅ |
| 并行执行 | Agent选择多个分支 | ✅ |
| 循环执行 | Agent决定继续/退出 | ✅ |
| 人工审核 | 人工回调决定 | ❌ |

---

## 二、Agent决策协议

### 2.1 协议格式

Agent输出中应包含决策块，格式如下：

```
[NODE_DECISION]
node_ids: <节点ID列表，逗号分隔>
reason: <决策原因>
[/NODE_DECISION]
```

**示例**：

```
[NODE_DECISION]
node_ids: node_test_success
reason: 代码审查通过，所有测试用例通过，进入部署流程
[/NODE_DECISION]
```

### 2.2 决策提示模板

系统会根据节点类型，自动注入对应的决策提示：

```markdown
## 工作流决策指引

你正在执行工作流节点「{{node_name}}」。

### 当前可选择的下游节点：

{{#each downstream_nodes}}
- `{{this.id}}`: {{this.name}} - {{this.description}}
{{/each}}

### 输出要求：

完成任务后，请按以下格式输出你的决策：

```
[NODE_DECISION]
node_ids: <从上述节点中选择一个或多个>
reason: <简短说明选择原因>
[/NODE_DECISION]
```

**注意**：
1. node_ids 必须从上述可选项中选择
2. 可以选择多个节点（用逗号分隔）以实现并行执行
3. 如果任务失败或无法继续，选择合适的失败处理节点
```

### 2.3 解析规则

```java
public class NodeDecision {
    private List<String> nodeIds;    // 选中的节点ID列表
    private String reason;           // 决策原因
}

// 解析流程
1. 提取 [NODE_DECISION]...[/NODE_DECISION] 块
2. 解析 node_ids 字段（按逗号分隔）
3. 验证 node_ids 是否在下游节点列表中
4. 验证通过则返回，否则使用默认节点
```

---

## 三、各节点类型详细设计

### 3.1 条件判断节点

#### 3.1.1 概念模型

```
                    ┌── [branch_success] ──→ 成功处理
                    │
[前置节点] → [条件判断] ─── [branch_failure] ──→ 失败处理
                    │
                    └── [branch_timeout] ──→ 超时处理
```

#### 3.1.2 节点配置

```json
{
  "id": "node_condition_1",
  "type": "condition",
  "name": "代码检查结果判断",
  "description": "根据代码检查结果决定后续流程",
  "branches": [
    {
      "id": "branch_success",
      "name": "检查通过",
      "description": "代码检查全部通过，进入测试阶段",
      "targetNodeId": "node_test"
    },
    {
      "id": "branch_warning",
      "name": "存在警告",
      "description": "有警告但无错误，需人工确认",
      "targetNodeId": "node_manual_confirm"
    },
    {
      "id": "branch_failure",
      "name": "检查失败",
      "description": "存在错误，需要修复",
      "targetNodeId": "node_fix"
    }
  ],
  "defaultBranch": "branch_failure"
}
```

#### 3.1.3 决策提示生成

```java
public String buildDecisionPrompt(ConditionNode node) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("## 条件判断决策\n\n");
    prompt.append("当前节点：").append(node.getName()).append("\n\n");
    prompt.append("### 请根据任务执行结果，选择一个分支：\n\n");

    for (Branch branch : node.getBranches()) {
        prompt.append("- `").append(branch.getId()).append("`: ");
        prompt.append(branch.getName()).append(" - ");
        prompt.append(branch.getDescription()).append("\n");
    }

    prompt.append("\n### 输出格式：\n");
    prompt.append("```\n");
    prompt.append("[NODE_DECISION]\n");
    prompt.append("node_ids: <分支ID>\n");
    prompt.append("reason: <选择原因>\n");
    prompt.append("[/NODE_DECISION]\n");
    prompt.append("```\n");

    return prompt.toString();
}
```

#### 3.1.4 执行流程

```
┌─────────────────────────────────────────────────────────────┐
│                    条件判断节点执行流程                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 获取节点配置                                             │
│     └── 读取 branches 列表和 defaultBranch                   │
│                                                             │
│  2. 构建决策提示                                             │
│     ├── 生成分支选项描述                                      │
│     └── 注入到 Agent 系统提示                                 │
│                                                             │
│  3. 执行 Agent                                               │
│     ├── Agent 分析当前上下文                                  │
│     └── Agent 返回决策结果                                    │
│                                                             │
│  4. 解析决策                                                 │
│     ├── 提取 node_ids                                        │
│     ├── 验证是否在 branches 中                                │
│     └── 无效则使用 defaultBranch                              │
│                                                             │
│  5. 返回下一节点                                              │
│     └── 返回选中分支的 targetNodeId                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 3.1.5 异常处理

| 异常情况 | 处理方式 |
|---------|---------|
| Agent未输出决策块 | 使用 defaultBranch |
| node_ids不在可选列表 | 使用 defaultBranch |
| Agent执行超时 | 使用 defaultBranch |
| Agent执行失败 | 触发 on_fail 节点 |

---

### 3.2 并行执行节点

#### 3.2.1 概念模型

```
                    ┌── [branch_code_style] ────┐
                    │                           │
[开始] → [并行执行] ─── [branch_unit_test] ────┼──→ [合并] → [结束]
                    │                           │
                    └── [branch_security] ──────┘
```

#### 3.2.2 节点配置

```json
{
  "id": "node_parallel_1",
  "type": "parallel",
  "name": "代码质量检查",
  "description": "并行执行多个代码检查任务",
  "branches": [
    {
      "id": "branch_code_style",
      "name": "代码风格检查",
      "description": "检查代码格式和风格规范",
      "targetNodeId": "node_style_check"
    },
    {
      "id": "branch_unit_test",
      "name": "单元测试",
      "description": "运行单元测试套件",
      "targetNodeId": "node_unit_test"
    },
    {
      "id": "branch_security",
      "name": "安全扫描",
      "description": "检查安全漏洞",
      "targetNodeId": "node_security_scan"
    }
  ],
  "mergeNode": "node_merge_1",
  "executionMode": "DYNAMIC",
  "defaultBranches": ["branch_code_style", "branch_unit_test", "branch_security"]
}
```

#### 3.2.3 执行模式

| 模式 | 说明 | Agent决策 |
|------|------|----------|
| `ALL` | 执行所有分支 | 不需要Agent决策 |
| `DYNAMIC` | Agent选择执行哪些分支 | 需要Agent决策 |
| `CONDITIONAL` | 根据条件选择 | Agent决策 |

#### 3.2.4 决策提示生成

```java
public String buildDecisionPrompt(ParallelNode node) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("## 并行执行决策\n\n");
    prompt.append("当前节点：").append(node.getName()).append("\n\n");
    prompt.append("### 请选择需要执行的分支（可多选）：\n\n");

    for (Branch branch : node.getBranches()) {
        prompt.append("- `").append(branch.getId()).append("`: ");
        prompt.append(branch.getName()).append(" - ");
        prompt.append(branch.getDescription()).append("\n");
    }

    prompt.append("\n### 输出格式：\n");
    prompt.append("```\n");
    prompt.append("[NODE_DECISION]\n");
    prompt.append("node_ids: <分支ID1>, <分支ID2>, ...\n");
    prompt.append("reason: <选择这些分支的原因>\n");
    prompt.append("[/NODE_DECISION]\n");
    prompt.append("```\n");
    prompt.append("\n**提示**：可以选择多个分支并行执行，用逗号分隔。\n");

    return prompt.toString();
}
```

#### 3.2.5 执行流程

```
┌─────────────────────────────────────────────────────────────┐
│                    并行执行节点执行流程                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 检查执行模式                                             │
│     ├── ALL: 直接执行所有分支                                 │
│     └── DYNAMIC: 需要Agent决策                               │
│                                                             │
│  2. Agent决策阶段                                            │
│     ├── 构建决策提示                                          │
│     ├── Agent 选择要执行的分支                                │
│     └── 解析得到 selectedBranches                            │
│                                                             │
│  3. 创建并行执行任务                                          │
│     ├── 为每个选中分支创建执行上下文                          │
│     ├── 记录分支状态为 PENDING                               │
│     └── 返回所有选中分支的 targetNodeId                       │
│                                                             │
│  4. 引擎并行执行                                              │
│     ├── 同时启动所有分支执行                                  │
│     └── 每个分支独立执行到 mergeNode                          │
│                                                             │
│  5. 分支状态追踪                                              │
│     ├── 记录每个分支的执行状态                                │
│     └── 所有分支完成后触发合并节点                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 3.2.6 状态管理

```java
// 并行执行状态
public class ParallelExecutionState {
    private String parallelNodeId;           // 并行节点ID
    private String executionId;              // 执行ID
    private List<BranchExecution> branches;  // 分支执行状态
    private String mergeNodeId;              // 合并节点ID
    private ParallelStatus status;           // WAITING/RUNNING/COMPLETED
}

// 单个分支状态
public class BranchExecution {
    private String branchId;        // 分支ID
    private String targetNodeId;    // 目标节点ID
    private BranchStatus status;    // PENDING/RUNNING/COMPLETED/FAILED
    private String current nodeId;  // 当前执行到的节点
    private Object output;          // 分支输出结果
}
```

---

### 3.3 循环执行节点

#### 3.3.1 概念模型

```
┌──────────────────────────────────────────┐
│                                          │
│  ┌───┐    ┌─────────┐    ┌──────────┐   │
│  │开始│──→│循环开始  │──→│ 循环体    │   │
│  └───┘    └─────────┘    └──────────┘   │
│                ↑              │          │
│                └──────────────┘          │
│                                          │
│  循环条件满足或达到上限 → [循环结束] → [后续节点]
└──────────────────────────────────────────┘
```

#### 3.3.2 节点配置

```json
{
  "id": "node_loop_1",
  "type": "loop",
  "name": "遍历处理文件",
  "description": "遍历处理每个待处理文件",
  "loopConfig": {
    "loopVariable": "currentFile",
    "iteratorSource": "{{files}}",
    "maxIterations": 100,
    "iterationTimeout": 300
  },
  "loopBody": {
    "entryNodeId": "node_process_file",
    "exitNodeId": "node_loop_end"
  },
  "onLoopComplete": "node_summary"
}
```

#### 3.3.3 循环变量传递

每次迭代时，会将循环变量注入上下文：

```json
{
  "loopContext": {
    "iteration": 3,
    "maxIterations": 100,
    "loopVariable": "currentFile",
    "currentValue": {
      "name": "document.pdf",
      "path": "/docs/document.pdf"
    },
    "hasMore": true,
    "previousOutputs": [
      { "iteration": 1, "output": {...} },
      { "iteration": 2, "output": {...} }
    ]
  }
}
```

#### 3.3.4 决策提示生成

```java
public String buildDecisionPrompt(LoopNode node, LoopContext context) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("## 循环执行决策\n\n");
    prompt.append("当前节点：").append(node.getName()).append("\n");
    prompt.append("当前迭代：第 ").append(context.getIteration()).append(" 次\n");
    prompt.append("最大迭代：").append(context.getMaxIterations()).append(" 次\n\n");

    prompt.append("### 循环变量：\n");
    prompt.append("- `").append(node.getLoopVariable()).append("`: ");
    prompt.append(context.getCurrentValue()).append("\n\n");

    prompt.append("### 请决定下一步操作：\n\n");
    prompt.append("- `continue`: 继续下一次迭代\n");
    prompt.append("- `exit`: 退出循环（任务已完成或无法继续）\n\n");

    prompt.append("### 输出格式：\n");
    prompt.append("```\n");
    prompt.append("[NODE_DECISION]\n");
    prompt.append("node_ids: continue 或 exit\n");
    prompt.append("reason: <决策原因>\n");
    prompt.append("[/NODE_DECISION]\n");
    prompt.append("```\n");

    return prompt.toString();
}
```

#### 3.3.5 执行流程

```
┌─────────────────────────────────────────────────────────────┐
│                    循环节点执行流程                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 初始化循环                                               │
│     ├── 解析迭代数据源                                        │
│     ├── 初始化 loopContext                                   │
│     └── iteration = 0                                        │
│                                                             │
│  2. 开始迭代                                                 │
│     ├── iteration++                                          │
│     ├── 检查 iteration <= maxIterations                      │
│     ├── 获取当前迭代值 currentValue                          │
│     └── 注入 loopContext 到执行上下文                         │
│                                                             │
│  3. 执行循环体                                               │
│     ├── 执行 loopBody.entryNodeId                            │
│     └── 循环体内部节点依次执行                                │
│                                                             │
│  4. Agent决策（循环体结束时）                                 │
│     ├── Agent 判断是否继续                                   │
│     ├── continue: 返回步骤2                                  │
│     └── exit: 进入步骤5                                      │
│                                                             │
│  5. 退出循环                                                 │
│     ├── 收集所有迭代输出                                      │
│     ├── 更新最终状态                                          │
│     └── 跳转到 onLoopComplete 节点                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 3.3.6 状态管理

```java
// 循环执行状态
public class LoopExecutionState {
    private String loopNodeId;           // 循环节点ID
    private String executionId;          // 执行ID
    private int currentIteration;        // 当前迭代次数
    private int maxIterations;           // 最大迭代次数
    private LoopStatus status;           // RUNNING/COMPLETED/MAX_REACHED
    private List<IterationResult> results; // 各迭代结果
    private Object currentValue;         // 当前迭代值
}

// 单次迭代结果
public class IterationResult {
    private int iteration;         // 迭代序号
    private Object input;          // 输入值
    private Object output;         // 输出结果
    private IterationStatus status; // SUCCESS/FAILED
    private long duration;         // 执行时长(ms)
}
```

#### 3.3.7 特殊情况处理

| 情况 | 处理方式 |
|------|---------|
| 迭代数据源为空 | 直接跳过循环，进入 onLoopComplete |
| 达到最大迭代次数 | 强制退出循环，记录警告日志 |
| 单次迭代失败 | 可配置：继续/中断/等待重试 |
| 循环体执行超时 | 标记当前迭代失败，进入决策 |

---

### 3.4 人工审核节点

#### 3.4.1 概念模型

```
[前置节点] → [人工审核] ──(等待)──→ 用户确认 ──→ [后续节点]
                 │
                 └──→ 发送通知（飞书/邮件）
```

#### 3.4.2 节点配置

```json
{
  "id": "node_review_1",
  "type": "human_review",
  "name": "部署审批",
  "description": "等待人工审批后继续执行",
  "reviewConfig": {
    "approvers": ["user_001", "user_002"],
    "approvalType": "ANY_ONE",
    "timeoutSeconds": 86400,
    "timeoutAction": "REJECT",
    "message": "请审批部署申请"
  },
  "branches": {
    "on_approve": "node_deploy",
    "on_reject": "node_notify_reject",
    "on_timeout": "node_notify_timeout"
  }
}
```

#### 3.4.3 审批类型

| 类型 | 说明 |
|------|------|
| `ANY_ONE` | 任一审批人通过即可 |
| `ALL` | 所有审批人都需要通过 |
| `MAJORITY` | 多数通过即可 |

#### 3.4.4 执行流程

```
┌─────────────────────────────────────────────────────────────┐
│                    人工审核节点执行流程                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 初始化审核                                               │
│     ├── 创建审核记录                                          │
│     ├── 设置状态为 WAITING_APPROVAL                          │
│     └── 持久化到数据库                                        │
│                                                             │
│  2. 发送通知                                                 │
│     ├── 构建飞书消息卡片                                      │
│     ├── 包含审核链接（带token）                               │
│     └── 发送给审批人                                          │
│                                                             │
│  3. 等待审批                                                 │
│     ├── 暂停当前执行                                          │
│     ├── 释放线程资源                                          │
│     └── 设置超时定时器                                        │
│                                                             │
│  4. 接收回调                                                 │
│     ├── 用户点击审批链接                                      │
│     ├── 验证 token 有效性                                     │
│     └── 记录审批结果                                          │
│                                                             │
│  5. 恢复执行                                                 │
│     ├── 更新审核状态                                          │
│     ├── approved → on_approve 节点                           │
│     ├── rejected → on_reject 节点                            │
│     └── timeout → on_timeout 节点                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 3.4.5 飞书消息卡片

```json
{
  "msg_type": "interactive",
  "card": {
    "header": {
      "title": { "tag": "plain_text", "content": "工作流审批通知" },
      "template": "blue"
    },
    "elements": [
      {
        "tag": "div",
        "text": { "tag": "lark_md", "content": "**工作流**：部署流程\n**节点**：部署审批\n**提交人**：张三\n**等待时间**：已等待 2 小时" }
      },
      {
        "tag": "action",
        "actions": [
          {
            "tag": "button",
            "text": { "tag": "plain_text", "content": "通过" },
            "type": "primary",
            "url": "https://workflow.example.com/approve?token=xxx&action=approve"
          },
          {
            "tag": "button",
            "text": { "tag": "plain_text", "content": "拒绝" },
            "type": "danger",
            "url": "https://workflow.example.com/approve?token=xxx&action=reject"
          }
        ]
      }
    ]
  }
}
```

#### 3.4.6 回调API设计

```
POST /api/executions/{executionId}/review/{nodeId}

Request:
{
  "token": "review_token_xxx",
  "action": "approve",  // approve | reject
  "comment": "同意部署",
  "reviewer": "user_001"
}

Response:
{
  "success": true,
  "message": "审批成功，工作流将继续执行"
}
```

#### 3.4.7 状态管理

```java
// 人工审核状态
public class ReviewExecutionState {
    private String reviewNodeId;         // 审核节点ID
    private String executionId;          // 执行ID
    private ReviewStatus status;         // WAITING/APPROVED/REJECTED/TIMEOUT
    private List<String> approvers;      // 审批人列表
    private List<ApprovalRecord> approvals; // 审批记录
    private Date createTime;             // 创建时间
    private Date expireTime;             // 过期时间
}

// 单条审批记录
public class ApprovalRecord {
    private String reviewer;      // 审批人
    private ApprovalAction action; // APPROVE/REJECT
    private String comment;        // 审批意见
    private Date approveTime;      // 审批时间
}
```

---

## 四、核心代码设计

### 4.1 类图

```
┌─────────────────────────────────────────────────────────────────┐
│                         NodeHandler (抽象类)                     │
├─────────────────────────────────────────────────────────────────┤
│ + getDownstreamNodes(node): List<NodeInfo>                      │
│ + buildDecisionPrompt(node, context): String                    │
│ + parseDecision(agentOutput): NodeDecision                      │
│ + execute(context): NodeResult                                  │
│ + validateDecision(decision, downstreamNodes): boolean          │
└─────────────────────────────────────────────────────────────────┘
                              △
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────┴───────┐    ┌────────┴────────┐   ┌──────┴──────┐
│ConditionHandler│    │ ParallelHandler │   │ LoopHandler │
└───────────────┘    └─────────────────┘   └─────────────┘
```

### 4.2 核心接口

```java
/**
 * 节点决策结果
 */
public class NodeDecision {
    private List<String> nodeIds;
    private String reason;
    private Map<String, Object> metadata;

    // Getters and Setters
}

/**
 * Agent输出解析器
 */
public class AgentOutputParser {

    private static final Pattern DECISION_PATTERN = Pattern.compile(
        "\\[NODE_DECISION\\]\\s*([\\s\\S]*?)\\s*\\[/NODE_DECISION\\]"
    );

    /**
     * 从Agent输出中解析决策
     */
    public NodeDecision parse(String agentOutput) {
        Matcher matcher = DECISION_PATTERN.matcher(agentOutput);
        if (!matcher.find()) {
            return null;
        }

        String decisionBlock = matcher.group(1);
        return parseDecisionBlock(decisionBlock);
    }

    private NodeDecision parseDecisionBlock(String block) {
        NodeDecision decision = new NodeDecision();

        // 解析 node_ids
        Pattern nodeIdsPattern = Pattern.compile("node_ids:\\s*(.+)");
        Matcher nodeIdsMatcher = nodeIdsPattern.matcher(block);
        if (nodeIdsMatcher.find()) {
            String nodeIdsStr = nodeIdsMatcher.group(1).trim();
            decision.setNodeIds(Arrays.asList(nodeIdsStr.split("\\s*,\\s*")));
        }

        // 解析 reason
        Pattern reasonPattern = Pattern.compile("reason:\\s*(.+)");
        Matcher reasonMatcher = reasonPattern.matcher(block);
        if (reasonMatcher.find()) {
            decision.setReason(reasonMatcher.group(1).trim());
        }

        return decision;
    }
}

/**
 * 决策验证器
 */
public class DecisionValidator {

    /**
     * 验证决策中的节点ID是否合法
     */
    public boolean validate(NodeDecision decision, List<NodeInfo> downstreamNodes) {
        if (decision == null || decision.getNodeIds() == null) {
            return false;
        }

        Set<String> validNodeIds = downstreamNodes.stream()
            .map(NodeInfo::getId)
            .collect(Collectors.toSet());

        for (String nodeId : decision.getNodeIds()) {
            if (!validNodeIds.contains(nodeId)) {
                return false;
            }
        }

        return true;
    }
}
```

### 4.3 节点处理器基类

```java
/**
 * 节点处理器抽象基类
 */
public abstract class AbstractNodeHandler implements NodeHandler {

    protected final AgentOutputParser outputParser = new AgentOutputParser();
    protected final DecisionValidator validator = new DecisionValidator();

    @Override
    public NodeResult execute(NodeExecutionContext context) {
        // 1. 获取下游节点
        List<NodeInfo> downstreamNodes = getDownstreamNodes(context.getNode());

        // 2. 构建决策提示
        String decisionPrompt = buildDecisionPrompt(context.getNode(), downstreamNodes);

        // 3. 执行Agent
        AgentExecutionResult agentResult = executeAgent(context, decisionPrompt);

        // 4. 解析决策
        NodeDecision decision = outputParser.parse(agentResult.getOutput());

        // 5. 验证决策
        if (!validator.validate(decision, downstreamNodes)) {
            decision = getDefaultDecision(context.getNode());
        }

        // 6. 返回结果
        return buildResult(agentResult, decision);
    }

    /**
     * 获取下游节点信息
     */
    protected abstract List<NodeInfo> getDownstreamNodes(Node node);

    /**
     * 构建决策提示
     */
    protected abstract String buildDecisionPrompt(Node node, List<NodeInfo> downstreamNodes);

    /**
     * 获取默认决策（当解析失败时使用）
     */
    protected abstract NodeDecision getDefaultDecision(Node node);

    /**
     * 执行Agent
     */
    protected AgentExecutionResult executeAgent(NodeExecutionContext context, String decisionPrompt) {
        // 调用OpenClaw Agent执行
        return openClawClient.execute(
            context.getAgentId(),
            buildFullPrompt(context, decisionPrompt)
        );
    }
}
```

### 4.4 条件判断处理器实现

```java
/**
 * 条件判断节点处理器
 */
public class ConditionNodeHandler extends AbstractNodeHandler {

    @Override
    protected List<NodeInfo> getDownstreamNodes(Node node) {
        ConditionNode conditionNode = (ConditionNode) node;
        return conditionNode.getBranches().stream()
            .map(b -> new NodeInfo(b.getId(), b.getName(), b.getDescription()))
            .collect(Collectors.toList());
    }

    @Override
    protected String buildDecisionPrompt(Node node, List<NodeInfo> downstreamNodes) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("## 条件判断决策\n\n");
        prompt.append("当前节点：").append(node.getName()).append("\n\n");
        prompt.append("### 请根据任务执行结果，选择一个分支：\n\n");

        for (NodeInfo info : downstreamNodes) {
            prompt.append("- `").append(info.getId()).append("`: ");
            prompt.append(info.getName()).append(" - ");
            prompt.append(info.getDescription()).append("\n");
        }

        appendDecisionFormat(prompt);
        return prompt.toString();
    }

    @Override
    protected NodeDecision getDefaultDecision(Node node) {
        ConditionNode conditionNode = (ConditionNode) node;
        NodeDecision decision = new NodeDecision();
        decision.setNodeIds(Collections.singletonList(conditionNode.getDefaultBranch()));
        decision.setReason("使用默认分支");
        return decision;
    }
}
```

---

## 五、数据库设计

### 5.1 节点执行状态表

```sql
CREATE TABLE node_execution_state (
    id TEXT PRIMARY KEY,
    execution_id TEXT NOT NULL,
    node_id TEXT NOT NULL,
    node_type TEXT NOT NULL,
    status TEXT NOT NULL,           -- PENDING/RUNNING/COMPLETED/FAILED
    input TEXT,                     -- JSON格式的输入
    output TEXT,                    -- JSON格式的输出
    agent_output TEXT,              -- Agent原始输出
    decision TEXT,                  -- JSON格式的决策结果
    retry_count INTEGER DEFAULT 0,
    error_message TEXT,
    start_time DATETIME,
    end_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 5.2 并行执行状态表

```sql
CREATE TABLE parallel_execution_state (
    id TEXT PRIMARY KEY,
    execution_id TEXT NOT NULL,
    parallel_node_id TEXT NOT NULL,
    branch_id TEXT NOT NULL,
    target_node_id TEXT NOT NULL,
    status TEXT NOT NULL,           -- PENDING/RUNNING/COMPLETED/FAILED
    current_node_id TEXT,           -- 当前执行到的节点
    output TEXT,                    -- JSON格式的分支输出
    start_time DATETIME,
    end_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 5.3 循环执行状态表

```sql
CREATE TABLE loop_execution_state (
    id TEXT PRIMARY KEY,
    execution_id TEXT NOT NULL,
    loop_node_id TEXT NOT NULL,
    current_iteration INTEGER DEFAULT 0,
    max_iterations INTEGER NOT NULL,
    status TEXT NOT NULL,           -- RUNNING/COMPLETED/MAX_REACHED
    current_value TEXT,             -- JSON格式的当前迭代值
    iteration_results TEXT,         -- JSON数组，存储每次迭代结果
    start_time DATETIME,
    end_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 5.4 人工审核状态表

```sql
CREATE TABLE review_execution_state (
    id TEXT PRIMARY KEY,
    execution_id TEXT NOT NULL,
    review_node_id TEXT NOT NULL,
    status TEXT NOT NULL,           -- WAITING/APPROVED/REJECTED/TIMEOUT
    approvers TEXT,                 -- JSON数组，审批人列表
    approval_records TEXT,          -- JSON数组，审批记录
    timeout_seconds INTEGER,
    expire_time DATETIME,
    review_token TEXT,              -- 审核链接token
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 六、API设计补充

### 6.1 人工审核回调API

```
POST /api/executions/{executionId}/review/{nodeId}

Description: 人工审核回调接口

Request Body:
{
    "token": "review_token_xxx",
    "action": "approve",
    "comment": "同意执行",
    "reviewer": "user_001"
}

Response:
{
    "success": true,
    "data": {
        "executionId": "exec_xxx",
        "nodeId": "node_review_1",
        "status": "APPROVED",
        "message": "审批成功，工作流将继续执行"
    }
}
```

### 6.2 获取并行执行状态API

```
GET /api/executions/{executionId}/parallel/{nodeId}/status

Response:
{
    "success": true,
    "data": {
        "parallelNodeId": "node_parallel_1",
        "status": "RUNNING",
        "branches": [
            {
                "branchId": "branch_a",
                "status": "COMPLETED",
                "currentNodeId": "node_merge"
            },
            {
                "branchId": "branch_b",
                "status": "RUNNING",
                "currentNodeId": "node_unit_test"
            }
        ]
    }
}
```

### 6.3 获取循环执行状态API

```
GET /api/executions/{executionId}/loop/{nodeId}/status

Response:
{
    "success": true,
    "data": {
        "loopNodeId": "node_loop_1",
        "currentIteration": 5,
        "maxIterations": 100,
        "status": "RUNNING",
        "iterationResults": [
            { "iteration": 1, "status": "SUCCESS" },
            { "iteration": 2, "status": "SUCCESS" },
            { "iteration": 3, "status": "FAILED" },
            { "iteration": 4, "status": "SUCCESS" }
        ]
    }
}
```

---

## 七、异常处理汇总

### 7.1 Agent决策异常

| 异常类型 | 场景 | 处理方式 |
|---------|------|---------|
| `DecisionNotFoundException` | Agent输出中没有决策块 | 使用默认决策 |
| `InvalidNodeIdException` | 决策中的节点ID无效 | 使用默认决策 |
| `AgentExecutionException` | Agent执行失败 | 触发 on_fail 分支 |
| `AgentTimeoutException` | Agent执行超时 | 使用默认决策 |

### 7.2 并行执行异常

| 异常类型 | 场景 | 处理方式 |
|---------|------|---------|
| `BranchExecutionException` | 某分支执行失败 | 标记分支失败，继续其他分支 |
| `AllBranchesFailedException` | 所有分支都失败 | 触发 on_fail 分支 |

### 7.3 循环执行异常

| 异常类型 | 场景 | 处理方式 |
|---------|------|---------|
| `IterationException` | 单次迭代失败 | 可配置：继续/中断/等待 |
| `MaxIterationsException` | 达到最大迭代次数 | 强制退出，记录警告 |
| `EmptyIteratorException` | 迭代数据源为空 | 跳过循环 |

### 7.4 人工审核异常

| 异常类型 | 场景 | 处理方式 |
|---------|------|---------|
| `ReviewTimeoutException` | 审核超时 | 执行 timeoutAction |
| `InvalidTokenException` | 回调token无效 | 拒绝请求 |
| `AlreadyReviewedException` | 已经审核过 | 返回已审核状态 |

---

## 八、测试用例

### 8.1 条件判断节点测试

```java
@Test
public void testConditionNode_SuccessBranch() {
    // 准备
    ConditionNode node = createConditionNode();
    String agentOutput = "分析完成，测试全部通过。\n\n[NODE_DECISION]\nnode_ids: branch_success\nreason: 测试通过\n[/NODE_DECISION]";

    // 执行
    NodeResult result = handler.execute(node, createContext(agentOutput));

    // 验证
    assertEquals("branch_success", result.getNextNodeIds().get(0));
}

@Test
public void testConditionNode_NoDecision_UsesDefault() {
    // 准备
    ConditionNode node = createConditionNode();
    String agentOutput = "分析完成";  // 没有决策块

    // 执行
    NodeResult result = handler.execute(node, createContext(agentOutput));

    // 验证
    assertEquals(node.getDefaultBranch(), result.getNextNodeIds().get(0));
}
```

### 8.2 并行执行节点测试

```java
@Test
public void testParallelNode_MultipleBranches() {
    // 准备
    ParallelNode node = createParallelNode();
    String agentOutput = "[NODE_DECISION]\nnode_ids: branch_a, branch_b\nreason: 执行两个分支\n[/NODE_DECISION]";

    // 执行
    NodeResult result = handler.execute(node, createContext(agentOutput));

    // 验证
    assertEquals(2, result.getNextNodeIds().size());
    assertTrue(result.getNextNodeIds().contains("branch_a"));
    assertTrue(result.getNextNodeIds().contains("branch_b"));
}
```

### 8.3 循环节点测试

```java
@Test
public void testLoopNode_Continue() {
    // 准备
    LoopNode node = createLoopNode();
    LoopContext context = createLoopContext(3, 100);
    String agentOutput = "[NODE_DECISION]\nnode_ids: continue\nreason: 还有未处理项\n[/NODE_DECISION]";

    // 执行
    NodeResult result = handler.execute(node, context);

    // 验证
    assertEquals("continue", result.getDecision().getNodeIds().get(0));
}

@Test
public void testLoopNode_Exit() {
    // 准备
    LoopNode node = createLoopNode();
    LoopContext context = createLoopContext(10, 10);
    String agentOutput = "[NODE_DECISION]\nnode_ids: exit\nreason: 处理完成\n[/NODE_DECISION]";

    // 执行
    NodeResult result = handler.execute(node, context);

    // 验证
    assertEquals("exit", result.getDecision().getNodeIds().get(0));
}
```

---

## 九、实施计划

### 9.1 开发阶段

| 阶段 | 内容 | 预计工期 |
|------|------|---------|
| **Phase 1** | Agent决策协议 + 解析器 | 2天 |
| **Phase 2** | 条件判断节点 | 1天 |
| **Phase 3** | 并行执行节点 | 2天 |
| **Phase 4** | 循环节点 | 2天 |
| **Phase 5** | 人工审核节点 | 2天 |
| **Phase 6** | 联调测试 | 2天 |

### 9.2 依赖关系

```
Phase 1 (决策协议)
    │
    ├──→ Phase 2 (条件判断)
    │
    ├──→ Phase 3 (并行执行)
    │
    └──→ Phase 4 (循环执行)

Phase 5 (人工审核) ──→ 独立开发

Phase 1-5 完成 → Phase 6 (联调测试)
```

---

## 十、附录

### 10.1 Agent输出示例

#### 条件判断输出

```
我已经完成了代码检查的分析。以下是检查结果：

1. 代码风格：通过
2. 单元测试：通过（覆盖率 85%）
3. 安全扫描：发现1个低风险警告

建议：虽然有低风险警告，但不影响主要功能，可以继续部署。

[NODE_DECISION]
node_ids: branch_success
reason: 核心检查全部通过，低风险警告可在后续版本修复
[/NODE_DECISION]
```

#### 并行执行输出

```
根据项目类型和需求分析，建议执行以下检查：

1. 代码风格检查 - 必要，保证代码质量
2. 单元测试 - 必要，验证功能正确性
3. 安全扫描 - 本次为内部工具，可暂不执行

[NODE_DECISION]
node_ids: branch_code_style, branch_unit_test
reason: 执行代码风格和单元测试，安全扫描本次跳过
[/NODE_DECISION]
```

#### 循环节点输出

```
正在处理第3个文件：document.pdf

处理结果：
- 文件解析：成功
- 内容提取：成功
- 已存储到数据库

当前进度：3/10

[NODE_DECISION]
node_ids: continue
reason: 还有7个文件待处理，继续下一次迭代
[/NODE_DECISION]
```