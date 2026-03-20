# 前端代码重构对比报告

## 概述

| 项目 | 数值 |
|------|------|
| 原始 index.html 行数 | 7427 行 |
| 重构后 JS 文件数 | 34 个 |
| 原始函数数量 | 195 个 |
| 当前函数数量 | 175 个 |
| **丢失函数数量** | **35 个** |
| 新增函数数量 | 15 个 |

---

## ⚠️ 关键问题：被调用但丢失的函数

以下函数在当前代码中被调用，但函数本身已丢失，**会导致运行时错误**：

| 函数名 | 调用位置 | 影响 |
|--------|----------|------|
| `addAgentLog` | execution.js | Agent间交互日志无法记录，TypeError |
| `addAgentMessage` | execution.js | Agent消息无法记录，TypeError |
| `renderLogs` | execution-history.js | 日志无法渲染，TypeError |
| `restartExecution` | execution-history.js | 重新执行功能失效，TypeError |

---

## 一、丢失的函数列表 (35个)

以下是原始代码中存在但重构后丢失的函数：

| 序号 | 函数名 | 功能描述 | 是否被调用 |
|------|--------|----------|------------|
| 1 | addAgentLog | Agent间交互日志记录 | **是 - 关键** |
| 2 | addAgentMessage | Agent消息记录 | **是 - 关键** |
| 3 | addNode | 添加节点 | 否 |
| 4 | clearCurrentLogs | 清空当前日志 | 否 |
| 5 | cloneWorkflow | 克隆工作流 | 否 |
| 6 | copyTraceId | 复制TraceID | 否 |
| 7 | createFolder | 创建文件夹 | 否 |
| 8 | createFromTemplate | 从模板创建工作流 | 否 |
| 9 | createWorkflow | 创建工作流 | 否 |
| 10 | deleteFolder | 删除文件夹 | 否 |
| 11 | deleteWorkflow | 删除工作流 | 否 |
| 12 | exportCurrentLogs | 导出当前日志 | 否 |
| 13 | generatePrompt | AI生成提示词 | 否 |
| 14 | generateTraceId | 生成TraceID | 否 |
| 15 | handleFolderDragLeave | 文件夹拖拽离开处理 | 否 |
| 16 | handleFolderDragOver | 文件夹拖拽悬停处理 | 否 |
| 17 | handleFolderDrop | 文件夹拖拽放下处理 | 否 |
| 18 | handleWorkflowDragEnd | 工作流拖拽结束处理 | 否 |
| 19 | handleWorkflowDragStart | 工作流拖拽开始处理 | 否 |
| 20 | onResize | 窗口大小改变处理 | 否 |
| 21 | refreshOperationLogs | 刷新操作日志 | 否 |
| 22 | renameFolder | 重命名文件夹 | 否 |
| 23 | renderApiLogs | 渲染API日志 | 否 |
| 24 | renderLogs | 渲染日志 | **是 - 关键** |
| 25 | renderOperationLogs | 渲染操作日志 | 否 |
| 26 | renderWorkflowItem | 渲染工作流项 | 否 |
| 27 | restartExecution | 重新执行工作流 | **是 - 关键** |
| 28 | saveCurrentTaskConfig | 保存当前任务配置 | 否 |
| 29 | showExecutionDetail | 显示执行详情 | 否 |
| 30 | showTemplateModal | 显示模板选择弹窗 | 否 |
| 31 | stopResize | 停止调整大小 | 否 |
| 32 | switchLogTab | 切换日志标签页 | 否 |
| 33 | toggleFolder | 展开/折叠文件夹 | 否 |
| 34 | updateLogCounts | 更新日志计数 | 否 |
| 35 | viewExecutionRecord | 查看执行记录 | 否 |

---

## 二、新增的函数列表 (15个)

重构后新增的函数：

| 序号 | 函数名 | 说明 |
|------|--------|------|
| 1 | finishBoxSelect | 完成框选 |
| 2 | getNodeOptions | 获取节点选项HTML |
| 3 | initApp | 初始化应用 |
| 4 | initCanvasInteract | 初始化画布交互 |
| 5 | onMouseMove | 鼠标移动处理（重构） |
| 6 | onMouseUp | 鼠标释放处理（重构） |
| 7 | renderAgentFields | 渲染Agent节点字段 |
| 8 | renderApiFields | 渲染API节点字段 |
| 9 | renderBasicFields | 渲染基础字段 |
| 10 | renderCommonFields | 渲染通用字段 |
| 11 | renderConditionFields | 渲染条件节点字段 |
| 12 | renderHistoryDetail | 渲染历史详情 |
| 13 | renderLoopFields | 渲染循环节点字段 |
| 14 | renderParallelFields | 渲染并行节点字段 |
| 15 | renderTypeSpecificFields | 渲染类型特定字段 |

---

## 三、关键丢失函数的完整代码

