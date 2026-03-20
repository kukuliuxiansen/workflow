
function showTaskConfigModal() {
  // 检查是否选择了工作流
  if (!state.currentWorkflow) {
    showToast('warn', '请先选择一个工作流');
    return;
  }

  // 加载当前任务配置
  document.getElementById('taskName').value = taskConfig.name || '';
  document.getElementById('taskDescription').value = taskConfig.description || '';
  document.getElementById('taskProjectPath').value = taskConfig.projectPath || '';

  document.getElementById('taskConfigModal').classList.add('show');
}

// 选择任务项目路径
async function selectTaskProjectPath() {
  openPathSelectModal({
    mode: 'directory',
    targetId: 'taskProjectPath',
    title: '📁 选择项目目录',
    showLocalUpload: false
  });
}

// 保存任务配置
async function saveTaskConfig() {
  const name = document.getElementById('taskName').value.trim();
  const description = document.getElementById('taskDescription').value.trim();
  const projectPath = document.getElementById('taskProjectPath').value.trim();

  if (!name) {
    showToast('warn', '请输入任务名称');
    return;
  }
  if (!description) {
    showToast('warn', '请输入任务描述');
    return;
  }
  if (!projectPath) {
    showToast('warn', '请输入项目路径');
    return;
  }

  taskConfig = {
    ...taskConfig,
    name,
    description,
    projectPath
  };

  // 保存到工作流数据中
  if (state.currentWorkflow) {
    state.currentWorkflow.taskConfig = {
      name,
      description,
      projectPath
    };
    // 自动保存工作流
    await saveWorkflow(true);
  }

  closeModal('taskConfigModal');
  showToast('success', '任务配置已保存');
  addLog('success', `任务配置: ${name}`);
  updateTaskConfigDisplay();
}

// 获取Agent下拉选项HTML
function getAgentSelectOptions(selectedId) {
  const agents = globalConfig.availableAgents || [];
  let html = '<option value="">请选择 Agent</option>';
  agents.forEach(a => {
    const selected = a.id === selectedId ? 'selected' : '';
    html += `<option value="${a.id}" ${selected}>${a.id}${a.name ? ' - ' + a.name : ''}</option>`;
  });
  // 添加手动输入选项
  if (selectedId && !agents.find(a => a.id === selectedId)) {
    html += `<option value="${selectedId}" selected>${selectedId} (自定义)</option>`;
  }
  html += '<option value="__custom__">✏️ 手动输入...</option>';
  return html;
}

// 处理Agent下拉选择
function handleAgentSelect(select) {
  const customInput = document.getElementById('agentIdCustom');
  if (select.value === '__custom__') {
    select.style.display = 'none';
    customInput.style.display = 'block';
    customInput.focus();
  } else if (select.value) {
    updateNode('agent_id', select.value);
  }
}

// AI 生成工作流
function showAiGenerateModal() {
  document.getElementById('aiWorkflowName').value = '';
  document.getElementById('aiWorkflowDesc').value = '';
  document.getElementById('aiGenerateModal').classList.add('show');
}

function appendAiDesc(text) {
  const textarea = document.getElementById('aiWorkflowDesc');
  if (textarea.value && !textarea.value.endsWith('、') && !textarea.value.endsWith('，')) {
    textarea.value += '、';
  }
  textarea.value += text;
  textarea.focus();
}

async function generateWorkflowWithAI() {
  const description = document.getElementById('aiWorkflowDesc').value.trim();
  const name = document.getElementById('aiWorkflowName').value.trim();

  if (!description) {
    showToast('warn', '请描述您的需求');
    return;
  }

  const btn = document.getElementById('aiGenerateBtn');
  btn.disabled = true;
  btn.textContent = '⏳ 生成中...';

  try {
    const res = await fetch(`${API}/ai/generate-workflow`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ description, name })
    });
    const data = await res.json();

    if (data.success) {
      const workflow = data.data;

      // 保存工作流到后端
      const saveRes = await fetch(`${API}/workflows`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(workflow)
      });
      const saveData = await saveRes.json();

      if (saveData.success) {
        closeModal('aiGenerateModal');
        await loadWorkflows();
        await selectWorkflow(workflow.id);
        showToast('success', '工作流已生成');
        addLog('success', `AI生成工作流: ${workflow.name}`);
      } else {
        showToast('error', '保存失败: ' + (saveData.error || '未知错误'));
      }
    } else {
      showToast('error', '生成失败: ' + (data.error || '未知错误'));
    }
  } catch (e) {
    showToast('error', '生成失败: ' + e.message);
  } finally {
    btn.disabled = false;
    btn.textContent = '✨ 生成工作流';
  }
}

function getIcon(type) {
  return { agent_execution:'🤖', api_call:'🔗', start:'▶️', finish:'⏹️', condition:'❓', human_review:'👤' }[type] || '📦';
}

function getTypeName(type) {
  return { agent_execution:'Agent执行', api_call:'API调用', start:'开始', finish:'结束', condition:'条件判断', human_review:'人工审核', parallel:'并行执行', loop:'循环执行' }[type] || type;
}

function getNodeDesc(node) {
  if (node.type === 'agent_execution') return node.agent_id ? `Agent: ${node.agent_id}` : '未配置 Agent';
  if (node.type === 'api_call') return node.url || '未配置 URL';
  return node.description || '';
}

function getStatusText(s) {
  return { running:'执行中', success:'成功', failed:'失败' }[s] || '';
}

function updateStatus(s) {
  const dot = document.getElementById('statusDot');
  const text = document.getElementById('statusText');
  dot.className = 'status-dot ' + (s === 'idle' ? '' : s);
  text.textContent = { idle:'就绪', running:'执行中', success:'完成', error:'失败' }[s] || s;
}

function formatDate(d) {
  return d ? new Date(d).toLocaleDateString('zh-CN') : '';
}

