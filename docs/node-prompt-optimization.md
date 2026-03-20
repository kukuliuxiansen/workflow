# 决策类节点提示词优化设计

## 一、问题分析

当前决策类节点存在以下问题：

1. **提示词不够明确**：没有告诉Agent当前在工作流中的位置、可选的下游节点
2. **前端配置不够灵活**：用户无法方便地配置条件规则
3. **缺少通用模板**：每种决策节点需要一套标准化的提示词模板

## 二、节点类型分析

### 2.1 条件判断节点 (CONDITION)

**作用**：根据条件选择一个分支执行

**需要的信息**：
- 当前节点名称和位置
- 所有可选分支及其描述
- 每个分支的触发条件（用户配置）
- 上游节点输出内容
- 判断依据说明

**默认提示词模板**：
```
## 条件判断决策任务

你当前在工作流的【{节点名称}】节点，需要根据上游输出决定执行哪个分支。

### 当前工作流上下文
- 工作流ID: {workflowId}
- 执行ID: {executionId}
- 当前节点: {nodeName} ({nodeId})

### 上游节点输出
{上游节点输出内容}

### 可选分支
请从以下分支中选择一个执行：

{分支列表}

| 分支ID | 分支名称 | 触发条件说明 |
|--------|---------|-------------|
| {branch1_id} | {branch1_name} | {branch1_condition} |
| {branch2_id} | {branch2_name} | {branch2_condition} |

### 决策要求
1. 仔细分析上游节点的输出内容
2. 根据上述条件说明，判断应该走哪个分支
3. 必须选择一个分支

### 输出格式
请严格按照以下格式输出你的决策：

[NODE_DECISION]
node_ids: {选择的分支ID}
reason: {选择该分支的原因}
[/NODE_DECISION]
```

### 2.2 并行执行节点 (PARALLEL)

**作用**：选择一个或多个分支并行执行

**需要的信息**：
- 执行模式（ALL/DYNAMIC）
- 所有可选分支
- 每个分支的执行条件

**默认提示词模板**：
```
## 并行执行决策任务

你当前在工作流的【{节点名称}】节点，需要决定并行执行哪些分支。

### 执行模式
当前模式: {executionMode}
- ALL: 执行所有分支
- DYNAMIC: 由Agent动态选择分支

### 可选分支
{分支列表}

### 决策要求
1. 如果模式是ALL，必须选择所有分支
2. 如果模式是DYNAMIC，根据上游输出选择需要执行的分支
3. 可以选择多个分支，用逗号分隔

### 输出格式
[NODE_DECISION]
node_ids: {分支ID1}, {分支ID2}, ...
reason: {选择这些分支的原因}
[/NODE_DECISION]
```

### 2.3 循环节点 (LOOP)

**作用**：重复执行某个流程直到满足退出条件

**需要的信息**：
- 循环模式（iterator/condition）
- 当前迭代次数
- 最大迭代次数
- 循环变量
- 退出条件

**默认提示词模板**：
```
## 循环决策任务

你当前在工作流的【{节点名称}】节点，需要判断是否继续循环。

### 循环状态
- 当前迭代: 第 {currentIteration} 次
- 最大迭代: {maxIterations} 次
- 循环模式: {loopMode}

### 循环上下文
{循环变量内容}

### 决策选项
- `continue`: 继续执行下一次循环
- `exit`: 退出循环，继续后续流程

### 退出条件
{用户配置的退出条件说明}

### 输出格式
[NODE_DECISION]
node_ids: continue 或 exit
reason: {继续或退出的原因}
[/NODE_DECISION]
```

### 2.4 人工审核节点 (HUMAN_REVIEW)

**作用**：等待人工审批

**需要的信息**：
- 审批人列表
- 审批类型（ANY_ONE/ALL）
- 审批选项
- 超时设置

## 三、前端配置设计

### 3.1 条件判断节点配置界面

```typescript
interface ConditionNodeConfig {
  // 决策模式
  decisionMode: 'agent' | 'expression';

  // 分支配置
  branches: Array<{
    id: string;              // 分支ID
    name: string;            // 分支名称（显示用）
    description: string;     // 分支描述
    targetNodeId: string;    // 目标节点ID
    conditionExpr?: string;  // 表达式模式的条件
    conditionDesc?: string;  // 条件描述（给Agent看）
  }>;

  // 默认分支
  defaultBranch: string;

  // 高级配置
  advanced?: {
    decisionAgentId?: string;  // 决策Agent
    customPrompt?: string;     // 自定义提示词
  };
}
```

