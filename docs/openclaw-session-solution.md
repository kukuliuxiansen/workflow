# OpenClaw 会话问题解决方案

## 文档信息

| 项目名称 | OpenClaw 会话管理问题解决方案 |
|---------|------------------------------|
| 版本 | 1.0.0 |
| 编写日期 | 2026-03-20 |
| 状态 | 解决方案 |

---

## 一、问题分析

### 1.1 问题描述

每次调用 `openclaw agent` 命令时，不会生成新的会话，而是复用现有会话。这导致在工作流中多次调用同一个 Agent 时，上下文会累积，无法实现隔离。

### 1.2 根本原因

在 OpenClaw 源码中，会话创建逻辑如下：

```javascript
// gateway-cli-CuZs0RlJ.js 第4076-4098行
if (!params.forceNew && entry?.sessionId) {
    const resetPolicy = resolveSessionResetPolicy({
        sessionCfg,
        resetType: "direct"
    });

    // 关键：检查会话是否"新鲜"
    if (evaluateSessionFreshness({
        updatedAt: entry.updatedAt,
        now: params.nowMs,
        policy: resetPolicy
    }).fresh) {
        // 会话新鲜 -> 复用现有会话ID
        sessionId = entry.sessionId;
        isNewSession = false;
    } else {
        // 会话过期 -> 生成新会话ID
        sessionId = crypto.randomUUID();
        isNewSession = true;
    }
} else {
    // forceNew = true -> 强制生成新会话ID
    sessionId = crypto.randomUUID();
    isNewSession = true;
}
```

**核心问题**：
1. `forceNew` 参数未在 `openclaw agent` CLI 中暴露
2. 会话默认策略是复用（只要未过期）
3. 过期策略默认是空闲60分钟或每日重置

### 1.3 会话新鲜度判断逻辑

```javascript
// reply-Bm8VrLQh.js 第28799-28809行
function evaluateSessionFreshness(params) {
    const dailyResetAt = params.policy.mode === "daily"
        ? resolveDailyResetAtMs(params.now, params.policy.atHour)
        : void 0;

    const idleExpiresAt = params.policy.idleMinutes != null
        ? params.updatedAt + params.policy.idleMinutes * 60000
        : void 0;

    const staleDaily = dailyResetAt != null && params.updatedAt < dailyResetAt;
    const staleIdle = idleExpiresAt != null && params.now > idleExpiresAt;

    return {
        fresh: !(staleDaily || staleIdle),  // 都不过期才"新鲜"
        dailyResetAt,
        idleExpiresAt
    };
}
```

---

## 二、解决方案

### 方案一：使用唯一的 `--session-id` 参数（推荐）

**原理**：每次调用时传入不同的 `session-id`，强制创建新会话。

**实现方式**：

```bash
# 方式1：使用时间戳
openclaw agent --agent project-manager \
    --message "执行任务" \
    --session-id "task_$(date +%s)"

# 方式2：使用UUID
openclaw agent --agent project-manager \
    --message "执行任务" \
    --session-id "session_$(uuidgen)"

# 方式3：在工作流中使用组合ID
openclaw agent --agent project-manager \
    --message "执行任务" \
    --session-id "${workflowId}_${executionId}_${nodeId}"
```

**工作流集成**：

```javascript
// agent-connector.js
async function executeAgent(agentId, task, context) {
    // 生成唯一会话ID
    const sessionId = `${context.workflowId}_${context.executionId}_${context.nodeId}_${Date.now()}`;

    const command = `openclaw agent \
        --agent ${agentId} \
        --message "${task}" \
        --session-id "${sessionId}" \
        --json`;

    return execSync(command, { encoding: 'utf8' });
}
```

**优点**：
- 简单直接，无需修改配置
- 完全可控，每次调用都是新会话
- 支持会话追踪和调试

**缺点**：
- 需要手动管理会话ID
- 会话文件会累积（需要定期清理）

---

### 方案二：配置会话立即过期

**原理**：设置 `idleMinutes: 0`，使会话立即过期，下次调用时自动创建新会话。

**配置方式**：

```json
// ~/.openclaw/openclaw.json
{
    "session": {
        "dmScope": "per-channel-peer",
        "idleMinutes": 0,
        "reset": {
            "mode": "idle",
            "idleMinutes": 0
        }
    }
}
```

**优点**：
- 无需修改调用代码
- 自动创建新会话

