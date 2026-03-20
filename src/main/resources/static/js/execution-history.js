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
        const [recordRes, execLogsRes, agentLogsRes, taskConfigRes] = await Promise.all([
          fetch(`${API}/executions/records/${executionId}`),
          fetch(`${API}/executions/${executionId}/logs/execution`),
          fetch(`${API}/executions/${executionId}/logs/agent`),
          fetch(`${API}/executions/${executionId}/task-config`)
        ]);

        const recordData = await recordRes.json();
        const execLogsData = await execLogsRes.json();
        const agentLogsData = await agentLogsRes.json();
        const taskConfigData = await taskConfigRes.json();

        if (recordData.success) {
          const record = recordData.data;

          if (execLogsData.success) state.logs.execution = execLogsData.data;
          if (agentLogsData.success) state.logs.agent = agentLogsData.data;
          renderLogs();

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

          let detailHtml = renderHistoryDetail(record, executionId);
          content.innerHTML = detailHtml;
        }
      } catch (e) {
        content.innerHTML = '<div class="empty-state" style="padding:20px;"><div class="title">加载详情失败</div></div>';
      }
    }

    // 渲染历史详情
    function renderHistoryDetail(record, executionId) {
      let html = `
        <div class="detail-block">
          <div class="detail-block-title">
            <span>基本信息</span>
            <button class="log-action-btn" onclick="restartExecution('${executionId}')">🔄 重新执行</button>
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