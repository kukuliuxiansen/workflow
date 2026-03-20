            const targetId = edge.targetNodeId || edge.target_node_id;
            const targetNode = state.currentWorkflow.nodes.find(n => n.id === targetId);
            return `<option value="${targetId}" ${node.defaultBranch===targetId?'selected':''}>${targetNode?.name || targetId}</option>`;
          }).join('')}
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">自定义提示词（可选）</label>
        <textarea class="form-input form-textarea" style="min-height:60px;font-size:12px;" placeholder="添加额外的提示信息给Agent" onchange="updateNodeAndSave('customPrompt',this.value)">${node.customPrompt || ''}</textarea>
      </div>
    ` : ''}
    ${node.type === 'loop' ? `
      <div class="form-group">
        <label class="form-label">循环类型</label>
        <select class="form-select" onchange="updateNode('loop_type',this.value)">
          <option value="count" ${node.loop_type==='count' || !node.loop_type?'selected':''}>计数循环</option>
          <option value="condition" ${node.loop_type==='condition'?'selected':''}>条件循环</option>
          <option value="foreach" ${node.loop_type==='foreach'?'selected':''}>遍历循环</option>
        </select>
      </div>
      ${node.loop_type === 'count' || !node.loop_type ? `
        <div class="form-group">
          <label class="form-label">循环次数</label>
          <input type="number" class="form-input" value="${node.count || 1}" onchange="updateNode('count',parseInt(this.value))" min="1">
        </div>
      ` : ''}
      ${node.loop_type === 'condition' ? `
        <div class="form-group">
          <label class="form-label">循环条件</label>
          <input type="text" class="form-input" value="${node.condition || ''}" onchange="updateNode('condition',this.value)" placeholder="$node1.status === 'success'">
        </div>
        <div class="form-group">
          <label class="form-label">循环模式</label>
          <select class="form-select" onchange="updateNode('loop_mode',this.value)">
            <option value="while" ${node.loop_mode==='while' || !node.loop_mode?'selected':''}>While (条件为真继续)</option>
            <option value="until" ${node.loop_mode==='until'?'selected':''}>Until (条件为真停止)</option>
          </select>
        </div>
        <div class="form-group">
          <div style="background:#1a1a1a;border-radius:4px;padding:8px;">
            <label style="font-size:10px;color:#6b5ce7;display:block;margin-bottom:4px;">✨ 退出条件说明（给Agent看）</label>
            <textarea class="form-input form-textarea" style="min-height:60px;font-size:11px;" placeholder="例如：当所有文件都处理完成，或遇到错误时退出" onchange="updateNode('exitCondition',this.value)">${node.exitCondition || ''}</textarea>
          </div>
        </div>
      ` : ''}
      ${node.loop_type === 'foreach' ? `
        <div class="form-group">
          <label class="form-label">遍历数组 (变量或JSON)</label>
          <textarea class="form-input form-textarea" onchange="updateNode('items',this.value)" placeholder='["item1","item2"] 或 $node1.output.items'>${typeof node.items === 'string' ? node.items : JSON.stringify(node.items || [])}</textarea>
        </div>
      ` : ''}
      <div class="form-group">
        <label class="form-label">循环体节点</label>
        <select class="form-select" onchange="updateNode('body_node',this.value)">
          <option value="">无</option>
          ${(state.currentWorkflow?.nodes || []).filter(n=>n.id!==node.id).map(n=>`<option value="${n.id}" ${node.body_node===n.id?'selected':''}>${n.name||n.id}</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">最大迭代次数</label>
        <input type="number" class="form-input" value="${node.max_iterations || 100}" onchange="updateNode('max_iterations',parseInt(this.value))" min="1">
      </div>
      <div class="form-group">
        <label class="form-label">循环间隔(秒)</label>
        <input type="number" class="form-input" value="${node.interval || 0}" onchange="updateNode('interval',parseInt(this.value))" min="0">
      </div>
    ` : ''}
    ${node.type === 'parallel' ? `
      <div class="form-group">
        <label class="form-label">执行模式</label>
        <select class="form-select" onchange="updateNode('executionMode',this.value)">
          <option value="ALL" ${node.executionMode==='ALL' || !node.executionMode?'selected':''}>执行所有分支</option>
          <option value="DYNAMIC" ${node.executionMode==='DYNAMIC'?'selected':''}>Agent智能选择分支</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">最大并发数</label>
        <input type="number" class="form-input" value="${node.max_concurrency || 5}" onchange="updateNode('max_concurrency',parseInt(this.value))" min="1" max="10">
      </div>
      <div class="form-group">
        <label class="form-label">失败策略</label>
        <select class="form-select" onchange="updateNode('failure_strategy',this.value)">
          <option value="all" ${node.failure_strategy==='all' || !node.failure_strategy?'selected':''}>全部成功才算成功</option>
          <option value="any" ${node.failure_strategy==='any'?'selected':''}>任意成功就算成功</option>
          <option value="majority" ${node.failure_strategy==='majority'?'selected':''}>多数成功就算成功</option>
        </select>
      </div>
      <div class="config-section" style="margin-top:12px;padding:12px;background:#1a1a1a;border-radius:6px;">
        <div class="config-section-title" style="margin-bottom:8px;">⚡ 并行执行连线</div>
        <p style="font-size:11px;color:#888;margin-bottom:8px;">并行节点会同时执行所有连接的下游节点</p>
        ${getDownstreamEdges(node.id).length > 0 ? `
          <div style="background:#252525;border-radius:4px;padding:8px;">
            ${getDownstreamEdges(node.id).map((edge, idx) => {
              const targetId = edge.targetNodeId || edge.target_node_id;
              const targetNode = state.currentWorkflow.nodes.find(n => n.id === targetId);
              return `<div style="display:flex;align-items:center;gap:8px;padding:4px 0;${idx > 0 ? 'border-top:1px solid #3a3a3a;' : ''}">
                <span style="font-size:12px;color:#89b4fa;">→</span>
                <span style="font-size:12px;">${targetNode?.name || targetId}</span>
                <span style="font-size:10px;color:#888;background:#1a1a1a;padding:2px 6px;border-radius:3px;">${edge.type || edge.edgeType || 'success'}</span>
              </div>`;
            }).join('')}
          </div>
        ` : '<p style="color:#888;font-size:12px;text-align:center;padding:10px;">暂无出边，请先在画布上拖拽连线到目标节点</p>'}
      </div>
      <div class="form-group" style="margin-top:12px;">
        <label class="form-label">合并节点（可选）</label>
        <select class="form-select" onchange="updateNode('mergeNode',this.value)">
          <option value="">无</option>
          ${(state.currentWorkflow?.nodes || []).filter(n=>n.id!==node.id).map(n=>`<option value="${n.id}" ${node.mergeNode===n.id?'selected':''}>${n.name||n.id}</option>`).join('')}
        </select>
      </div>
    ` : ''}
    <div class="form-group">
      <label class="form-label">超时(秒)</label>
      <input type="number" class="form-input" value="${node.timeout || 600}" onchange="updateNode('timeout',parseInt(this.value))">
    </div>
    <div class="form-group">
      <label class="form-label">成功跳转</label>
      <select class="form-select" onchange="updateNode('on_success',this.value)">
        <option value="">无</option>
        ${(state.currentWorkflow?.nodes || []).filter(n=>n.id!==node.id).map(n=>`<option value="${n.id}" ${node.on_success===n.id?'selected':''}>${n.name||n.id}</option>`).join('')}
      </select>
    </div>
    <div class="form-group">
      <label class="form-label">失败跳转</label>
      <select class="form-select" onchange="updateNode('on_fail',this.value)">
        <option value="">无</option>
        ${(state.currentWorkflow?.nodes || []).filter(n=>n.id!==node.id).map(n=>`<option value="${n.id}" ${node.on_fail===n.id?'selected':''}>${n.name||n.id}</option>`).join('')}
      </select>
    </div>
  `;
}

// 选择节点