**缺点**：
- 会影响所有 Agent 的行为
- 不适合需要会话延续的场景
- 可能存在竞态条件

---

### 方案三：使用 Cron 的 `isolated` 模式

**原理**：OpenClaw 的 Cron 功能支持 `sessionTarget: "isolated"`，会设置 `forceNew: true`。

**源码证据**：

```javascript
// gateway-cli-CuZs0RlJ.js 第4303行
const cronSession = resolveCronSession({
    cfg: params.cfg,
    sessionKey: agentSessionKey,
    agentId,
    nowMs: now,
    forceNew: params.job.sessionTarget === "isolated"  // 关键！
});
```

**配置方式**：

```json
// cron 配置
{
    "jobs": [{
        "schedule": "0 9 * * *",
        "agent": "project-manager",
        "message": "每日任务",
        "sessionTarget": "isolated"
    }]
}
```

**优点**：
- 官方支持的隔离模式
- 可靠的会话隔离

**缺点**：
- 仅适用于定时任务场景
- 不适合实时调用

---

### 方案四：删除会话存储文件

**原理**：在调用前删除会话存储文件，强制创建新会话。

**实现方式**：

```bash
#!/bin/bash
# 在调用前清理会话

AGENT_ID="project-manager"
SESSION_STORE="$HOME/.openclaw/agents/${AGENT_ID}/sessions/sessions.json"

# 删除会话存储
rm -f "$SESSION_STORE"

# 调用 Agent
openclaw agent --agent "$AGENT_ID" --message "执行任务"
```

**工作流集成**：

```javascript
async function executeWithFreshSession(agentId, task) {
    const sessionStore = path.join(
        process.env.HOME,
        '.openclaw',
        'agents',
        agentId,
        'sessions',
        'sessions.json'
    );

    // 清理会话存储
    try {
        await fs.unlink(sessionStore);
    } catch (e) {
        // 文件不存在，忽略
    }

    // 调用 Agent
    return execSync(`openclaw agent --agent ${agentId} --message "${task}"`, {
        encoding: 'utf8'
    });
}
```

**优点**：
- 保证干净会话
- 适合测试场景

**缺点**：
- 会丢失所有会话历史
- 需要文件系统访问权限
- 可能影响并发执行

---

### 方案五：修改 Agent 连接器（推荐用于工作流）

**原理**：在现有的 `agent-connector.js` 基础上，实现更可靠的会话管理。

**完整实现**：

```javascript
// sessions-manager.js - 增强版
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

class SessionsManager {
    constructor(basePath = process.env.HOME + '/.openclaw') {
        this.basePath = basePath;
    }

    /**
     * 生成唯一会话ID
     * @param {string} workflowId 工作流ID
     * @param {string} executionId 执行ID
     * @param {string} nodeId 节点ID
     * @returns {string} 唯一会话ID
     */
    generateUniqueSessionId(workflowId, executionId, nodeId) {
        const timestamp = Date.now();
        const random = crypto.randomBytes(4).toString('hex');
        return `${workflowId}_${executionId}_${nodeId}_${timestamp}_${random}`;
    }

    /**
     * 清理指定Agent的所有会话
     * @param {string} agentId Agent ID
     */
    async clearAgentSessions(agentId) {
        const sessionsDir = path.join(
            this.basePath,
            'agents',
            agentId,
            'sessions'
        );

        try {
            const files = await fs.readdir(sessionsDir);
            for (const file of files) {
                if (file.endsWith('.json') || file.endsWith('.jsonl')) {
                    await fs.unlink(path.join(sessionsDir, file));
                }
            }
        } catch (e) {
            // 目录不存在，忽略
        }
    }

    /**
     * 获取会话存储路径
     * @param {string} agentId Agent ID
     * @returns {string} 会话存储路径
     */
    getSessionStorePath(agentId) {
        return path.join(
            this.basePath,
            'agents',
            agentId,
            'sessions',
            'sessions.json'
        );
    }

    /**
     * 删除特定会话
     * @param {string} agentId Agent ID
     * @param {string} sessionKey 会话Key
     */
    async deleteSession(agentId, sessionKey) {
        const storePath = this.getSessionStorePath(agentId);

        try {
            const content = await fs.readFile(storePath, 'utf8');
            const store = JSON.parse(content);

            if (store[sessionKey]) {
                delete store[sessionKey];
                await fs.writeFile(storePath, JSON.stringify(store, null, 2));
            }
        } catch (e) {
            // 文件不存在，忽略
        }
    }
}

module.exports = new SessionsManager();
```

