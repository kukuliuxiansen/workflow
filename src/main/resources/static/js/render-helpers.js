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
