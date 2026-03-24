// 执行历史管理

    // 打开执行历史面板
    async function openHistoryPanel() {
      const panel = document.getElementById('historyPanel');
      panel.classList.add('show');
      await refreshHistoryList();
    }

    // 关闭执行历史面板
    function closeHistoryPanel() {
      document.getElementById('historyPanel').classList.remove('show');
    }

    // 刷新历史记录列表
    async function refreshHistoryList() {
      const list = document.getElementById('historyList');
      list.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

      try {
        const workflowId = state.currentWorkflow?.id;
        const url = workflowId
          ? `${API}/executions/records?workflowId=${workflowId}`
          : `${API}/executions/records`;

        const res = await fetch(url);
        const data = await res.json();
        if (data.success && data.data.length > 0) {
          state.historyRecords = data.data;
          list.innerHTML = data.data.map(e => `
            <div class="history-item" data-id="${e.executionId || e.id}" onclick="selectHistoryItem('${e.executionId || e.id}')">
              <div class="history-item-title">${e.taskConfig?.name || e.task_name || '未命名任务'}</div>
              <div class="history-item-meta">
                <span class="history-item-status ${e.status}">${getStatusText(e.status)}</span>
                <span>${formatDate(e.startTime || e.created_at)}</span>
              </div>
            </div>
          `).join('');
        } else {
          list.innerHTML = '<div class="empty-state" style="padding:20px;"><div class="title">暂无执行记录</div></div>';
        }
      } catch (e) {
        list.innerHTML = '<div class="empty-state" style="padding:20px;"><div class="title">加载失败</div></div>';
      }
    }

    // 选择历史记录项
    async function selectHistoryItem(executionId) {
      document.querySelectorAll('.history-item').forEach(el => {
        el.classList.toggle('selected', el.dataset.id === executionId);
      });
      state.selectedHistoryId = executionId;

      const content = document.getElementById('historyDetailContent');
      content.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

      try {
        const [recordRes, execLogsRes, agentLogsRes, taskConfigRes, nodeStatusRes] = await Promise.all([
          fetch(`${API}/executions/records/${executionId}`),
          fetch(`${API}/executions/${executionId}/logs/execution`),
          fetch(`${API}/executions/${executionId}/logs/agent`),
          fetch(`${API}/executions/${executionId}/task-config`),
          fetch(`${API}/executions/${executionId}/node-statuses`)
        ]);

        const recordData = await recordRes.json();
        const execLogsData = await execLogsRes.json();
        const agentLogsData = await agentLogsRes.json();
        const taskConfigData = await taskConfigRes.json();
        const nodeStatusData = await nodeStatusRes.json();

        if (recordData.success) {
          const record = recordData.data;

          if (execLogsData.success) state.logs.execution = execLogsData.data;
          if (agentLogsData.success) state.logs.agent = agentLogsData.data;
          renderLogs();

          // 加载节点状态并渲染到画布
          if (nodeStatusData.success && nodeStatusData.data) {
            state.nodeStatus.clear();
            Object.entries(nodeStatusData.data).forEach(([nodeId, status]) => {
              state.nodeStatus.set(nodeId, status);
            });
            renderCanvas();
          }

          if (taskConfigData.success && taskConfigData.data) {
            taskConfig = {
              executionId: executionId,
              name: taskConfigData.data.name || '',
              description: taskConfigData.data.description || '',
              projectPath: taskConfigData.data.projectPath || '',
              contextFilePath: record.contextFilePath || ''
            };
          } else if (record.taskConfig) {
            taskConfig = {
              executionId: executionId,
              name: record.taskConfig.name || '',
              description: record.taskConfig.description || '',
              projectPath: record.taskConfig.projectPath || '',
              contextFilePath: record.contextFilePath || ''
            };
          } else {
            taskConfig = { executionId, name: '', description: '', projectPath: '', contextFilePath: record.contextFilePath || '' };
          }

          // 更新任务配置显示
          updateTaskConfigDisplay();

          // 检查是否有智能节点（优先使用执行记录的 workflowId 加载工作流）
          let smartDecomposeNodeId = null;
          const workflowId = record.workflowId;
          if (workflowId) {
            try {
              const wfRes = await fetch(`${API}/workflows/${workflowId}`);
              const wfData = await wfRes.json();
              if (wfData.success && wfData.data?.nodes) {
                const smartNode = wfData.data.nodes.find(n => n.type === 'smart_decompose');
                if (smartNode) {
                  smartDecomposeNodeId = smartNode.id;
                }
              }
            } catch (e) {
              console.warn('加载工作流失败:', e);
            }
          }

          let detailHtml = renderHistoryDetail(record, executionId, smartDecomposeNodeId);
          content.innerHTML = detailHtml;
        }
      } catch (e) {
        content.innerHTML = '<div class="empty-state" style="padding:20px;"><div class="title">加载详情失败</div></div>';
      }
    }

    // 打开任务树页面
    function openTaskTree(executionId, nodeId) {
      const url = `task-tree.html?executionId=${executionId}${nodeId ? `&nodeId=${nodeId}` : ''}`;
      window.open(url, '_blank');
    }

    // 渲染历史详情
    function renderHistoryDetail(record, executionId, smartDecomposeNodeId) {
      const status = record.status || '';
      const isRunning = status === 'running';
      const isPaused = status === 'paused';
      const isFinished = status === 'completed';

      // 根据状态显示不同操作按钮（纯文字，填充色）
      let actionButtons = '';
      // 智能节点按钮
      if (smartDecomposeNodeId) {
        actionButtons += `<button class="log-action-btn" onclick="openTaskTree('${executionId}', '${smartDecomposeNodeId}')">查看任务树</button>`;
      }
      if (isRunning) {
        actionButtons += `
          <button class="log-action-btn warning" onclick="pauseHistoryExecution('${executionId}', this)">暂停</button>`;
      } else if (isPaused) {
        actionButtons += `
          <button class="log-action-btn primary" onclick="resumeHistoryExecution('${executionId}', this)">继续</button>`;
      } else if (isFinished) {
        actionButtons += `
          <button class="log-action-btn primary" onclick="restartExecution('${executionId}')">重新执行</button>
          <button class="log-action-btn danger" onclick="deleteExecutionRecord('${executionId}')">删除</button>`;
      }

      let html = `
        <div class="detail-block">
          <div class="detail-block-title">
            <span>基本信息</span>
            ${actionButtons}
          </div>
          <div class="detail-block-content">
            <p><strong>执行ID:</strong> ${record.executionId || record.id}</p>
            <p><strong>状态:</strong> <span class="history-item-status ${record.status}">${getStatusText(record.status)}</span></p>
            <p><strong>任务名称:</strong> ${record.taskConfig?.name || '未命名'}</p>
            <p><strong>任务描述:</strong> ${record.taskConfig?.description || '无描述'}</p>
            <p><strong>项目路径:</strong> ${record.taskConfig?.projectPath || '未设置'}</p>
            <p><strong>开始时间:</strong> ${formatDate(record.startTime)}</p>
            ${record.endTime ? `<p><strong>结束时间:</strong> ${formatDate(record.endTime)}</p>` : ''}
          </div>
        </div>`;

      if (record.contextData?.agentPayloads?.length > 0) {
        html += `
          <div class="detail-block">
            <div class="detail-block-title">
              <span>Agent执行记录 (${record.contextData.agentPayloads.length}条)</span>
            </div>
            <div class="detail-block-content">
              ${record.contextData.agentPayloads.map(p => `
                <div class="payload-item">
                  <div class="payload-item-header">
                    <span><strong>Agent:</strong> ${p.agentId}</span>
                    <span><strong>节点:</strong> ${p.nodeName}</span>
                  </div>
                  <div class="payload-text">${escapeHtml(p.payloadText || '(无内容)').substring(0, 500)}${(p.payloadText || '').length > 500 ? '...' : ''}</div>
                </div>
              `).join('')}
            </div>
          </div>`;
      }
      return html;
    }

    async function showHistoryModal() {
      await openHistoryPanel();
    }

    // 删除执行记录
    async function deleteExecutionRecord(executionId) {
      if (!await confirmAsync('确定删除此执行记录吗？\n\n删除后无法恢复。')) return;

      try {
        const res = await fetch(`${API}/executions/${executionId}`, { method: 'DELETE' });
        const data = await res.json();
        if (data.success) {
          showToast('success', '记录已删除');
          // 如果删除的是当前执行的任务，重置工具栏
          if (state.execution?.executionId === executionId) {
            resetExecution();
          }
          // 刷新列表
          await refreshHistoryList();
          // 清空详情
          document.getElementById('historyDetailContent').innerHTML = '<div class="empty-state" style="padding:20px;"><div class="title">选择一条记录查看详情</div></div>';
          state.selectedHistoryId = null;
        } else {
          showToast('error', data.message || '删除失败');
        }
      } catch (e) {
        showToast('error', '删除失败: ' + e.message);
      }
    }

    // 从历史记录暂停执行
    async function pauseHistoryExecution(executionId, btnEl) {
      if (!await confirmAsync('确定暂停任务吗？\n\n暂停后可以继续执行。')) return;

      if (btnEl) {
        btnEl.classList.add('loading');
        btnEl.textContent = '处理中...';
      }

      try {
        const res = await fetch(`${API}/executions/${executionId}/pause`, { method: 'POST' });
        const data = await res.json();
        if (data.success) {
          showToast('success', '执行已暂停');
          // 同步工具栏状态
          syncToolbarState(executionId, 'paused');
          await selectHistoryItem(executionId);
        } else {
          showToast('error', data.message || '暂停失败');
          if (btnEl) {
            btnEl.classList.remove('loading');
            btnEl.textContent = '暂停';
          }
        }
      } catch (e) {
        showToast('error', '暂停失败');
        if (btnEl) {
          btnEl.classList.remove('loading');
          btnEl.textContent = '暂停';
        }
      }
    }

    // 从历史记录恢复执行
    async function resumeHistoryExecution(executionId, btnEl) {
      if (!await confirmAsync('确定继续执行任务吗？')) return;

      if (btnEl) {
        btnEl.classList.add('loading');
        btnEl.textContent = '处理中...';
      }

      try {
        const res = await fetch(`${API}/executions/${executionId}/resume`, { method: 'POST' });
        const data = await res.json();
        if (data.success) {
          showToast('success', '执行已恢复');
          // 同步工具栏状态
          syncToolbarState(executionId, 'running');
          await selectHistoryItem(executionId);
        } else {
          showToast('error', data.message || '恢复失败');
          if (btnEl) {
            btnEl.classList.remove('loading');
            btnEl.textContent = '继续';
          }
        }
      } catch (e) {
        showToast('error', '恢复失败');
        if (btnEl) {
          btnEl.classList.remove('loading');
          btnEl.textContent = '继续';
        }
      }
    }

    
    // 同步工具栏按钮状态
    function syncToolbarState(executionId, newStatus) {
      // 只有当前执行的任务才同步工具栏
      if (state.execution?.executionId !== executionId) return;

      const btnExecute = document.getElementById('btnExecute');
      const btnPause = document.getElementById('btnPause');
      const btnResume = document.getElementById('btnResume');

      if (newStatus === 'paused') {
        if (btnExecute) btnExecute.style.display = 'none';
        if (btnPause) btnPause.style.display = 'none';
        if (btnResume) btnResume.style.display = 'inline-flex';
        state.executionStatus = 'paused';
        updateStatus('idle');
      } else if (newStatus === 'running') {
        if (btnExecute) btnExecute.style.display = 'none';
        if (btnPause) btnPause.style.display = 'inline-flex';
        if (btnResume) btnResume.style.display = 'none';
        state.executionStatus = 'running';
        updateStatus('running');
      } else if (newStatus === 'completed') {
        if (btnExecute) btnExecute.style.display = 'inline-flex';
        if (btnPause) btnPause.style.display = 'none';
        if (btnResume) btnResume.style.display = 'none';
        state.execution = null;
        state.executionStatus = 'idle';
        updateStatus('idle');
      }
    }