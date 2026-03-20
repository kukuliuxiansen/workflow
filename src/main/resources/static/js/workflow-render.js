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

