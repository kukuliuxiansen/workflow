
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

