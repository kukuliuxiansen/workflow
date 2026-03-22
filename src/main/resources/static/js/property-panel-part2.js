// 属性面板渲染 - Part 2 (Loop/Parallel/Common字段)

    // 循环节点字段
    function renderLoopFields(node) {
      const loopType = node.loop_type || 'count';
      let typeFields = '';

      if (loopType === 'count' || !loopType) {
        typeFields = `
          <div class="form-group">
            <label class="form-label">循环次数</label>
            <input type="number" class="form-input" value="${node.count || 1}" onchange="updateNode('count',parseInt(this.value))" min="1">
          </div>`;
      } else if (loopType === 'condition') {
        typeFields = `
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
            <label class="form-label">退出条件说明</label>
            <textarea class="form-input form-textarea" style="min-height:60px;font-size:12px;" placeholder="例如：当所有文件都处理完成" onchange="updateNode('exitCondition',this.value)">${node.exitCondition || ''}</textarea>
          </div>`;
      } else if (loopType === 'foreach') {
        typeFields = `
          <div class="form-group">
            <label class="form-label">遍历数组</label>
            <textarea class="form-input form-textarea" onchange="updateNode('items',this.value)" placeholder='["item1","item2"] 或 $node1.output.items'>${typeof node.items === 'string' ? node.items : JSON.stringify(node.items || [])}</textarea>
          </div>`;
      }

      return `
        <div class="form-group">
          <label class="form-label">循环类型</label>
          <select class="form-select" onchange="updateNode('loop_type',this.value)">
            <option value="count" ${loopType==='count'?'selected':''}>计数循环</option>
            <option value="condition" ${loopType==='condition'?'selected':''}>条件循环</option>
            <option value="foreach" ${loopType==='foreach'?'selected':''}>遍历循环</option>
          </select>
        </div>
        ${typeFields}
        <div class="form-group">
          <label class="form-label">循环体节点</label>
          <select class="form-select" onchange="updateNode('body_node',this.value)">
            <option value="">无</option>
            ${getNodeOptions(node.body_node, node.id)}
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">最大迭代次数</label>
          <input type="number" class="form-input" value="${node.max_iterations || 100}" onchange="updateNode('max_iterations',parseInt(this.value))" min="1">
        </div>
        <div class="form-group">
          <label class="form-label">循环间隔(秒)</label>
          <input type="number" class="form-input" value="${node.interval || 0}" onchange="updateNode('interval',parseInt(this.value))" min="0">
        </div>`;
    }

    // 并行节点字段
    function renderParallelFields(node) {
      const edges = getDownstreamEdges(node.id);
      const branchesHtml = edges.length > 0 ? `
        <div style="background:#252525;border-radius:4px;padding:8px;">
          ${edges.map((edge, idx) => {
            const targetId = edge.targetNodeId || edge.target_node_id;
            const targetNode = state.currentWorkflow.nodes.find(n => n.id === targetId);
            return `<div style="display:flex;align-items:center;gap:8px;padding:4px 0;${idx > 0 ? 'border-top:1px solid #3a3a3a;' : ''}">
              <span style="font-size:12px;color:#89b4fa;">→</span>
              <span style="font-size:12px;">${targetNode?.name || targetId}</span>
              <span style="font-size:10px;color:#888;background:#1a1a1a;padding:2px 6px;border-radius:3px;">${edge.type || edge.edgeType || 'success'}</span>
            </div>`;
          }).join('')}
        </div>` : '<p style="color:#888;font-size:12px;text-align:center;padding:10px;">暂无出边，请先连线</p>';

      return `
        <div class="form-group">
          <label class="form-label">执行模式</label>
          <select class="form-select" onchange="updateNode('executionMode',this.value)">
            <option value="ALL" ${node.executionMode==='ALL' || !node.executionMode?'selected':''}>执行所有分支</option>
            <option value="DYNAMIC" ${node.executionMode==='DYNAMIC'?'selected':''}>Agent智能选择</option>
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
          <div class="config-section-title" style="margin-bottom:8px;">并行执行连线</div>
          <p style="font-size:11px;color:#888;margin-bottom:8px;">并行节点会同时执行所有下游节点</p>
          ${branchesHtml}
        </div>
        <div class="form-group" style="margin-top:12px;">
          <label class="form-label">合并节点（可选）</label>
          <select class="form-select" onchange="updateNode('mergeNode',this.value)">
            <option value="">无</option>
            ${getNodeOptions(node.mergeNode, node.id)}
          </select>
        </div>`;
    }

    // 通用字段（超时）
    function renderCommonFields(node) {
      if (node.type === 'start' || node.type === 'finish') return '';
      return `
        <div class="form-group">
          <label class="form-label">超时(秒)</label>
          <input type="number" class="form-input" value="${node.timeout || 600}" onchange="updateNode('timeout',parseInt(this.value))">
        </div>`;
    }

    // 获取节点选项HTML
    function getNodeOptions(selectedId, excludeId) {
      return (state.currentWorkflow?.nodes || [])
        .filter(n => n.id !== excludeId)
        .map(n => `<option value="${n.id}" ${selectedId===n.id?'selected':''}>${n.name||n.id}</option>`)
        .join('');
    }