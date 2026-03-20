// 全局配置

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
      executionId: null,
      name: '',
      description: '',
      projectPath: '',
      contextFilePath: ''
    };

    function showGlobalConfigModal() {
      document.getElementById('openclawJsonPath').value = globalConfig.openclawJsonPath || '';
      document.getElementById('globalPromptFile').value = globalConfig.globalPromptFile || '';
      document.getElementById('globalPromptContent').value = globalConfig.globalPromptContent || '';
      document.getElementById('globalFeishuOpenId').value = globalConfig.feishuOpenId || '';
      document.getElementById('globalMaxLoop').value = globalConfig.maxGlobalLoop || 3;
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