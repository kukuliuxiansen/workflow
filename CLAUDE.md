# CLAUDE.md - Workflow Engine Project

## 项目概述

Spring Boot 工作流引擎项目，使用 SQLite 数据库，前端为单页面 HTML 应用。

## 技术栈

- **后端**: Spring Boot 2.7.18, Java 8+, JPA/Hibernate
- **数据库**: SQLite (文件: `workflow.db`)
- **前端**: 原生 HTML/CSS/JS，无框架
- **端口**: 3001
- **API前缀**: `/api`

## 项目结构

```
src/main/java/com/openclaw/workflow/
├── controller/         # REST API 控制器
├── service/           # 业务逻辑层
├── engine/            # 工作流引擎核心
│   ├── handler/       # 节点处理器
│   ├── model/         # 执行上下文模型
│   └── connector/     # 外部连接器
├── entity/            # JPA 实体
├── repository/        # 数据访问层
├── dto/               # 数据传输对象
└── util/              # 工具类

src/main/resources/
├── static/index.html  # 前端单页应用
├── application.yml    # 配置文件
└── schema.sql         # 数据库初始化
```

## 节点类型

| 类型 | 名称 | 说明 |
|------|------|------|
| `start` | 开始节点 | 无输入端口，工作流入口 |
| `finish` | 结束节点 | 无输出端口，工作流出口 |
| `agent_execution` | Agent执行 | 调用外部Agent执行任务 |
| `condition` | 条件判断 | 根据条件选择分支 |
| `parallel` | 并行执行 | 同时执行多个分支 |
| `loop` | 循环执行 | 重复执行直到条件满足 |
| `human_review` | 人工审核 | 等待人工审批 |

## 开发规范

### Git 提交规范

```
feat:     新功能
fix:      Bug修复
style:    样式修改
refactor: 代码重构
docs:     文档更新
test:     测试相关
```

### 代码规范

1. **Java 8 兼容**: 不使用 text blocks (`"""`)、switch 表达式等 Java 9+ 特性
2. **空值检查**: 前端操作 DOM 前必须检查元素是否存在
3. **日志规范**: 每条日志必须有 traceId
4. **API 响应**: 统一使用 `ApiResponse<T>` 包装

### 前端规范

1. **元素检查**: `document.getElementById()` 后检查是否为 null
2. **日志记录**: 使用 `addLog(level, msg, type)` 添加日志
3. **API 调用**: 使用 `${API}` 前缀拼接 URL

## 关键约定

### Agent 节点提示词结构

执行 Agent 节点时，构建的提示词包含：
1. 工作流上下文 (workflowId, executionId, nodeId, projectPath)
2. 任务描述 (task_description)
3. 全局提示 (global_prompt)
4. 节点配置的提示词
5. 上游节点输出
6. 决策提示 (如有下游决策节点)

### 前端请求格式

```json
{
  "input": { "task_description": "...", "task_name": "..." },
  "taskConfig": { "projectPath": "...", "globalPrompt": "..." }
}
```

### 日志系统

- traceId 格式: `TRC-时间戳-随机码`
- 持久化位置: `logs/operation.log`
- API 接口:
  - `GET /api/logs/file` - 获取日志文件
  - `POST /api/logs/save` - 保存前端日志

## 常用命令

```bash
# 编译
mvn compile -q

# 运行
mvn spring-boot:run

# 查看 API 文档
http://localhost:3001/swagger-ui.html
```

## 常用 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/workflows | 工作流列表 |
| GET | /api/workflows/{id} | 工作流详情 |
| POST | /api/workflows/{id}/executions | 启动执行 |
| POST | /api/workflows/{id}/clone | 克隆工作流 |
| DELETE | /api/workflows/{id} | 删除工作流 |
| GET | /api/executions/records | 执行历史 |
| GET | /api/logs/file | 获取日志 |

## 已知问题与解决方案

### Agent 提示词不完整
**问题**: Agent 只收到上游节点输出
**解决**: `StartExecutionRequest.inputData` 添加 `@JsonAlias("input")`

### Bean 定义冲突
**问题**: 多个配置类创建相同 Bean
**解决**: 删除重复配置，使用 `@Autowired` 注入

### 前端空指针异常
**问题**: DOM 元素不存在时报错
**解决**: 操作前检查元素是否为 null

## 注意事项

1. **不要使用 alert/confirm**: 使用 Toast 和模态框替代
2. **不要过度工程**: 简单问题简单解决
3. **保持向后兼容**: 修改 API 时考虑已有调用方