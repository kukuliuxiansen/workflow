// 配置文件加载

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