
    // 1. 键盘快捷键
    function setupKeyboardShortcuts() {
      document.addEventListener('keydown', (e) => {
        // 忽略输入框中的快捷键
        const isInputFocused = document.activeElement.tagName === 'INPUT' ||
                               document.activeElement.tagName === 'TEXTAREA';
        const isCtrlOrCmd = e.ctrlKey || e.metaKey;

        // Ctrl/Cmd + S: 保存
        if (isCtrlOrCmd && e.key === 's') {
          e.preventDefault();
          saveWorkflow();
        }
        // Ctrl/Cmd + Z: 撤销
        if (isCtrlOrCmd && e.key === 'z' && !e.shiftKey) {
          e.preventDefault();
          undo();
        }
        // Ctrl/Cmd + Y 或 Ctrl/Cmd + Shift + Z: 重做
        if ((isCtrlOrCmd && e.key === 'y') || (isCtrlOrCmd && e.key === 'z' && e.shiftKey)) {
          e.preventDefault();
          redo();
        }
        // Ctrl/Cmd + C: 复制节点
        if (isCtrlOrCmd && e.key === 'c') {
          if (state.selectedNode || state.selectedNodes.size > 0) {
            e.preventDefault();
            copyNodes();
          }
        }
        // Ctrl/Cmd + V: 粘贴节点
        if (isCtrlOrCmd && e.key === 'v') {
          e.preventDefault();
          pasteNodes();
        }
        // Ctrl/Cmd + X: 剪切节点
        if (isCtrlOrCmd && e.key === 'x') {
          if (state.selectedNode || state.selectedNodes.size > 0) {
            e.preventDefault();
            cutNodes();
          }
        }
        // Ctrl/Cmd + D: 复制为副本
        if (isCtrlOrCmd && e.key === 'd') {
          e.preventDefault();
          duplicateNodes();
        }
        // Ctrl/Cmd + A: 全选
        if (isCtrlOrCmd && e.key === 'a' && !isInputFocused) {
          e.preventDefault();
          selectAllNodes();
        }
        // Delete/Backspace: 删除选中节点
        if ((e.key === 'Delete' || e.key === 'Backspace') && !isInputFocused) {
          if (state.selectedNode || state.selectedNodes.size > 0) {
            e.preventDefault();
            deleteSelectedNodes();
          }
        }
        // Escape: 取消选择/关闭弹窗
        if (e.key === 'Escape') {
          clearNodeSelection();
          closeContextMenu();
          closeSearchPanel();
          closeShortcutHelp();
          renderCanvas();
          renderPropertyPanel();
        }
        // Ctrl/Cmd + F: 搜索节点
        if (isCtrlOrCmd && e.key === 'f') {
          e.preventDefault();
          toggleSearchPanel();
        }
        // Ctrl/Cmd + 0: 适应画布
        if (isCtrlOrCmd && e.key === '0') {
          e.preventDefault();
          fitCanvas();
        }
        // Ctrl/Cmd + +: 放大
        if (isCtrlOrCmd && (e.key === '+' || e.key === '=')) {
          e.preventDefault();
          zoomIn();
        }
        // Ctrl/Cmd + -: 缩小
        if (isCtrlOrCmd && e.key === '-') {
          e.preventDefault();
          zoomOut();
        }
        // ?: 显示快捷键帮助
        if (e.key === '?' && !isInputFocused) {
          e.preventDefault();
          toggleShortcutHelp();
        }
      });
    }

    // 2. 自动保存
    function setupAutoSave() {
      if (state.autoSaveTimer) {
        clearInterval(state.autoSaveTimer);
      }
      state.autoSaveTimer = setInterval(() => {
        if (state.isDirty && state.currentWorkflow) {
          saveWorkflow(true);  // true = 静默保存
        }
      }, 30000);  // 30秒自动保存
    }

    function markDirty() {
      state.isDirty = true;
      updateSaveIndicator();
    }

    function updateSaveIndicator() {
      const indicator = document.getElementById('saveIndicator');
      if (indicator) {
        if (state.isDirty) {
          indicator.textContent = '● 未保存';
          indicator.className = 'save-indicator dirty';
        } else {
          indicator.textContent = state.lastSaveTime ? `✓ 已保存 ${new Date(state.lastSaveTime).toLocaleTimeString()}` : '✓ 已保存';
          indicator.className = 'save-indicator saved';
        }
      }
    }

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

