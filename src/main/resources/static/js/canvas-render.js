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
        const toolbarHtml = (isStart || isFinish) ? '' : `
          <div class="node-toolbar">
            <button class="node-toolbar-btn" onclick="event.stopPropagation();selectNode('${node.id}')" title="编辑">✏️</button>
            <button class="node-toolbar-btn" onclick="event.stopPropagation();duplicateNodeById('${node.id}')" title="复制">📋</button>
            <button class="node-toolbar-btn" onclick="event.stopPropagation();deleteNodeById('${node.id}')" title="删除">🗑️</button>
          </div>
        `;

        html += `
          <div class="node ${selectionClass} ${status}"
               id="node-${node.id}"
               style="left:${x}px;top:${y}px;"
               data-color="${nodeColor}"
               onclick="selectNode('${node.id}')"
               onmousedown="startDrag(event,'${node.id}')"
               oncontextmenu="${isStart || isFinish ? '' : `showNodeContextMenu(event,'${node.id}')`}">
            ${toolbarHtml}
            <div class="node-header">
              <div class="node-icon ${type.split('_')[0]}">${getIcon(type)}</div>
              <div class="node-info">
                <div class="node-name">${node.name || node.id}</div>
                <div class="node-type">${getTypeName(type)}</div>
              </div>
              ${status ? `<div class="node-status-badge ${status}">${getStatusText(status)}</div>` : ''}
            </div>
            <div class="node-body">${getNodeDesc(node)}</div>
            <div class="node-ports">
              ${inputPortHtml}
              ${outputPortsHtml}
            </div>
          </div>
        `;
      });

      content.innerHTML = html;
      renderEdges();
      // 确保画布变换正确应用，节点能正确显示
      setTimeout(() => updateCanvasTransform(), 0);
    }

    // 渲染连线
    function renderEdges() {
      const svg = document.getElementById('edgesSvg');
      const workflow = state.currentWorkflow;

      if (!svg) return;
      if (!workflow || !workflow.nodes) { svg.innerHTML = ''; return; }

      // 构建节点位置和尺寸映射
      const nodeMap = {};
      workflow.nodes.forEach((node, i) => {
        // 兼容 snake_case 和 camelCase
        const x = node.position_x || node.positionX || 50 + (i % 3) * 280;
        const y = node.position_y || node.positionY || 50 + Math.floor(i / 3) * 180;
        nodeMap[node.id] = { x, y, width: 200, height: 100 };
      });

      let paths = '';

      // 使用 edges 数组渲染连线
      if (workflow.edges && Array.isArray(workflow.edges)) {
        workflow.edges.forEach((edge) => {
          // 支持两种命名方式：camelCase（后端返回）和 snake_case（兼容旧数据）
          const sourceNodeId = edge.sourceNodeId || edge.source_node_id;
          const targetNodeId = edge.targetNodeId || edge.target_node_id;
          const srcNode = nodeMap[sourceNodeId];
          const tgtNode = nodeMap[targetNodeId];

          if (!srcNode || !tgtNode) return;

          // 获取实际节点元素
          const srcEl = document.getElementById(`node-${sourceNodeId}`);
          const tgtEl = document.getElementById(`node-${targetNodeId}`);

          if (srcEl) {
            srcNode.width = srcEl.offsetWidth;
            srcNode.height = srcEl.offsetHeight;
          }
          if (tgtEl) {
            tgtNode.width = tgtEl.offsetWidth;
            tgtNode.height = tgtEl.offsetHeight;
          }

          const edgeType = edge.edgeType || edge.edge_type || 'success';

          // 端口大小为10px，在节点边缘外
          const portRadius = 5;
          const portGap = 2; // 端口间距

          // 源节点右侧端口的Y位置（在node-ports区域内）
          // node-ports padding: 6px 12px，端口在右下角
          const srcPortsCenterY = srcNode.y + srcNode.height - 11; // 端口中心Y

          // 源端口位置：从节点右边沿开始，端口中心
          const x1 = srcNode.x + srcNode.width; // 节点右边沿
          const y1 = srcPortsCenterY + (edgeType === 'success' ? -(portRadius + portGap) : (portRadius + portGap));

          // 目标节点左侧输入端口
          const x2 = tgtNode.x; // 节点左边沿
          const y2 = tgtNode.y + tgtNode.height - 11; // 输入端口中心Y

          // 绘制平滑的贝塞尔曲线
          const controlOffset = Math.min(100, Math.abs(x2 - x1) / 2);

          // 获取连线类型（success/failure/condition/default）
          const pathType = edge.type || edgeType;
          const typeClass = pathType === 'failure' ? 'failure' :
                           pathType === 'condition' ? 'condition' :
                           pathType === 'default' ? 'default' : 'success';

          // 是否显示动画（执行中时）
          const animateClass = state.nodeStatus.get(sourceNodeId) === 'running' ? 'animated' : '';

          // 生成唯一edgeId
          const edgeIdStr = edge.id || `${sourceNodeId}-${targetNodeId}`;

          paths += `<path class="edge-path ${typeClass} ${animateClass}"
                         d="M${x1},${y1} C${x1 + controlOffset},${y1} ${x2 - controlOffset},${y2} ${x2},${y2}"
                         id="edge-${edgeIdStr}"
                         oncontextmenu="event.preventDefault();showEdgeContextMenu(event,'${edgeIdStr}')"/>`;

          // 如果有标签，显示标签
          if (edge.label) {
            const midX = (x1 + x2) / 2;
            const midY = (y1 + y2) / 2;
            paths += `<text x="${midX}" y="${midY}" class="edge-label" text-anchor="middle">${edge.label}</text>`;
          }
        });
      }

      svg.innerHTML = paths;
    }

    // 通过edge id删除连线
    async function deleteEdgeById(edgeId) {
      if (!await confirmAsync('删除此连线？')) return;
      try {
        await fetch(`${API}/workflows/${state.currentWorkflow.id}/edges/${edgeId}`, {
          method: 'DELETE'
        });
        await selectWorkflow(state.currentWorkflow.id);
        showToast('success', '连线已删除');
      } catch (e) {
        showToast('error', '删除失败');
      }
    }

    // 切换右侧面板标签页
