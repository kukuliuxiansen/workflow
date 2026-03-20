    function resetExecution() {
      document.getElementById('btnExecute').style.display = 'inline-flex';
      document.getElementById('btnStop').style.display = 'none';
      updateStatus('idle');
      state.execution = null;
      state.nodeStatus.clear();
    }

    // WebSocket
    function connectWS(executionId) {
      const ws = new WebSocket(`ws://${location.host}/ws/executions?executionId=${executionId}`);
      ws.onmessage = (e) => {
        const msg = JSON.parse(e.data);
        if (msg.type === 'log') {
          // 根据logType区分日志类型，默认为execution
          const logType = msg.data.logType || 'execution';
          addLog(msg.data.level, msg.data.message, logType);
        }
        else if (msg.type === 'agentMessage') {
          // Agent交互消息
          addAgentMessage(msg.data.agentId, msg.data.message, msg.data.messageType);
        }
        else if (msg.type === 'agentInteraction') {
          // Agent间交互
          addAgentLog(msg.data.fromAgent, msg.data.toAgent, msg.data.action, msg.data.detail);
        }
        else if (msg.type === 'status') {
          if (msg.data.status === 'completed') {
            updateStatus('success');
            addLog('success', '执行完成');
            resetExecution();
          } else if (msg.data.status === 'failed') {
            updateStatus('error');
            addLog('error', '执行失败: ' + (msg.data.error || ''));
            resetExecution();
          }
        } else if (msg.type === 'nodeStatus') {
          state.nodeStatus.set(msg.data.nodeId, msg.data.status);
          renderCanvas();
        }
      };
      ws.onclose = () => addLog('info', '连接关闭');
      state.ws = ws;
    }

    // 验证工作流
    async function validateWorkflow() {
      if (!state.currentWorkflow) {
        showToast('warn', '请先选择工作流');
        return;
      }
      try {
        const res = await fetch(`${API}/workflows/${state.currentWorkflow.id}/validate`, { method: 'POST' });
        const data = await res.json();
        if (data.success) {
          const r = data.data;
          if (r.valid) {
            showToast('success', '验证通过');
            addLog('success', `验证通过 (${r.nodeCount}节点, ${r.edgeCount}连线)`);
          } else {
            showToast('error', `验证失败: ${r.errors.length}个错误`);
            r.errors.forEach(e => addLog('error', e.message));
          }
          r.warnings.forEach(w => addLog('warn', w.message));
        }
      } catch (e) {
        showToast('error', '验证失败');
      }
    }

    // 导出YAML
    async function exportYAML() {
      if (!state.currentWorkflow) {
        showToast('warn', '请先选择工作流');
        return;
      }
      const res = await fetch(`${API}/workflows/${state.currentWorkflow.id}/export`);
      const yaml = await res.text();
      const blob = new Blob([yaml], { type: 'text/yaml' });
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = `${state.currentWorkflow.id}.yaml`;
      a.click();
      showToast('success', '导出成功');
    }

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
        // 根据当前工作流过滤执行历史
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

    // 当前选中的历史记录
    state.historyRecords = [];
    state.selectedHistoryId = null;

    // 选择历史记录项
    async function selectHistoryItem(executionId) {
      // 更新选中状态
      document.querySelectorAll('.history-item').forEach(el => {
        el.classList.toggle('selected', el.dataset.id === executionId);
      });
      state.selectedHistoryId = executionId;

      // 加载详情
      const content = document.getElementById('historyDetailContent');
      content.innerHTML = '<div class="loading"><div class="spinner"></div></div>';

      try {
        // 并行加载执行记录、日志和任务配置
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

          // 更新日志状态
          if (execLogsData.success) {
            state.logs.execution = execLogsData.data;
          }
          if (agentLogsData.success) {
            state.logs.agent = agentLogsData.data;
          }
          renderLogs();

          // 更新任务配置
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
            taskConfig = {
              executionId: executionId,
              name: '',
              description: '',
              projectPath: '',
              contextFilePath: record.contextFilePath || ''
            };
          }

          let detailHtml = `
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
            </div>
          `;

          // Agent执行记录
          if (record.contextData?.agentPayloads?.length > 0) {
            detailHtml += `
              <div class="detail-block">
                <div class="detail-block-title">
                  <span>Agent执行记录 (${record.contextData.agentPayloads.length}条)</span>
                </div>
                <div class="detail-block-content">
                  ${record.contextData.agentPayloads.map((p, i) => `
                    <div class="payload-item">
                      <div class="payload-item-header">
                        <span><strong>Agent:</strong> ${p.agentId}</span>
                        <span><strong>节点:</strong> ${p.nodeName}</span>
                      </div>
                      <div class="payload-text">${escapeHtml(p.payloadText || '(无内容)').substring(0, 500)}${(p.payloadText || '').length > 500 ? '...' : ''}</div>
                    </div>
                  `).join('')}
                </div>
              </div>
            `;
          }

          content.innerHTML = detailHtml;
        }
      } catch (e) {
        content.innerHTML = '<div class="empty-state" style="padding:20px;"><div class="title">加载详情失败</div></div>';
      }
    }

    // HTML转义
    function escapeHtml(text) {
      if (!text) return '';
      return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    // 显示历史(兼容旧代码)
    async function showHistoryModal() {
      await openHistoryPanel();
    }

