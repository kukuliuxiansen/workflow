// 3. 撤销/重做
function pushUndo() {
  if (state.currentWorkflow) {
    state.undoStack.push(JSON.stringify(state.currentWorkflow));
    if (state.undoStack.length > state.maxUndoSize) {
      state.undoStack.shift();
    }
    state.redoStack = [];  // 新操作清空重做栈
  }
}

function undo() {
  if (state.undoStack.length > 0) {
    state.redoStack.push(JSON.stringify(state.currentWorkflow));
    state.currentWorkflow = JSON.parse(state.undoStack.pop());
    renderCanvas();
    renderPropertyPanel();
    showToast('info', '已撤销');
  }
}

function redo() {
  if (state.redoStack.length > 0) {
    state.undoStack.push(JSON.stringify(state.currentWorkflow));
    state.currentWorkflow = JSON.parse(state.redoStack.pop());
    renderCanvas();
    renderPropertyPanel();
    showToast('info', '已重做');
  }
}

// 4. 复制/粘贴节点（调用批量版本）
function copyNode() {
  copyNodes();
}

function pasteNode() {
  pasteNodes();
}

// 5. 节点搜索
function searchNodes(query) {
  state.searchQuery = query.toLowerCase();
  renderCanvas();
}

// 6. 网格吸附
function snapToGrid(value) {
  if (state.gridSnap) {
    return Math.round(value / state.gridSize) * state.gridSize;
  }
  return value;
}

// 7. 右键上下文菜单
function showNodeContextMenu(event, nodeId) {
  event.preventDefault();
  event.stopPropagation();

  // 如果右键的节点不在选中列表中，则选中它
  if (!state.selectedNodes.has(nodeId) && state.selectedNode?.id !== nodeId) {
    clearNodeSelection();
    state.selectedNode = state.currentWorkflow?.nodes?.find(n => n.id === nodeId) || null;
    state.selectedNodes.add(nodeId);
    renderCanvas();
  }

  state.contextMenuNode = nodeId;
  const menu = document.getElementById('contextMenu');
  menu.style.left = event.clientX + 'px';
  menu.style.top = event.clientY + 'px';
  menu.classList.add('show');
}