```javascript
// agent-connector.js - 增强版
const { execSync } = require('child_process');
const sessionsManager = require('./sessions-manager');

/**
 * 执行Agent任务（隔离会话）
 * @param {Object} options 执行选项
 * @returns {Object} 执行结果
 */
async function executeAgentIsolated(options) {
    const {
        agentId,
        task,
        workflowId,
        executionId,
        nodeId,
        channel = 'feishu',
        timeout = 600,
        forceNewSession = true
    } = options;

    // 生成唯一会话ID
    const sessionId = forceNewSession
        ? sessionsManager.generateUniqueSessionId(workflowId, executionId, nodeId)
        : `${workflowId}_${executionId}`;

    // 构建命令
    const command = [
        'openclaw agent',
        `--agent ${agentId}`,
        `--message "${escapeShell(task)}"`,
        `--channel ${channel}`,
        `--session-id "${sessionId}"`,
        '--json',
        `--timeout ${timeout}`
    ].join(' ');

    console.log(`[AgentConnector] 执行命令: ${command}`);
    console.log(`[AgentConnector] 会话ID: ${sessionId}`);

    try {
        const startTime = Date.now();
        const result = execSync(command, {
            encoding: 'utf8',
            timeout: timeout * 1000,
            maxBuffer: 10 * 1024 * 1024
        });
        const duration = Date.now() - startTime;

        console.log(`[AgentConnector] 执行完成，耗时: ${duration}ms`);

        return {
            success: true,
            sessionId,
            duration,
            result: parseResult(result)
        };
    } catch (error) {
        console.error(`[AgentConnector] 执行失败: ${error.message}`);
        return {
            success: false,
            sessionId,
            error: error.message
        };
    }
}

/**
 * 转义Shell特殊字符
 */
function escapeShell(str) {
    return str
        .replace(/\\/g, '\\\\')
        .replace(/"/g, '\\"')
        .replace(/\$/g, '\\$')
        .replace(/`/g, '\\`');
}

/**
 * 解析执行结果
 */
function parseResult(rawResult) {
    try {
        return JSON.parse(rawResult);
    } catch (e) {
        return { raw: rawResult };
    }
}

module.exports = {
    executeAgentIsolated,
    sessionsManager
};
```

**使用示例**：

```javascript
const { executeAgentIsolated } = require('./agent-connector');

// 在工作流节点中调用
async function executeNode(node, context) {
    const result = await executeAgentIsolated({
        agentId: node.agentId,
        task: node.prompt,
        workflowId: context.workflowId,
        executionId: context.executionId,
        nodeId: node.id,
        forceNewSession: true  // 强制新会话
    });

    if (result.success) {
        console.log('Agent输出:', result.result);
    } else {
        console.error('执行失败:', result.error);
    }

    return result;
}
```

---

## 三、方案对比

| 方案 | 复杂度 | 可靠性 | 适用场景 | 推荐指数 |
|------|--------|--------|----------|----------|
| 方案一：唯一session-id | ⭐ | ⭐⭐⭐⭐⭐ | 所有场景 | ⭐⭐⭐⭐⭐ |
| 方案二：配置立即过期 | ⭐ | ⭐⭐⭐ | 简单场景 | ⭐⭐⭐ |
| 方案三：Cron隔离模式 | ⭐⭐ | ⭐⭐⭐⭐ | 定时任务 | ⭐⭐⭐ |
| 方案四：删除会话文件 | ⭐⭐ | ⭐⭐⭐ | 测试场景 | ⭐⭐ |
| 方案五：增强Agent连接器 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 工作流引擎 | ⭐⭐⭐⭐⭐ |

---

## 四、推荐实施方案

### 对于工作流引擎

**推荐：方案一 + 方案五 组合**

1. 使用增强版 `sessions-manager.js` 管理会话ID
2. 每次调用时生成唯一会话ID
3. 会话ID格式：`{workflowId}_{executionId}_{nodeId}_{timestamp}_{random}`

**关键配置**：

```javascript
// 工作流引擎调用配置
const agentConfig = {
    forceNewSession: true,  // 总是使用新会话
    sessionPrefix: `${workflowId}_${executionId}`,
    includeTimestamp: true,
    includeRandom: true
};
```

### 对于独立调用

**推荐：方案一**

```bash
# 使用时间戳保证唯一性
openclaw agent --agent project-manager \
    --message "执行任务" \
    --session-id "task_$(date +%Y%m%d%H%M%S)_$RANDOM"
