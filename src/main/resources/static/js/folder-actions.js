// 文件夹管理函数

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

    // 展开/折叠文件夹
    function toggleFolder(folderId) {
      if (state.expandedFolders.has(folderId)) {
        state.expandedFolders.delete(folderId);
      } else {
        state.expandedFolders.add(folderId);
      }
      renderWorkflowList();
    }

    // 文件夹拖拽悬停
    function handleFolderDragOver(e, folderId) {
      e.preventDefault();
      e.stopPropagation();
      if (state.draggedWorkflowId) {
        e.currentTarget.classList.add('drag-over');
      }
    }

    // 文件夹拖拽离开
    function handleFolderDragLeave(e) {
      e.currentTarget.classList.remove('drag-over');
    }

    // 文件夹拖拽放下
    async function handleFolderDrop(e, folderId) {
      e.preventDefault();
      e.stopPropagation();
      e.currentTarget.classList.remove('drag-over');

      const workflowId = state.draggedWorkflowId || e.dataTransfer.getData('workflowId');
      if (!workflowId) return;

      const targetFolderId = folderId === 'root' ? null : folderId;

      try {
        const res = await fetch(`${API}/workflows/${workflowId}/move`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ targetFolderId })
        });
        const data = await res.json();
        if (data.success) {
          const workflow = state.workflows.find(w => w.id === workflowId);
          if (workflow) workflow.folderId = targetFolderId;
          renderWorkflowList();
          showToast('success', '工作流已移动');
        } else {
          showToast('error', data.message || '移动失败');
        }
      } catch (e) {
        showToast('error', '移动失败: ' + e.message);
      }
    }

    // 工作流拖拽开始
    function handleWorkflowDragStart(e, workflowId) {
      e.dataTransfer.setData('workflowId', workflowId);
      e.dataTransfer.effectAllowed = 'move';
      state.draggedWorkflowId = workflowId;
      e.target.style.opacity = '0.5';
      document.querySelectorAll('.folder-header').forEach(el => {
        el.style.borderColor = '#6b5ce7';
      });
    }

    // 工作流拖拽结束
    function handleWorkflowDragEnd(e) {
      e.target.style.opacity = '1';
      state.draggedWorkflowId = null;
      document.querySelectorAll('.folder-header').forEach(el => {
        el.style.borderColor = '';
      });
      document.querySelectorAll('.drag-over').forEach(el => {
        el.classList.remove('drag-over');
      });
    }