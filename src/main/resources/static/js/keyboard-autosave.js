// 键盘快捷键
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
        // Ctrl/Cmd + C: 复制节点（输入框中不拦截）
        if (isCtrlOrCmd && e.key === 'c' && !isInputFocused) {
          if (state.selectedNode || state.selectedNodes.size > 0) {
            e.preventDefault();
            copyNodes();
          }
        }
        // Ctrl/Cmd + V: 粘贴节点（输入框中不拦截，允许文本粘贴）
        if (isCtrlOrCmd && e.key === 'v' && !isInputFocused) {
          e.preventDefault();
          pasteNodes();
        }
        // Ctrl/Cmd + X: 剪切节点（输入框中不拦截）
        if (isCtrlOrCmd && e.key === 'x' && !isInputFocused) {
          if (state.selectedNode || state.selectedNodes.size > 0) {
            e.preventDefault();
            cutNodes();
          }
        }
        // Ctrl/Cmd + D: 复制为副本
        if (isCtrlOrCmd && e.key === 'd' && !isInputFocused) {
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

    // 自动保存
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

    // 撤销/重做
    function pushUndo() {
      if (state.currentWorkflow) {
        state.undoStack.push(JSON.stringify(state.currentWorkflow));
        if (state.undoStack.length > state.maxUndoSize) {
          state.undoStack.shift();
        }
        state.redoStack = [];  // 新操作清空重做栈
        updateUndoRedoButtons();
      }
    }

    function undo() {
      if (state.undoStack.length > 0) {
        state.redoStack.push(JSON.stringify(state.currentWorkflow));
        state.currentWorkflow = JSON.parse(state.undoStack.pop());
        renderCanvas();
        renderPropertyPanel();
        markDirty();
        updateUndoRedoButtons();
        showToast('info', '已撤销，请手动保存');
      }
    }

    function redo() {
      if (state.redoStack.length > 0) {
        state.undoStack.push(JSON.stringify(state.currentWorkflow));
        state.currentWorkflow = JSON.parse(state.redoStack.pop());
        renderCanvas();
        renderPropertyPanel();
        markDirty();
        updateUndoRedoButtons();
        showToast('info', '已重做，请手动保存');
      }
    }

    function updateUndoRedoButtons() {
      const undoBtn = document.getElementById('undoBtn');
      const redoBtn = document.getElementById('redoBtn');
      const undoCount = document.getElementById('undoCount');

      if (undoBtn) {
        undoBtn.disabled = state.undoStack.length === 0;
      }
      if (redoBtn) {
        redoBtn.disabled = state.redoStack.length === 0;
      }
      if (undoCount) {
        if (state.undoStack.length > 0) {
          undoCount.textContent = state.undoStack.length;
          undoCount.style.display = 'inline';
        } else {
          undoCount.style.display = 'none';
        }
      }
    }