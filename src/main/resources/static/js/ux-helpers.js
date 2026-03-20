// 兼容旧函数名
function showContextMenu(event, nodeId) {
  showNodeContextMenu(event, nodeId);
}

function closeContextMenu() {
  document.querySelectorAll('.context-menu').forEach(menu => {
    menu.classList.remove('show');
  });
  state.contextMenuNode = null;
}

// 兼容旧的contextMenuAction函数（已在上面的画布优化功能中重写）
// 这里保留一个简化版本作为备份
function contextMenuActionOld(action) {
  const nodeId = state.contextMenuNode;
  closeContextMenu();

  if (!nodeId) return;

  switch(action) {
    case 'edit':
      selectNode(nodeId);
      break;
    case 'duplicate':
      selectNode(nodeId);
      copyNodes();
      pasteNodes();
      break;
    case 'delete':
      selectNode(nodeId);
      deleteSelectedNodes();
      break;
    case 'run-from':
      if (state.currentWorkflow) {
        confirmAsync('是否从此节点开始执行？').then(confirmed => {
          if (confirmed) executeWorkflow(nodeId);
        });
      }
      break;
  }
}

// 8. 节点状态徽章
function getNodeStatusBadge(nodeId) {
  const status = state.nodeStatus.get(nodeId);
  if (!status) return '';

  const statusMap = {
    'pending': '<span class="node-badge pending">等待</span>',
    'running': '<span class="node-badge running">执行中</span>',
    'success': '<span class="node-badge success">成功</span>',
    'failed': '<span class="node-badge failed">失败</span>',
    'skipped': '<span class="node-badge skipped">跳过</span>'
  };

  return statusMap[status] || '';
}

// 9. 快速操作按钮
function showQuickActions(nodeId, event) {
  event.stopPropagation();
  const panel = document.getElementById('quickActions');
  const node = state.currentWorkflow?.nodes?.find(n => n.id === nodeId);

  if (!panel || !node) return;

  panel.innerHTML = `
    <button class="quick-action-btn" onclick="selectNode('${nodeId}')" title="编辑">✏️</button>
    <button class="quick-action-btn" onclick="duplicateNodeById('${nodeId}')" title="复制">📋</button>
    <button class="quick-action-btn danger" onclick="deleteNodeById('${nodeId}')" title="删除">🗑️</button>
  `;

  const rect = event.target.getBoundingClientRect();
  panel.style.left = (rect.right + 5) + 'px';
  panel.style.top = rect.top + 'px';
  panel.classList.add('show');
}

function hideQuickActions() {
  const panel = document.getElementById('quickActions');
  if (panel) panel.classList.remove('show');
}

function duplicateNodeById(nodeId) {
  clearNodeSelection();
  state.selectedNode = state.currentWorkflow?.nodes?.find(n => n.id === nodeId) || null;
  state.selectedNodes.add(nodeId);
  duplicateNodes();
}

function deleteNodeById(nodeId) {
  clearNodeSelection();
  state.selectedNode = state.currentWorkflow?.nodes?.find(n => n.id === nodeId) || null;
  state.selectedNodes.add(nodeId);
  deleteSelectedNodes();
}

// 兼容旧函数名
function deleteSelectedNode() {
  deleteSelectedNodes();
}

// 10. 双击编辑节点
function handleNodeDoubleClick(nodeId) {
  selectNode(nodeId);
  // 聚焦到名称输入框
  const nameInput = document.querySelector('.panel-body input[type="text"]');
  if (nameInput) nameInput.focus();
}

// 初始化UX功能
function initUXFeatures() {
  setupKeyboardShortcuts();
  setupAutoSave();

  // 点击其他地方关闭上下文菜单
  document.addEventListener('click', (e) => {
    if (!e.target.closest('.context-menu')) {
      closeContextMenu();
    }
    if (!e.target.closest('.quick-action-btn')) {
      hideQuickActions();
    }
    // 关闭快捷键帮助面板
    if (!e.target.closest('#shortcutHelpPanel')) {
      closeShortcutHelp();
    }
  });

  // 初始化小地图
  updateMinimap();

  // 初始化画布信息
  updateCanvasInfo();
  updateZoomLevel();
}

