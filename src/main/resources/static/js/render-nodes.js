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

      // 清空当前日志
      state.logs = { execution: [], agent: [] };
      state.selectedHistoryId = null;

      // 从工作流数据中加载任务配置
      // 后端返回的是 snake_case 格式 (task_config)
      const taskConfigData = state.currentWorkflow.taskConfig || state.currentWorkflow.task_config;
      if (taskConfigData) {
        // 如果是字符串，先解析
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
    // 刷新执行历史列表
    await refreshExecutionHistory();

    const res = await fetch(`${API}/executions/records?workflowId=${workflowId}&limit=1`);
    const data = await res.json();
    if (data.success && data.data && data.data.length > 0) {
      const latest = data.data[0];
      // 自动选择最新的执行历史
      await selectHistoryItemMini(latest.executionId || latest.id);
      // 切换到历史标签页
      switchRightPanelTab('history');
    }
    // 不再清空任务配置，保持从工作流加载的配置
  } catch (e) {
    console.error('加载执行历史失败:', e);
  }
}

// 渲染画布
function renderCanvas() {
  const content = document.getElementById('canvasContent');
  const workflow = state.currentWorkflow;

  // 更新统计信息
  updateCanvasInfo();

  if (!workflow || !workflow.nodes || workflow.nodes.length === 0) {
    content.innerHTML = `
      <svg class="edges-svg" id="edgesSvg"></svg>
      <div class="empty-state">
        <div class="icon">📝</div>
        <div class="title">暂无节点</div>
        <div class="desc">双击画布添加节点 · 或点击工具栏"添加节点"</div>
      </div>
    `;
    updateCanvasTransform();
    return;
  }

  let html = '<svg class="edges-svg" id="edgesSvg"></svg>';
  html += '<svg class="edges-svg" id="dragLineSvg" style="z-index:10;"><path id="dragLine" class="drag-line" d="" style="display:none;"/></svg>';

  workflow.nodes.forEach((node, i) => {
    // 兼容 snake_case 和 camelCase
    const x = node.position_x || node.positionX || 50 + (i % 3) * 280;
    const y = node.position_y || node.positionY || 50 + Math.floor(i / 3) * 180;
    const type = node.type || 'agent_execution';
    const status = state.nodeStatus.get(node.id) || '';

    // 多选和高亮
    const isSelected = state.selectedNode?.id === node.id || state.selectedNodes.has(node.id);
    const selectionClass = isSelected ? (state.selectedNodes.size > 1 ? 'selected-multi' : 'selected') : '';

    // 节点颜色
    let nodeColor = '';
    try {
      const config = typeof node.config === 'string' ? JSON.parse(node.config || '{}') : (node.config || {});
      if (config.color) {
        nodeColor = config.color;
      }
    } catch (e) {}

    // 根据节点类型决定端口显示
    const isStart = type === 'start';
    const isFinish = type === 'finish';

    // 输入端口：开始节点不显示
    const inputPortHtml = isStart ? '' : '<div class="port input" title="输入"></div>';

    // 输出端口：结束节点不显示
    const outputPortsHtml = isFinish ? '' : `
      <div style="display:flex;gap:4px;">
        <div class="port fail" title="失败→拖拽连线" onmousedown="startDragConnect(event,'${node.id}','fail')"></div>
        <div class="port success" title="成功→拖拽连线" onmousedown="startDragConnect(event,'${node.id}','success')"></div>
      </div>
    `;

    // 开始和结束节点不显示工具栏（不能复制删除）
