// 节点和执行相关函数

    // 添加节点（模态框方式）
    async function addNode() {
      if (!state.currentWorkflow) {
        showToast('warn', '请先选择工作流');
        return;
      }

      const name = document.getElementById('newNodeName').value || '新节点';
      try {
        await fetch(`${API}/workflows/${state.currentWorkflow.id}/nodes`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            type: state.selectedNodeType,
            name,
            position_x: 100 + ((state.currentWorkflow.nodes?.length || 0) % 3) * 280,
            position_y: 100 + Math.floor((state.currentWorkflow.nodes?.length || 0) / 3) * 180
          })
        });
        closeModal('addNodeModal');
        await selectWorkflow(state.currentWorkflow.id);
        showToast('success', '节点已添加');
      } catch (e) {
        showToast('error', '添加失败');
      }
    }

    // AI生成提示词
    async function generatePrompt(e) {
      e.preventDefault();
      const btn = e.target;
      const node = state.selectedNode;
      if (!node) return;

      btn.disabled = true;
      btn.textContent = '⏳ 生成中...';

      try {
        const res = await fetch(`${API}/ai/generate-prompt`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            nodeInfo: {
              id: node.id,
              name: node.name,
              type: node.type,
              on_success: node.on_success,
              on_fail: node.on_fail
            },
            workflowContext: {
              name: state.currentWorkflow?.name,
              nodes: state.currentWorkflow?.nodes
            }
          })
        });
        const data = await res.json();
        if (data.success) {
          const textarea = document.getElementById('promptTextarea');
          if (textarea) {
            textarea.value = data.data.prompt;
            updateNode('prompt', data.data.prompt);
          }
          showToast('success', '提示词已生成');
        }
      } catch (e) {
        showToast('error', '生成失败: ' + e.message);
      } finally {
        btn.disabled = false;
        btn.textContent = '✨ AI生成';
      }
    }

    // 重新执行
    async function restartExecution(executionId) {
      if (!await confirmAsync('确定要重新执行此任务吗？')) return;

      try {
        const res = await fetch(`${API}/executions/records/${executionId}/restart`, {
          method: 'POST'
        });
        const data = await res.json();
        if (data.success) {
          showToast('success', '已启动重新执行');
          closeHistoryPanel();

          state.execution = data.data;
          taskConfig.contextFilePath = data.data.contextFilePath || '';
          connectWS(data.data.executionId);
          document.getElementById('btnExecute').style.display = 'none';
          document.getElementById('btnStop').style.display = 'inline-flex';
          addLog('info', '重新执行: ' + data.data.executionId);
        }
      } catch (e) {
        showToast('error', '重新执行失败');
      }
    }

    // 查看执行记录
    async function viewExecutionRecord(executionId) {
      try {
        const res = await fetch(`${API}/executions/records/${executionId}`);
        const data = await res.json();
        if (data.success) {
          const record = data.data;
          const statusColors = {
            'running': { bg: '#3b82f6', icon: '⚡' },
            'completed': { bg: '#22c55e', icon: '✓' },
            'failed': { bg: '#ef4444', icon: '✕' },
            'stopped': { bg: '#f97316', icon: '◼' }
          };
          const statusStyle = statusColors[record.status] || { bg: '#6b7280', icon: '?' };

          const modalHtml = `
            <div class="modal-overlay show" onclick="this.remove()">
              <div class="modal" style="min-width:420px;" onclick="event.stopPropagation()">
                <div style="padding:24px;border-bottom:1px solid #333;display:flex;align-items:center;gap:16px;">
                  <div style="width:48px;height:48px;background:${statusStyle.bg};border-radius:12px;display:flex;align-items:center;justify-content:center;font-size:24px;">${statusStyle.icon}</div>
                  <div>
                    <h3 style="margin:0;font-size:18px;">${record.taskConfig?.name || '未命名任务'}</h3>
                    <p style="margin:4px 0 0;font-size:12px;color:#888;">执行详情</p>
                  </div>
                  <button onclick="this.closest('.modal-overlay').remove()" style="margin-left:auto;background:#333;border:none;width:32px;height:32px;border-radius:8px;color:#888;cursor:pointer;font-size:18px;">&times;</button>
                </div>
                <div style="padding:20px;max-height:400px;overflow-y:auto;">
                  <div style="display:grid;gap:12px;">
                    <div style="display:flex;justify-content:space-between;padding:12px;background:#1a1a1a;border-radius:10px;">
                      <span style="color:#888;font-size:13px;">执行ID</span>
                      <code style="color:#89b4fa;">${record.executionId || record.id}</code>
                    </div>
                    <div style="display:flex;justify-content:space-between;padding:12px;background:#1a1a1a;border-radius:10px;">
                      <span style="color:#888;font-size:13px;">状态</span>
                      <span style="color:${statusStyle.bg};">${getStatusText(record.status)}</span>
                    </div>
                    <div style="display:flex;justify-content:space-between;padding:12px;background:#1a1a1a;border-radius:10px;">
                      <span style="color:#888;font-size:13px;">开始时间</span>
                      <span style="color:#e0e0e0;">${formatDate(record.startTime)}</span>
                    </div>
                  </div>
                </div>
                <div style="padding:16px 20px;border-top:1px solid #333;display:flex;justify-content:flex-end;">
                  <button onclick="this.closest('.modal-overlay').remove()" style="padding:10px 24px;background:#6366f1;border:none;border-radius:10px;color:#fff;cursor:pointer;">关闭</button>
                </div>
              </div>
            </div>`;

          document.body.insertAdjacentHTML('beforeend', modalHtml);
        }
      } catch (e) {
        showToast('error', '加载详情失败');
      }
    }

    // 显示执行详情（兼容）
    async function showExecutionDetail(id) {
      await viewExecutionRecord(id);
    }