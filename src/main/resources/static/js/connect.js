// 保留旧的点击模式作为备选（Shift+点击端口）
function startConnect(e, nodeId, type) {
  e.stopPropagation();
  connectingFrom = { nodeId, type };
  addLog('info', `开始连线: ${nodeId} (${type})`);
  document.getElementById('canvas').onclick = handleConnect;
}

async function handleConnect(e) {
  const node = e.target.closest('.node');
  if (node && connectingFrom) {
    const targetId = node.id.replace('node-', '');
    if (targetId !== connectingFrom.nodeId) {
      try {
        await fetch(`${API}/workflows/${state.currentWorkflow.id}/edges`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ source: connectingFrom.nodeId, target: targetId, type: connectingFrom.type })
        });
        await selectWorkflow(state.currentWorkflow.id);
        showToast('success', '连线创建成功');
      } catch (e) {
        showToast('error', '创建连线失败');
      }
    }
  }
  connectingFrom = null;
  document.getElementById('canvas').onclick = null;
}

async function deleteEdge(source, type) {
  if (!await confirmAsync('删除此连线？')) return;
  try {
    await fetch(`${API}/workflows/${state.currentWorkflow.id}/edges`, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ source, type })
    });
    await selectWorkflow(state.currentWorkflow.id);
    showToast('success', '连线已删除');
  } catch (e) {
    showToast('error', '删除失败');
  }
}

// 拖拽
let dragging = null;
function startDrag(e, id) {
  if (e.button !== 0 || e.altKey) return;
  e.stopPropagation();

  // 如果拖动的节点不在选中集合中，则只选中该节点
  if (!state.selectedNodes.has(id)) {
    state.selectedNodes.clear();
    state.selectedNodes.add(id);
    state.selectedNode = state.currentWorkflow?.nodes?.find(n => n.id === id);
    renderCanvas();
  }

  const el = document.getElementById(`node-${id}`);
  dragging = {
    id,
    startX: e.clientX,
    startY: e.clientY,
    // 记录所有选中节点的初始位置
    initialPositions: {}
  };

  // 记录所有选中节点的初始位置
  state.currentWorkflow?.nodes?.forEach(node => {
    if (state.selectedNodes.has(node.id)) {
      dragging.initialPositions[node.id] = {
        x: node.position_x || 0,
        y: node.position_y || 0
      };
    }
  });

  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', stopDrag);
}

function onDrag(e) {
  if (!dragging) return;

  const dx = (e.clientX - dragging.startX) / state.zoom;
  const dy = (e.clientY - dragging.startY) / state.zoom;

  // 移动所有选中的节点
  state.currentWorkflow?.nodes?.forEach(node => {
    if (state.selectedNodes.has(node.id) && dragging.initialPositions[node.id]) {
      const initial = dragging.initialPositions[node.id];
      node.position_x = Math.max(0, initial.x + dx);
      node.position_y = Math.max(0, initial.y + dy);

      const el = document.getElementById(`node-${node.id}`);
      if (el) {
        el.style.left = node.position_x + 'px';
        el.style.top = node.position_y + 'px';
      }
    }
  });

  renderEdges();
}

async function stopDrag() {
  if (dragging && state.selectedNodes.size > 0) {
    // 批量保存所有移动节点的位置
    const promises = [];
    state.currentWorkflow?.nodes?.forEach(node => {
      if (state.selectedNodes.has(node.id)) {
        promises.push(
          fetch(`${API}/workflows/${state.currentWorkflow.id}/nodes/${node.id}/position`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ x: node.position_x, y: node.position_y })
          })
        );
      }
    });
    await Promise.all(promises);
  }
  dragging = null;
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
}

// 节点类型选择
function selectNodeType(el) {
  document.querySelectorAll('.node-type-card').forEach(c => c.classList.remove('selected'));
  el.classList.add('selected');
  state.selectedNodeType = el.dataset.type;
}

// 工具函数
function showCreateModal() {
  document.getElementById('newWorkflowName').value = '';
  document.getElementById('newWorkflowDesc').value = '';
  document.getElementById('createModal').classList.add('show');
}

function showAddNodeModal() {
  if (!state.currentWorkflow) {
    showToast('warn', '请先选择工作流');
    return;
  }
  document.getElementById('newNodeName').value = '';
  document.getElementById('addNodeModal').classList.add('show');
}

function closeModal(id) {
  document.getElementById(id).classList.remove('show');
}

