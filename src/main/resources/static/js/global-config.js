    let globalConfig = {
      openclawJsonPath: '',
      globalPromptFile: '',
      globalPromptContent: '',
      feishuOpenId: '',
      maxGlobalLoop: 3,
      availableAgents: []
    };

    // 任务配置（每次执行前配置）
    let taskConfig = {
      executionId: null,    // 关联的执行ID
      name: '',
      description: '',
      projectPath: '',
      contextFilePath: ''  // 上下文文件路径（执行时生成）
    };

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

    function renderAvailableAgents() {
      const container = document.getElementById('availableAgents');
      if (globalConfig.availableAgents && globalConfig.availableAgents.length > 0) {
        container.innerHTML = globalConfig.availableAgents.map(a =>
          `<div class="agent-item">
            <span class="agent-id">${a.id}</span>
            <span class="agent-name">${a.name || ''}</span>
          </div>`
        ).join('');
        document.getElementById('agentListSection').style.display = 'block';
      } else {
        container.innerHTML = '<span style="color:#666;">尚未加载 Agent</span>';
        document.getElementById('agentListSection').style.display = 'block';
      }
    }

    // 选择 openclaw.json 文件
    async function selectOpenclawJson() {
      openPathSelectModal({
        mode: 'file',
        targetId: 'openclawJsonPath',
        title: '📁 选择 openclaw.json 配置文件',
        filters: [{ name: 'JSON Files', extensions: ['json'] }],
        showLocalUpload: true,
        localFileAccept: '.json',
        onLocalUpload: async (file) => {
          const reader = new FileReader();
          reader.onload = (ev) => {
            try {
              const json = JSON.parse(ev.target.result);
              parseOpenclawJson(json);
              showToast('success', '已从本地文件加载 Agent');
            } catch (err) {
              showToast('error', '解析 JSON 文件失败');
            }
          };
          reader.readAsText(file);
        },
        onSelect: async (path) => {
          await loadAgentsFromConfig();
        }
      });
    }

    // 从配置文件加载agents
    async function loadAgentsFromConfig() {
      const path = document.getElementById('openclawJsonPath').value.trim();
      if (!path) {
        showToast('warn', '请先输入或选择配置文件路径');
        return;
      }

      try {
        const res = await fetch(`${API}/config/load-agents`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ path })
        });
        const data = await res.json();
        if (data.success) {
          globalConfig.availableAgents = data.agents || [];
          globalConfig.openclawJsonPath = path;
          renderAvailableAgents();
          showToast('success', `已加载 ${globalConfig.availableAgents.length} 个 Agent`);
        } else {
          showToast('error', data.error || '加载失败');
        }
      } catch (e) {
        showToast('error', '加载失败: ' + e.message);
      }
    }

    // 解析 openclaw.json 内容
    function parseOpenclawJson(json) {
      const agents = [];
      if (json.agents) {
        json.agents.forEach(a => agents.push({ id: a.id || a.agentId, name: a.name || a.description || '' }));
      } else if (json.agentIds) {
        json.agentIds.forEach(id => agents.push({ id, name: '' }));
      } else if (Array.isArray(json)) {
        json.forEach(a => agents.push({ id: a.id || a.agentId, name: a.name || '' }));
      }
      globalConfig.availableAgents = agents;
      renderAvailableAgents();
      showToast('success', `已解析 ${agents.length} 个 Agent`);
    }

    // 选择提示词文件
    async function selectPromptFile() {
      openPathSelectModal({
        mode: 'file',
        targetId: 'globalPromptFile',
        title: '📄 选择提示词文件',
        filters: [{ name: 'Text Files', extensions: ['md', 'txt'] }],
        showLocalUpload: true,
        localFileAccept: '.md,.txt',
        onLocalUpload: async (file) => {
          const reader = new FileReader();
          reader.onload = (ev) => {
            document.getElementById('globalPromptContent').value = ev.target.result;
            showToast('success', '已从本地文件加载提示词');
          };
          reader.readAsText(file);
        },
        onSelect: async (path) => {
          await loadGlobalPrompt();
        }
      });
    }

    // 加载全局提示词
    async function loadGlobalPrompt() {
      const path = document.getElementById('globalPromptFile').value.trim();
      if (!path) {
        showToast('warn', '请先输入或选择提示词文件路径');
        return;
      }

      try {
        const res = await fetch(`${API}/config/load-file`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ path })
        });
        const data = await res.json();
        if (data.success) {
          document.getElementById('globalPromptContent').value = data.content;
          showToast('success', '已加载提示词文件');
        } else {
          showToast('error', data.error || '加载失败');
        }
      } catch (e) {
        showToast('error', '加载失败: ' + e.message);
      }
    }

    // 保存全局配置
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

    // 加载保存的全局配置
    function loadSavedGlobalConfig() {
      const saved = localStorage.getItem('openclaw_global_config');
      if (saved) {
        try {
          globalConfig = JSON.parse(saved);
        } catch (e) {
          globalConfig = {};
        }
      }
    }

