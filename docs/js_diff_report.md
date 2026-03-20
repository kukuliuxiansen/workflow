# JS 函数差异详细报告

## 概览
- 原始函数数量: 195
- 当前函数数量: 210
- 实现不同的函数: 39

---
## renderPropertyPanel
- 原始行数: 226
- 当前行数: 14
- 差异: 212 行

### 原始实现
```javascript
function renderPropertyPanel() {
      const panelContent = document.getElementById('propertyPanelContent');
      const node = state.selectedNode;

      if (!node) {
        panelContent.innerHTML = '<div class="empty-state" style="position:relative;top:50%;transform:translateY(-50%);"><div class="icon">📝</div><div class="title">选择节点查看属性</div></div>';
        return;
      }

      const isStartOrFinish = node.type === 'start' || node.type === 'finish';

      panelContent.innerHTML = `
        <div class="form-group">
          <label class="form-label">节点名称</label>
          <input type="text" class="form-input" value="${node.name || ''}" disabled style="background:#1a1a1a;color:#888;cursor:not-allowed;">
          <p style="font-size:11px;color:#666;margin-top:4px;">节点名称创建后不可修改</p>
        </div>
        <div class="form-group">
          <label class="form-label">节点类型</label>
          <select class="form-select" disabled style="background:#1a1a1a;color:#888;cursor:not-allowed;">
            <option value="agent_execution" ${node.type==='agent_execution'?'selected':''}>Agent 执行</option>
            <option value="api_call" ${node.type==='api_call'?'selected':''}>API 调用</option>
            <option value="start" ${node.type==='start'?'selected':''}>开始</option>
            <option value="finish" ${node.type==='finish'?'selected':''}>结束</option>
            <option value="condition" ${node.type==='condition'?'selected':''}>条件判断</option>
            <option value="human_review" ${node.type==='human_review'?'selected':''}>人工审核</option>
            <option value="parallel" ${node.type==='parallel'?'selected':''}>并行执行</option>
            <option value="loop" ${node.type==='loop'?'selected':''}>循环执行</option>
          </select>
          <p style="font-size:11px;color:#666;margin-top:4px;">节点类型创建后不可修改</p>
        </div>
        ${node.type === 'agent_execution' ? `
          <div class="form-group">
            <label class="form-label">Agent ID</label>
            <
... (truncated)
```

### 当前实现
```javascript
function renderPropertyPanel() {
      const panel = document.getElementById('propertyPanelContent');
      const node = state.selectedNode;

      if (!node) {
        panel.innerHTML = '<div class="empty-state" style="position:relative;top:50%;transform:translateY(-50%);"><div class="icon">📝</div><div class="title">选择节点查看属性</div></div>';
        return;
      }

      let html = renderBasicFields(node);
      html += renderTypeSpecificFields(node);
      html += renderCommonFields(node);
      panel.innerHTML = html;
    }
```

---
## initCanvasZoom
- 原始行数: 193
- 当前行数: 27
- 差异: 166 行

### 原始实现
```javascript
function initCanvasZoom() {
      const canvas = document.getElementById('canvas');
      const content = document.getElementById('canvasContent');

      // 鼠标滚轮缩放
      canvas.addEventListener('wheel', (e) => {
        e.preventDefault();
        const delta = e.deltaY > 0 ? -0.1 : 0.1;
        const newZoom = Math.max(0.1, Math.min(4, state.zoom + delta));

        // 以鼠标位置为中心缩放
        const rect = canvas.getBoundingClientRect();
        const mouseX = e.clientX - rect.left;
        const mouseY = e.clientY - rect.top;

        const zoomRatio = newZoom / state.zoom;
        state.panX = mouseX - (mouseX - state.panX) * zoomRatio;
        state.panY = mouseY - (mouseY - state.panY) * zoomRatio;

        state.zoom = newZoom;
        updateCanvasTransform();
        updateMinimap();
        updateZoomLevel();
      });

      // 画布拖拽和框选
      let isPanning = false;
      let isBoxSelecting = false;
      let boxSelectStartX = 0;
      let boxSelectStartY = 0;
      let lastPanX = 0;
      let lastPanY = 0;

      canvas.addEventListener('mousedown', (e) => {
        // 中键或Ctrl+左键: 平移
        if (e.button === 1 || (e.button === 0 && e.ctrlKey)) {
          isPanning = true;
          lastPanX = e.clientX;
          lastPanY = e.clientY;
          canvas.style.cursor = 'grabbing';
          e.preventDefault();
          return;
        }

        // 左键在空白处: 框选
        if (e.button === 0 && !e.target.closest('.node') && !e.target.closest('.edge')) {
          isBoxSelecting = true;
          const rect = canvas.getBoundingClientRect();
          boxSelectStartX = e.clientX - rect.left;
          boxSelectStartY = e.clientY - rect.top;

          // 如果没有按Shift，清除之前的选择
          if (!e.shiftKey) {
            clearNodeSelection();
          }

          const selectionRect = document.getElementById('selectionRect');
          selectionRect.style.left = boxSelectStartX + 'px';
          selectionRect.style.top = boxSelectStartY + 'px';
          selectionRect.style.wid
... (truncated)
```

### 当前实现
```javascript
function initCanvasZoom() {
      const canvas = document.getElementById('canvas');

      // 鼠标滚轮缩放
      canvas.addEventListener('wheel', (e) => {
        e.preventDefault();
        const delta = e.deltaY > 0 ? -0.1 : 0.1;
        const newZoom = Math.max(0.1, Math.min(4, state.zoom + delta));

        // 以鼠标位置为中心缩放
        const rect = canvas.getBoundingClientRect();
        const mouseX = e.clientX - rect.left;
        const mouseY = e.clientY - rect.top;

        const zoomRatio = newZoom / state.zoom;
        state.panX = mouseX - (mouseX - state.panX) * zoomRatio;
        state.panY = mouseY - (mouseY - state.panY) * zoomRatio;

        state.zoom = newZoom;
        updateCanvasTransform();
        updateMinimap();
        updateZoomLevel();
      });

      // 初始化画布交互
      initCanvasInteract();
    }
```

---
## selectHistoryItem
- 原始行数: 110
- 当前行数: 56
- 差异: 54 行

### 原始实现
```javascript
async function selectHistoryItem(executionId) {
      // 更新选中状态
      document.querySelectorAll('.history-item').forEach(el => {
        el.classList.toggle('selected', el.dataset.id === executionId);
      });
      state.selectedHistoryId = executionId;

      // 加载详情
      const content = document.getElementById('historyDetailContent');
      content.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

      try {
        // 并行加载执行记录、日志和任务配置
        const [recordRes, execLogsRes, agentLogsRes, taskConfigRes] = await Promise.all([
          fetch(`${API}/executions/records/${executionId}`),
          fetch(`${API}/executions/${executionId}/logs/execution`),
          fetch(`${API}/executions/${executionId}/logs/agent`),
          fetch(`${API}/executions/${executionId}/task-config`)
        ]);

        const recordData = await recordRes.json();
        const execLogsData = await execLogsRes.json();
        const agentLogsData = await agentLogsRes.json();
        const taskConfigData = await taskConfigRes.json();

        if (recordData.success) {
          const record = recordData.data;

          // 更新日志状态
          if (execLogsData.success) {
            state.logs.execution = execLogsData.data;
          }
          if (agentLogsData.success) {
            state.logs.agent = agentLogsData.data;
          }
          renderLogs();

          // 更新任务配置
          if (taskConfigData.success && taskConfigData.data) {
            taskConfig = {
              executionId: executionId,
              name: taskConfigData.data.name || '',
              description: taskConfigData.data.description || '',
              projectPath: taskConfigData.data.projectPath || '',
              contextFilePath: record.contextFilePath || ''
            };
          } else if (record.taskConfig) {
            taskConfig = {
              executionId: executionId,
              name: record.taskConfig.name || '',
              description: record.taskConfig.descripti
... (truncated)
```

### 当前实现
```javascript
async function selectHistoryItem(executionId) {
      document.querySelectorAll('.history-item').forEach(el => {
        el.classList.toggle('selected', el.dataset.id === executionId);
      });
      state.selectedHistoryId = executionId;

      const content = document.getElementById('historyDetailContent');
      content.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

      try {
        const [recordRes, execLogsRes, agentLogsRes, taskConfigRes] = await Promise.all([
          fetch(`${API}/executions/records/${executionId}`),
          fetch(`${API}/executions/${executionId}/logs/execution`),
          fetch(`${API}/executions/${executionId}/logs/agent`),
          fetch(`${API}/executions/${executionId}/task-config`)
        ]);

        const recordData = await recordRes.json();
        const execLogsData = await execLogsRes.json();
        const agentLogsData = await agentLogsRes.json();
        const taskConfigData = await taskConfigRes.json();

        if (recordData.success) {
          const record = recordData.data;

          if (execLogsData.success) state.logs.execution = execLogsData.data;
          if (agentLogsData.success) state.logs.agent = agentLogsData.data;
          renderLogs();

          if (taskConfigData.success && taskConfigData.data) {
            taskConfig = {
              executionId: executionId,
              name: taskConfigData.data.name || '',
              description: taskConfigData.data.description || '',
              projectPath: taskConfigData.data.projectPath || '',
              contextFilePath: record.contextFilePath || ''
            };
          } else if (record.taskConfig) {
            taskConfig = {
              executionId: executionId,
              name: record.taskConfig.name || '',
              description: record.taskConfig.description || '',
              projectPath: record.taskConfig.projectPath || '',
              contextFilePath: record.contextFilePath || ''
            };
 
... (truncated)
```

---
## renderWorkflowList
- 原始行数: 72
- 当前行数: 23
- 差异: 49 行

### 原始实现
```javascript
function renderWorkflowList() {
      const list = document.getElementById('workflowList');
      const searchInput = document.getElementById('searchInput');
      const search = searchInput ? searchInput.value.toLowerCase() : '';

      // 过滤工作流 - 兼容 snake_case 和 camelCase
      const workflows = state.workflows || [];
      const folders = state.folders || [];
      const filtered = workflows.filter(w =>
        w && w.name && w.id && (w.name.toLowerCase().includes(search) || w.id.toLowerCase().includes(search))
      );

      if (filtered.length === 0 && folders.length === 0) {
        list.innerHTML = '<div class="empty-state"><div class="icon">📭</div><div class="title">暂无工作流</div></div>';
        return;
      }

      let html = '';

      // 渲染文件夹
      folders.forEach(folder => {
        const isExpanded = state.expandedFolders.has(folder.id);
        // 兼容 folder_id 和 folderId
        const folderWorkflows = filtered.filter(w => (w.folderId || w.folder_id) === folder.id);

        html += `
          <div class="folder-item">
            <div class="folder-header ${state.draggedWorkflowId ? 'drag-target' : ''}"
                 data-folder-id="${folder.id}"
                 onclick="toggleFolder('${folder.id}')"
                 ondragover="handleFolderDragOver(event, '${folder.id}')"
                 ondragleave="handleFolderDragLeave(event)"
                 ondrop="handleFolderDrop(event, '${folder.id}')">
              <div class="folder-toggle ${isExpanded ? 'expanded' : ''}">▶</div>
              <span class="folder-icon">📁</span>
              <span class="folder-name">${folder.name}</span>
              <span class="folder-count">${folderWorkflows.length}</span>
              <div class="folder-actions">
                <button class="folder-action-btn" onclick="event.stopPropagation(); renameFolder('${folder.id}')" title="重命名">✏️</button>
                <button class="folder-action-btn danger" onclick="event.stopPropagation(); deleteFolder('${fol
... (truncated)
```

### 当前实现
```javascript
function renderWorkflowList() {
      const list = document.getElementById('workflowList');
      if (!list) return;

      const searchTerm = (state.searchTerm || '').toLowerCase();
      const filtered = state.workflows.filter(w =>
        !searchTerm || w.name.toLowerCase().includes(searchTerm)
      );

      if (filtered.length === 0) {
        list.innerHTML = '<div class="empty-state">暂无工作流</div>';
        return;
      }

      list.innerHTML = filtered.map(w => `
        <div class="workflow-item ${state.currentWorkflow?.id === w.id ? 'active' : ''}"
             onclick="selectWorkflow('${w.id}')">
          <span class="workflow-icon">📄</span>
          <span class="workflow-name">${w.name}</span>
          <span class="workflow-node-count">${w.node_count || 0} 节点</span>
        </div>
      `).join('');
    }
```

---
## viewExecutionRecord
- 原始行数: 79
- 当前行数: 53
- 差异: 26 行

### 原始实现
```javascript
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
             
... (truncated)
```

### 当前实现
```javascript
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

          const modalHtml = `
            <div class="modal-overlay show" onclick="this.remove()">
              <div class="modal" style="min-width:420px;" onclick="event.stopPropagation()">
                <div style="padding:24px;border-bottom:1px solid #333;display:flex;align-items:center;gap:16px;">
                  <div style="width:48px;height:48px;background:${statusStyle.bg};border-radius:12px;display:flex;align-items:center;justify-content:center;font-size:24px;">${statusStyle.icon}</div>
                  <div>
                    <h3 style="margin:0;font-size:18px;">${record.taskConfig?.name || '未命名任务'}</h3>
                    <p style="margin:4px 0 0;font-size:12px;color:#888;">执行详情</p>
                  </div>
                  <button onclick="this.closest('.modal-overlay').remove()" style="margin-left:auto;background:#333;border:none;width:32px;height:32px;border-radius:8px;color:#888;cursor:pointer;font-size:18px;">&times;</button>
                </div>
                <div style="padding:20px;max-height:400px;overflow-y:auto;">
                  <div style="display:grid;gap:12px;">
                    <div style="display:flex;justify-content:space-between;padding:12px;background:#1a1a1a;border-radius:10px;">
                      <span style="color:#888;font-size:13px;">执行ID</span>
                      <code style="color:#89b4fa;">${record.executionId || record.id
... (truncated)
```

---
## loadWorkflows
- 原始行数: 31
- 当前行数: 12
- 差异: 19 行

### 原始实现
```javascript
async function loadWorkflows() {
      console.log('开始加载工作流列表...');
      try {
        const [workflowsRes, foldersRes] = await Promise.all([
          fetch(`${API}/workflows`),
          fetch(`${API}/folders`)
        ]);
        console.log('API响应:', workflowsRes.status, foldersRes.status);
        const workflowsData = await workflowsRes.json();
        const foldersData = await foldersRes.json();
        console.log('工作流数据:', workflowsData);
        console.log('文件夹数据:', foldersData);

        if (workflowsData.success && Array.isArray(workflowsData.data)) {
          state.workflows = workflowsData.data;
        } else {
          state.workflows = [];
        }
        if (foldersData.success && Array.isArray(foldersData.data)) {
          state.folders = foldersData.data;
        } else {
          state.folders = [];
        }
        console.log('开始渲染工作流列表...');
        renderWorkflowList();
        console.log('工作流列表渲染完成');
      } catch (e) {
        console.error('loadWorkflows error:', e);
        alert('加载失败: ' + (e.message || e) + '\n请检查控制台获取详细信息');
      }
    }
```

### 当前实现
```javascript
async function loadWorkflows() {
      try {
        const res = await fetch(`${API}/workflows`);
        const data = await res.json();
        if (data.success) {
          state.workflows = data.data || [];
          renderWorkflowList();
        }
      } catch (e) {
        console.error('加载工作流列表失败:', e);
      }
    }
```

---
## renderEdges
- 原始行数: 91
- 当前行数: 72
- 差异: 19 行

### 原始实现
```javascript
function renderEdges() {
      const svg = document.getElementById('edgesSvg');
      const workflow = state.currentWorkflow;

      if (!svg) return;
      if (!workflow || !workflow.nodes) { svg.innerHTML = ''; return; }

      // 构建节点位置和尺寸映射
      const nodeMap = {};
      workflow.nodes.forEach((node, i) => {
        // 兼容 snake_case 和 camelCase
        const x = node.position_x || node.positionX || 50 + (i % 3) * 280;
        const y = node.position_y || node.positionY || 50 + Math.floor(i / 3) * 180;
        nodeMap[node.id] = { x, y, width: 200, height: 100 };
      });

      let paths = '';

      // 使用 edges 数组渲染连线
      if (workflow.edges && Array.isArray(workflow.edges)) {
        workflow.edges.forEach((edge) => {
          // 支持两种命名方式：camelCase（后端返回）和 snake_case（兼容旧数据）
          const sourceNodeId = edge.sourceNodeId || edge.source_node_id;
          const targetNodeId = edge.targetNodeId || edge.target_node_id;
          const srcNode = nodeMap[sourceNodeId];
          const tgtNode = nodeMap[targetNodeId];

          if (!srcNode || !tgtNode) return;

          // 获取实际节点元素
          const srcEl = document.getElementById(`node-${sourceNodeId}`);
          const tgtEl = document.getElementById(`node-${targetNodeId}`);

          if (srcEl) {
            srcNode.width = srcEl.offsetWidth;
            srcNode.height = srcEl.offsetHeight;
          }
          if (tgtEl) {
            tgtNode.width = tgtEl.offsetWidth;
            tgtNode.height = tgtEl.offsetHeight;
          }

          const edgeType = edge.edgeType || edge.edge_type || 'success';

          // 端口大小为10px，在节点边缘外
          const portRadius = 5;
          const portGap = 2; // 端口间距

          // 源节点右侧端口的Y位置（在node-ports区域内）
          // node-ports padding: 6px 12px，端口在右下角
          const srcPortsCenterY = srcNode.y + srcNode.height - 11; // 端口中心Y

          // 源端口位置：从节点右边沿开始，端口中心
          const x1 = srcNode.x + srcNode.width; // 节点右边沿
          const y1 = srcPortsCenterY + (edgeType === 
... (truncated)
```

### 当前实现
```javascript
function renderEdges() {
      const svg = document.getElementById('edgesSvg');
      const workflow = state.currentWorkflow;

      if (!svg) return;
      if (!workflow || !workflow.nodes) { svg.innerHTML = ''; return; }

      const nodeMap = {};
      workflow.nodes.forEach((node, i) => {
        const x = node.position_x || node.positionX || 50 + (i % 3) * 280;
        const y = node.position_y || node.positionY || 50 + Math.floor(i / 3) * 180;
        nodeMap[node.id] = { x, y, width: 200, height: 100 };
      });

      let paths = '';

      if (workflow.edges && Array.isArray(workflow.edges)) {
        workflow.edges.forEach((edge) => {
          const sourceNodeId = edge.sourceNodeId || edge.source_node_id;
          const targetNodeId = edge.targetNodeId || edge.target_node_id;
          const srcNode = nodeMap[sourceNodeId];
          const tgtNode = nodeMap[targetNodeId];

          if (!srcNode || !tgtNode) return;

          const srcEl = document.getElementById(`node-${sourceNodeId}`);
          const tgtEl = document.getElementById(`node-${targetNodeId}`);

          if (srcEl) {
            srcNode.width = srcEl.offsetWidth;
            srcNode.height = srcEl.offsetHeight;
          }
          if (tgtEl) {
            tgtNode.width = tgtEl.offsetWidth;
            tgtNode.height = tgtEl.offsetHeight;
          }

          const edgeType = edge.edgeType || edge.edge_type || 'success';
          const portRadius = 5;
          const portGap = 2;

          const srcPortsCenterY = srcNode.y + srcNode.height - 11;
          const x1 = srcNode.x + srcNode.width;
          const y1 = srcPortsCenterY + (edgeType === 'success' ? -(portRadius + portGap) : (portRadius + portGap));
          const x2 = tgtNode.x;
          const y2 = tgtNode.y + tgtNode.height - 11;

          const controlOffset = Math.min(100, Math.abs(x2 - x1) / 2);

          const pathType = edge.type || edgeType;
          const typeClass = pathType === 'failure' ? 'failure' :
       
... (truncated)
```

---
## addLog
- 原始行数: 20
- 当前行数: 3
- 差异: 17 行

### 原始实现
```javascript
function addLog(level, msg, type = 'execution') {
      const time = new Date().toLocaleTimeString();
      const traceId = generateTraceId();
      const logEntry = { level, msg, time, type, traceId };

      // 存储到对应类型的日志数组
      if (!state.logs[type]) state.logs[type] = [];
      state.logs[type].push(logEntry);

      // 更新对应的内容区
      const contentId = type === 'agent' ? 'agentLogContent' : 'execLogContent';
      const content = document.getElementById(contentId);
      if (content) {
        content.innerHTML += `<div class="log-entry"><span class="log-time">${time}</span><span class="log-trace" title="点击复制" onclick="copyTraceId('${traceId}')">${traceId}</span><span class="log-level ${level}">[${level.toUpperCase()}]</span><span class="log-message">${msg}</span></div>`;
        content.scrollTop = content.scrollHeight;
      }

      // 更新计数
      updateLogCounts();
    }
```

### 当前实现
```javascript
function addLog(type, message) {
      console.log(`[${type}] ${message}`);
    }
```

---
## renderCanvas
- 原始行数: 98
- 当前行数: 83
- 差异: 15 行

### 原始实现
```javascript
function renderCanvas() {
      const content = document.getElementById('canvasContent');
      const workflow = state.currentWorkflow;

      // 更新统计信息
      updateCanvasInfo();

      if (!workflow || !workflow.nodes || workflow.nodes.length === 0) {
        content.innerHTML = `
          <svg class="edges-svg" id="edgesSvg"></svg>
          <div class="empty-state">
            <div class="icon">📝</div>
            <div class="title">暂无节点</div>
            <div class="desc">双击画布添加节点 · 或点击工具栏"添加节点"</div>
          </div>
        `;
        updateCanvasTransform();
        return;
      }

      let html = '<svg class="edges-svg" id="edgesSvg"></svg>';
      html += '<svg class="edges-svg" id="dragLineSvg" style="z-index:10;"><path id="dragLine" class="drag-line" d="" style="display:none;"/></svg>';

      workflow.nodes.forEach((node, i) => {
        // 兼容 snake_case 和 camelCase
        const x = node.position_x || node.positionX || 50 + (i % 3) * 280;
        const y = node.position_y || node.positionY || 50 + Math.floor(i / 3) * 180;
        const type = node.type || 'agent_execution';
        const status = state.nodeStatus.get(node.id) || '';

        // 多选和高亮
        const isSelected = state.selectedNode?.id === node.id || state.selectedNodes.has(node.id);
        const selectionClass = isSelected ? (state.selectedNodes.size > 1 ? 'selected-multi' : 'selected') : '';

        // 节点颜色
        let nodeColor = '';
        try {
          const config = typeof node.config === 'string' ? JSON.parse(node.config || '{}') : (node.config || {});
          if (config.color) {
            nodeColor = config.color;
          }
        } catch (e) {}

        // 根据节点类型决定端口显示
        const isStart = type === 'start';
        const isFinish = type === 'finish';

        // 输入端口：开始节点不显示
        const inputPortHtml = isStart ? '' : '<div class="port input" title="输入"></div>';

        // 输出端口：结束节点不显示
        const outputPortsHtml = isFinish ? '' : `
          <div style="dis
... (truncated)
```

### 当前实现
```javascript
function renderCanvas() {
      const content = document.getElementById('canvasContent');
      const workflow = state.currentWorkflow;

      updateCanvasInfo();

      if (!workflow || !workflow.nodes || workflow.nodes.length === 0) {
        content.innerHTML = `
          <svg class="edges-svg" id="edgesSvg"></svg>
          <div class="empty-state">
            <div class="icon">📝</div>
            <div class="title">暂无节点</div>
            <div class="desc">双击画布添加节点 · 或点击工具栏"添加节点"</div>
          </div>
        `;
        updateCanvasTransform();
        return;
      }

      let html = '<svg class="edges-svg" id="edgesSvg"></svg>';
      html += '<svg class="edges-svg" id="dragLineSvg" style="z-index:10;"><path id="dragLine" class="drag-line" d="" style="display:none;"/></svg>';

      workflow.nodes.forEach((node, i) => {
        const x = node.position_x || node.positionX || 50 + (i % 3) * 280;
        const y = node.position_y || node.positionY || 50 + Math.floor(i / 3) * 180;
        const type = node.type || 'agent_execution';
        const status = state.nodeStatus.get(node.id) || '';

        const isSelected = state.selectedNode?.id === node.id || state.selectedNodes.has(node.id);
        const selectionClass = isSelected ? (state.selectedNodes.size > 1 ? 'selected-multi' : 'selected') : '';

        let nodeColor = '';
        try {
          const config = typeof node.config === 'string' ? JSON.parse(node.config || '{}') : (node.config || {});
          if (config.color) nodeColor = config.color;
        } catch (e) {}

        const isStart = type === 'start';
        const isFinish = type === 'finish';

        const inputPortHtml = isStart ? '' : '<div class="port input" title="输入"></div>';
        const outputPortsHtml = isFinish ? '' : `
          <div style="display:flex;gap:4px;">
            <div class="port fail" title="失败→拖拽连线" onmousedown="startDragConnect(event,'${node.id}','fail')"></div>
            <div class="port success" title="成功→
... (truncated)
```

---
## exportCanvasAsImage
- 原始行数: 17
- 当前行数: 9
- 差异: 8 行

### 原始实现
```javascript
function exportCanvasAsImage() {
      showToast('info', '正在导出图片...');

      // 使用html2canvas或svg转图片
      const svg = document.getElementById('edgesSvg');
      const content = document.getElementById('canvasContent');

      // 简单实现：创建截图
      const canvas = document.createElement('canvas');
      const rect = content.getBoundingClientRect();
      canvas.width = rect.width;
      canvas.height = rect.height;

      // 这里需要更复杂的实现来捕获SVG和HTML节点
      // 暂时提示用户
      showToast('warn', '图片导出功能开发中，请使用浏览器截图');
    }
```

### 当前实现
```javascript
function exportCanvasAsImage() {
      showToast('info', '正在导出图片...');
      const content = document.getElementById('canvasContent');
      const canvas = document.createElement('canvas');
      const rect = content.getBoundingClientRect();
      canvas.width = rect.width;
      canvas.height = rect.height;
      showToast('warn', '图片导出功能开发中，请使用浏览器截图');
    }
```

---
## refreshOperationLogs
- 原始行数: 29
- 当前行数: 22
- 差异: 7 行

### 原始实现
```javascript
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
```

### 当前实现
```javascript
async function refreshOperationLogs() {
      const typeFilter = document.getElementById('opLogTypeFilter')?.value || '';
      const dateFilter = document.getElementById('opLogDateFilter')?.value || '';

      try {
        let url = `${API}/logs/file?limit=200`;
        if (dateFilter) url += `&date=${dateFilter}`;

        const res = await fetch(url);
        const data = await res.json();

        if (data.success) {
          let logs = data.data || [];
          if (typeFilter) logs = logs.filter(l => l.type === typeFilter);
          state.operationLogs = logs;
          renderOperationLogs(logs);
          updateLogCounts();
        }
      } catch (e) {
        console.error('获取操作日志失败:', e);
      }
    }
```

---
## renderOperationLogs
- 原始行数: 39
- 当前行数: 34
- 差异: 5 行

### 原始实现
```javascript
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
            ${log.output ? `<details style="margin-top:4px;"><summary style="cursor:pointer;color:#2196F3;">输出</summary><pre style="background:#1a1a1a;padding:
... (truncated)
```

### 当前实现
```javascript
function renderOperationLogs(logs) {
      const container = document.getElementById('opLogList');
      if (!container) return;

      if (!logs || logs.length === 0) {
        container.innerHTML = '<div class="empty-state" style="text-align:center;padding:20px;color:#888;">暂无日志</div>';
        return;
      }

      const typeColors = {
        'NODE': '#4CAF50', 'AI': '#2196F3', 'API': '#FF9800',
        'WORKFLOW': '#9C27B0', 'ERROR': '#f44336',
        'AI_INPUT': '#2196F3', 'AI_OUTPUT': '#00BCD4'
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
            ${log.output ? `<details style="margin-top:4px;"><summary style="cursor:pointer;color:#2196F3;">输出</summary><pre style="background:#1a1a1a;padding:8px;margin:4px 0;overflow-x:auto
... (truncated)
```

---
## handleFolderDrop
- 原始行数: 33
- 当前行数: 29
- 差异: 4 行

### 原始实现
```javascript
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
```

### 当前实现
```javascript
async function handleFolderDrop(e, folderId) {
      e.preventDefault();
      e.stopPropagation();
      e.currentTarget.classList.remove('drag-over');

      const workflowId = state.draggedWorkflowId || e.dataTransfer.getData('workflowId');
      if (!workflowId) return;

      const targetFolderId = folderId === 'root' ? null : folderId;

      try {
        const res = await fetch(`${API}/workflows/${workflowId}/move`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ folderId: targetFolderId })
        });
        const data = await res.json();
        if (data.success) {
          const workflow = state.workflows.find(w => w.id === workflowId);
          if (workflow) workflow.folderId = targetFolderId;
          renderWorkflowList();
          showToast('success', '工作流已移动');
        } else {
          showToast('error', data.message || '移动失败');
        }
      } catch (e) {
        showToast('error', '移动失败: ' + e.message);
      }
    }
```

---
## loadLatestExecutionHistory
- 原始行数: 19
- 当前行数: 15
- 差异: 4 行

### 原始实现
```javascript
async function loadLatestExecutionHistory(workflowId) {
      try {
        // 刷新执行历史列表
        await refreshExecutionHistory();

        const res = await fetch(`${API}/executions/records?workflowId=${workflowId}&limit=1`);
        const data = await res.json();
        if (data.success && data.data && data.data.length > 0) {
          const latest = data.data[0];
          // 自动选择最新的执行历史
          await selectHistoryItemMini(latest.executionId || latest.id);
          // 切换到历史标签页
          switchRightPanelTab('history');
        }
        // 不再清空任务配置，保持从工作流加载的配置
      } catch (e) {
        console.error('加载执行历史失败:', e);
      }
    }
```

### 当前实现
```javascript
async function loadLatestExecutionHistory(workflowId) {
      try {
        await refreshExecutionHistory();

        const res = await fetch(`${API}/executions/records?workflowId=${workflowId}&limit=1`);
        const data = await res.json();
        if (data.success && data.data && data.data.length > 0) {
          const latest = data.data[0];
          await selectHistoryItemMini(latest.executionId || latest.id);
          switchRightPanelTab('history');
        }
      } catch (e) {
        console.error('加载执行历史失败:', e);
      }
    }
```

---
## showGlobalConfigModal
- 原始行数: 13
- 当前行数: 9
- 差异: 4 行

### 原始实现
```javascript
function showGlobalConfigModal() {
      // 加载当前配置到表单
      document.getElementById('openclawJsonPath').value = globalConfig.openclawJsonPath || '';
      document.getElementById('globalPromptFile').value = globalConfig.globalPromptFile || '';
      document.getElementById('globalPromptContent').value = globalConfig.globalPromptContent || '';
      document.getElementById('globalFeishuOpenId').value = globalConfig.feishuOpenId || '';
      document.getElementById('globalMaxLoop').value = globalConfig.maxGlobalLoop || 3;

      // 显示已加载的agents
      renderAvailableAgents();

      document.getElementById('globalConfigModal').classList.add('show');
    }
```

### 当前实现
```javascript
function showGlobalConfigModal() {
      document.getElementById('openclawJsonPath').value = globalConfig.openclawJsonPath || '';
      document.getElementById('globalPromptFile').value = globalConfig.globalPromptFile || '';
      document.getElementById('globalPromptContent').value = globalConfig.globalPromptContent || '';
      document.getElementById('globalFeishuOpenId').value = globalConfig.feishuOpenId || '';
      document.getElementById('globalMaxLoop').value = globalConfig.maxGlobalLoop || 3;
      renderAvailableAgents();
      document.getElementById('globalConfigModal').classList.add('show');
    }
```

---
## connectWS
- 原始行数: 35
- 当前行数: 32
- 差异: 3 行

### 原始实现
```javascript
function connectWS(executionId) {
      const ws = new WebSocket(`ws://${location.host}/ws/executions?executionId=${executionId}`);
      ws.onmessage = (e) => {
        const msg = JSON.parse(e.data);
        if (msg.type === 'log') {
          // 根据logType区分日志类型，默认为execution
          const logType = msg.data.logType || 'execution';
          addLog(msg.data.level, msg.data.message, logType);
        }
        else if (msg.type === 'agentMessage') {
          // Agent交互消息
          addAgentMessage(msg.data.agentId, msg.data.message, msg.data.messageType);
        }
        else if (msg.type === 'agentInteraction') {
          // Agent间交互
          addAgentLog(msg.data.fromAgent, msg.data.toAgent, msg.data.action, msg.data.detail);
        }
        else if (msg.type === 'status') {
          if (msg.data.status === 'completed') {
            updateStatus('success');
            addLog('success', '执行完成');
            resetExecution();
          } else if (msg.data.status === 'failed') {
            updateStatus('error');
            addLog('error', '执行失败: ' + (msg.data.error || ''));
            resetExecution();
          }
        } else if (msg.type === 'nodeStatus') {
          state.nodeStatus.set(msg.data.nodeId, msg.data.status);
          renderCanvas();
        }
      };
      ws.onclose = () => addLog('info', '连接关闭');
      state.ws = ws;
    }
```

### 当前实现
```javascript
function connectWS(executionId) {
      const ws = new WebSocket(`ws://${location.host}/ws/executions?executionId=${executionId}`);
      ws.onmessage = (e) => {
        const msg = JSON.parse(e.data);
        if (msg.type === 'log') {
          const logType = msg.data.logType || 'execution';
          addLog(msg.data.level, msg.data.message, logType);
        }
        else if (msg.type === 'agentMessage') {
          addAgentMessage(msg.data.agentId, msg.data.message, msg.data.messageType);
        }
        else if (msg.type === 'agentInteraction') {
          addAgentLog(msg.data.fromAgent, msg.data.toAgent, msg.data.action, msg.data.detail);
        }
        else if (msg.type === 'status') {
          if (msg.data.status === 'completed') {
            updateStatus('success');
            addLog('success', '执行完成');
            resetExecution();
          } else if (msg.data.status === 'failed') {
            updateStatus('error');
            addLog('error', '执行失败: ' + (msg.data.error || ''));
            resetExecution();
          }
        } else if (msg.type === 'nodeStatus') {
          state.nodeStatus.set(msg.data.nodeId, msg.data.status);
          renderCanvas();
        }
      };
      ws.onclose = () => addLog('info', '连接关闭');
      state.ws = ws;
    }
```

---
## escapeHtml
- 原始行数: 6
- 当前行数: 4
- 差异: 2 行

### 原始实现
```javascript
function escapeHtml(text) {
      if (!text) return '';
      const div = document.createElement('div');
      div.textContent = text;
      return div.innerHTML;
    }
```

### 当前实现
```javascript
function escapeHtml(str) {
      if (!str) return '';
      return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }
```

---
## selectWorkflow
- 原始行数: 63
- 当前行数: 61
- 差异: 2 行

### 原始实现
```javascript
async function selectWorkflow(id) {
      console.log('选择工作流:', id);
      try {
        const res = await fetch(`${API}/workflows/${id}`);
        const data = await res.json();
        console.log('工作流数据:', data);
        if (data.success) {
          state.currentWorkflow = data.data;
          state.selectedNode = null;

          // 清空当前日志
          state.logs = { execution: [], agent: [] };
          state.selectedHistoryId = null;

          // 从工作流数据中加载任务配置
          // 后端返回的是 snake_case 格式 (task_config)
          const taskConfigData = state.currentWorkflow.taskConfig || state.currentWorkflow.task_config;
          if (taskConfigData) {
            // 如果是字符串，先解析
            let config = taskConfigData;
            if (typeof config === 'string') {
              try {
                config = JSON.parse(config);
              } catch (e) {
                config = {};
              }
            }
            taskConfig = {
              executionId: null,
              ...config
            };
          } else {
            taskConfig = {
              executionId: null,
              name: '',
              description: '',
              projectPath: '',
              contextFilePath: ''
            };
          }
          updateTaskConfigDisplay();

          renderWorkflowList();
          renderCanvas();
          renderPropertyPanel();
          addLog('info', `已加载: ${data.data.name}`);

          // 保存最后选择的工作流
          const prefs = getPrefs();
          prefs.lastWorkflowId = id;
          savePrefs(prefs);

          // 自动加载该工作流的最新执行历史
          await loadLatestExecutionHistory(id);
        } else {
          console.error('加载工作流失败:', data);
          alert('加载工作流失败: ' + (data.message || '未知错误'));
        }
      } catch (e) {
        console.error('selectWorkflow error:', e);
        alert('加载失败: ' + (e.message || e));
      }
    }
```

### 当前实现
```javascript
async function selectWorkflow(id) {
      console.log('选择工作流:', id);
      try {
        const res = await fetch(`${API}/workflows/${id}`);
        const data = await res.json();
        console.log('工作流数据:', data);
        if (data.success) {
          state.currentWorkflow = data.data;
          state.selectedNode = null;

          // 清空当前日志
          state.logs = { execution: [], agent: [] };
          state.selectedHistoryId = null;

          // 从工作流数据中加载任务配置
          const taskConfigData = state.currentWorkflow.taskConfig || state.currentWorkflow.task_config;
          if (taskConfigData) {
            let config = taskConfigData;
            if (typeof config === 'string') {
              try {
                config = JSON.parse(config);
              } catch (e) {
                config = {};
              }
            }
            taskConfig = {
              executionId: null,
              ...config
            };
          } else {
            taskConfig = {
              executionId: null,
              name: '',
              description: '',
              projectPath: '',
              contextFilePath: ''
            };
          }
          updateTaskConfigDisplay();

          renderWorkflowList();
          renderCanvas();
          renderPropertyPanel();
          addLog('info', `已加载: ${data.data.name}`);

          // 保存最后选择的工作流
          const prefs = getPrefs();
          prefs.lastWorkflowId = id;
          savePrefs(prefs);

          // 自动加载该工作流的最新执行历史
          await loadLatestExecutionHistory(id);
        } else {
          console.error('加载工作流失败:', data);
          alert('加载工作流失败: ' + (data.message || '未知错误'));
        }
      } catch (e) {
        console.error('selectWorkflow error:', e);
        alert('加载失败: ' + (e.message || e));
      }
    }
```

---
## endDragConnect
- 原始行数: 65
- 当前行数: 63
- 差异: 2 行

### 原始实现
```javascript
async function endDragConnect(e) {
      // 移除高亮
      document.querySelectorAll('.port.dragging').forEach(p => p.classList.remove('dragging'));
      document.querySelectorAll('.node.connect-target').forEach(n => n.classList.remove('connect-target'));

      // 隐藏临时线
      const dragLine = document.getElementById('dragLine');
      if (dragLine) dragLine.style.display = 'none';

      // 移除事件监听
      document.removeEventListener('mousemove', onDragConnect);
      document.removeEventListener('mouseup', endDragConnect);

      // 检测目标节点
      if (connectingFrom && state.currentWorkflow) {
        const targetEl = document.elementFromPoint(e.clientX, e.clientY);
        const targetNode = targetEl ? targetEl.closest('.node') : null;

        if (targetNode) {
          const targetId = targetNode.id.replace('node-', '');
          if (targetId !== connectingFrom.nodeId) {
            // 检查目标节点是否是开始节点（开始节点不能有输入连线）
            const targetNodeData = state.currentWorkflow.nodes.find(n => n.id === targetId);
            if (targetNodeData && targetNodeData.type === 'start') {
              showToast('warn', '开始节点不能有输入连线');
              connectingFrom = null;
              isDraggingConnect = false;
              return;
            }

            try {
              addLog('info', `创建连线: ${connectingFrom.nodeId} -> ${targetId} (${connectingFrom.type})`);

              const res = await fetch(`${API}/workflows/${state.currentWorkflow.id}/edges`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                  source: connectingFrom.nodeId,
                  target: targetId,
                  type: connectingFrom.type
                })
              });

              if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
              }

              const data = await res.json();
              if (data.success) {
                await selectWorkflow(state.curren
... (truncated)
```

### 当前实现
```javascript
async function endDragConnect(e) {
      // 移除高亮
      document.querySelectorAll('.port.dragging').forEach(p => p.classList.remove('dragging'));
      document.querySelectorAll('.node.connect-target').forEach(n => n.classList.remove('connect-target'));

      // 隐藏临时线
      const dragLine = document.getElementById('dragLine');
      if (dragLine) dragLine.style.display = 'none';

      // 移除事件监听
      document.removeEventListener('mousemove', onDragConnect);
      document.removeEventListener('mouseup', endDragConnect);

      // 检测目标节点
      if (connectingFrom && state.currentWorkflow) {
        const targetEl = document.elementFromPoint(e.clientX, e.clientY);
        const targetNode = targetEl ? targetEl.closest('.node') : null;

        if (targetNode) {
          const targetId = targetNode.id.replace('node-', '');
          if (targetId !== connectingFrom.nodeId) {
            // 检查目标节点是否是开始节点
            const targetNodeData = state.currentWorkflow.nodes.find(n => n.id === targetId);
            if (targetNodeData && targetNodeData.type === 'start') {
              showToast('warn', '开始节点不能有输入连线');
              connectingFrom = null;
              isDraggingConnect = false;
              return;
            }

            try {
              addLog('info', `创建连线: ${connectingFrom.nodeId} -> ${targetId}`);

              const res = await fetch(`${API}/workflows/${state.currentWorkflow.id}/edges`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                  source: connectingFrom.nodeId,
                  target: targetId,
                  type: connectingFrom.type
                })
              });

              if (!res.ok) throw new Error(`HTTP ${res.status}`);

              const data = await res.json();
              if (data.success) {
                await selectWorkflow(state.currentWorkflow.id);
                showToast('success', '连线创建成功');
         
... (truncated)
```

---
## saveGlobalConfig
- 原始行数: 26
- 当前行数: 24
- 差异: 2 行

### 原始实现
```javascript
async function saveGlobalConfig() {
      globalConfig = {
        openclawJsonPath: document.getElementById('openclawJsonPath').value.trim(),
        globalPromptFile: document.getElementById('globalPromptFile').value.trim(),
        globalPromptContent: document.getElementById('globalPromptContent').value,
        feishuOpenId: document.getElementById('globalFeishuOpenId').value.trim(),
        maxGlobalLoop: parseInt(document.getElementById('globalMaxLoop').value) || 3,
        availableAgents: globalConfig.availableAgents || []
      };

      // 保存到localStorage
      localStorage.setItem('openclaw_global_config', JSON.stringify(globalConfig));

      // 保存到后端
      try {
        await fetch(`${API}/config/global`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(globalConfig)
        });
      } catch (e) {}

      closeModal('globalConfigModal');
      showToast('success', '全局配置已保存');
      addLog('success', '全局配置已更新');
    }
```

### 当前实现
```javascript
async function saveGlobalConfig() {
      globalConfig = {
        openclawJsonPath: document.getElementById('openclawJsonPath').value.trim(),
        globalPromptFile: document.getElementById('globalPromptFile').value.trim(),
        globalPromptContent: document.getElementById('globalPromptContent').value,
        feishuOpenId: document.getElementById('globalFeishuOpenId').value.trim(),
        maxGlobalLoop: parseInt(document.getElementById('globalMaxLoop').value) || 3,
        availableAgents: globalConfig.availableAgents || []
      };

      localStorage.setItem('openclaw_global_config', JSON.stringify(globalConfig));

      try {
        await fetch(`${API}/config/global`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(globalConfig)
        });
      } catch (e) {}

      closeModal('globalConfigModal');
      showToast('success', '全局配置已保存');
      addLog('success', '全局配置已更新');
    }
```

---
## renderApiLogs
- 原始行数: 35
- 当前行数: 34
- 差异: 1 行

### 原始实现
```javascript
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
```

### 当前实现
```javascript
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
          </div>`;
      }).join('');

      container.innerHTML = html;
    }
```

---
## renderWorkflowItem
- 原始行数: 20
- 当前行数: 19
- 差异: 1 行

### 原始实现
```javascript
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
```

### 当前实现
```javascript
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
        </div>`;
    }
```

---
## handleWorkflowDragStart
- 原始行数: 10
- 当前行数: 9
- 差异: 1 行

### 原始实现
```javascript
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
```

### 当前实现
```javascript
function handleWorkflowDragStart(e, workflowId) {
      e.dataTransfer.setData('workflowId', workflowId);
      e.dataTransfer.effectAllowed = 'move';
      state.draggedWorkflowId = workflowId;
      e.target.style.opacity = '0.5';
      document.querySelectorAll('.folder-header').forEach(el => {
        el.style.borderColor = '#6b5ce7';
      });
    }
```

---
## handleWorkflowDragEnd
- 原始行数: 11
- 当前行数: 10
- 差异: 1 行

### 原始实现
```javascript
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
```

### 当前实现
```javascript
function handleWorkflowDragEnd(e) {
      e.target.style.opacity = '1';
      state.draggedWorkflowId = null;
      document.querySelectorAll('.folder-header').forEach(el => {
        el.style.borderColor = '';
      });
      document.querySelectorAll('.drag-over').forEach(el => {
        el.classList.remove('drag-over');
      });
    }
```

---
## deleteWorkflow
- 原始行数: 23
- 当前行数: 22
- 差异: 1 行

### 原始实现
```javascript
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
```

### 当前实现
```javascript
async function deleteWorkflow(id) {
      if (!await confirmAsync('确定要删除此工作流吗？\n此操作不可恢复。')) return;

      try {
        const res = await fetch(`${API}/workflows/${id}`, { method: 'DELETE' });
        const data = await res.json();
        if (data.success) {
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
```

---
## refreshHistoryList
- 原始行数: 31
- 当前行数: 30
- 差异: 1 行

### 原始实现
```javascript
async function refreshHistoryList() {
      const list = document.getElementById('historyList');
      list.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

      try {
        // 根据当前工作流过滤执行历史
        const workflowId = state.currentWorkflow?.id;
        const url = workflowId
          ? `${API}/executions/records?workflowId=${workflowId}`
          : `${API}/executions/records`;

        const res = await fetch(url);
        const data = await res.json();
        if (data.success && data.data.length > 0) {
          state.historyRecords = data.data;
          list.innerHTML = data.data.map(e => `
            <div class="history-item" data-id="${e.executionId || e.id}" onclick="selectHistoryItem('${e.executionId || e.id}')">
              <div class="history-item-title">${e.taskConfig?.name || e.task_name || '未命名任务'}</div>
              <div class="history-item-meta">
                <span class="history-item-status ${e.status}">${getStatusText(e.status)}</span>
                <span>${formatDate(e.startTime || e.created_at)}</span>
              </div>
            </div>
          `).join('');
        } else {
          list.innerHTML = '<div class="empty-state" style="padding:20px;"><div class="title">暂无执行记录</div></div>';
        }
      } catch (e) {
        list.innerHTML = '<div class="empty-state" style="padding:20px;"><div class="title">加载失败</div></div>';
      }
    }
```

### 当前实现
```javascript
async function refreshHistoryList() {
      const list = document.getElementById('historyList');
      list.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

      try {
        const workflowId = state.currentWorkflow?.id;
        const url = workflowId
          ? `${API}/executions/records?workflowId=${workflowId}`
          : `${API}/executions/records`;

        const res = await fetch(url);
        const data = await res.json();
        if (data.success && data.data.length > 0) {
          state.historyRecords = data.data;
          list.innerHTML = data.data.map(e => `
            <div class="history-item" data-id="${e.executionId || e.id}" onclick="selectHistoryItem('${e.executionId || e.id}')">
              <div class="history-item-title">${e.taskConfig?.name || e.task_name || '未命名任务'}</div>
              <div class="history-item-meta">
                <span class="history-item-status ${e.status}">${getStatusText(e.status)}</span>
                <span>${formatDate(e.startTime || e.created_at)}</span>
              </div>
            </div>
          `).join('');
        } else {
          list.innerHTML = '<div class="empty-state" style="padding:20px;"><div class="title">暂无执行记录</div></div>';
        }
      } catch (e) {
        list.innerHTML = '<div class="empty-state" style="padding:20px;"><div class="title">加载失败</div></div>';
      }
    }
```

---
## restartExecution
- 原始行数: 24
- 当前行数: 23
- 差异: 1 行

### 原始实现
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
          document.getElementById('btnExecute').style.display = 'none';
          document.getElementById('btnStop').style.display = 'inline-flex';
          addLog('info', '重新执行: ' + data.data.executionId);
        }
      } catch (e) {
        showToast('error', '重新执行失败');
      }
    }
```

### 当前实现
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
          closeHistoryPanel();

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
```

---
## showExecutionDetail
- 原始行数: 4
- 当前行数: 3
- 差异: 1 行

### 原始实现
```javascript
async function showExecutionDetail(id) {
      // 改用新的查看详情函数
      await viewExecutionRecord(id);
    }
```

### 当前实现
```javascript
async function showExecutionDetail(id) {
      await viewExecutionRecord(id);
    }
```

---
## startDrag
- 原始行数: 34
- 当前行数: 33
- 差异: 1 行

### 原始实现
```javascript
function startDrag(e, id) {
      if (e.button !== 0 || e.altKey) return;
      e.stopPropagation();

      // 如果拖动的节点不在选中集合中，则只选中该节点
      if (!state.selectedNodes.has(id)) {
        state.selectedNodes.clear();
        state.selectedNodes.add(id);
        state.selectedNode = state.currentWorkflow?.nodes?.find(n => n.id === id);
        renderCanvas();
      }

      const el = document.getElementById(`node-${id}`);
      dragging = {
        id,
        startX: e.clientX,
        startY: e.clientY,
        // 记录所有选中节点的初始位置
        initialPositions: {}
      };

      // 记录所有选中节点的初始位置
      state.currentWorkflow?.nodes?.forEach(node => {
        if (state.selectedNodes.has(node.id)) {
          dragging.initialPositions[node.id] = {
            x: node.position_x || 0,
            y: node.position_y || 0
          };
        }
      });

      document.addEventListener('mousemove', onDrag);
      document.addEventListener('mouseup', stopDrag);
    }
```

### 当前实现
```javascript
function startDrag(e, id) {
      if (e.button !== 0 || e.altKey) return;
      e.stopPropagation();

      // 如果拖动的节点不在选中集合中，则只选中该节点
      if (!state.selectedNodes.has(id)) {
        state.selectedNodes.clear();
        state.selectedNodes.add(id);
        state.selectedNode = state.currentWorkflow?.nodes?.find(n => n.id === id);
        renderCanvas();
      }

      const el = document.getElementById(`node-${id}`);
      dragging = {
        id,
        startX: e.clientX,
        startY: e.clientY,
        initialPositions: {}
      };

      // 记录所有选中节点的初始位置
      state.currentWorkflow?.nodes?.forEach(node => {
        if (state.selectedNodes.has(node.id)) {
          dragging.initialPositions[node.id] = {
            x: node.position_x || 0,
            y: node.position_y || 0
          };
        }
      });

      document.addEventListener('mousemove', onDrag);
      document.addEventListener('mouseup', stopDrag);
    }
```

---
## switchLogTab
- 原始行数: 19
- 当前行数: 18
- 差异: 1 行

### 原始实现
```javascript
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
```

### 当前实现
```javascript
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

      if (tab === 'operation') {
        refreshOperationLogs();
      } else if (tab === 'api') {
        renderApiLogs();
      }
    }
```

---
## clearCurrentLogs
- 原始行数: 17
- 当前行数: 16
- 差异: 1 行

### 原始实现
```javascript
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
```

### 当前实现
```javascript
function clearCurrentLogs() {
      const tab = state.currentLogTab;
      if (tab === 'operation') {
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
```

---
## stopResize
- 原始行数: 18
- 当前行数: 17
- 差异: 1 行

### 原始实现
```javascript
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
```

### 当前实现
```javascript
function stopResize() {
      if (resizing) {
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
```

---
## updateTaskConfigDisplay
- 原始行数: 10
- 当前行数: 10
- 差异: 0 行

### 原始实现
```javascript
function updateTaskConfigDisplay() {
      const displayExecutionId = document.getElementById('displayExecutionId');
      const displayTaskName = document.getElementById('displayTaskName');
      const displayTaskDesc = document.getElementById('displayTaskDesc');
      const displayProjectPath = document.getElementById('displayProjectPath');
      if (displayExecutionId) displayExecutionId.textContent = taskConfig.executionId || '-';
      if (displayTaskName) displayTaskName.textContent = taskConfig.name || '-';
      if (displayTaskDesc) displayTaskDesc.textContent = taskConfig.description || '-';
      if (displayProjectPath) displayProjectPath.textContent = taskConfig.projectPath || '-';
    }
```

### 当前实现
```javascript
function updateTaskConfigDisplay() {
      const el = document.getElementById('taskConfigDisplay');
      if (!el) return;

      if (taskConfig && taskConfig.description) {
        el.innerHTML = `<span class="task-desc">${taskConfig.description}</span>`;
      } else {
        el.innerHTML = '<span class="task-placeholder">点击配置任务</span>';
      }
    }
```

---
## confirmAsync
- 原始行数: 5
- 当前行数: 5
- 差异: 0 行

### 原始实现
```javascript
function confirmAsync(message) {
      return new Promise((resolve) => {
        showConfirm(message, () => resolve(true), () => resolve(false));
      });
    }
```

### 当前实现
```javascript
function confirmAsync(message) {
      return new Promise(resolve => {
        showConfirm(message, () => resolve(true), () => resolve(false));
      });
    }
```

---
## updateApiLogCount
- 原始行数: 4
- 当前行数: 6
- 差异: -2 行

### 原始实现
```javascript
function updateApiLogCount() {
      const el = document.getElementById('apiLogCount');
      if (el) el.textContent = state.logs.api.length;
    }
```

### 当前实现
```javascript
function updateApiLogCount() {
      const el = document.getElementById('apiLogCount');
      if (el) {
        el.textContent = state.logs.api.length;
      }
    }
```

---
## togglePanel
- 原始行数: 15
- 当前行数: 18
- 差异: -3 行

### 原始实现
```javascript
function togglePanel(panelId) {
      const panel = document.getElementById(panelId);
      const isCollapsed = panel.classList.toggle('collapsed');

      // 保存偏好
      const prefs = getPrefs();
      prefs.panels[panelId] = !isCollapsed;
      savePrefs(prefs);

      // 更新按钮位置和文本
      updateButtonPositions();

      // 重新渲染画布连线（确保连线位置正确）
      setTimeout(() => renderEdges(), 50);
    }
```

### 当前实现
```javascript
function togglePanel(panelId) {
      const panel = document.getElementById(panelId);
      if (!panel) return;

      const isCollapsed = panel.classList.toggle('collapsed');

      // 保存偏好
      const prefs = getPrefs();
      if (!prefs.panels) prefs.panels = {};
      prefs.panels[panelId] = !isCollapsed;
      savePrefs(prefs);

      // 更新按钮位置和文本
      updateButtonPositions();

      // 重新渲染画布连线（确保连线位置正确）
      setTimeout(() => renderEdges(), 50);
    }
```

---
## loadPanelStates
- 原始行数: 26
- 当前行数: 31
- 差异: -5 行

### 原始实现
```javascript
function loadPanelStates() {
      const prefs = getPrefs();
      Object.entries(prefs.panels).forEach(([panelId, isOpen]) => {
        const panel = document.getElementById(panelId);
        if (!isOpen) {
          panel.classList.add('collapsed');
        }
      });

      // 恢复面板大小
      if (prefs.sidebarWidth) {
        document.getElementById('sidebar').style.width = prefs.sidebarWidth + 'px';
      }
      if (prefs.rightPanelWidth) {
        document.getElementById('rightPanel').style.width = prefs.rightPanelWidth + 'px';
      }
      if (prefs.logPanelHeight) {
        document.getElementById('logPanel').style.height = prefs.logPanelHeight + 'px';
      }

      // 初始化按钮位置
      updateButtonPositions();

      // 初始化画布缩放
      initCanvasZoom();
    }
```

### 当前实现
```javascript
function loadPanelStates() {
      const prefs = getPrefs();
      const panels = prefs.panels || {};
      Object.entries(panels).forEach(([panelId, isOpen]) => {
        const panel = document.getElementById(panelId);
        if (panel && !isOpen) {
          panel.classList.add('collapsed');
        }
      });

      // 恢复面板大小
      const sidebar = document.getElementById('sidebar');
      const rightPanel = document.getElementById('rightPanel');
      const logPanel = document.getElementById('logPanel');

      if (prefs.sidebarWidth && sidebar) {
        sidebar.style.width = prefs.sidebarWidth + 'px';
      }
      if (prefs.rightPanelWidth && rightPanel) {
        rightPanel.style.width = prefs.rightPanelWidth + 'px';
      }
      if (prefs.logPanelHeight && logPanel) {
        logPanel.style.height = prefs.logPanelHeight + 'px';
      }

      // 初始化按钮位置
      updateButtonPositions();

      // 初始化画布缩放
      initCanvasZoom();
    }
```

---
## updateButtonPositions
- 原始行数: 35
- 当前行数: 43
- 差异: -8 行

### 原始实现
```javascript
function updateButtonPositions() {
      const sidebar = document.getElementById('sidebar');
      const rightPanel = document.getElementById('rightPanel');
      const logPanel = document.getElementById('logPanel');
      const sidebarBtn = document.getElementById('toggleSidebarBtn');
      const rightBtn = document.getElementById('toggleRightPanelBtn');
      const logBtn = document.getElementById('toggleLogPanelBtn');

      // 更新侧边栏按钮位置
      if (sidebar.classList.contains('collapsed')) {
        sidebarBtn.style.left = '10px';
        sidebarBtn.textContent = '▶';
      } else {
        sidebarBtn.style.left = sidebar.offsetWidth + 'px';
        sidebarBtn.textContent = '◀';
      }

      // 更新右侧面板按钮位置
      if (rightPanel.classList.contains('collapsed')) {
        rightBtn.style.right = '10px';
        rightBtn.textContent = '◀';
      } else {
        rightBtn.style.right = rightPanel.offsetWidth + 'px';
        rightBtn.textContent = '▶';
      }

      // 更新日志面板按钮位置
      if (logPanel.classList.contains('collapsed')) {
        logBtn.style.bottom = '10px';
        logBtn.textContent = '▲';
      } else {
        logBtn.style.bottom = logPanel.offsetHeight + 'px';
        logBtn.textContent = '▼';
      }
    }
```

### 当前实现
```javascript
function updateButtonPositions() {
      const sidebar = document.getElementById('sidebar');
      const rightPanel = document.getElementById('rightPanel');
      const logPanel = document.getElementById('logPanel');
      const sidebarBtn = document.getElementById('toggleSidebarBtn');
      const rightBtn = document.getElementById('toggleRightPanelBtn');
      const logBtn = document.getElementById('toggleLogPanelBtn');

      if (!sidebar || !rightPanel || !logPanel) return;

      // 更新侧边栏按钮位置
      if (sidebarBtn) {
        if (sidebar.classList.contains('collapsed')) {
          sidebarBtn.style.left = '10px';
          sidebarBtn.textContent = '▶';
        } else {
          sidebarBtn.style.left = sidebar.offsetWidth + 'px';
          sidebarBtn.textContent = '◀';
        }
      }

      // 更新右侧面板按钮位置
      if (rightBtn) {
        if (rightPanel.classList.contains('collapsed')) {
          rightBtn.style.right = '10px';
          rightBtn.textContent = '◀';
        } else {
          rightBtn.style.right = rightPanel.offsetWidth + 'px';
          rightBtn.textContent = '▶';
        }
      }

      // 更新日志面板按钮位置
      if (logBtn) {
        if (logPanel.classList.contains('collapsed')) {
          logBtn.style.bottom = '10px';
          logBtn.textContent = '▲';
        } else {
          logBtn.style.bottom = logPanel.offsetHeight + 'px';
          logBtn.textContent = '▼';
        }
      }
    }
```

---
## startResize
- 原始行数: 7
- 当前行数: 32
- 差异: -25 行

### 原始实现
```javascript
function startResize(e, panelId) {
      if (e.button !== 0) return;
      e.preventDefault();
      resizing = { panelId, startX: e.clientX, startY: e.clientY };
      document.addEventListener('mousemove', onResize);
      document.addEventListener('mouseup', stopResize);
    }
```

### 当前实现
```javascript
function startResize(e, panelId) {
      e.preventDefault();
      const panel = document.getElementById(panelId);
      const startX = e.clientX;
      const startY = e.clientY;
      const startWidth = panel.offsetWidth;
      const startHeight = panel.offsetHeight;

      function onMouseMove(e) {
        if (panelId === 'sidebar') {
          panel.style.width = Math.max(150, startWidth + e.clientX - startX) + 'px';
        } else if (panelId === 'rightPanel') {
          panel.style.width = Math.max(200, startWidth - e.clientX + startX) + 'px';
        } else if (panelId === 'logPanel') {
          panel.style.height = Math.max(100, startHeight - e.clientY + startY) + 'px';
        }
      }

      function onMouseUp() {
        document.removeEventListener('mousemove', onMouseMove);
        document.removeEventListener('mouseup', onMouseUp);
        // 保存面板大小
        const prefs = getPrefs();
        if (panelId === 'sidebar') prefs.sidebarWidth = panel.offsetWidth;
        if (panelId === 'rightPanel') prefs.rightPanelWidth = panel.offsetWidth;
        if (panelId === 'logPanel') prefs.logPanelHeight = panel.offsetHeight;
        savePrefs(prefs);
      }

      document.addEventListener('mousemove', onMouseMove);
      document.addEventListener('mouseup', onMouseUp);
    }
```
