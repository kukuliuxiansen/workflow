
    // 节点选择管理
    function clearNodeSelection() {
      state.selectedNode = null;
      state.selectedNodes.clear();
      updateSelectedCount();
    }

    function selectAllNodes() {
      if (state.currentWorkflow && state.currentWorkflow.nodes) {
        state.selectedNodes.clear();
        state.currentWorkflow.nodes.forEach(node => state.selectedNodes.add(node.id));
        if (state.selectedNodes.size === 1) {
          state.selectedNode = state.currentWorkflow.nodes[0];
        } else {
          state.selectedNode = null;
        }
        renderCanvas();
        updateSelectedCount();
        showToast('info', `已选中 ${state.selectedNodes.size} 个节点`);
      }
    }

    function invertSelection() {
      if (state.currentWorkflow && state.currentWorkflow.nodes) {
        const allNodeIds = new Set(state.currentWorkflow.nodes.map(n => n.id));
        const newSelection = new Set();
        allNodeIds.forEach(id => {
          if (!state.selectedNodes.has(id)) {
            newSelection.add(id);
          }
        });
        state.selectedNodes = newSelection;
        state.selectedNode = null;
        renderCanvas();
        updateSelectedCount();
      }
    }

    // 复制/粘贴/剪切功能
    function copyNodes() {
      const nodesToCopy = [];
      if (state.selectedNode) {
        nodesToCopy.push(state.selectedNode);
      } else if (state.selectedNodes.size > 0) {
        state.currentWorkflow.nodes.forEach(node => {
          if (state.selectedNodes.has(node.id)) {
            nodesToCopy.push(node);
          }
        });
      }

      // 过滤掉开始和结束节点（不能复制）
      const filteredNodes = nodesToCopy.filter(node => {
        if (node.type === 'start' || node.type === 'finish') {
          showToast('warn', `开始和结束节点不能复制: ${node.name}`);
          return false;
        }
        return true;
      });

      if (filteredNodes.length > 0) {
        state.clipboard = {
          nodes: JSON.parse(JSON.stringify(filteredNodes)),
          edges: [] // TODO: 也要复制连线
        };
        showToast('success', `已复制 ${filteredNodes.length} 个节点`);
      } else if (nodesToCopy.length > 0) {
        showToast('warn', '选中的节点无法复制');
      }
    }

    async function pasteNodes() {
      if (!state.clipboard || !state.clipboard.nodes || state.clipboard.nodes.length === 0) {
        showToast('warn', '剪贴板为空');
        return;
      }
      if (!state.currentWorkflow) {
        showToast('warn', '请先选择工作流');
        return;
      }

      clearNodeSelection();

      const offset = 30;
      let successCount = 0;

      for (let i = 0; i < state.clipboard.nodes.length; i++) {
        const node = state.clipboard.nodes[i];
        try {
          const res = await fetch(`${API}/workflows/${state.currentWorkflow.id}/nodes`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              type: node.type,
              name: node.name + ' (副本)',
              position_x: node.position_x + offset,
              position_y: node.position_y + offset,
              config: node.config
            })
          });
          const data = await res.json();
          if (data.success) {
            successCount++;
            state.selectedNodes.add(data.data.id);
          }
        } catch (e) {
          console.error('粘贴节点失败:', e);
        }
      }

      await selectWorkflow(state.currentWorkflow.id);
      updateSelectedCount();
      if (successCount > 0) {
        showToast('success', `已粘贴 ${successCount} 个节点`);
      } else {
        showToast('error', '粘贴失败');
      }
    }

    function cutNodes() {
      copyNodes();
      deleteSelectedNodes();
    }

    function duplicateNodes() {
      copyNodes();
      pasteNodes();
    }

    // 删除选中节点
    async function deleteSelectedNodes() {
      if (!state.currentWorkflow) return;

      const nodeIdsToDelete = [];
      if (state.selectedNode) {
        nodeIdsToDelete.push(state.selectedNode.id);
      } else if (state.selectedNodes.size > 0) {
        nodeIdsToDelete.push(...state.selectedNodes);
      }

      if (nodeIdsToDelete.length === 0) return;

      // 过滤掉开始和结束节点（不能删除）
      const filteredNodeIds = nodeIdsToDelete.filter(nodeId => {
        const node = state.currentWorkflow.nodes.find(n => n.id === nodeId);
        if (node && (node.type === 'start' || node.type === 'finish')) {
          showToast('warn', `开始和结束节点不能删除: ${node.name}`);
          return false;
        }
        return true;
      });

      if (filteredNodeIds.length === 0) {
        showToast('warn', '选中的节点无法删除');
        return;
      }

      let successCount = 0;
      for (const nodeId of filteredNodeIds) {
        try {
          await fetch(`${API}/workflows/${state.currentWorkflow.id}/nodes/${nodeId}`, {
            method: 'DELETE'
          });
          successCount++;
        } catch (e) {
          console.error('删除节点失败:', nodeId, e);
        }
      }

      clearNodeSelection();
      await selectWorkflow(state.currentWorkflow.id);
      renderPropertyPanel();

      if (successCount > 0) {
        showToast('success', `已删除 ${successCount} 个节点`);
      } else {
        showToast('error', '删除失败');
      }
    }
