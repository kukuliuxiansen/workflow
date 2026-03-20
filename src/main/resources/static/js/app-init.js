// 应用初始化

    // 加载工作流列表
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

    // 渲染工作流列表
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
    function addLog(type, message) {
      console.log(`[${type}] ${message}`);
    }

    // 初始化应用
    async function initApp() {
      console.log('初始化应用...');

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