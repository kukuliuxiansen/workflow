function alignNodes(direction) {
  if (state.selectedNodes.size < 2) {
    showToast('warn', '请至少选中2个节点');
    return;
  }

  const nodes = state.currentWorkflow.nodes.filter(n => state.selectedNodes.has(n.id));
  if (nodes.length < 2) return;

  pushUndo();

  switch(direction) {
    case 'left':
      const minLeft = Math.min(...nodes.map(n => n.position_x));
      nodes.forEach(n => n.position_x = minLeft);
      break;
    case 'right':
      const maxRight = Math.max(...nodes.map(n => n.position_x + 180));
      nodes.forEach(n => n.position_x = maxRight - 180);
      break;
    case 'top':
      const minTop = Math.min(...nodes.map(n => n.position_y));
      nodes.forEach(n => n.position_y = minTop);
      break;
    case 'bottom':
      const maxBottom = Math.max(...nodes.map(n => n.position_y + 80));
      nodes.forEach(n => n.position_y = maxBottom - 80);
      break;
    case 'center-h':
      const avgX = nodes.reduce((sum, n) => sum + n.position_x + 90, 0) / nodes.length;
      nodes.forEach(n => n.position_x = avgX - 90);
      break;
    case 'center-v':
      const avgY = nodes.reduce((sum, n) => sum + n.position_y + 40, 0) / nodes.length;
      nodes.forEach(n => n.position_y = avgY - 40);
      break;
  }

  renderCanvas();
  markDirty();
  showToast('success', '节点已对齐');
}

// 节点分布
function distributeNodes(direction) {
  if (state.selectedNodes.size < 3) {
    showToast('warn', '请至少选中3个节点');
    return;
  }

  const nodes = state.currentWorkflow.nodes.filter(n => state.selectedNodes.has(n.id));
  if (nodes.length < 3) return;

  pushUndo();

  if (direction === 'horizontal') {
    nodes.sort((a, b) => a.position_x - b.position_x);
    const minX = nodes[0].position_x;
    const maxX = nodes[nodes.length - 1].position_x + 180;
    const spacing = (maxX - minX - 180 * nodes.length) / (nodes.length - 1);

    nodes.forEach((node, index) => {
      node.position_x = minX + index * (180 + spacing);
    });
  } else {
    nodes.sort((a, b) => a.position_y - b.position_y);
    const minY = nodes[0].position_y;
    const maxY = nodes[nodes.length - 1].position_y + 80;
    const spacing = (maxY - minY - 80 * nodes.length) / (nodes.length - 1);

    nodes.forEach((node, index) => {
      node.position_y = minY + index * (80 + spacing);
    });
  }

  renderCanvas();
  markDirty();
  showToast('success', '节点已均匀分布');
}

// 节点颜色设置
function setNodeColor(color) {
  const nodeIds = [];
  if (state.selectedNode) {
    nodeIds.push(state.selectedNode.id);
  } else if (state.selectedNodes.size > 0) {
    nodeIds.push(...state.selectedNodes);
  }

  if (nodeIds.length === 0) return;

  pushUndo();

  nodeIds.forEach(nodeId => {
    const node = state.currentWorkflow.nodes.find(n => n.id === nodeId);
    if (node) {
      if (!node.config) node.config = {};
      try {
        const config = typeof node.config === 'string' ? JSON.parse(node.config) : node.config;
        config.color = color === 'none' ? undefined : color;
        node.config = JSON.stringify(config);
      } catch (e) {
        node.config = JSON.stringify({ color: color === 'none' ? undefined : color });
      }
    }
  });

  renderCanvas();
  markDirty();
  showToast('success', '节点颜色已更新');
}

// 增强的右键菜单动作
function contextMenuAction(action) {