**前端界面元素**：
- 分支列表（可增删）
- 每个分支的条件输入框
- 预览决策提示词按钮
- 测试决策按钮

### 3.2 并行执行节点配置界面

```typescript
interface ParallelNodeConfig {
  // 执行模式
  executionMode: 'ALL' | 'DYNAMIC';

  // 分支配置
  branches: Array<{
    id: string;
    name: string;
    description: string;
    targetNodeId: string;
    executionCondition?: string; // DYNAMIC模式下的执行条件说明
  }>;

  // 合并节点
  mergeNode?: string;

  // 高级配置
  advanced?: {
    decisionAgentId?: string;
    customPrompt?: string;
  };
}
```

### 3.3 循环节点配置界面

```typescript
interface LoopNodeConfig {
  // 循环模式
  loopMode: 'iterator' | 'condition';

  // 迭代模式配置
  iteratorConfig?: {
    iteratorSource: string;   // 迭代数据来源
    loopVariable: string;     // 循环变量名
  };

  // 条件模式配置
  conditionConfig?: {
    exitCondition: string;    // 退出条件说明
    maxIterations: number;    // 最大迭代次数
  };

  // 循环体
  loopBody: {
    entryNodeId: string;
  };

  // 循环完成后执行的节点
  onLoopComplete?: string;

  // 高级配置
  advanced?: {
    decisionAgentId?: string;
    customPrompt?: string;
  };
}
```

## 四、实现计划

### 4.1 后端改进

1. 创建 `NodePromptBuilder` 工具类
   - 为每种决策节点生成标准化提示词
   - 支持自定义提示词模板

2. 增强 `ConditionNodeHandler`
   - 使用新的提示词生成逻辑
   - 添加更详细的上下文信息

3. 增强 `ParallelNodeHandler`
   - 添加DYNAMIC模式的Agent决策

4. 增强 `LoopNodeHandler`
   - 完善condition模式的提示词

### 4.2 前端改进

1. 创建节点配置组件
   - `ConditionNodeEditor`
   - `ParallelNodeEditor`
   - `LoopNodeEditor`

2. 添加条件输入界面
   - 分支条件编辑器
   - 提示词预览
   - 决策测试

## 五、示例配置

### 5.1 条件判断节点示例

```json
{
  "decisionMode": "agent",
  "branches": [
    {
      "id": "branch_success",
      "name": "检查通过",
      "description": "所有测试通过，可以继续部署",
      "targetNodeId": "node_deploy",
      "conditionDesc": "当所有测试用例都通过，覆盖率大于80%"
    },
    {
      "id": "branch_failure",
      "name": "检查失败",
      "description": "存在测试失败，需要修复",
      "targetNodeId": "node_fix",
      "conditionDesc": "当存在任何测试失败，或覆盖率低于80%"
    }
  ],
  "defaultBranch": "branch_failure"
}
```

### 5.2 并行执行节点示例

```json
{
  "executionMode": "DYNAMIC",
  "branches": [
    {
      "id": "branch_code_style",
      "name": "代码风格检查",
      "description": "检查代码是否符合规范",
      "targetNodeId": "node_style_check",
      "executionCondition": "当修改了代码文件时执行"
    },
    {
      "id": "branch_unit_test",
      "name": "单元测试",
      "description": "运行单元测试",
      "targetNodeId": "node_unit_test",
      "executionCondition": "当修改了业务逻辑代码时执行"
    },
    {
      "id": "branch_security",
      "name": "安全扫描",
      "description": "扫描安全漏洞",
      "targetNodeId": "node_security_scan",
      "executionCondition": "当修改了涉及安全的代码时执行"
    }
  ],
  "mergeNode": "node_merge"
}
```

### 5.3 循环节点示例

```json
{
  "loopMode": "condition",
  "conditionConfig": {
    "exitCondition": "当所有文件都处理完成，或遇到错误时退出",
    "maxIterations": 100
  },
  "loopBody": {
    "entryNodeId": "node_process_file"
  },
  "onLoopComplete": "node_summary"
}
```

---

**文档版本**: 1.0.0
**创建日期**: 2026-03-20