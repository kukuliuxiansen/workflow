// 应用初始化

    // 加载工作流列表
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
        showToast('error', '加载失败: ' + (e.message || e));
      }
    }

    // 渲染工作流列表
    function renderWorkflowList() {
      const list = document.getElementById('workflowList');
      if (!list) return;

      const searchInput = document.getElementById('searchInput');
      const search = searchInput ? searchInput.value.toLowerCase() : '';

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
                <button class="folder-action-btn danger" onclick="event.stopPropagation(); deleteFolder('${folder.id}')" title="删除">🗑️</button>
              </div>
            </div>
            <div class="folder-content ${isExpanded ? 'expanded' : ''}">
              ${folderWorkflows.map(w => renderWorkflowItem(w)).join('')}
            </div>
          </div>`;
      });

      // 渲染根目录工作流
      const rootWorkflows = filtered.filter(w => !(w.folderId || w.folder_id));
      if (rootWorkflows.length > 0) {
        html += `<div class="root-workflows">
          ${rootWorkflows.map(w => renderWorkflowItem(w)).join('')}
        </div>`;
      }

      list.innerHTML = html;
    }

    // 渲染属性面板 - 在 property-panel.js 中定义

    // 更新节点 - 在 node-select.js 中定义

    // 选择节点 - 在 node-select.js 中定义

    // 展开右侧面板 - 在 node-select.js 中定义

    // 收起右侧面板 - 在 node-select.js 中定义

    // 切换右侧面板标签页
    function switchRightPanelTab(tab) {
      const tabs = document.querySelectorAll('.right-panel-tab');
      tabs.forEach(t => t.classList.remove('active'));
      const activeTab = document.querySelector(`.right-panel-tab[data-tab="${tab}"]`);
      if (activeTab) activeTab.classList.add('active');
    }

    // 获取/保存偏好设置
    function getPrefs() {
      try {
        const prefs = JSON.parse(localStorage.getItem('workflowPrefs') || '{}');
        if (!prefs.panels) prefs.panels = {};
        return prefs;
      } catch {
        return { panels: {} };
      }
    }

    function savePrefs(prefs) {
      localStorage.setItem('workflowPrefs', JSON.stringify(prefs));
    }

    // 更新任务配置显示
    function updateTaskConfigDisplay() {
      const el = document.getElementById('taskConfigDisplay');
      if (!el) return;

      if (taskConfig && taskConfig.description) {
        el.innerHTML = `<span class="task-desc">${taskConfig.description}</span>`;
      } else {
        el.innerHTML = '<span class="task-placeholder">点击配置任务</span>';
      }
    }

    // 刷新执行历史
    async function refreshExecutionHistory() {
      // 占位函数
    }

    // 选择历史项
    async function selectHistoryItemMini(id) {
      // 占位函数
    }

    // 添加日志
    function addLog(type, message, logType = 'execution') {
      console.log(`[${type}] ${message}`);
      const timestamp = new Date().toISOString();
      const logEntry = { time: timestamp, level: type, msg: message };

      if (logType === 'agent') {
        if (!state.logs.agent) state.logs.agent = [];
        state.logs.agent.push(logEntry);
      } else {
        if (!state.logs.execution) state.logs.execution = [];
        state.logs.execution.push(logEntry);
      }

      // 更新日志计数
      if (typeof updateLogCounts === 'function') {
        updateLogCounts();
      }
    }

    // 初始化应用
    async function initApp() {
      console.log('初始化应用...');

      // 加载全局配置（从数据库优先，localStorage 作为备份）
      if (typeof loadSavedGlobalConfig === 'function') {
        await loadSavedGlobalConfig();
      }

      // 加载面板状态
      loadPanelStates();

      // 加载工作流列表
      await loadWorkflows();

      // 恢复上次选择的工作流
      const prefs = getPrefs();
      if (prefs.lastWorkflowId) {
        const exists = state.workflows.find(w => w.id === prefs.lastWorkflowId);
        if (exists) {
          await selectWorkflow(prefs.lastWorkflowId);
        }
      }

      // 初始化UX功能
      if (typeof initUXFeatures === 'function') {
        initUXFeatures();
      }

      console.log('应用初始化完成');
    }

    // 页面加载后初始化
    document.addEventListener('DOMContentLoaded', initApp);