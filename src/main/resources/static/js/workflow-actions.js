// 工作流拖拽处理
function handleWorkflowDragStart(e, workflowId) {
  e.dataTransfer.setData('workflowId', workflowId);
  e.dataTransfer.effectAllowed = 'move';
  state.draggedWorkflowId = workflowId;
  e.target.style.opacity = '0.5';
  // 高亮可放置区域
  document.querySelectorAll('.folder-header').forEach(el => {
    el.style.borderColor = '#6b5ce7';
  });
}

function handleWorkflowDragEnd(e) {
  e.target.style.opacity = '1';
  state.draggedWorkflowId = null;
  // 移除高亮
  document.querySelectorAll('.folder-header').forEach(el => {
    el.style.borderColor = '';
  });
  document.querySelectorAll('.drag-over').forEach(el => {
    el.classList.remove('drag-over');
  });
}

// 删除工作流
async function deleteWorkflow(id) {
  if (!await confirmAsync('确定要删除此工作流吗？\n此操作不可恢复。')) return;

  try {
    const res = await fetch(`${API}/workflows/${id}`, { method: 'DELETE' });
    const data = await res.json();
    if (data.success) {
      // 如果删除的是当前工作流，清空选择
      if (state.currentWorkflow?.id === id) {
        state.currentWorkflow = null;
        state.selectedNode = null;
        renderCanvas();
        renderPropertyPanel();
      }
      await loadWorkflows();
      showToast('success', '工作流已删除');
    } else {
      showToast('error', data.message || '删除失败');
    }
  } catch (e) {
    showToast('error', '删除失败: ' + e.message);
  }
}

// 克隆工作流
async function cloneWorkflow(id) {
  const name = prompt('请输入新工作流名称:', '副本');
  if (!name) return;

  try {
    const res = await fetch(`${API}/workflows/${id}/clone`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name })
    });
    const data = await res.json();
    if (data.success) {
      await loadWorkflows();
      await selectWorkflow(data.data.id);
      showToast('success', '工作流已克隆');
    } else {
      showToast('error', data.message || '克隆失败');
    }
  } catch (e) {
    showToast('error', '克隆失败: ' + e.message);
  }
}

