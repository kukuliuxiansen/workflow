  const displayExecutionId = document.getElementById('displayExecutionId');
  const displayTaskName = document.getElementById('displayTaskName');
  const displayTaskDesc = document.getElementById('displayTaskDesc');
  const displayProjectPath = document.getElementById('displayProjectPath');
  if (displayExecutionId) displayExecutionId.textContent = taskConfig.executionId || '-';
  if (displayTaskName) displayTaskName.textContent = taskConfig.name || '-';
  if (displayTaskDesc) displayTaskDesc.textContent = taskConfig.description || '-';
  if (displayProjectPath) displayProjectPath.textContent = taskConfig.projectPath || '-';
}

// 保存当前任务配置
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

// 渲染属性面板
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
        <select class="form-select" id="agentIdSelect" onchange="handleAgentSelect(this)">
          ${getAgentSelectOptions(node.agent_id)}
        </select>
        <input type="text" class="form-input" id="agentIdCustom" style="display:none;margin-top:8px;" placeholder="输入自定义 Agent ID" onchange="updateNode('agent_id',this.value)">
      </div>
      <div class="form-group">
        <label class="form-label">提示词 <button class="ai-gen-btn" onclick="generatePrompt(event)">✨ AI生成</button></label>
        <textarea class="form-input form-textarea" id="promptTextarea" onchange="updateNode('prompt',this.value)">${node.prompt || ''}</textarea>
      </div>
    ` : ''}
    ${node.type === 'api_call' ? `
      <div class="form-group">
        <label class="form-label">URL</label>
        <input type="text" class="form-input" value="${node.url || ''}" onchange="updateNode('url',this.value)" placeholder="https://api.example.com">
      </div>
      <div class="form-group">
        <label class="form-label">方法</label>
        <select class="form-select" onchange="updateNode('method',this.value)">
          <option value="GET" ${node.method==='GET'?'selected':''}>GET</option>
          <option value="POST" ${node.method==='POST'?'selected':''}>POST</option>
        </select>
      </div>
    ` : ''}
    ${node.type === 'condition' ? `
      <div class="form-group">
        <label class="form-label">决策模式</label>
        <select class="form-select" onchange="updateNode('decisionMode',this.value)">
          <option value="agent" ${node.decisionMode==='agent' || !node.decisionMode?'selected':''}>Agent智能决策</option>
          <option value="expression" ${node.decisionMode==='expression'?'selected':''}>表达式判断</option>
        </select>
      </div>
      <div class="config-section" style="margin-top:12px;padding:12px;background:#1a1a1a;border-radius:6px;">
        <div class="config-section-title" style="margin-bottom:8px;">🔀 分支连线配置</div>
        <p style="font-size:11px;color:#888;margin-bottom:12px;">为每条出边配置条件说明，Agent会根据这些描述判断走哪条路径</p>
        ${getDownstreamEdges(node.id).length > 0 ? getDownstreamEdges(node.id).map((edge, idx) => {
          const targetId = edge.targetNodeId || edge.target_node_id;
          const targetNode = state.currentWorkflow.nodes.find(n => n.id === targetId);
          const branchConfig = (node.branches || []).find(b => b.targetNodeId === targetId) || {};
          return `
            <div style="background:#252525;border:1px solid #3a3a3a;border-radius:6px;padding:10px;margin-bottom:8px;">
              <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px;">
                <span style="font-size:12px;color:#89b4fa;">→</span>
                <span style="font-size:13px;font-weight:500;">${targetNode?.name || targetId}</span>
                <span style="font-size:10px;color:#888;background:#1a1a1a;padding:2px 6px;border-radius:3px;">${edge.type || edge.edgeType || 'success'}</span>
              </div>
              <div style="background:#1a1a1a;border-radius:4px;padding:8px;">
                <label style="font-size:10px;color:#6b5ce7;display:block;margin-bottom:4px;">✨ 条件说明（什么情况下走这个分支）</label>
                <textarea class="form-input form-textarea" style="min-height:50px;font-size:12px;"
                  placeholder="例如：当测试全部通过时走这个分支"
                  onchange="updateBranchConfig('${targetId}', 'conditionDesc', this.value)">${branchConfig.conditionDesc || ''}</textarea>
              </div>
            </div>
          `;
        }).join('') : '<p style="color:#888;font-size:12px;text-align:center;padding:20px;">暂无出边，请先在画布上拖拽连线到目标节点</p>'}
      </div>
      <div class="form-group" style="margin-top:12px;">
        <label class="form-label">默认分支（无法判断时）</label>
        <select class="form-select" onchange="updateNodeAndSave('defaultBranch',this.value)">
          <option value="">自动选择第一个</option>
          ${getDownstreamEdges(node.id).map(edge => {