以下是4个被调用但丢失的关键函数：

### 3.1 addAgentLog

```javascript
function addAgentLog(fromAgent, toAgent, action, detail) {
  const msg = `<span class="agent-from">[${fromAgent}]</span> → <span class="agent-to">[${toAgent}]</span> ${action}: ${detail}`;
  addLog('info', msg, 'agent');
}
```

### 3.2 addAgentMessage

```javascript
function addAgentMessage(agentId, message, messageType = 'response') {
  const icon = messageType === 'response' ? '📤' : messageType === 'request' ? '📥' : '💬';
  const msg = `<span class="agent-name">${icon} ${agentId}</span>: ${message}`;
  addLog('info', msg, 'agent');
}
```

### 3.3 renderLogs

```javascript
    function renderLogs() {
      // 渲染执行日志
      const execContent = document.getElementById('execLogContent');
      if (execContent && state.logs.execution) {
        execContent.innerHTML = state.logs.execution.map(l => {
          const time = l.timestamp || l.time || new Date().toLocaleTimeString();
          const level = l.level || 'info';
          const msg = l.message || l.msg || '';
          return `<div class="log-entry"><span class="log-time">${time}</span><span class="log-level ${level}">[${level.toUpperCase()}]</span><span class="log-message">${msg}</span></div>`;
        }).join('');
        execContent.scrollTop = execContent.scrollHeight;
      }

      // 渲染Agent日志
      const agentContent = document.getElementById('agentLogContent');
      if (agentContent && state.logs.agent) {
        agentContent.innerHTML = state.logs.agent.map(l => {
          const time = l.timestamp || l.time || new Date().toLocaleTimeString();
          const level = l.level || 'info';
          const msg = l.message || l.msg || '';
          const agentId = l.agentId || '';
          const agentPrefix = agentId ? `<span class="agent-name">🤖 ${agentId}</span>: ` : '';
          return `<div class="log-entry"><span class="log-time">${time}</span><span class="log-level ${level}">[${level.toUpperCase()}]</span><span class="log-message">${agentPrefix}${msg}</span></div>`;
        }).join('');
        agentContent.scrollTop = agentContent.scrollHeight;
      }

      updateLogCounts();
    }

    // 生成traceId
```

### 3.4 restartExecution

```javascript
    async function restartExecution(executionId) {
      if (!await confirmAsync('确定要重新执行此任务吗？')) return;

      try {
        const res = await fetch(`${API}/executions/records/${executionId}/restart`, {
          method: 'POST'
        });
        const data = await res.json();
        if (data.success) {
          showToast('success', '已启动重新执行');
          closeHistoryPanel();  // 关闭历史面板

          // 连接到新的执行
          state.execution = data.data;
          taskConfig.contextFilePath = data.data.contextFilePath || '';
          connectWS(data.data.executionId);
```

---

## 四、全部丢失函数的完整代码


============================================================
## addAgentLog
============================================================
    function addAgentLog(fromAgent, toAgent, action, detail) {
      const msg = `<span class="agent-from">[${fromAgent}]</span> → <span class="agent-to">[${toAgent}]</span> ${action}: ${detail}`;
      addLog('info', msg, 'agent');
    }
============================================================
## addAgentMessage
============================================================
    function addAgentMessage(agentId, message, messageType = 'response') {
      const icon = messageType === 'response' ? '📤' : messageType === 'request' ? '📥' : '💬';
      const msg = `<span class="agent-name">${icon} ${agentId}</span>: ${message}`;
      addLog('info', msg, 'agent');
    }
============================================================
## addNode
============================================================
    async function addNode() {
      if (!state.currentWorkflow) {
        showToast('warn', '请先选择工作流');
        return;
      }

      const name = document.getElementById('newNodeName').value || '新节点';
      try {
        await fetch(`${API}/workflows/${state.currentWorkflow.id}/nodes`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            type: state.selectedNodeType,
            name,
            position_x: 100 + ((state.currentWorkflow.nodes?.length || 0) % 3) * 280,
            position_y: 100 + Math.floor((state.currentWorkflow.nodes?.length || 0) / 3) * 180
          })
        });
        closeModal('addNodeModal');
        await selectWorkflow(state.currentWorkflow.id);
        showToast('success', '节点已添加');
      } catch (e) {
        showToast('error', '添加失败');
      }
    }
============================================================
## clearCurrentLogs
============================================================
    function clearCurrentLogs() {
      const tab = state.currentLogTab;
      if (tab === 'operation') {
        // 清空操作日志需要调用API
        fetch(`${API}/logs/recent`, { method: 'DELETE' })
          .then(() => {
            state.operationLogs = [];
            renderOperationLogs([]);
            updateLogCounts();
          });
      } else {
        state.logs[tab] = [];
        const contentId = tab === 'agent' ? 'agentLogContent' : 'execLogContent';
        document.getElementById(contentId).innerHTML = '';
        updateLogCounts();
      }
    }
