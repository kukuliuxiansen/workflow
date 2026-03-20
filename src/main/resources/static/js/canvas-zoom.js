    function initCanvasZoom() {
      const canvas = document.getElementById('canvas');
      const content = document.getElementById('canvasContent');

      // 鼠标滚轮缩放
      canvas.addEventListener('wheel', (e) => {
        e.preventDefault();
        const delta = e.deltaY > 0 ? -0.1 : 0.1;
        const newZoom = Math.max(0.1, Math.min(4, state.zoom + delta));

        // 以鼠标位置为中心缩放
        const rect = canvas.getBoundingClientRect();
        const mouseX = e.clientX - rect.left;
        const mouseY = e.clientY - rect.top;

        const zoomRatio = newZoom / state.zoom;
        state.panX = mouseX - (mouseX - state.panX) * zoomRatio;
        state.panY = mouseY - (mouseY - state.panY) * zoomRatio;

        state.zoom = newZoom;
        updateCanvasTransform();
        updateMinimap();
        updateZoomLevel();
      });

      // 画布拖拽和框选
      let isPanning = false;
      let isBoxSelecting = false;
      let boxSelectStartX = 0;
      let boxSelectStartY = 0;
      let lastPanX = 0;
      let lastPanY = 0;

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
        if (e.button === 0 && !e.target.closest('.node') && !e.target.closest('.edge')) {
          isBoxSelecting = true;
          const rect = canvas.getBoundingClientRect();
          boxSelectStartX = e.clientX - rect.left;
          boxSelectStartY = e.clientY - rect.top;

          // 如果没有按Shift，清除之前的选择
          if (!e.shiftKey) {
            clearNodeSelection();
          }

          const selectionRect = document.getElementById('selectionRect');
          selectionRect.style.left = boxSelectStartX + 'px';
          selectionRect.style.top = boxSelectStartY + 'px';
          selectionRect.style.width = '0';
          selectionRect.style.height = '0';
          selectionRect.classList.add('active');
          e.preventDefault();
        }
      });

      document.addEventListener('mousemove', (e) => {
        // 平移
        if (isPanning) {
          state.panX += e.clientX - lastPanX;
          state.panY += e.clientY - lastPanY;
          lastPanX = e.clientX;
          lastPanY = e.clientY;
          updateCanvasTransform();
          updateMinimap();
        }

        // 框选
        if (isBoxSelecting) {
          const rect = canvas.getBoundingClientRect();
          const currentX = e.clientX - rect.left;
          const currentY = e.clientY - rect.top;

          const selectionRect = document.getElementById('selectionRect');
          const left = Math.min(boxSelectStartX, currentX);
          const top = Math.min(boxSelectStartY, currentY);
          const width = Math.abs(currentX - boxSelectStartX);
          const height = Math.abs(currentY - boxSelectStartY);

          selectionRect.style.left = left + 'px';
          selectionRect.style.top = top + 'px';
          selectionRect.style.width = width + 'px';
          selectionRect.style.height = height + 'px';
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
          selectionRect.classList.remove('active');

          // 计算框选区域内的节点
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
              const nodeRight = node.position_x + 180;  // 节点宽度
              const nodeBottom = node.position_y + 80;   // 节点高度

              // 检查是否相交（部分选中模式）
              if (nodeRight >= canvasBoxLeft && nodeLeft <= canvasBoxRight &&
                  nodeBottom >= canvasBoxTop && nodeTop <= canvasBoxBottom) {
                state.selectedNodes.add(node.id);
              }
            });
          }

          // 如果只选中了一个节点，设置为selectedNode以便兼容
          if (state.selectedNodes.size === 1) {
            state.selectedNode = state.currentWorkflow?.nodes?.find(n => state.selectedNodes.has(n.id)) || null;
          } else {
            state.selectedNode = null;
          }

          renderCanvas();
          updateSelectedCount();
        }
      });

      // 画布右键菜单
      canvas.addEventListener('contextmenu', (e) => {
        if (!e.target.closest('.node') && !e.target.closest('.edge')) {
          e.preventDefault();
          const menu = document.getElementById('canvasContextMenu');
          menu.style.left = e.clientX + 'px';
          menu.style.top = e.clientY + 'px';
          menu.classList.add('show');
        }
      });

      // 双击画布添加节点
      canvas.addEventListener('dblclick', (e) => {
        if (e.target.closest('.node')) return; // 不在节点上响应
        if (!state.currentWorkflow) {
          showToast('warn', '请先选择工作流');
          return;
        }

        // 计算画布坐标位置
        const rect = canvas.getBoundingClientRect();
        const x = (e.clientX - rect.left - state.panX) / state.zoom;
        const y = (e.clientY - rect.top - state.panY) / state.zoom;

        // 直接添加节点
        addNodeAtPosition(x, y);
      });

      // 点击画布空白处收起右侧面板
      canvas.addEventListener('click', (e) => {
        if (!e.target.closest('.node') && !e.target.closest('.edge')) {
          // 点击空白处，取消选中并收起面板
          if (state.selectedNode) {
            state.selectedNode = null;
            state.selectedNodes.clear();
            renderCanvas();
            renderPropertyPanel();
          }
          collapseRightPanel();
        }
      });
    }

    // 在指定位置添加节点
    async function addNodeAtPosition(x, y) {
      const name = '节点_' + (state.currentWorkflow.nodes?.length || 0);
      try {
        await fetch(`${API}/workflows/${state.currentWorkflow.id}/nodes`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            type: 'agent_execution',
            name,
            position_x: Math.max(0, x),
            position_y: Math.max(0, y)
          })
        });
        await selectWorkflow(state.currentWorkflow.id);
        showToast('success', '节点已添加');
      } catch (e) {
        showToast('error', '添加失败');
      }
    }

    function updateCanvasTransform() {
      const content = document.getElementById('canvasContent');
      if (content) {
        content.style.transform = `translate(${state.panX}px, ${state.panY}px) scale(${state.zoom})`;
      }
      const zoomLevelEl = document.getElementById('zoomLevel');
      if (zoomLevelEl) {
        zoomLevelEl.textContent = Math.round(state.zoom * 100) + '%';
      }
    }

    function zoomIn() {
      state.zoom = Math.min(2, state.zoom + 0.1);
      updateCanvasTransform();
    }

    function zoomOut() {
      state.zoom = Math.max(0.25, state.zoom - 0.1);
      updateCanvasTransform();
    }

    function zoomReset() {
      state.zoom = 1;
      state.panX = 0;
      state.panY = 0;
      updateCanvasTransform();
