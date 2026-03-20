    function startDragConnect(e, nodeId, type) {
      e.stopPropagation();
      e.preventDefault();

      // 检查源节点是否是结束节点（结束节点不能有输出）
      const sourceNode = state.currentWorkflow?.nodes?.find(n => n.id === nodeId);
      if (sourceNode && sourceNode.type === 'finish') {
        showToast('warn', '结束节点不能有输出连线');
        return;
      }

      connectingFrom = { nodeId, type };
      isDraggingConnect = true;

      // 高亮端口
      e.target.classList.add('dragging');

      // 显示临时线
      const dragLine = document.getElementById('dragLine');
      if (!dragLine) return;
      dragLine.style.display = 'block';
      dragLine.classList.remove('success', 'fail');
      dragLine.classList.add(type);

      // 获取节点元素和数据
      const nodeEl = document.getElementById(`node-${nodeId}`);
      const nodeData = state.currentWorkflow?.nodes?.find(n => n.id === nodeId);
      if (!nodeData || !nodeEl) return;

      // 端口大小为10px，计算端口中心位置
      const portRadius = 5;
      const portGap = 2;

      // 源端口位置：节点右边沿
      const startX = nodeData.position_x + nodeEl.offsetWidth;
      const portsCenterY = nodeData.position_y + nodeEl.offsetHeight - 11;
      const startY = portsCenterY + (type === 'success' ? -(portRadius + portGap) : (portRadius + portGap));

      connectingFrom.startX = startX;
      connectingFrom.startY = startY;

      // 添加鼠标移动和释放事件
      document.addEventListener('mousemove', onDragConnect);
      document.addEventListener('mouseup', endDragConnect);

      addLog('info', `开始拖拽连线: ${nodeId} (${type})`);
    }

    function onDragConnect(e) {
      if (!isDraggingConnect || !connectingFrom) return;

      const dragLine = document.getElementById('dragLine');
      if (!dragLine) return;

      const canvas = document.getElementById('canvas');
      if (!canvas) return;

      const canvasRect = canvas.getBoundingClientRect();

      // 计算鼠标在画布上的位置
      const mouseX = (e.clientX - canvasRect.left) / state.zoom;
      const mouseY = (e.clientY - canvasRect.top) / state.zoom;

      // 绘制贝塞尔曲线
      const x1 = connectingFrom.startX;
      const y1 = connectingFrom.startY;
      let x2 = mouseX;
      let y2 = mouseY;

      // 检测目标节点并吸附
      const targetEl = document.elementFromPoint(e.clientX, e.clientY);
      const targetNode = targetEl ? targetEl.closest('.node') : null;
      document.querySelectorAll('.node.connect-target').forEach(n => n.classList.remove('connect-target'));

      if (targetNode && targetNode.id !== `node-${connectingFrom.nodeId}`) {
        targetNode.classList.add('connect-target');
        // 吸附到目标节点的输入端口位置
        const tgtId = targetNode.id.replace('node-', '');
        const tgtNodeData = state.currentWorkflow?.nodes?.find(n => n.id === tgtId);
        if (tgtNodeData) {
          x2 = tgtNodeData.position_x;
          y2 = tgtNodeData.position_y + targetNode.offsetHeight - 11;
        }
      }

      // 绘制平滑曲线
      const controlOffset = Math.min(100, Math.abs(x2 - x1) / 2);
      dragLine.setAttribute('d', `M${x1},${y1} C${x1 + controlOffset},${y1} ${x2 - controlOffset},${y2} ${x2},${y2}`);
    }

    async function endDragConnect(e) {
      // 移除高亮
      document.querySelectorAll('.port.dragging').forEach(p => p.classList.remove('dragging'));
      document.querySelectorAll('.node.connect-target').forEach(n => n.classList.remove('connect-target'));

      // 隐藏临时线
      const dragLine = document.getElementById('dragLine');
      if (dragLine) dragLine.style.display = 'none';

      // 移除事件监听
      document.removeEventListener('mousemove', onDragConnect);
      document.removeEventListener('mouseup', endDragConnect);

      // 检测目标节点
      if (connectingFrom && state.currentWorkflow) {
        const targetEl = document.elementFromPoint(e.clientX, e.clientY);
        const targetNode = targetEl ? targetEl.closest('.node') : null;

        if (targetNode) {
          const targetId = targetNode.id.replace('node-', '');
          if (targetId !== connectingFrom.nodeId) {
            // 检查目标节点是否是开始节点（开始节点不能有输入连线）
            const targetNodeData = state.currentWorkflow.nodes.find(n => n.id === targetId);
            if (targetNodeData && targetNodeData.type === 'start') {
              showToast('warn', '开始节点不能有输入连线');
              connectingFrom = null;
              isDraggingConnect = false;
              return;
            }

            try {
              addLog('info', `创建连线: ${connectingFrom.nodeId} -> ${targetId} (${connectingFrom.type})`);

              const res = await fetch(`${API}/workflows/${state.currentWorkflow.id}/edges`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                  source: connectingFrom.nodeId,
                  target: targetId,
                  type: connectingFrom.type
                })
              });

              if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
              }

              const data = await res.json();
              if (data.success) {
                await selectWorkflow(state.currentWorkflow.id);
                showToast('success', '连线创建成功');
              } else {
                showToast('error', data.message || '创建连线失败');
              }
            } catch (err) {
              showToast('error', '创建连线失败');
              addLog('error', '创建连线失败: ' + err.message);
            }
          }
        }
      }

      isDraggingConnect = false;
      connectingFrom = null;
    }

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