============================================================
## cloneWorkflow
============================================================
    async function cloneWorkflow(id) {
      const name = prompt('请输入新工作流名称:', '副本');
      if (!name) return;

      try {
        const res = await fetch(`${API}/workflows/${id}/clone`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name })
        });
        const data = await res.json();
        if (data.success) {
          await loadWorkflows();
          await selectWorkflow(data.data.id);
          showToast('success', '工作流已克隆');
        } else {
          showToast('error', data.message || '克隆失败');
        }
      } catch (e) {
        showToast('error', '克隆失败: ' + e.message);
      }
    }
============================================================
## copyTraceId
============================================================
    function copyTraceId(traceId) {
      navigator.clipboard.writeText(traceId).then(() => {
        showToast('success', 'TraceID已复制: ' + traceId);
      }).catch(() => {
        showToast('error', '复制失败');
      });
    }
============================================================
## createFolder
============================================================
    async function createFolder() {
      const name = prompt('请输入文件夹名称:', '新文件夹');
      if (!name) return;

      try {
        const res = await fetch(`${API}/folders`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name })
        });
        const data = await res.json();
        if (data.success) {
          await loadWorkflows();
          showToast('success', '文件夹创建成功');
        } else {
          showToast('error', data.message || '创建失败');
        }
      } catch (e) {
        showToast('error', '创建失败: ' + e.message);
      }
    }
============================================================
## createFromTemplate
============================================================
    async function createFromTemplate(id) {
      const name = prompt('请输入名称:', '新工作流');
      if (!name) return;
      try {
        const res = await fetch(`${API}/templates/${id}/create-workflow`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name })
        });
        const data = await res.json();
        if (data.success) {
          closeModal('templateModal');
          await loadWorkflows();
          await selectWorkflow(data.data.id);
          showToast('success', '创建成功');
        }
      } catch (e) {
        showToast('error', '创建失败');
      }
    }
============================================================
## createWorkflow
============================================================
    async function createWorkflow() {
      const name = document.getElementById('newWorkflowName').value;
      if (!name) { showToast('warn', '请输入名称'); return; }

      const id = 'wf_' + Date.now();
      try {
        await fetch(`${API}/workflows`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            id,
            name,
            description: document.getElementById('newWorkflowDesc').value,
            nodes: [
              { id: 'start', type: 'start', name: '开始', position_x: 100, position_y: 100 },
              { id: 'finish', type: 'finish', name: '结束', position_x: 500, position_y: 100 }
            ]
          })
        });
        closeModal('createModal');
        await loadWorkflows();
        await selectWorkflow(id);
        showToast('success', '创建成功');
      } catch (e) {
        showToast('error', '创建失败');
      }
    }
============================================================
## deleteFolder
============================================================
    async function deleteFolder(folderId) {
      const folder = state.folders.find(f => f.id === folderId);
      if (!folder) return;

      if (!await confirmAsync(`确定要删除文件夹"${folder.name}"吗？\n文件夹内的工作流将移至根目录。`)) return;

      try {
        const res = await fetch(`${API}/folders/${folderId}`, { method: 'DELETE' });
        const data = await res.json();
        if (data.success) {
          await loadWorkflows();
          showToast('success', '文件夹已删除');
        } else {
          showToast('error', data.message || '删除失败');
        }
      } catch (e) {
        showToast('error', '删除失败: ' + e.message);
      }
    }
============================================================
## deleteWorkflow
============================================================
    async function deleteWorkflow(id) {
      if (!await confirmAsync('确定要删除此工作流吗？\n此操作不可恢复。')) return;

      try {
        const res = await fetch(`${API}/workflows/${id}`, { method: 'DELETE' });
        const data = await res.json();
        if (data.success) {
          // 如果删除的是当前工作流，清空选择
          if (state.currentWorkflow?.id === id) {
            state.currentWorkflow = null;
            state.selectedNode = null;
            renderCanvas();
            renderPropertyPanel();
          }
          await loadWorkflows();
          showToast('success', '工作流已删除');
        } else {
          showToast('error', data.message || '删除失败');
        }
      } catch (e) {
        showToast('error', '删除失败: ' + e.message);
      }
    }