```

---

## 五、会话清理策略

随着时间推移，会话文件会累积。建议实现定期清理：

```javascript
// session-cleaner.js
const fs = require('fs').promises;
const path = require('path');

async function cleanOldSessions(agentId, maxAgeMs = 7 * 24 * 60 * 60 * 1000) {
    const sessionsDir = path.join(
        process.env.HOME,
        '.openclaw',
        'agents',
        agentId,
        'sessions'
    );

    try {
        const files = await fs.readdir(sessionsDir);
        const now = Date.now();

        for (const file of files) {
            const filePath = path.join(sessionsDir, file);
            const stat = await fs.stat(filePath);

            if (now - stat.mtimeMs > maxAgeMs) {
                await fs.unlink(filePath);
                console.log(`已清理旧会话文件: ${file}`);
            }
        }
    } catch (e) {
        console.error('清理会话失败:', e.message);
    }
}

// 每天清理一次
setInterval(() => {
    cleanOldSessions('project-manager');
}, 24 * 60 * 60 * 1000);
```

---

## 六、Java 实现（推荐用于工作流引擎）

### 6.1 核心原理

通过 Gateway 的 OpenAI 兼容 API (`/v1/chat/completions`) 调用 Agent，使用 HTTP Header 控制会话：

```
关键 HTTP Headers:
- x-openclaw-session-key: 直接指定 sessionKey（实现会话隔离）
- x-openclaw-agent-id: 指定目标 Agent
- Authorization: Bearer {token} 认证
```

### 6.2 使用方式

```java
// 1. 创建客户端
OpenClawGatewayClient client = new OpenClawGatewayClient(
    "http://localhost:18789",
    "your-gateway-token"
);

// 2. 执行 Agent（每次都是新会话）
AgentResponse response = client.executeAgent(
    AgentRequest.builder()
        .agentId("project-manager")
        .message("执行任务")
        .context("workflow_123_execution_456_node_789")  // 用于生成唯一 sessionKey
        .build()
);

// 3. 获取结果
System.out.println(response.getContent());
System.out.println("SessionKey: " + response.getSessionKey());
```

### 6.3 关键代码位置

| 文件 | 说明 |
|------|------|
| `OpenClawGatewayClient.java` | Gateway API 客户端 |
| `AgentExecutionHandler.java` | 工作流节点处理器 |
| `GatewayApiExample.java` | 使用示例 |

### 6.4 配置要求

在 `~/.openclaw/openclaw.json` 中启用 chatCompletions 端点：

```json
{
  "gateway": {
    "port": 18789,
    "auth": {
      "mode": "token",
      "token": "your-token"
    },
    "http": {
      "endpoints": {
        "chatCompletions": {
          "enabled": true
        }
      }
    }
  }
}
```

---

## 七、验证方法

### 测试脚本

```bash
#!/bin/bash
# test-session-isolation.sh

AGENT_ID="project-manager"

echo "测试会话隔离..."

# 第一次调用
echo "=== 第一次调用 ==="
RESULT1=$(openclaw agent --agent $AGENT_ID \
    --message "记住数字123" \
    --session-id "test_$(date +%s)_1" \
    --json)
echo "$RESULT1"

sleep 2

# 第二次调用（不同session-id）
echo "=== 第二次调用（新会话） ==="
RESULT2=$(openclaw agent --agent $AGENT_ID \
    --message "我刚才让你记住什么数字？" \
    --session-id "test_$(date +%s)_2" \
    --json)
echo "$RESULT2"

# 如果第二次回答"我没有让你记住任何数字"，说明会话隔离成功
```

### 预期结果

- 每次调用使用不同 `session-id`
- Agent 不应该记住之前会话的内容
- 会话文件独立存储

---

## 七、总结

| 关键点 | 说明 |
|--------|------|
| **根本原因** | OpenClaw 默认复用会话，`forceNew` 参数未在CLI暴露 |
| **最佳方案** | 每次调用传入唯一的 `--session-id` |
| **会话ID格式** | `{workflowId}_{executionId}_{nodeId}_{timestamp}_{random}` |
| **清理策略** | 定期清理超过7天的会话文件 |
| **代码位置** | 增强版 `agent-connector.js` 和 `sessions-manager.js` |