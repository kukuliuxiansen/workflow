// 点击式连线（备选模式）

    // 保留旧的点击模式作为备选
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