============================================================
## exportCurrentLogs
============================================================
    function exportCurrentLogs() {
      const tab = state.currentLogTab;
      const logs = state.logs[tab];
      const text = logs.map(l => `[${l.time}] [${l.level.toUpperCase()}] ${l.msg}`).join('\n');
      const blob = new Blob([text], { type: 'text/plain' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${tab === 'agent' ? 'agent' : 'execution'}_log_${new Date().toISOString().slice(0,10)}.txt`;
      a.click();
      URL.revokeObjectURL(url);
    }
============================================================
## generatePrompt
============================================================
    async function generatePrompt(e) {
      e.preventDefault();
      const btn = e.target;
      const node = state.selectedNode;
      if (!node) return;

      btn.disabled = true;
      btn.textContent = '⏳ 生成中...';

      try {
        const res = await fetch(`${API}/ai/generate-prompt`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            nodeInfo: {
              id: node.id,
              name: node.name,
              type: node.type,
              on_success: node.on_success,
              on_fail: node.on_fail
            },
            workflowContext: {
              name: state.currentWorkflow?.name,
              nodes: state.currentWorkflow?.nodes
            }
          })
        });
        const data = await res.json();
        if (data.success) {
          const textarea = document.getElementById('promptTextarea');
          if (textarea) {
            textarea.value = data.data.prompt;
            updateNode('prompt', data.data.prompt);
          }
          showToast('success', '提示词已生成');
        }
      } catch (e) {
        showToast('error', '生成失败: ' + e.message);
      } finally {
        btn.disabled = false;
        btn.textContent = '✨ AI生成';
      }
    }
============================================================
## generateTraceId
============================================================
    function generateTraceId() {
      return 'TRC-' + Date.now() + '-' + Math.random().toString(16).substr(2, 4).toUpperCase();
    }
============================================================
## handleFolderDragLeave
============================================================
    function handleFolderDragLeave(e) {
      e.currentTarget.classList.remove('drag-over');
    }
============================================================
## handleFolderDragOver
============================================================
    function handleFolderDragOver(e, folderId) {
      e.preventDefault();
      e.stopPropagation();
      if (state.draggedWorkflowId) {
        e.currentTarget.classList.add('drag-over');
      }
    }
============================================================
## handleFolderDrop
============================================================
    async function handleFolderDrop(e, folderId) {
      e.preventDefault();
      e.stopPropagation();
      e.currentTarget.classList.remove('drag-over');

      const workflowId = state.draggedWorkflowId || e.dataTransfer.getData('workflowId');
      if (!workflowId) return;

      // 移动到文件夹或根目录
      const targetFolderId = folderId === 'root' ? null : folderId;

      try {
        const res = await fetch(`${API}/workflows/${workflowId}/move`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ folderId: targetFolderId })
        });
        const data = await res.json();
        if (data.success) {
          // 更新本地状态
          const workflow = state.workflows.find(w => w.id === workflowId);
          if (workflow) {
            workflow.folderId = targetFolderId;
          }
          renderWorkflowList();
          showToast('success', '工作流已移动');
        } else {
          showToast('error', data.message || '移动失败');
        }
      } catch (e) {
        showToast('error', '移动失败: ' + e.message);
      }
    }
============================================================
## handleWorkflowDragEnd
============================================================
    function handleWorkflowDragEnd(e) {
      e.target.style.opacity = '1';
      state.draggedWorkflowId = null;
      // 移除高亮
      document.querySelectorAll('.folder-header').forEach(el => {
        el.style.borderColor = '';
      });
      document.querySelectorAll('.drag-over').forEach(el => {
        el.classList.remove('drag-over');
      });
    }
============================================================
## handleWorkflowDragStart
============================================================
    function handleWorkflowDragStart(e, workflowId) {
      e.dataTransfer.setData('workflowId', workflowId);
      e.dataTransfer.effectAllowed = 'move';
      state.draggedWorkflowId = workflowId;
      e.target.style.opacity = '0.5';
      // 高亮可放置区域
      document.querySelectorAll('.folder-header').forEach(el => {
        el.style.borderColor = '#6b5ce7';
      });
    }
============================================================
## onResize
============================================================
    function onResize(e) {
      if (!resizing) return;
      const panel = document.getElementById(resizing.panelId);

      if (resizing.panelId === 'sidebar') {
        const newWidth = Math.max(200, Math.min(400, e.clientX));
        panel.style.width = newWidth + 'px';
      } else if (resizing.panelId === 'rightPanel') {
        const newWidth = Math.max(250, Math.min(500, window.innerWidth - e.clientX));
        panel.style.width = newWidth + 'px';
      } else if (resizing.panelId === 'logPanel') {
        const newHeight = Math.max(100, Math.min(400, window.innerHeight - e.clientY));
        panel.style.height = newHeight + 'px';
      }
    }
============================================================
## refreshOperationLogs
============================================================
    async function refreshOperationLogs() {
      const typeFilter = document.getElementById('opLogTypeFilter').value;
      const dateFilter = document.getElementById('opLogDateFilter').value;

      try {
        let url = `${API}/logs/file?limit=200`;
        if (dateFilter) {
          url += `&date=${dateFilter}`;
        }

        const res = await fetch(url);
        const data = await res.json();

        if (data.success) {
          let logs = data.data || [];

          // 类型过滤
          if (typeFilter) {
            logs = logs.filter(l => l.type === typeFilter);
          }

          state.operationLogs = logs;
          renderOperationLogs(logs);
          updateLogCounts();
        }
      } catch (e) {
        console.error('获取操作日志失败:', e);
      }
    }
============================================================
## renameFolder
============================================================
    async function renameFolder(folderId) {
      const folder = state.folders.find(f => f.id === folderId);
      if (!folder) return;

      const name = prompt('请输入新的文件夹名称:', folder.name);
      if (!name || name === folder.name) return;

      try {
        const res = await fetch(`${API}/folders/${folderId}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name })
        });
        const data = await res.json();
        if (data.success) {
          await loadWorkflows();
          showToast('success', '文件夹已重命名');
        } else {
          showToast('error', data.message || '重命名失败');
        }
      } catch (e) {
        showToast('error', '重命名失败: ' + e.message);
      }
    }
