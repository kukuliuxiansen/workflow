// 画布交互（平移、框选、右键菜单）

    // 画布平移和框选状态
    let isPanning = false;
    let isBoxSelecting = false;
    let boxSelectStartX = 0;
    let boxSelectStartY = 0;
    let lastPanX = 0;
    let lastPanY = 0;

    function initCanvasInteract() {
      const canvas = document.getElementById('canvas');

      canvas.addEventListener('mousedown', (e) => {
        // 中键或Ctrl+左键: 平移
        if (e.button === 1 || (e.button === 0 && e.ctrlKey)) {
          isPanning = true;
          lastPanX = e.clientX;
          lastPanY = e.clientY;
          canvas.style.cursor = 'grabbing';
          e.preventDefault();
          return;
        }

        // 左键在空白处: 框选
        if (e.button === 0 && !e.target.closest('.node') && !e.target.closest('.edge-path')) {
          isBoxSelecting = true;
          const rect = canvas.getBoundingClientRect();
          boxSelectStartX = e.clientX - rect.left;
          boxSelectStartY = e.clientY - rect.top;

          if (!e.shiftKey) clearNodeSelection();

          const selectionRect = document.getElementById('selectionRect');
          if (selectionRect) {
            selectionRect.style.left = boxSelectStartX + 'px';
            selectionRect.style.top = boxSelectStartY + 'px';
            selectionRect.style.width = '0';
            selectionRect.style.height = '0';
            selectionRect.classList.add('active');
          }
          e.preventDefault();
        }
      });

      document.addEventListener('mousemove', (e) => {
        if (isPanning) {
          state.panX += e.clientX - lastPanX;
          state.panY += e.clientY - lastPanY;
          lastPanX = e.clientX;
          lastPanY = e.clientY;
          updateCanvasTransform();
          updateMinimap();
        }

        if (isBoxSelecting) {
          const rect = canvas.getBoundingClientRect();
          const currentX = e.clientX - rect.left;
          const currentY = e.clientY - rect.top;

          const selectionRect = document.getElementById('selectionRect');
          if (selectionRect) {
            selectionRect.style.left = Math.min(boxSelectStartX, currentX) + 'px';
            selectionRect.style.top = Math.min(boxSelectStartY, currentY) + 'px';
            selectionRect.style.width = Math.abs(currentX - boxSelectStartX) + 'px';
            selectionRect.style.height = Math.abs(currentY - boxSelectStartY) + 'px';
          }
        }
      });

      document.addEventListener('mouseup', (e) => {
        if (isPanning) {
          isPanning = false;
          canvas.style.cursor = '';
        }

        if (isBoxSelecting) {
          isBoxSelecting = false;
          const selectionRect = document.getElementById('selectionRect');
          if (selectionRect) selectionRect.classList.remove('active');
          finishBoxSelect(e);
        }
      });

      // 画布右键菜单
      canvas.addEventListener('contextmenu', (e) => {
        if (!e.target.closest('.node')) {
          e.preventDefault();
          const menu = document.getElementById('canvasContextMenu');
          if (menu) {
            menu.style.left = e.clientX + 'px';
            menu.style.top = e.clientY + 'px';
            menu.classList.add('show');
          }
        }
      });

      // 点击连线删除
      canvas.addEventListener('click', (e) => {
        const edgePath = e.target.closest('.edge-path');
        if (edgePath) {
          const edgeId = edgePath.getAttribute('data-edge-id');
          if (edgeId && typeof deleteEdgeById === 'function') {
            deleteEdgeById(edgeId);
          }
          return;
        }
        if (!e.target.closest('.node')) {
          if (state.selectedNode) {
            state.selectedNode = null;
            state.selectedNodes.clear();
            renderCanvas();
            renderPropertyPanel();
          }
          collapseRightPanel();
        }
      });

      // 双击画布添加节点
      canvas.addEventListener('dblclick', (e) => {
        if (e.target.closest('.node')) return;
        if (!state.currentWorkflow) {
          showToast('warn', '请先选择工作流');
          return;
        }
        const rect = canvas.getBoundingClientRect();
        const x = (e.clientX - rect.left - state.panX) / state.zoom;
        const y = (e.clientY - rect.top - state.panY) / state.zoom;
        addNodeAtPosition(x, y);
      });
    }

    // 完成框选
    function finishBoxSelect(e) {
      const canvas = document.getElementById('canvas');
      const rect = canvas.getBoundingClientRect();
      const endX = e.clientX - rect.left;
      const endY = e.clientY - rect.top;

      const boxLeft = Math.min(boxSelectStartX, endX);
      const boxTop = Math.min(boxSelectStartY, endY);
      const boxRight = Math.max(boxSelectStartX, endX);
      const boxBottom = Math.max(boxSelectStartY, endY);

      // 转换为画布坐标
      const canvasBoxLeft = (boxLeft - state.panX) / state.zoom;
      const canvasBoxTop = (boxTop - state.panY) / state.zoom;
      const canvasBoxRight = (boxRight - state.panX) / state.zoom;
      const canvasBoxBottom = (boxBottom - state.panY) / state.zoom;

      // 检查每个节点是否在框选区域内
      if (state.currentWorkflow && state.currentWorkflow.nodes) {
        state.currentWorkflow.nodes.forEach(node => {
          const nodeLeft = node.position_x;
          const nodeTop = node.position_y;
          const nodeRight = node.position_x + 180;
          const nodeBottom = node.position_y + 80;

          if (nodeRight >= canvasBoxLeft && nodeLeft <= canvasBoxRight &&
              nodeBottom >= canvasBoxTop && nodeTop <= canvasBoxBottom) {
            state.selectedNodes.add(node.id);
          }
        });
      }

      if (state.selectedNodes.size === 1) {
        state.selectedNode = state.currentWorkflow?.nodes?.find(n => state.selectedNodes.has(n.id)) || null;
      } else {
        state.selectedNode = null;
      }

      renderCanvas();
      updateSelectedCount();
    }

    // 在指定位置添加节点
    async function addNodeAtPosition(x, y) {
      const type = state.selectedNodeType || 'agent_execution';
      const name = getTypeName(type);

      // 保存撤销点
      pushUndo();

      try {
        await fetch(`${API}/workflows/${state.currentWorkflow.id}/nodes`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            type,
            name,
            position_x: Math.max(0, x),
            position_y: Math.max(0, y)
          })
        });
        await selectWorkflow(state.currentWorkflow.id);
        markDirty();
        updateUndoRedoButtons();
        showToast('success', '节点已添加');
      } catch (e) {
        showToast('error', '添加失败');
      }
    }

    // 清除节点选择
    function clearNodeSelection() {
      state.selectedNodes.clear();
      state.selectedNode = null;
      updateSelectedCount();
    }