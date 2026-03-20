    }

    // 初始化
    document.addEventListener('DOMContentLoaded', async () => {
      loadPanelStates();
      loadSavedGlobalConfig();  // 加载全局配置
      await loadWorkflows();  // 等待工作流加载完成
      initUXFeatures();  // 初始化UX功能

      // 恢复上次选择的工作流
      const prefs = getPrefs();
      if (prefs.lastWorkflowId) {
        await selectWorkflow(prefs.lastWorkflowId);
      }

      // 设置本地文件上传事件监听
      const pathLocalFile = document.getElementById('pathLocalFile');
      if (pathLocalFile) {
        pathLocalFile.addEventListener('change', async (e) => {
          const file = e.target.files[0];
          if (file && state.pathSelect.onLocalUpload) {
            await state.pathSelect.onLocalUpload(file);
            closeModal('pathSelectModal');
          }
        });
      }
    });

    // 加载工作流列表
    async function loadWorkflows() {
      console.log('开始加载工作流列表...');
      try {
        const [workflowsRes, foldersRes] = await Promise.all([
          fetch(`${API}/workflows`),
          fetch(`${API}/folders`)
        ]);
        console.log('API响应:', workflowsRes.status, foldersRes.status);
        const workflowsData = await workflowsRes.json();
        const foldersData = await foldersRes.json();
        console.log('工作流数据:', workflowsData);
        console.log('文件夹数据:', foldersData);

        if (workflowsData.success && Array.isArray(workflowsData.data)) {
          state.workflows = workflowsData.data;
        } else {
          state.workflows = [];
        }
        if (foldersData.success && Array.isArray(foldersData.data)) {
          state.folders = foldersData.data;
        } else {
          state.folders = [];
        }
        console.log('开始渲染工作流列表...');
        renderWorkflowList();
        console.log('工作流列表渲染完成');
      } catch (e) {
        console.error('loadWorkflows error:', e);
        alert('加载失败: ' + (e.message || e) + '\n请检查控制台获取详细信息');
      }
    }

    // 渲染工作流列表（支持文件夹分组）
    function renderWorkflowList() {
      const list = document.getElementById('workflowList');
      const searchInput = document.getElementById('searchInput');
      const search = searchInput ? searchInput.value.toLowerCase() : '';

      // 过滤工作流 - 兼容 snake_case 和 camelCase
      const workflows = state.workflows || [];
      const folders = state.folders || [];
      const filtered = workflows.filter(w =>
        w && w.name && w.id && (w.name.toLowerCase().includes(search) || w.id.toLowerCase().includes(search))
      );

      if (filtered.length === 0 && folders.length === 0) {
        list.innerHTML = '<div class="empty-state"><div class="icon">📭</div><div class="title">暂无工作流</div></div>';
        return;
      }

      let html = '';

      // 渲染文件夹
      folders.forEach(folder => {
        const isExpanded = state.expandedFolders.has(folder.id);
        // 兼容 folder_id 和 folderId
        const folderWorkflows = filtered.filter(w => (w.folderId || w.folder_id) === folder.id);

        html += `
          <div class="folder-item">
            <div class="folder-header ${state.draggedWorkflowId ? 'drag-target' : ''}"
                 data-folder-id="${folder.id}"
                 onclick="toggleFolder('${folder.id}')"
                 ondragover="handleFolderDragOver(event, '${folder.id}')"
                 ondragleave="handleFolderDragLeave(event)"
                 ondrop="handleFolderDrop(event, '${folder.id}')">
              <div class="folder-toggle ${isExpanded ? 'expanded' : ''}">▶</div>
              <span class="folder-icon">📁</span>
              <span class="folder-name">${folder.name}</span>
              <span class="folder-count">${folderWorkflows.length}</span>
              <div class="folder-actions">
                <button class="folder-action-btn" onclick="event.stopPropagation(); renameFolder('${folder.id}')" title="重命名">✏️</button>
                <button class="folder-action-btn danger" onclick="event.stopPropagation(); deleteFolder('${folder.id}')" title="删除">🗑️</button>
              </div>
            </div>
            <div class="folder-content ${isExpanded ? 'expanded' : ''}" id="folder-content-${folder.id}">
              ${folderWorkflows.map(w => renderWorkflowItem(w)).join('')}
            </div>
          </div>
        `;
      });

      // 渲染未分组的工作流（根目录）- 兼容 snake_case 和 camelCase
      const rootWorkflows = filtered.filter(w => {
        const fid = w.folderId || w.folder_id;
        return !fid || !folders.find(f => f.id === fid);
      });
      if (rootWorkflows.length > 0) {
        html += `<div class="folder-drop-zone" data-folder-id="root"
                      ondragover="handleFolderDragOver(event, 'root')"
                      ondragleave="handleFolderDragLeave(event)"
                      ondrop="handleFolderDrop(event, 'root')"></div>`;
        html += rootWorkflows.map(w => renderWorkflowItem(w)).join('');
      }

      // 新建文件夹按钮
      html += `
        <div class="new-folder-btn" onclick="createFolder()">
          <span class="icon">➕</span>
          <span>新建文件夹</span>
        </div>
      `;

      list.innerHTML = html;
    }

    // 渲染单个工作流项
    function renderWorkflowItem(w) {
      const nodeCount = w.node_count ?? w.nodeCount ?? w.nodes?.length ?? 0;
      return `
        <div class="workflow-item ${state.currentWorkflow?.id === w.id ? 'active' : ''}"
             data-id="${w.id}"
             draggable="true"
             ondragstart="handleWorkflowDragStart(event, '${w.id}')"
             ondragend="handleWorkflowDragEnd(event)"
             onclick="selectWorkflow('${w.id}')">
          <div class="workflow-item-content">
            <div class="name">${w.name}</div>
            <div class="meta">${nodeCount} 个节点 · ${formatDate(w.updated_at || w.updatedAt)}</div>
          </div>
          <div class="workflow-actions">
            <button class="workflow-action-btn" onclick="event.stopPropagation(); cloneWorkflow('${w.id}')" title="克隆">📋</button>
            <button class="workflow-action-btn danger" onclick="event.stopPropagation(); deleteWorkflow('${w.id}')" title="删除">🗑️</button>
          </div>
        </div>
      `;
    }

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