============================================================
## renderApiLogs
============================================================
    function renderApiLogs() {
      const container = document.getElementById('apiLogContent');
      if (!container) return;

      if (state.logs.api.length === 0) {
        container.innerHTML = '<div style="text-align:center;padding:20px;color:#888;">暂无API日志</div>';
        return;
      }

      const html = state.logs.api.map(l => {
        const statusColor = l.success ? '#4CAF50' : '#f44336';
        const methodColor = l.method === 'GET' ? '#2196F3' : l.method === 'POST' ? '#4CAF50' : l.method === 'DELETE' ? '#f44336' : '#FF9800';
        return `
          <div style="border:1px solid #3a3a3a;border-radius:4px;margin-bottom:8px;background:#1a1a1a;">
            <div style="padding:8px;display:flex;gap:8px;align-items:center;border-bottom:1px solid #3a3a3a;">
              <span style="color:#22d3ee;font-size:10px;cursor:pointer;" onclick="navigator.clipboard.writeText('${l.traceId}')">${l.traceId}</span>
              <span style="color:${methodColor};font-weight:bold;min-width:50px;">${l.method}</span>
              <span style="color:#fff;flex:1;">${l.url}</span>
              <span style="color:${statusColor};">${l.status}</span>
              <span style="color:#888;">${l.duration}</span>
            </div>
            <div style="padding:8px;font-size:11px;">
              <div style="color:#4CAF50;margin-bottom:4px;">Request:</div>
              <pre style="margin:0;white-space:pre-wrap;word-break:break-all;color:#aaa;max-height:100px;overflow:auto;">${escapeHtml(l.request || '(empty)')}</pre>
            </div>
            <div style="padding:8px;font-size:11px;border-top:1px solid #3a3a3a;">
              <div style="color:#2196F3;margin-bottom:4px;">Response:</div>
              <pre style="margin:0;white-space:pre-wrap;word-break:break-all;color:#aaa;max-height:100px;overflow:auto;">${escapeHtml(l.response || '(empty)')}</pre>
            </div>
          </div>
        `;
      }).join('');

      container.innerHTML = html;
    }
============================================================
## renderLogs
============================================================
    function renderLogs() {
      // 渲染执行日志
      const execContent = document.getElementById('execLogContent');
      if (execContent && state.logs.execution) {
        execContent.innerHTML = state.logs.execution.map(l => {
          const time = l.timestamp || l.time || new Date().toLocaleTimeString();
          const level = l.level || 'info';
          const msg = l.message || l.msg || '';
          return `<div class="log-entry"><span class="log-time">${time}</span><span class="log-level ${level}">[${level.toUpperCase()}]</span><span class="log-message">${msg}</span></div>`;
        }).join('');
        execContent.scrollTop = execContent.scrollHeight;
      }

      // 渲染Agent日志
      const agentContent = document.getElementById('agentLogContent');
      if (agentContent && state.logs.agent) {
        agentContent.innerHTML = state.logs.agent.map(l => {
          const time = l.timestamp || l.time || new Date().toLocaleTimeString();
          const level = l.level || 'info';
          const msg = l.message || l.msg || '';
          const agentId = l.agentId || '';
          const agentPrefix = agentId ? `<span class="agent-name">🤖 ${agentId}</span>: ` : '';
          return `<div class="log-entry"><span class="log-time">${time}</span><span class="log-level ${level}">[${level.toUpperCase()}]</span><span class="log-message">${agentPrefix}${msg}</span></div>`;
        }).join('');
        agentContent.scrollTop = agentContent.scrollHeight;
      }

      updateLogCounts();
    }
