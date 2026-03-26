    function switchRightPanelTab(tabName) {
      // 更新标签页状态
      document.querySelectorAll('.panel-tab').forEach(tab => tab.classList.remove('active'));
      document.getElementById(tabName + 'Tab').classList.add('active');

      // 更新面板内容显示
      document.getElementById('propertyPanel').classList.remove('active');
      document.getElementById('executionHistoryPanel').classList.remove('active');
      document.getElementById('taskConfigPanel').classList.remove('active');
      document.getElementById('promptsPanel').classList.remove('active');

      const panelMap = {
        'property': 'propertyPanel',
        'history': 'executionHistoryPanel',
        'taskconfig': 'taskConfigPanel',
        'prompts': 'promptsPanel'
      };
      document.getElementById(panelMap[tabName]).classList.add('active');

      // 切换到提示词模板时加载数据
      if (tabName === 'prompts') {
        loadPromptTemplates();
      }
    }

    // 刷新执行历史列表（右侧面板版本）
    async function refreshExecutionHistory() {
      const list = document.getElementById('executionHistoryList');
      const workflowId = state.currentWorkflow?.id;

      if (!workflowId) {
        list.innerHTML = '<div class="empty-state-mini">请先选择工作流</div>';
        return;
      }

      list.innerHTML = '<div class="empty-state-mini">加载中...</div>';

      try {
        const res = await fetch(`${API}/executions/records?workflowId=${workflowId}&limit=10`);
        const data = await res.json();

        if (data.success && data.data && data.data.length > 0) {
          state.historyRecords = data.data;
          list.innerHTML = data.data.map(e => {
            const execId = e.executionId || e.id;
            return `
              <div class="history-item-mini ${state.selectedHistoryId === execId ? 'selected' : ''}"
                   onclick="selectHistoryItemMini('${execId}')">
                <span>${e.taskConfig?.name || '未命名任务'}</span>
                <span class="status ${e.status}">${getStatusText(e.status)}</span>
              </div>
            `;
          }).join('');
        } else {
          list.innerHTML = '<div class="empty-state-mini">暂无执行记录</div>';
        }
      } catch (e) {
        list.innerHTML = '<div class="empty-state-mini">加载失败</div>';
      }
    }

    // 选择历史记录项（右侧面板版本）
    async function selectHistoryItemMini(executionId) {
      // 更新选中状态
      document.querySelectorAll('.history-item-mini').forEach(el => {
        el.classList.toggle('selected', el.onclick.toString().includes(executionId));
      });
      state.selectedHistoryId = executionId;

      try {
        // 并行加载数据
        const [recordRes, taskConfigRes] = await Promise.all([
          fetch(`${API}/executions/records/${executionId}`),
          fetch(`${API}/executions/${executionId}/task-config`)
        ]);

        const recordData = await recordRes.json();
        const taskConfigData = await taskConfigRes.json();

        if (recordData.success && recordData.data) {
          const record = recordData.data;

          // 显示详情
          const detailSection = document.getElementById('executionDetailSection');
          const detailContent = document.getElementById('executionDetailContent');
          if (detailSection) detailSection.style.display = 'block';
          if (detailContent) {
            detailContent.innerHTML = `
              <p><strong>执行ID:</strong> ${record.executionId || record.id}</p>
              <p><strong>状态:</strong> ${getStatusText(record.status)}</p>
              <p><strong>开始时间:</strong> ${formatDate(record.startTime)}</p>
              ${record.endTime ? `<p><strong>结束时间:</strong> ${formatDate(record.endTime)}</p>` : ''}
            `;
          }

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
          }

          // 更新任务配置显示
          updateTaskConfigDisplay();
        }
      } catch (e) {
        console.error('加载历史详情失败:', e);
      }
    }

    // 更新任务配置显示
