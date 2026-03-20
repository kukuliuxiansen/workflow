async function saveWorkflow(silent = false) {
  if (!state.currentWorkflow) {
    if (!silent) showToast('warn', '请先选择工作流');
    return;
  }
  try {
    // 准备保存数据，使用 snake_case 格式匹配后端 Jackson 配置
    const workflowData = {
      name: state.currentWorkflow.name,
      description: state.currentWorkflow.description,
      global_config: state.currentWorkflow.globalConfig || state.currentWorkflow.global_config,
      task_config: state.currentWorkflow.taskConfig
        ? JSON.stringify(state.currentWorkflow.taskConfig)
        : null,
      nodes: state.currentWorkflow.nodes,
      edges: state.currentWorkflow.edges
    };

    await fetch(`${API}/workflows/${state.currentWorkflow.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(workflowData)
    });
    state.isDirty = false;
    state.lastSaveTime = Date.now();
    updateSaveIndicator();
    if (!silent) {
      showToast('success', '保存成功');
      addLog('success', '工作流已保存');
    }
  } catch (e) {
    if (!silent) showToast('error', '保存失败');
  }
}

// 执行工作流
async function executeWorkflow() {
  if (!state.currentWorkflow) {
    showToast('warn', '请先选择工作流');
    return;
  }

  // 检查任务配置
  if (!taskConfig.projectPath || !taskConfig.name) {
    showToast('warn', '请先配置任务（点击"任务配置"按钮）');
    showTaskConfigModal();
    return;
  }

  // 确认执行
  if (!await confirmAsync(`确认执行任务？\n\n任务名称: ${taskConfig.name}\n项目路径: ${taskConfig.projectPath}`)) return;

  try {
    updateStatus('running');
    const res = await fetch(`${API}/workflows/${state.currentWorkflow.id}/executions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        input: {
          task_description: taskConfig.description,
          task_name: taskConfig.name
        },
        taskConfig: {
          name: taskConfig.name,
          description: taskConfig.description,
          projectPath: taskConfig.projectPath,
          workflowPath: taskConfig.workflowPath,
          globalPrompt: globalConfig.globalPromptContent || ''
        }
      })
    });
    const data = await res.json();
    if (data.success) {
      state.execution = data.data;
      // 保存上下文文件路径
      taskConfig.contextFilePath = data.data.contextFilePath || '';
      connectWS(data.data.executionId);
      document.getElementById('btnExecute').style.display = 'none';
      document.getElementById('btnStop').style.display = 'inline-flex';
      addLog('info', '开始执行: ' + data.data.executionId);
      addLog('info', '上下文文件: ' + (data.data.contextFilePath || '未创建'), 'execution');
    }
  } catch (e) {
    updateStatus('error');
    showToast('error', '启动失败');
  }
}

// 停止执行
async function stopExecution() {
  if (!state.execution) return;
  try {
    await fetch(`${API}/executions/${state.execution.executionId}/stop`, { method: 'POST' });
    addLog('warn', '执行已停止');
    resetExecution();
  } catch (e) {
    showToast('error', '停止失败');
  }
}