============================================================
## renderOperationLogs
============================================================
    function renderOperationLogs(logs) {
      const container = document.getElementById('opLogList');
      if (!container) return;

      if (!logs || logs.length === 0) {
        container.innerHTML = '<div class="empty-state" style="text-align:center;padding:20px;color:#888;">暂无日志</div>';
        return;
      }

      const typeColors = {
        'NODE': '#4CAF50',
        'AI': '#2196F3',
        'API': '#FF9800',
        'WORKFLOW': '#9C27B0',
        'ERROR': '#f44336',
        'AI_INPUT': '#2196F3',
        'AI_OUTPUT': '#00BCD4'
      };

      container.innerHTML = logs.map(log => {
        const typeColor = typeColors[log.type] || '#888';
        const successClass = log.success === false ? 'op-log-fail' : '';

        return `
          <div class="op-log-item ${successClass}" style="padding:8px;border-bottom:1px solid #333;font-size:12px;">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px;">
              <span style="background:${typeColor};color:#fff;padding:2px 6px;border-radius:3px;font-size:11px;">${log.type || '-'}</span>
              <span style="color:#888;">${log.timestamp || '-'}</span>
            </div>
            <div style="color:#ccc;margin:4px 0;">${log.operation || '-'}</div>
            ${log.executionId ? `<div style="color:#888;font-size:11px;">执行ID: ${log.executionId}</div>` : ''}
            ${log.nodeId ? `<div style="color:#888;font-size:11px;">节点: ${log.nodeId}</div>` : ''}
            ${log.input ? `<details style="margin-top:4px;"><summary style="cursor:pointer;color:#4CAF50;">输入</summary><pre style="background:#1a1a1a;padding:8px;margin:4px 0;overflow-x:auto;white-space:pre-wrap;word-break:break-all;">${escapeHtml(typeof log.input === 'object' ? JSON.stringify(log.input, null, 2) : log.input)}</pre></details>` : ''}
            ${log.output ? `<details style="margin-top:4px;"><summary style="cursor:pointer;color:#2196F3;">输出</summary><pre style="background:#1a1a1a;padding:8px;margin:4px 0;overflow-x:auto;white-space:pre-wrap;word-break:break-all;">${escapeHtml(typeof log.output === 'object' ? JSON.stringify(log.output, null, 2) : log.output)}</pre></details>` : ''}
            ${log.error ? `<div style="color:#f44336;margin-top:4px;">错误: ${escapeHtml(log.error)}</div>` : ''}
          </div>
        `;
      }).join('');
    }
============================================================
## renderWorkflowItem
============================================================
    function renderWorkflowItem(w) {
      const nodeCount = w.node_count ?? w.nodeCount ?? w.nodes?.length ?? 0;
      return `
        <div class="workflow-item ${state.currentWorkflow?.id === w.id ? 'active' : ''}"
             data-id="${w.id}"
             draggable="true"
             ondragstart="handleWorkflowDragStart(event, '${w.id}')"
             ondragend="handleWorkflowDragEnd(event)"
             onclick="selectWorkflow('${w.id}')">
          <div class="workflow-item-content">
            <div class="name">${w.name}</div>
            <div class="meta">${nodeCount} 个节点 · ${formatDate(w.updated_at || w.updatedAt)}</div>
          </div>
          <div class="workflow-actions">
            <button class="workflow-action-btn" onclick="event.stopPropagation(); cloneWorkflow('${w.id}')" title="克隆">📋</button>
            <button class="workflow-action-btn danger" onclick="event.stopPropagation(); deleteWorkflow('${w.id}')" title="删除">🗑️</button>
          </div>
        </div>
      `;
    }
============================================================
## restartExecution
============================================================
    async function restartExecution(executionId) {
      if (!await confirmAsync('确定要重新执行此任务吗？')) return;

      try {
        const res = await fetch(`${API}/executions/records/${executionId}/restart`, {
          method: 'POST'
        });
        const data = await res.json();
        if (data.success) {
          showToast('success', '已启动重新执行');
          closeHistoryPanel();  // 关闭历史面板

          // 连接到新的执行
          state.execution = data.data;
          taskConfig.contextFilePath = data.data.contextFilePath || '';
          connectWS(data.data.executionId);
          document.getElementById('btnExecute').style.display = 'none';
          document.getElementById('btnStop').style.display = 'inline-flex';
          addLog('info', '重新执行: ' + data.data.executionId);
        }
      } catch (e) {
        showToast('error', '重新执行失败');
      }
    }
============================================================
## saveCurrentTaskConfig
============================================================
    async function saveCurrentTaskConfig() {
      if (!taskConfig.executionId) {
        showToast('warn', '请先选择一条执行记录');
        return;
      }

      try {
        const res = await fetch(`${API}/executions/${taskConfig.executionId}/task-config`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: taskConfig.name,
            description: taskConfig.description,
            projectPath: taskConfig.projectPath,
            workflowId: state.currentWorkflow?.id
          })
        });
        const data = await res.json();
        if (data.success) {
          showToast('success', '任务配置已保存');
        } else {
          showToast('error', data.message || '保存失败');
        }
      } catch (e) {
        showToast('error', '保存失败');
      }
    }
============================================================
## showExecutionDetail
============================================================
    async function showExecutionDetail(id) {
      // 改用新的查看详情函数
      await viewExecutionRecord(id);
    }
