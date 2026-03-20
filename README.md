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

### 日志系统-排查问题

- traceId 格式: `TRC-时间戳-随机码`
- 持久化位置: `logs/operation.log`
