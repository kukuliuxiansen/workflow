// 画布渲染

    // 渲染画布
    function renderCanvas() {
      const content = document.getElementById('canvasContent');
      const workflow = state.currentWorkflow;

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
        const x = node.position_x || node.positionX || 50 + (i % 3) * 280;
        const y = node.position_y || node.positionY || 50 + Math.floor(i / 3) * 180;
        const type = node.type || 'agent_execution';
        const status = state.nodeStatus.get(node.id) || '';

        const isSelected = state.selectedNode?.id === node.id || state.selectedNodes.has(node.id);
        const selectionClass = isSelected ? (state.selectedNodes.size > 1 ? 'selected-multi' : 'selected') : '';

        // 检查节点配置是否完整，添加警告类
        let warningClass = '';
        if (type === 'agent_execution' && !node.agent_id) {
          warningClass = 'warning';
        } else if (type === 'api_call' && !node.url) {
          warningClass = 'warning';
        }

        let nodeColor = '';
        try {
          const config = typeof node.config === 'string' ? JSON.parse(node.config || '{}') : (node.config || {});
          if (config.color) nodeColor = config.color;
        } catch (e) {}

        const isStart = type === 'start';
        const isFinish = type === 'finish';
        const headerClass = type === 'smart_decompose' ? 'smart' : type.split('_')[0];

        const inputPortHtml = isStart ? '' : '<div class="port input" title="输入"></div>';
        let outputPortsHtml = '';
        if (isStart) {
          outputPortsHtml = `<div></div><div class="port success" title="成功→拖拽连线" onmousedown="startDragConnect(event,'${node.id}','success')"></div>`;
        } else if (!isFinish) {
          outputPortsHtml = `
          <div style="display:flex;gap:4px;">
            <div class="port fail" title="失败→拖拽连线" onmousedown="startDragConnect(event,'${node.id}','fail')"></div>
            <div class="port success" title="成功→拖拽连线" onmousedown="startDragConnect(event,'${node.id}','success')"></div>
          </div>`;
        }

        html += `
          <div class="node ${selectionClass} ${status} ${warningClass}"
               id="node-${node.id}"
               style="left:${x}px;top:${y}px;"
               data-color="${nodeColor}"
               onclick="selectNode('${node.id}')"
               onmousedown="startDrag(event,'${node.id}')"
               oncontextmenu="${isStart || isFinish ? '' : `showNodeContextMenu(event,'${node.id}')`}">
            <div class="node-header ${headerClass}">
              <div class="node-icon ${headerClass}">${getIcon(type)}</div>
              <div class="node-info">
                <div class="node-name">${node.name || node.id}</div>
                <div class="node-type">${getTypeName(type)}</div>
              </div>
              ${warningClass ? '<div class="node-warning-badge" title="节点未完整配置">⚠️</div>' : ''}
              ${status ? `<div class="node-status-badge ${status}">${getStatusText(status)}</div>` : ''}
            </div>
            <div class="node-body">${getNodeDesc(node)}</div>
            <div class="node-ports">
              ${inputPortHtml}
              ${outputPortsHtml}
            </div>
          </div>`;
      });

      content.innerHTML = html;
      renderEdges();
      setTimeout(() => updateCanvasTransform(), 0);
    }

    // 渲染连线
    function renderEdges() {
      const svg = document.getElementById('edgesSvg');
      const workflow = state.currentWorkflow;

      if (!svg) return;
      if (!workflow || !workflow.nodes) { svg.innerHTML = ''; return; }

      const nodeMap = {};
      workflow.nodes.forEach((node, i) => {
        const x = node.position_x || node.positionX || 50 + (i % 3) * 280;
        const y = node.position_y || node.positionY || 50 + Math.floor(i / 3) * 180;
        nodeMap[node.id] = { x, y, width: 200, height: 100 };
      });

      let paths = '';

      if (workflow.edges && Array.isArray(workflow.edges)) {
        workflow.edges.forEach((edge) => {
          const sourceNodeId = edge.sourceNodeId || edge.source_node_id;
          const targetNodeId = edge.targetNodeId || edge.target_node_id;
          const srcNode = nodeMap[sourceNodeId];
          const tgtNode = nodeMap[targetNodeId];

          if (!srcNode || !tgtNode) return;

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
          const portRadius = 5;
          const portGap = 2;

          const srcPortsCenterY = srcNode.y + srcNode.height - 11;
          const x1 = srcNode.x + srcNode.width;
          const y1 = srcPortsCenterY + (edgeType === 'success' ? -(portRadius + portGap) : (portRadius + portGap));
          const x2 = tgtNode.x;
          const y2 = tgtNode.y + tgtNode.height - 11;

          const controlOffset = Math.min(100, Math.abs(x2 - x1) / 2);

          const pathType = edge.type || edgeType;
          const typeClass = pathType === 'failure' ? 'failure' :
                           pathType === 'condition' ? 'condition' :
                           pathType === 'default' ? 'default' : 'success';

          const animateClass = state.nodeStatus.get(sourceNodeId) === 'running' ? 'animated' : '';
          const edgeIdStr = edge.id || `${sourceNodeId}-${targetNodeId}`;

          paths += `<path class="edge-path ${typeClass} ${animateClass}"
                         d="M${x1},${y1} C${x1 + controlOffset},${y1} ${x2 - controlOffset},${y2} ${x2},${y2}"
                         id="edge-${edgeIdStr}"
                         data-edge-id="${edgeIdStr}"/>`;

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

      // 保存撤销点
      pushUndo();

      try {
        await fetch(`${API}/workflows/${state.currentWorkflow.id}/edges/${edgeId}`, {
          method: 'DELETE'
        });
        await selectWorkflow(state.currentWorkflow.id);
        showToast('success', '连线已删除');
        markDirty();
        updateUndoRedoButtons();
      } catch (e) {
        showToast('error', '删除失败');
      }
    }