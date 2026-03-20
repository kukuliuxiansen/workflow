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