============================================================
## showTemplateModal
============================================================
    async function showTemplateModal() {
      document.getElementById('templateModal').classList.add('show');
      const list = document.getElementById('templateList');
      try {
        const res = await fetch(`${API}/templates`);
        const data = await res.json();
        if (data.success && data.data.length > 0) {
          list.innerHTML = data.data.map(t => `
            <div class="template-card" onclick="createFromTemplate('${t.id}')">
              <div class="name">${t.name}</div>
              <div class="desc">${t.description}</div>
              <div class="meta">${t.nodeCount} 个节点</div>
            </div>
          `).join('');
        } else {
          list.innerHTML = '<div class="empty-state"><div class="title">暂无模板</div></div>';
        }
      } catch (e) {
        list.innerHTML = '<div class="empty-state"><div class="title">加载失败</div></div>';
      }
    }
============================================================
## stopResize
============================================================
    function stopResize() {
      if (resizing) {
        // 保存面板大小
        const panel = document.getElementById(resizing.panelId);
        const prefs = getPrefs();
        if (resizing.panelId === 'sidebar') {
          prefs.sidebarWidth = panel.offsetWidth;
        } else if (resizing.panelId === 'rightPanel') {
          prefs.rightPanelWidth = panel.offsetWidth;
        } else if (resizing.panelId === 'logPanel') {
          prefs.logPanelHeight = panel.offsetHeight;
        }
        savePrefs(prefs);
      }
      resizing = null;
      document.removeEventListener('mousemove', onResize);
      document.removeEventListener('mouseup', stopResize);
    }
============================================================
## switchLogTab
============================================================
    function switchLogTab(tab) {
      state.currentLogTab = tab;
      document.querySelectorAll('.log-main-tab').forEach(t => t.classList.remove('active'));
      event.target.closest('.log-main-tab').classList.add('active');

      document.querySelectorAll('.log-content').forEach(c => c.classList.remove('active'));
      let contentId = 'execLogContent';
      if (tab === 'agent') contentId = 'agentLogContent';
      else if (tab === 'api') contentId = 'apiLogContent';
      else if (tab === 'operation') contentId = 'operationLogContent';
      document.getElementById(contentId).classList.add('active');

      // 如果切换到操作日志tab，刷新日志
      if (tab === 'operation') {
        refreshOperationLogs();
      } else if (tab === 'api') {
        renderApiLogs();
      }
    }
============================================================
## toggleFolder
============================================================
    function toggleFolder(folderId) {
      if (state.expandedFolders.has(folderId)) {
        state.expandedFolders.delete(folderId);
      } else {
        state.expandedFolders.add(folderId);
      }
      renderWorkflowList();
    }
============================================================
## updateLogCounts
============================================================
    function updateLogCounts() {
      const execLogCount = document.getElementById('execLogCount');
      const agentLogCount = document.getElementById('agentLogCount');
      const apiLogCount = document.getElementById('apiLogCount');
      const opLogCount = document.getElementById('opLogCount');
      if (execLogCount) execLogCount.textContent = state.logs.execution.length;
      if (agentLogCount) agentLogCount.textContent = state.logs.agent.length;
      if (apiLogCount) apiLogCount.textContent = state.logs.api.length;
      if (opLogCount) opLogCount.textContent = state.operationLogs.length;
    }
