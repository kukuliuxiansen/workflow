# SmartDecompose V2 代码与文档差异清单

**审查日期**: 2026-03-21

---

## 一、核心功能差异

### 1. 提示词模板系统

| 项目 | 文档设计 | 代码实现 | 状态 |
|------|---------|---------|------|
| 模板存储 | 数据库 prompt_template 表 | 未实现 | ❌ 完全缺失 |
| PromptTemplate 实体 | 需创建 | 未创建 | ❌ 缺失 |
| PromptTemplateRepository | 需创建 | 未创建 | ❌ 缺失 |
| loadTemplates() | 从数据库加载模板 | 空方法 TODO | ❌ 未实现 |
| context.decisionTemplate | PromptTemplate 类型 | String 类型 | ❌ 类型不一致 |

### 2. PromptBuilder

| 项目 | 文档设计 | 代码实现 | 状态 |
|------|---------|---------|------|
| 获取模板 | templateRepository.findDefaultByType() | context.getDecisionTemplateContent() | ❌ 不一致 |
| 模板循环 {{#each}} | 支持 | 未实现 | ❌ 缺失 |
| formatCompletedTasks | 单独方法 | getCompletedTasksInfo() 在 context 中 | ⚠️ 位置不同 |

### 3. 重试模板

| 项目 | 文档设计 | 代码实现 | 状态 |
|------|---------|---------|------|
| 独立 retry 模板 | 有专门模板类型 | 复用 decision 模板 | ❌ 不一致 |
| buildRetryPrompt() | 从 repository 加载 retry 模板 | 在 decision 提示词后追加内容 | ❌ 不一致 |

### 4. 模板管理 API

| 项目 | 文档设计 | 代码实现 | 状态 |
|------|---------|---------|------|
| PromptTemplateController | 有完整 CRUD API | 未创建 | ❌ 缺失 |
| 预览渲染 API | POST /api/prompt-templates/preview | 未创建 | ❌ 缺失 |
| 设置默认模板 API | POST /{id}/set-default | 未创建 | ❌ 缺失 |

---

## 二、数据库表差异

### 文档设计的表（未创建）：

- `prompt_template` - 提示词模板表
- `decompose_execution_state` - 执行状态表
- `decompose_decision_history` - 决策历史表
- `decompose_subtask` - 子任务表
- `manual_review` - 人工审核表
- `prompt_template_version` - 模板版本表

### 代码使用的表（复用旧表）：

- `smart_decompose_state` (旧表，字段不完全匹配)
- `decision_history` (旧表，字段不完全匹配)

---

## 三、初始化流程差异

| 步骤 | 文档设计 | 代码实现 | 状态 |
|------|---------|---------|------|
| Step 4 加载模板 | 从数据库加载 | 空方法 TODO | ❌ 未实现 |
| globalPrompt | context.setGlobalPrompt() | 未处理 | ❌ 缺失 |
| taskDescription | ctx.getTaskDescription() | 从 ctx.getInput() 获取 | ⚠️ 来源不同 |

---

## 四、人工审核差异

| 项目 | 文档设计 | 代码实现 | 状态 |
|------|---------|---------|------|
| ManualReviewRepository | 需创建 | 未创建 | ❌ 缺失 |
| ManualReviewController | 有完整回调 API | 未创建 | ❌ 缺失 |
| ManualReviewRecord 实体 | 需创建 | 未创建 | ❌ 缺失 |
| 审核回调 API | POST /api/executions/{id}/manual-review/{reviewId} | 未实现 | ❌ 缺失 |

---

## 五、扩展机制差异

| 项目 | 文档设计 | 代码实现 | 状态 |
|------|---------|---------|------|
| DecisionHandler 接口 | 支持自定义决策处理器 | 未实现 | ❌ 缺失 |
| ReviewStrategy 接口 | 支持自定义审核策略 | 未实现 | ❌ 缺失 |
| TaskSplitInterceptor | 拆分拦截器 | 未实现 | ❌ 缺失 |
| MaxDepthInterceptor | 深度限制拦截器 | 未实现 | ❌ 缺失 |

---

## 六、监控 API 差异

| API | 文档设计 | 代码实现 | 状态 |
|-----|---------|---------|------|
| GET /api/executions/{id}/decisions | 决策历史查询 | 未实现 | ❌ 缺失 |
| GET /api/executions/{id}/decompose-status | 执行状态查询 | 未实现 | ❌ 缺失 |

---

## 七、其他差异

| 项目 | 文档设计 | 代码实现 | 状态 |
|------|---------|---------|------|
| 模板文件位置 | src/main/resources/templates/*.md | 未创建 | ❌ 缺失 |
| PromptTemplateInitializer | 启动时初始化默认模板 | 未创建 | ❌ 缺失 |
| 模板版本管理 | 有版本表和回滚功能 | 未实现 | ❌ 缺失 |
| 缓存配置 | Caffeine 缓存模板 | 未配置 | ❌ 缺失 |

---

## 总结

**核心功能缺失统计：**
- 完全缺失：14 项
- 实现不一致：5 项
- 位置/来源不同：2 项

**需要优先补齐的功能：**
1. 提示词模板系统（实体、Repository、数据库表）
2. loadTemplates() 实现
3. 模板渲染器支持 {{#each}} 循环
4. 人工审核回调机制