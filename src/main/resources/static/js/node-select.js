    function selectNode(id) {
      state.selectedNode = state.currentWorkflow?.nodes?.find(n => n.id === id);
      renderCanvas();
      renderPropertyPanel();
      // 选中节点时展开右侧面板
      expandRightPanel();
    }

    // 展开右侧面板
    function expandRightPanel() {
      const panel = document.getElementById('rightPanel');
      panel.classList.remove('collapsed');
      updateToggleButtonPosition();
    }

    // 收起右侧面板
    function collapseRightPanel() {
      const panel = document.getElementById('rightPanel');
      panel.classList.add('collapsed');
      updateToggleButtonPosition();
    }

    // 更新切换按钮位置
    function updateToggleButtonPosition() {
      const panel = document.getElementById('rightPanel');
      const btn = document.getElementById('toggleRightPanelBtn');
      if (panel.classList.contains('collapsed')) {
        btn.style.right = '10px';
        btn.textContent = '◀';
      } else {
        btn.style.right = panel.offsetWidth + 'px';
        btn.textContent = '▶';
      }
    }

    // 更新节点
    function updateNode(key, value) {
      if (!state.selectedNode) return;
      pushUndo();  // 保存当前状态到撤销栈
      state.selectedNode[key] = value;
      markDirty();  // 标记为已修改
      renderCanvas();
      addLog('info', `更新: ${key} = ${value}`);
    }

    // 获取下游边（从当前节点出发的边）
    function getDownstreamEdges(nodeId) {
      if (!state.currentWorkflow || !state.currentWorkflow.edges) return [];
      return state.currentWorkflow.edges.filter(e => (e.sourceNodeId || e.source_node_id) === nodeId);
    }

    // 更新分支配置（基于目标节点ID）
    function updateBranchConfig(targetNodeId, key, value) {
      if (!state.selectedNode) return;
      if (!state.selectedNode.branches) state.selectedNode.branches = [];
      let branch = state.selectedNode.branches.find(b => b.targetNodeId === targetNodeId);
      if (!branch) {
        branch = { targetNodeId: targetNodeId };
        state.selectedNode.branches.push(branch);
      }
      branch[key] = value;
      markDirty();
    }

    // 更新节点并保存到后端
    async function updateNodeAndSave(key, value) {
      if (!state.selectedNode) return;
      updateNode(key, value);
      await saveWorkflow(true);  // 静默保存
    }

    // ==================== 条件判断节点分支管理 ====================

    function addConditionBranch(event) {
      event.preventDefault();
      if (!state.selectedNode) return;
      if (!state.selectedNode.branches) state.selectedNode.branches = [];
      const newBranch = {
        id: 'branch_' + Date.now(),
        name: '新分支',
        description: '',
        targetNodeId: '',
        conditionDesc: ''
      };
      state.selectedNode.branches.push(newBranch);
      markDirty();
      renderPropertyPanel();
    }

    function removeConditionBranch(event, index) {
      event.preventDefault();
      if (!state.selectedNode || !state.selectedNode.branches) return;
      state.selectedNode.branches.splice(index, 1);
      markDirty();
      renderPropertyPanel();
    }

    function updateConditionBranch(index, key, value) {
      if (!state.selectedNode || !state.selectedNode.branches) return;
      state.selectedNode.branches[index][key] = value;
      markDirty();
    }

    // ==================== 并行节点分支管理 ====================

    function addParallelBranch(event) {
      event.preventDefault();
      if (!state.selectedNode) return;
      if (!state.selectedNode.branches) state.selectedNode.branches = [];
      const newBranch = {
        id: 'branch_' + Date.now(),
        name: '新分支',
        description: '',
        targetNodeId: '',
        conditionDesc: ''
      };
      state.selectedNode.branches.push(newBranch);
      markDirty();
      renderPropertyPanel();
    }

    function removeParallelBranch(event, index) {
      event.preventDefault();
      if (!state.selectedNode || !state.selectedNode.branches) return;
      state.selectedNode.branches.splice(index, 1);
      markDirty();
      renderPropertyPanel();
    }

    function updateParallelBranch(index, key, value) {
      if (!state.selectedNode || !state.selectedNode.branches) return;
      state.selectedNode.branches[index][key] = value;
      markDirty();
    }