============================================================
## viewExecutionRecord
============================================================
    async function viewExecutionRecord(executionId) {
      try {
        const res = await fetch(`${API}/executions/records/${executionId}`);
        const data = await res.json();
        if (data.success) {
          const record = data.data;
          const statusColors = {
            'running': { bg: '#3b82f6', icon: '⚡' },
            'completed': { bg: '#22c55e', icon: '✓' },
            'failed': { bg: '#ef4444', icon: '✕' },
            'stopped': { bg: '#f97316', icon: '◼' }
          };
          const statusStyle = statusColors[record.status] || { bg: '#6b7280', icon: '?' };

          // 创建模态框显示详情
          const modalHtml = `
            <div class="modal-overlay show" onclick="this.remove()">
              <div class="modal" style="background:linear-gradient(145deg, #2d2d2d, #252525);border:1px solid #444;border-radius:16px;box-shadow:0 20px 60px rgba(0,0,0,0.5);min-width:420px;max-width:90vw;overflow:hidden;" onclick="event.stopPropagation()">
                <div style="padding:24px;border-bottom:1px solid #333;display:flex;align-items:center;gap:16px;">
                  <div style="width:48px;height:48px;background:${statusStyle.bg};border-radius:12px;display:flex;align-items:center;justify-content:center;font-size:24px;box-shadow:0 4px 15px ${statusStyle.bg}40;">${statusStyle.icon}</div>
                  <div>
                    <h3 style="margin:0;font-size:18px;color:#fff;">${record.taskConfig?.name || '未命名任务'}</h3>
                    <p style="margin:4px 0 0;font-size:12px;color:#888;">执行详情</p>
                  </div>
                  <button onclick="this.closest('.modal-overlay').remove()" style="margin-left:auto;background:#333;border:none;width:32px;height:32px;border-radius:8px;color:#888;cursor:pointer;font-size:18px;display:flex;align-items:center;justify-content:center;" onmouseover="this.style.background='#444';this.style.color='#fff';" onmouseout="this.style.background='#333';this.style.color='#888';">&times;</button>
                </div>
                <div style="padding:20px;max-height:400px;overflow-y:auto;">
                  <div style="display:grid;gap:12px;">
                    <div style="display:flex;justify-content:space-between;padding:12px;background:#1a1a1a;border-radius:10px;">
                      <span style="color:#888;font-size:13px;">执行ID</span>
                      <code style="color:#89b4fa;font-size:13px;">${record.executionId || record.id}</code>
                    </div>
                    <div style="display:flex;justify-content:space-between;padding:12px;background:#1a1a1a;border-radius:10px;">
                      <span style="color:#888;font-size:13px;">状态</span>
                      <span style="color:${statusStyle.bg};font-weight:500;">${getStatusText(record.status)}</span>
                    </div>
                    <div style="display:flex;justify-content:space-between;padding:12px;background:#1a1a1a;border-radius:10px;">
                      <span style="color:#888;font-size:13px;">项目路径</span>
                      <code style="color:#a78bfa;font-size:12px;max-width:200px;overflow:hidden;text-overflow:ellipsis;">${record.taskConfig?.projectPath || '未设置'}</code>
                    </div>
                    <div style="display:flex;justify-content:space-between;padding:12px;background:#1a1a1a;border-radius:10px;">
                      <span style="color:#888;font-size:13px;">开始时间</span>
                      <span style="color:#e0e0e0;font-size:13px;">${formatDate(record.startTime)}</span>
                    </div>
                    ${record.endTime ? `
                    <div style="display:flex;justify-content:space-between;padding:12px;background:#1a1a1a;border-radius:10px;">
                      <span style="color:#888;font-size:13px;">结束时间</span>
                      <span style="color:#e0e0e0;font-size:13px;">${formatDate(record.endTime)}</span>
                    </div>` : ''}
                  </div>
                  ${record.contextData?.agentPayloads?.length > 0 ? `
                    <div style="margin-top:20px;">
                      <div style="font-size:13px;color:#888;margin-bottom:10px;">Agent执行记录 (${record.contextData.agentPayloads.length}条)</div>
                      <div style="background:#1a1a1a;border-radius:10px;overflow:hidden;">
                        ${record.contextData.agentPayloads.slice(0, 5).map((p, i) => `
                          <div style="padding:10px 12px;${i > 0 ? 'border-top:1px solid #2a2a2a;' : ''}display:flex;align-items:center;gap:10px;">
                            <div style="width:8px;height:8px;background:#22c55e;border-radius:50%;"></div>
                            <span style="color:#89b4fa;font-size:13px;min-width:100px;">${p.agentId}</span>
                            <span style="color:#888;font-size:12px;">${p.nodeName}</span>
                          </div>
                        `).join('')}
                        ${record.contextData.agentPayloads.length > 5 ? `<div style="padding:10px 12px;color:#666;font-size:12px;text-align:center;">还有 ${record.contextData.agentPayloads.length - 5} 条记录</div>` : ''}
                      </div>
                    </div>
                  ` : ''}
                </div>
                <div style="padding:16px 20px;border-top:1px solid #333;display:flex;justify-content:flex-end;">
                  <button onclick="this.closest('.modal-overlay').remove()" style="padding:10px 24px;background:linear-gradient(135deg, #6366f1, #4f46e5);border:none;border-radius:10px;color:#fff;font-size:14px;cursor:pointer;box-shadow:0 4px 15px rgba(99,102,241,0.3);">关闭</button>
                </div>
              </div>
            </div>
          `;

          document.body.insertAdjacentHTML('beforeend', modalHtml);
        }
      } catch (e) {
        showToast('error', '加载详情失败');
      }
    }
---

## 五、修复状态

**修复时间**: 2026-03-21

所有35个丢失函数已通过以下文件恢复：

| 文件 | 函数数 | 行数 |
|------|--------|------|
| log-functions.js | 7 | 77 |
| log-render.js | 4 | 127 |
| workflow-actions.js | 7 | 143 |
| folder-actions.js | 10 | 148 |
| node-exec-actions.js | 7 | 157 |
| ui-utils.js | 7 | 98 |

**验证结果**:
- 原始函数数量: 195
- 修复后函数数量: 210
- 丢失函数数量: 0
- 状态: ✓ 已全部恢复

关键函数验证:
- ✓ addAgentLog - 已恢复
- ✓ addAgentMessage - 已恢复  
- ✓ renderLogs - 已恢复
- ✓ restartExecution - 已恢复

