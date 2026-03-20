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
