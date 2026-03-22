# SmartDecompose V2 代码与文档差异清单

**审查日期**: 2026-03-21
**更新日期**: 2026-03-22 (核心逻辑已补齐)

---

## 一、提示词模板系统 ✅ 已补齐

| 项目 | 文档设计 | 代码实现 | 状态 |
|------|---------|---------|------|
| 模板存储 | 数据库 prompt_template 表 | PromptTemplate 实体 + Repository | ✅ 已实现 |
| PromptTemplate 实体 | 需创建 | `entity/PromptTemplate.java` | ✅ 已创建 |
| PromptTemplateRepository | 需创建 | `repository/PromptTemplateRepository.java` | ✅ 已创建 |
| loadTemplates() | 从数据库加载 | SmartDecomposeHandler.loadTemplates() | ✅ 已实现 |
| context.decisionTemplate | PromptTemplate 类型 | String 类型（内容） | ✅ 符合设计 |
| PromptTemplateInitializer | 启动时初始化 | `config/PromptTemplateInitializer.java` | ✅ 已创建 |
| 模板文件 | templates/*.md | decision/review/retry-template.md | ✅ 已创建 |

---

## 二、PromptBuilder ✅ 已补齐

| 项目 | 文档设计 | 代码实现 | 状态 |
|------|---------|---------|------|
| 获取模板 | repository.findDefaultByType() | context.getTemplateContent() | ✅ 已实现 |
| 模板循环 {{#each}} | 支持 | PromptBuilder.processLoops() | ✅ 已实现 |
| 重试模板 | 独立 retry 模板 | context.getRetryTemplateContent() | ✅ 已实现 |

---

## 三、人工审核回调机制 ✅ 已补齐

| 项目 | 文档设计 | 代码实现 | 状态 |
|------|---------|---------|------|
| ManualReviewRecord 实体 | 需创建 | `entity/ManualReviewRecord.java` | ✅ 已创建 |
| ManualReviewRepository | 需创建 | `repository/ManualReviewRepository.java` | ✅ 已创建 |
| ManualReviewController | 回调 API | `controller/ManualReviewController.java` | ✅ 已创建 |
| 审核回调 API | POST /api/manual-reviews/{id} | 已实现 | ✅ 已实现 |
| ReviewProcessor 更新 | 创建审核记录 | 已实现 | ✅ 已实现 |

---

## 四、扩展机制 ✅ 已补齐

| 项目 | 文档设计 | 代码实现 | 状态 |
|------|---------|---------|------|
| DecisionHandler 接口 | 需创建 | `extension/DecisionHandler.java` | ✅ 已创建 |
| ReviewStrategy 接口 | 需创建 | `extension/ReviewStrategy.java` | ✅ 已创建 |
| TaskSplitInterceptor | 需创建 | `extension/TaskSplitInterceptor.java` | ✅ 已创建 |
| MaxDepthInterceptor | 深度限制 | `extension/MaxDepthInterceptor.java` | ✅ 已创建 |
| DefaultReviewStrategy | 默认策略 | `extension/DefaultReviewStrategy.java` | ✅ 已创建 |
| ExtensionRegistry | 注册中心 | `extension/ExtensionRegistry.java` | ✅ 已创建 |

---

## 五、监控 API ✅ 已补齐

| API | 文档设计 | 代码实现 | 状态 |
|-----|---------|---------|------|
| GET /api/prompt-templates | 模板列表 | PromptTemplateController | ✅ 已实现 |
| POST /api/prompt-templates | 创建模板 | PromptTemplateController | ✅ 已实现 |
| GET /api/executions/{id}/decisions | 决策历史 | SmartDecomposeMonitorController | ✅ 已实现 |
| GET /api/executions/{id}/decompose-status | 执行状态 | SmartDecomposeMonitorController | ✅ 已实现 |
| GET /api/manual-reviews/waiting | 待审核列表 | ManualReviewController | ✅ 已实现 |

---

## 六、数据库表

### 新建表：
- `prompt_template` - 提示词模板表 ✅
- `manual_review` - 人工审核表 ✅

### 复用旧表：
- `smart_decompose_state` - 执行状态表 ✅
- `decision_history` - 决策历史表 ✅

---

## 总结

**已补齐项目统计：**
- 完全缺失 → 已实现：14 项
- 实现不一致 → 已修正：5 项
- 位置/来源不同 → 已统一：2 项

**新增文件统计：**
- 实体类：2 个（PromptTemplate, ManualReviewRecord）
- Repository：2 个（PromptTemplateRepository, ManualReviewRepository）
- Controller：3 个（ManualReviewController, PromptTemplateController, SmartDecomposeMonitorController）
- 扩展接口：6 个（DecisionHandler, ReviewStrategy, TaskSplitInterceptor, MaxDepthInterceptor, DefaultReviewStrategy, ExtensionRegistry）
- 配置类：1 个（PromptTemplateInitializer）
- DTO：1 个（ManualReviewRequest）
- 模板文件：3 个（decision/review/retry-template.md）

---

## 七、核心逻辑集成 ✅ 已补齐 (2026-03-22)

| 问题 | 文档设计 | 代码实现 | 状态 |
|------|---------|---------|------|
| ExtensionRegistry 注入 | 编排器注入并调用 | DecomposeOrchestrator.autowired | ✅ 已集成 |
| TaskSplitInterceptor 调用 | handleSplit() 中调用 | beforeSplit/afterSplit 已调用 | ✅ 已实现 |
| DecisionHandler 调用 | 决策后调用处理器 | makeDecision() 中调用 | ✅ 已实现 |
| ReviewStrategy 使用 | ReviewProcessor 使用策略 | reviewAndRetry() 中使用 | ✅ 已实现 |
| OpenClaw 会话恢复 | 持久化 + 恢复 sessionId | StatePersister/ManualReviewController | ✅ 已实现 |
| failedTasks 持久化 | 状态表需保存 | SmartDecomposeState 新增字段 | ✅ 已实现 |
| 配置持久化 | maxRetries/requireManualReview | SmartDecomposeState 新增字段 | ✅ 已实现 |

### 新增数据库字段 (smart_decompose_state 表):
- `failed_tasks` - 失败任务列表 JSON
- `openclaw_session_id` - OpenClaw 会话ID
- `max_retries` - 最大重试次数
- `require_manual_review` - 是否需要人工审核
- `manual_review_id` - 当前人工审核ID