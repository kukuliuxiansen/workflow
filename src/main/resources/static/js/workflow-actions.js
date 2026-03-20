// 工作流管理函数

    // 创建工作流
    async function createWorkflow() {
      const name = document.getElementById('newWorkflowName').value;
      if (!name) { showToast('warn', '请输入名称'); return; }

      const id = 'wf_' + Date.now();
      try {
        await fetch(`${API}/workflows`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            id,
            name,
            description: document.getElementById('newWorkflowDesc').value,
            nodes: [
              { id: 'start', type: 'start', name: '开始', position_x: 100, position_y: 100 },
              { id: 'finish', type: 'finish', name: '结束', position_x: 500, position_y: 100 }
            ]
          })
        });
        closeModal('createModal');
        await loadWorkflows();
        await selectWorkflow(id);
        showToast('success', '创建成功');
      } catch (e) {
        showToast('error', '创建失败');
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

    // 删除工作流
    async function deleteWorkflow(id) {
      if (!await confirmAsync('确定要删除此工作流吗？\n此操作不可恢复。')) return;

      try {
        const res = await fetch(`${API}/workflows/${id}`, { method: 'DELETE' });
        const data = await res.json();
        if (data.success) {
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

    // 从模板创建
    async function createFromTemplate(id) {
      const name = prompt('请输入名称:', '新工作流');
      if (!name) return;
      try {
        const res = await fetch(`${API}/templates/${id}/create-workflow`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name })
        });
        const data = await res.json();
        if (data.success) {
          closeModal('templateModal');
          await loadWorkflows();
          await selectWorkflow(data.data.id);
          showToast('success', '创建成功');
        }
      } catch (e) {
        showToast('error', '创建失败');
      }
    }

    // 显示模板选择
    async function showTemplateModal() {
      document.getElementById('templateModal').classList.add('show');
      const list = document.getElementById('templateList');
      try {
        const res = await fetch(`${API}/templates`);
        const data = await res.json();
        if (data.success && data.data.length > 0) {
          list.innerHTML = data.data.map(t => `
            <div class="template-card" onclick="createFromTemplate('${t.id}')">
              <div class="name">${t.name}</div>
              <div class="desc">${t.description}</div>
              <div class="meta">${t.nodeCount} 个节点</div>
            </div>
          `).join('');
        } else {
          list.innerHTML = '<div class="empty-state"><div class="title">暂无模板</div></div>';
        }
      } catch (e) {
        list.innerHTML = '<div class="empty-state"><div class="title">加载失败</div></div>';
      }
    }

    // 渲染工作流项
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
        </div>`;
    }