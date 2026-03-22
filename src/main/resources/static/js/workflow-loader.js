// 工作流加载和选择

    function filterWorkflows() {
      renderWorkflowList();
    }

    // 选择工作流
    async function selectWorkflow(id) {
      console.log('选择工作流:', id);
      try {
        const res = await fetch(`${API}/workflows/${id}`);
        const data = await res.json();
        console.log('工作流数据:', data);
        if (data.success) {
          state.currentWorkflow = data.data;
          state.selectedNode = null;

          // 清空当前日志和节点状态
          state.logs = { execution: [], agent: [] };
          state.selectedHistoryId = null;
          state.nodeStatus.clear();
          state.execution = null;

          // 重置执行按钮状态
          resetExecution();

          // 从工作流数据中加载任务配置
          const taskConfigData = state.currentWorkflow.taskConfig || state.currentWorkflow.task_config;
          if (taskConfigData) {
            let config = taskConfigData;
            if (typeof config === 'string') {
              try {
                config = JSON.parse(config);
              } catch (e) {
                config = {};
              }
            }
            taskConfig = {
              executionId: null,
              ...config
            };
          } else {
            taskConfig = {
              executionId: null,
              name: '',
              description: '',
              projectPath: '',
              contextFilePath: ''
            };
          }
          updateTaskConfigDisplay();

          renderWorkflowList();
          renderCanvas();
          renderPropertyPanel();
          addLog('info', `已加载: ${data.data.name}`);

          // 保存最后选择的工作流
          const prefs = getPrefs();
          prefs.lastWorkflowId = id;
          savePrefs(prefs);

          // 自动加载该工作流的最新执行历史
          await loadLatestExecutionHistory(id);
        } else {
          console.error('加载工作流失败:', data);
          alert('加载工作流失败: ' + (data.message || '未知错误'));
        }
      } catch (e) {
        console.error('selectWorkflow error:', e);
        alert('加载失败: ' + (e.message || e));
      }
    }

    // 加载工作流的最新执行历史
    async function loadLatestExecutionHistory(workflowId) {
      try {
        await refreshExecutionHistory();

        const res = await fetch(`${API}/executions/records?workflowId=${workflowId}&limit=1`);
        const data = await res.json();
        if (data.success && data.data && data.data.length > 0) {
          const latest = data.data[0];
          await selectHistoryItemMini(latest.executionId || latest.id);
          switchRightPanelTab('history');
        }
      } catch (e) {
        console.error('加载执行历史失败:', e);
      }
    }