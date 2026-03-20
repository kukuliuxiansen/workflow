// 节点拖拽功能
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