// 切换文件夹展开状态
function toggleFolder(folderId) {
  if (state.expandedFolders.has(folderId)) {
    state.expandedFolders.delete(folderId);
  } else {
    state.expandedFolders.add(folderId);
  }
  renderWorkflowList();
}

// 创建文件夹
async function createFolder() {
  const name = prompt('请输入文件夹名称:', '新文件夹');
  if (!name) return;

  try {
    const res = await fetch(`${API}/folders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name })
    });
    const data = await res.json();
    if (data.success) {
      await loadWorkflows();
      showToast('success', '文件夹创建成功');
    } else {
      showToast('error', data.message || '创建失败');
    }
  } catch (e) {
    showToast('error', '创建失败: ' + e.message);
  }
}

// 重命名文件夹
async function renameFolder(folderId) {
  const folder = state.folders.find(f => f.id === folderId);
  if (!folder) return;

  const name = prompt('请输入新的文件夹名称:', folder.name);
  if (!name || name === folder.name) return;

  try {
    const res = await fetch(`${API}/folders/${folderId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name })
    });
    const data = await res.json();
    if (data.success) {
      await loadWorkflows();
      showToast('success', '文件夹已重命名');
    } else {
      showToast('error', data.message || '重命名失败');
    }
  } catch (e) {
    showToast('error', '重命名失败: ' + e.message);
  }
}

// 删除文件夹
async function deleteFolder(folderId) {
  const folder = state.folders.find(f => f.id === folderId);
  if (!folder) return;

  if (!await confirmAsync(`确定要删除文件夹"${folder.name}"吗？\n文件夹内的工作流将移至根目录。`)) return;

  try {
    const res = await fetch(`${API}/folders/${folderId}`, { method: 'DELETE' });
    const data = await res.json();
    if (data.success) {
      await loadWorkflows();
      showToast('success', '文件夹已删除');
    } else {
      showToast('error', data.message || '删除失败');
    }
  } catch (e) {
    showToast('error', '删除失败: ' + e.message);
  }
}

// 文件夹拖拽处理
function handleFolderDragOver(e, folderId) {
  e.preventDefault();
  e.stopPropagation();
  if (state.draggedWorkflowId) {
    e.currentTarget.classList.add('drag-over');
  }
}

function handleFolderDragLeave(e) {
  e.currentTarget.classList.remove('drag-over');
}

async function handleFolderDrop(e, folderId) {
  e.preventDefault();
  e.stopPropagation();
  e.currentTarget.classList.remove('drag-over');

  const workflowId = state.draggedWorkflowId || e.dataTransfer.getData('workflowId');
  if (!workflowId) return;

  // 移动到文件夹或根目录
  const targetFolderId = folderId === 'root' ? null : folderId;

  try {
    const res = await fetch(`${API}/workflows/${workflowId}/move`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ folderId: targetFolderId })
    });
    const data = await res.json();
    if (data.success) {
      // 更新本地状态
      const workflow = state.workflows.find(w => w.id === workflowId);
      if (workflow) {
        workflow.folderId = targetFolderId;
      }
      renderWorkflowList();
      showToast('success', '工作流已移动');
    } else {
      showToast('error', data.message || '移动失败');
    }
  } catch (e) {
    showToast('error', '移动失败: ' + e.message);
  }
}

