// 属性面板渲染

    // 渲染属性面板
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

    // 基础字段（名称、类型）
    function renderBasicFields(node) {
      const isStartOrFinish = node.type === 'start' || node.type === 'finish';
      const nameDisabled = isStartOrFinish ? 'disabled style="background:#222;color:#888;cursor:not-allowed;"' : 'onchange="updateNodeName(this.value)"';
      return `
        <div class="form-section-title">基础信息</div>
        <div class="form-group">
          <label class="form-label">节点名称</label>
          <input type="text" class="form-input" value="${node.name || ''}" ${nameDisabled}>
        </div>
        <div class="form-group">
          <label class="form-label">节点类型</label>
          <select class="form-select" disabled style="background:#222;color:#888;cursor:not-allowed;">
            <option value="agent_execution" ${node.type==='agent_execution'?'selected':''}>Agent 执行</option>
            <option value="api_call" ${node.type==='api_call'?'selected':''}>API 调用</option>
            <option value="start" ${node.type==='start'?'selected':''}>开始</option>
            <option value="finish" ${node.type==='finish'?'selected':''}>结束</option>
            <option value="condition" ${node.type==='condition'?'selected':''}>条件判断</option>
            <option value="human_review" ${node.type==='human_review'?'selected':''}>人工审核</option>
            <option value="parallel" ${node.type==='parallel'?'selected':''}>并行执行</option>
            <option value="loop" ${node.type==='loop'?'selected':''}>循环执行</option>
          </select>
        </div>`;
    }

    // 根据节点类型渲染特定字段
    function renderTypeSpecificFields(node) {
      switch(node.type) {
        case 'agent_execution': return renderAgentFields(node);
        case 'api_call': return renderApiFields(node);
        case 'condition': return renderConditionFields(node);
        case 'loop': return renderLoopFields(node);
        case 'parallel': return renderParallelFields(node);
        default: return '';
      }
    }

    // Agent执行节点字段
    function renderAgentFields(node) {
      return `
        <div class="form-divider"></div>
        <div class="form-section-title">Agent 配置</div>
        <div class="form-group">
          <label class="form-label">Agent ID</label>
          <select class="form-select" id="agentIdSelect" onchange="handleAgentSelect(this)">
            ${getAgentSelectOptions(node.agent_id)}
          </select>
          <input type="text" class="form-input" id="agentIdCustom" style="display:none;margin-top:8px;" placeholder="输入自定义 Agent ID" onchange="updateNode('agent_id',this.value)">
        </div>
        <div class="form-group">
          <label class="form-label">提示词 <button class="ai-gen-btn" onclick="generatePrompt(event)" style="margin-left:8px;padding:2px 8px;font-size:11px;background:linear-gradient(135deg,#6366f1,#8b5cf6);color:#fff;border:none;border-radius:4px;cursor:pointer;">✨ AI生成</button></label>
          <textarea class="form-input form-textarea" id="promptTextarea" onchange="updateNode('prompt',this.value)" placeholder="输入执行提示词...">${node.prompt || ''}</textarea>
        </div>
        <div class="form-group">
          <label class="form-label">超时时间（秒）</label>
          <input type="number" class="form-input" value="${node.timeout || 600}" onchange="updateNode('timeout',parseInt(this.value)||600)" placeholder="默认600秒" min="10" max="3600">
        </div>`;
    }

    // API调用节点字段
    function renderApiFields(node) {
      return `
        <div class="form-divider"></div>
        <div class="form-section-title">API 配置</div>
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
        </div>`;
    }

    // 条件判断节点字段
    function renderConditionFields(node) {
      const edges = getDownstreamEdges(node.id);
      const branchesHtml = edges.length > 0 ? edges.map(edge => {
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
              <label style="font-size:10px;color:#6b5ce7;display:block;margin-bottom:4px;">条件说明</label>
              <textarea class="form-input form-textarea" style="min-height:50px;font-size:12px;"
                placeholder="什么情况下走这个分支"
                onchange="updateBranchConfig('${targetId}', 'conditionDesc', this.value)">${branchConfig.conditionDesc || ''}</textarea>
            </div>
          </div>`;
      }).join('') : '<p style="color:#888;font-size:12px;text-align:center;padding:20px;">暂无出边，请先连线</p>';

      return `
        <div class="form-group">
          <label class="form-label">决策模式</label>
          <select class="form-select" onchange="updateNode('decisionMode',this.value)">
            <option value="agent" ${node.decisionMode==='agent' || !node.decisionMode?'selected':''}>Agent智能决策</option>
            <option value="expression" ${node.decisionMode==='expression'?'selected':''}>表达式判断</option>
          </select>
        </div>
        <div class="config-section" style="margin-top:12px;padding:12px;background:#1a1a1a;border-radius:6px;">
          <div class="config-section-title" style="margin-bottom:8px;">分支连线配置</div>
          <p style="font-size:11px;color:#888;margin-bottom:12px;">为每条出边配置条件说明</p>
          ${branchesHtml}
        </div>
        <div class="form-group" style="margin-top:12px;">
          <label class="form-label">默认分支</label>
          <select class="form-select" onchange="updateNodeAndSave('defaultBranch',this.value)">
            <option value="">自动选择第一个</option>
            ${edges.map(edge => {
              const targetId = edge.targetNodeId || edge.target_node_id;
              const targetNode = state.currentWorkflow.nodes.find(n => n.id === targetId);
              return `<option value="${targetId}" ${node.defaultBranch===targetId?'selected':''}>${targetNode?.name || targetId}</option>`;
            }).join('')}
          </select>
        </div>`;
    }