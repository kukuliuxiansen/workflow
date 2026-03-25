// 工作流管理函数

    // 前端定义的常用模板
    const WORKFLOW_TEMPLATES = [
      {
        id: 'tpl-basic',
        name: '基础工作流',
        description: '包含开始和结束节点的简单工作流',
        nodes: [
          { id: 'start', type: 'start', name: '开始', position_x: 100, position_y: 200 },
          { id: 'finish', type: 'finish', name: '结束', position_x: 500, position_y: 200 }
        ],
        edges: [{ source: 'start', target: 'finish', sourcePort: 'success', targetPort: 'input' }]
      },
      {
        id: 'tpl-agent',
        name: 'Agent执行流程',
        description: '包含Agent执行节点的工作流',
        nodes: [
          { id: 'start', type: 'start', name: '开始', position_x: 100, position_y: 200 },
          { id: 'agent1', type: 'agent_execution', name: 'Agent执行', position_x: 300, position_y: 200 },
          { id: 'finish', type: 'finish', name: '结束', position_x: 500, position_y: 200 }
        ],
        edges: [
          { source: 'start', target: 'agent1', sourcePort: 'success', targetPort: 'input' },
          { source: 'agent1', target: 'finish', sourcePort: 'success', targetPort: 'input' }
        ]
      },
      {
        id: 'tpl-condition',
        name: '条件分支流程',
        description: '包含条件判断的工作流',
        nodes: [
          { id: 'start', type: 'start', name: '开始', position_x: 100, position_y: 200 },
          { id: 'cond1', type: 'condition', name: '条件判断', position_x: 250, position_y: 200 },
          { id: 'agent1', type: 'agent_execution', name: 'Agent执行', position_x: 400, position_y: 100 },
          { id: 'agent2', type: 'agent_execution', name: 'Agent执行', position_x: 400, position_y: 300 },
          { id: 'finish', type: 'finish', name: '结束', position_x: 550, position_y: 200 }
        ],
        edges: [
          { source: 'start', target: 'cond1', sourcePort: 'success', targetPort: 'input' },
          { source: 'cond1', target: 'agent1', sourcePort: 'success', targetPort: 'input' },
          { source: 'cond1', target: 'agent2', sourcePort: 'fail', targetPort: 'input' },
          { source: 'agent1', target: 'finish', sourcePort: 'success', targetPort: 'input' },
          { source: 'agent2', target: 'finish', sourcePort: 'success', targetPort: 'input' }
        ]
      },
      {
        id: 'tpl-review',
        name: '人工审核流程',
        description: '包含人工审核节点的流程',
        nodes: [
          { id: 'start', type: 'start', name: '开始', position_x: 100, position_y: 200 },
          { id: 'agent1', type: 'agent_execution', name: 'Agent执行', position_x: 250, position_y: 200 },
          { id: 'review1', type: 'human_review', name: '人工审核', position_x: 400, position_y: 200 },
          { id: 'finish', type: 'finish', name: '结束', position_x: 550, position_y: 200 }
        ],
        edges: [
          { source: 'start', target: 'agent1', sourcePort: 'success', targetPort: 'input' },
          { source: 'agent1', target: 'review1', sourcePort: 'success', targetPort: 'input' },
          { source: 'review1', target: 'finish', sourcePort: 'success', targetPort: 'input' }
        ]
      }
    ];

    // 创建工作流
    async function createWorkflow() {
      const name = document.getElementById('newWorkflowName').value;
      if (!name) { showToast('warn', '请输入名称'); return; }

      try {
        const res = await fetch(`${API}/workflows`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name,
            description: document.getElementById('newWorkflowDesc').value,
            nodes: [
              { id: 'start', type: 'start', name: '开始', position_x: 100, position_y: 100 },
              { id: 'finish', type: 'finish', name: '结束', position_x: 500, position_y: 100 }
            ]
          })
        });
        const data = await res.json();
        if (data.success && data.data) {
          const actualId = data.data.id;  // 使用后端返回的实际ID
          closeModal('createModal');
          await loadWorkflows();
          await selectWorkflow(actualId);
          showToast('success', '创建成功');
        } else {
          showToast('error', data.message || '创建失败');
        }
      } catch (e) {
        showToast('error', '创建失败: ' + e.message);
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

    // 从模板创建（使用前端定义的模板）
    async function createFromTemplate(templateId) {
      const template = WORKFLOW_TEMPLATES.find(t => t.id === templateId);
      if (!template) {
        showToast('error', '模板不存在');
        return;
      }

      const name = prompt('请输入名称:', template.name);
      if (!name) return;

      try {
        // 深拷贝节点并生成新ID
        const nodeIdMap = {};
        const newNodes = template.nodes.map(node => {
          const newId = 'node_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
          nodeIdMap[node.id] = newId;
          return {
            ...node,
            id: newId
          };
        });

        // 更新边的源和目标节点ID
        const newEdges = template.edges.map(edge => ({
          ...edge,
          source: nodeIdMap[edge.source],
          target: nodeIdMap[edge.target]
        }));

        // 创建工作流
        const res = await fetch(`${API}/workflows`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name,
            description: template.description,
            nodes: newNodes,
            edges: newEdges
          })
        });
        const data = await res.json();

        if (data.success && data.data) {
          const actualId = data.data.id;
          closeModal('templateModal');
          await loadWorkflows();
          await selectWorkflow(actualId);
          showToast('success', '创建成功');
        } else {
          showToast('error', data.message || '创建失败');
        }
      } catch (e) {
        showToast('error', '创建失败: ' + e.message);
      }
    }

    // 显示模板选择（使用前端定义的模板）
    function showTemplateModal() {
      document.getElementById('templateModal').classList.add('show');
      const list = document.getElementById('templateList');

      if (WORKFLOW_TEMPLATES.length > 0) {
        list.innerHTML = WORKFLOW_TEMPLATES.map(t => `
          <div class="template-card" onclick="createFromTemplate('${t.id}')">
            <div class="name">${t.name}</div>
            <div class="desc">${t.description || ''}</div>
            <div class="meta">${t.nodes.length} 个节点</div>
          </div>
        `).join('');
      } else {
        list.innerHTML = '<div class="empty-state"><div class="title">暂无模板</div></div>';
      }
    }

    // 渲染工作流项
    function renderWorkflowItem(w) {
      const nodeCount = w.node_count ?? w.nodeCount ?? w.nodes?.length ?? 0;
      const description = w.description || '';
      const shortDesc = description.length > 30 ? description.substring(0, 30) + '...' : description;
      return `
        <div class="workflow-item ${state.currentWorkflow?.id === w.id ? 'active' : ''}"
             data-id="${w.id}"
             draggable="true"
             ondragstart="handleWorkflowDragStart(event, '${w.id}')"
             ondragend="handleWorkflowDragEnd(event)"
             onclick="selectWorkflow('${w.id}')">
          <div class="workflow-item-content">
            <div class="name">${w.name}</div>
            ${shortDesc ? `<div class="desc">${shortDesc}</div>` : ''}
            <div class="meta">${nodeCount} 个节点 · ${formatDate(w.updated_at || w.updatedAt)}</div>
          </div>
          <div class="workflow-actions">
            <button class="workflow-action-btn" onclick="event.stopPropagation(); editWorkflowInfo('${w.id}')" title="编辑">✏️</button>
            <button class="workflow-action-btn" onclick="event.stopPropagation(); cloneWorkflow('${w.id}')" title="克隆">📋</button>
            <button class="workflow-action-btn danger" onclick="event.stopPropagation(); deleteWorkflow('${w.id}')" title="删除">🗑️</button>
          </div>
        </div>`;
    }

    // 编辑工作流信息
    async function editWorkflowInfo(id) {
      const workflow = state.workflows.find(w => w.id === id);
      if (!workflow) return;

      const modal = document.createElement('div');
      modal.className = 'modal-overlay show';
      modal.id = 'editWorkflowModal';
      modal.innerHTML = `
        <div class="modal" style="max-width:400px;">
          <div class="modal-header">
            <h3>编辑工作流</h3>
            <button class="close-btn" onclick="closeModal('editWorkflowModal')">&times;</button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label class="form-label">名称</label>
              <input type="text" class="form-input" id="editWorkflowName" value="${workflow.name || ''}" placeholder="工作流名称">
            </div>
            <div class="form-group">
              <label class="form-label">描述</label>
              <textarea class="form-input form-textarea" id="editWorkflowDesc" placeholder="工作流描述">${workflow.description || ''}</textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-primary" onclick="saveWorkflowInfo('${id}')">保存</button>
            <button class="btn btn-secondary" onclick="closeModal('editWorkflowModal')">取消</button>
          </div>
        </div>
      `;
      document.body.appendChild(modal);
    }

    // 保存工作流信息
    async function saveWorkflowInfo(id) {
      const name = document.getElementById('editWorkflowName').value.trim();
      const description = document.getElementById('editWorkflowDesc').value.trim();

      if (!name) {
        showToast('warn', '请输入名称');
        return;
      }

      try {
        const res = await fetch(`${API}/workflows/${id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name,
            description,
            nodes: state.currentWorkflow?.nodes || [],
            edges: state.currentWorkflow?.edges || []
          })
        });
        const data = await res.json();
        if (data.success) {
          closeModal('editWorkflowModal');
          await loadWorkflows();
          if (state.currentWorkflow?.id === id) {
            state.currentWorkflow.name = name;
            state.currentWorkflow.description = description;
          }
          showToast('success', '保存成功');
        } else {
          showToast('error', data.message || '保存失败');
        }
      } catch (e) {
        showToast('error', '保存失败: ' + e.message);
      }
    }