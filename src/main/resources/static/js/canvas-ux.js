
    // 节点选择管理
    function clearNodeSelection() {
      state.selectedNode = null;
      state.selectedNodes.clear();
      updateSelectedCount();
    }

    function selectAllNodes() {
      if (state.currentWorkflow && state.currentWorkflow.nodes) {
        state.selectedNodes.clear();
        state.currentWorkflow.nodes.forEach(node => state.selectedNodes.add(node.id));
        if (state.selectedNodes.size === 1) {
          state.selectedNode = state.currentWorkflow.nodes[0];
        } else {
          state.selectedNode = null;
        }
        renderCanvas();
        updateSelectedCount();
        showToast('info', `已选中 ${state.selectedNodes.size} 个节点`);
      }
    }

    function invertSelection() {
      if (state.currentWorkflow && state.currentWorkflow.nodes) {
        const allNodeIds = new Set(state.currentWorkflow.nodes.map(n => n.id));
        const newSelection = new Set();
        allNodeIds.forEach(id => {
          if (!state.selectedNodes.has(id)) {
            newSelection.add(id);
          }
        });
        state.selectedNodes = newSelection;
        state.selectedNode = null;
        renderCanvas();
        updateSelectedCount();
      }
    }

    // 复制/粘贴/剪切功能
    function copyNodes() {
      const nodesToCopy = [];
      if (state.selectedNode) {
        nodesToCopy.push(state.selectedNode);
      } else if (state.selectedNodes.size > 0) {
        state.currentWorkflow.nodes.forEach(node => {
          if (state.selectedNodes.has(node.id)) {
            nodesToCopy.push(node);
          }
        });
      }

      // 过滤掉开始和结束节点（不能复制）
      const filteredNodes = nodesToCopy.filter(node => {
        if (node.type === 'start' || node.type === 'finish') {
          showToast('warn', `开始和结束节点不能复制: ${node.name}`);
          return false;
        }
        return true;
      });

      if (filteredNodes.length > 0) {
        state.clipboard = {
          nodes: JSON.parse(JSON.stringify(filteredNodes)),
          edges: [] // TODO: 也要复制连线
        };
        showToast('success', `已复制 ${filteredNodes.length} 个节点`);
      } else if (nodesToCopy.length > 0) {
        showToast('warn', '选中的节点无法复制');
      }
    }

    async function pasteNodes() {
      if (!state.clipboard || !state.clipboard.nodes || state.clipboard.nodes.length === 0) {
        showToast('warn', '剪贴板为空');
        return;
      }
      if (!state.currentWorkflow) {
        showToast('warn', '请先选择工作流');
        return;
      }

      clearNodeSelection();

      const offset = 30;
      let successCount = 0;

      for (let i = 0; i < state.clipboard.nodes.length; i++) {
        const node = state.clipboard.nodes[i];
        try {
          const res = await fetch(`${API}/workflows/${state.currentWorkflow.id}/nodes`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              type: node.type,
              name: node.name + ' (副本)',
              position_x: node.position_x + offset,
              position_y: node.position_y + offset,
              config: node.config
            })
          });
          const data = await res.json();
          if (data.success) {
            successCount++;
            state.selectedNodes.add(data.data.id);
          }
        } catch (e) {
          console.error('粘贴节点失败:', e);
        }
      }

      await selectWorkflow(state.currentWorkflow.id);
      updateSelectedCount();
      if (successCount > 0) {
        showToast('success', `已粘贴 ${successCount} 个节点`);
      } else {
        showToast('error', '粘贴失败');
      }
    }

    function cutNodes() {
      copyNodes();
      deleteSelectedNodes();
    }

    function duplicateNodes() {
      copyNodes();
      pasteNodes();
    }

    // 删除选中节点
    async function deleteSelectedNodes() {
      if (!state.currentWorkflow) return;

      const nodeIdsToDelete = [];
      if (state.selectedNode) {
        nodeIdsToDelete.push(state.selectedNode.id);
      } else if (state.selectedNodes.size > 0) {
        nodeIdsToDelete.push(...state.selectedNodes);
      }

      if (nodeIdsToDelete.length === 0) return;

      // 过滤掉开始和结束节点（不能删除）
      const filteredNodeIds = nodeIdsToDelete.filter(nodeId => {
        const node = state.currentWorkflow.nodes.find(n => n.id === nodeId);
        if (node && (node.type === 'start' || node.type === 'finish')) {
          showToast('warn', `开始和结束节点不能删除: ${node.name}`);
          return false;
        }
        return true;
      });

      if (filteredNodeIds.length === 0) {
        showToast('warn', '选中的节点无法删除');
        return;
      }

      let successCount = 0;
      for (const nodeId of filteredNodeIds) {
        try {
          await fetch(`${API}/workflows/${state.currentWorkflow.id}/nodes/${nodeId}`, {
            method: 'DELETE'
          });
          successCount++;
        } catch (e) {
          console.error('删除节点失败:', nodeId, e);
        }
      }

      clearNodeSelection();
      await selectWorkflow(state.currentWorkflow.id);
      renderPropertyPanel();

      if (successCount > 0) {
        showToast('success', `已删除 ${successCount} 个节点`);
      } else {
        showToast('error', '删除失败');
      }
    }

    // 缩放控制
    function zoomIn() {
      state.zoom = Math.min(4, state.zoom + 0.1);
      updateCanvasTransform();
      updateMinimap();
      updateZoomLevel();
    }

    function zoomOut() {
      state.zoom = Math.max(0.1, state.zoom - 0.1);
      updateCanvasTransform();
      updateMinimap();
      updateZoomLevel();
    }

    function zoomReset() {
      state.zoom = 1;
      state.panX = 0;
      state.panY = 0;
      updateCanvasTransform();
      updateMinimap();
      updateZoomLevel();
    }

    function updateZoomLevel() {
      const el = document.getElementById('zoomLevel');
      if (el) {
        el.textContent = Math.round(state.zoom * 100) + '%';
      }
    }

    function fitCanvas() {
      if (!state.currentWorkflow || !state.currentWorkflow.nodes || state.currentWorkflow.nodes.length === 0) {
        zoomReset();
        return;
      }

      // 计算所有节点的边界
      let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
      state.currentWorkflow.nodes.forEach(node => {
        minX = Math.min(minX, node.position_x);
        minY = Math.min(minY, node.position_y);
        maxX = Math.max(maxX, node.position_x + 180);
        maxY = Math.max(maxY, node.position_y + 80);
      });

      const padding = 50;
      const canvasEl = document.getElementById('canvas');
      const canvasWidth = canvasEl.clientWidth - padding * 2;
      const canvasHeight = canvasEl.clientHeight - padding * 2;

      const nodesWidth = maxX - minX;
      const nodesHeight = maxY - minY;

      const scaleX = canvasWidth / nodesWidth;
      const scaleY = canvasHeight / nodesHeight;
      state.zoom = Math.min(1, Math.min(scaleX, scaleY));

      // 居中
      const centerX = (minX + maxX) / 2;
      const centerY = (minY + maxY) / 2;
      state.panX = canvasEl.clientWidth / 2 - centerX * state.zoom;
      state.panY = canvasEl.clientHeight / 2 - centerY * state.zoom;

      updateCanvasTransform();
      updateMinimap();
      updateZoomLevel();
    }

    // 小地图
    function updateMinimap() {
      if (!state.showMinimap) return;

      const canvas = document.getElementById('minimapCanvas');
      if (!canvas) return;

      const ctx = canvas.getContext('2d');
      const width = canvas.width;
      const height = canvas.height;

      ctx.clearRect(0, 0, width, height);
      ctx.fillStyle = '#1a1a1a';
      ctx.fillRect(0, 0, width, height);

      if (!state.currentWorkflow || !state.currentWorkflow.nodes) return;

      // 计算边界
      let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
      state.currentWorkflow.nodes.forEach(node => {
        minX = Math.min(minX, node.position_x);
        minY = Math.min(minY, node.position_y);
        maxX = Math.max(maxX, node.position_x + 180);
        maxY = Math.max(maxY, node.position_y + 80);
      });

      if (minX === Infinity) return;

      const padding = 20;
      const scaleX = (width - padding * 2) / (maxX - minX || 1);
      const scaleY = (height - padding * 2) / (maxY - minY || 1);
      const scale = Math.min(scaleX, scaleY);

      // 绘制节点
      ctx.fillStyle = '#4a4a6a';
      state.currentWorkflow.nodes.forEach(node => {
        const x = padding + (node.position_x - minX) * scale;
        const y = padding + (node.position_y - minY) * scale;
        const w = 180 * scale;
        const h = 80 * scale;
        ctx.fillRect(x, y, Math.max(2, w), Math.max(2, h));
      });

      // 更新视口框
      const viewportEl = document.getElementById('minimapViewport');
      if (viewportEl) {
        const canvasEl = document.getElementById('canvas');
        const viewX = padding + (-state.panX / state.zoom - minX) * scale;
        const viewY = padding + (-state.panY / state.zoom - minY) * scale;
        const viewW = (canvasEl.clientWidth / state.zoom) * scale;
        const viewH = (canvasEl.clientHeight / state.zoom) * scale;

        viewportEl.style.left = Math.max(0, viewX) + 'px';
        viewportEl.style.top = Math.max(0, viewY) + 'px';
        viewportEl.style.width = Math.min(width, viewW) + 'px';
        viewportEl.style.height = Math.min(height, viewH) + 'px';
      }
    }

    // 统计更新
    function updateSelectedCount() {
      const el = document.getElementById('selectedCount');
      if (el) {
        const count = state.selectedNode ? 1 : state.selectedNodes.size;
        el.textContent = count;
      }
    }

    function updateCanvasInfo() {
      const nodeCountEl = document.getElementById('nodeCount');
      const edgeCountEl = document.getElementById('edgeCount');

      if (nodeCountEl) {
        nodeCountEl.textContent = state.currentWorkflow?.nodes?.length || 0;
      }
      if (edgeCountEl) {
        edgeCountEl.textContent = state.currentWorkflow?.edges?.length || 0;
      }
    }

    // 搜索功能
    function toggleSearchPanel() {
      const panel = document.getElementById('searchPanel');
      if (panel.classList.contains('show')) {
        closeSearchPanel();
      } else {
        panel.classList.add('show');
        document.getElementById('nodeSearchInput').focus();
      }
    }

    function closeSearchPanel() {
      const panel = document.getElementById('searchPanel');
      if (panel) panel.classList.remove('show');
    }

    function handleNodeSearch(query) {
      const resultsEl = document.getElementById('searchResults');
      if (!query.trim()) {
        resultsEl.innerHTML = '';
        return;
      }

      const lowerQuery = query.toLowerCase();
      const results = [];

      if (state.currentWorkflow && state.currentWorkflow.nodes) {
        state.currentWorkflow.nodes.forEach(node => {
          if (node.name.toLowerCase().includes(lowerQuery) ||
              (node.description && node.description.toLowerCase().includes(lowerQuery))) {
            results.push(node);
          }
        });
      }

      if (results.length === 0) {
        resultsEl.innerHTML = '<div style="padding:10px;text-align:center;color:#888;">无匹配结果</div>';
        return;
      }

      resultsEl.innerHTML = results.map(node => {
        const icon = getNodeIcon(node.type);
        return `<div class="search-result-item" onclick="focusNode('${node.id}')">
          <span>${icon}</span>
          <span>${node.name}</span>
          <span style="font-size:11px;color:#888;margin-left:auto;">${node.type}</span>
        </div>`;
      }).join('');
    }

    function focusNode(nodeId) {
      const node = state.currentWorkflow?.nodes?.find(n => n.id === nodeId);
      if (!node) return;

      // 平移画布使节点居中
      const canvasEl = document.getElementById('canvas');
      state.panX = canvasEl.clientWidth / 2 - node.position_x * state.zoom - 90 * state.zoom;
      state.panY = canvasEl.clientHeight / 2 - node.position_y * state.zoom - 40 * state.zoom;

      // 选中节点
      clearNodeSelection();
      state.selectedNode = node;
      state.selectedNodes.add(nodeId);

      updateCanvasTransform();
      renderCanvas();
      renderPropertyPanel();
      closeSearchPanel();
      updateSelectedCount();
    }

    function getNodeIcon(type) {
      const icons = {
        'start': '▶️',
        'finish': '⏹️',
        'agent_execution': '🤖',
        'api_call': '🌐',
        'condition': '❓',
        'parallel': '⚡',
        'loop': '🔄',
        'wait': '⏸️',
        'human_review': '👤',
        'subworkflow': '📦'
      };
      return icons[type] || '📄';
    }

    // 快捷键帮助
    function toggleShortcutHelp() {
      const panel = document.getElementById('shortcutHelpPanel');
      if (panel.classList.contains('show')) {
        closeShortcutHelp();
      } else {
        panel.classList.add('show');
      }
    }

    function closeShortcutHelp() {
      const panel = document.getElementById('shortcutHelpPanel');
      if (panel) panel.classList.remove('show');
    }

    // 节点对齐功能
    function alignNodes(direction) {
      if (state.selectedNodes.size < 2) {
        showToast('warn', '请至少选中2个节点');
        return;
      }

      const nodes = state.currentWorkflow.nodes.filter(n => state.selectedNodes.has(n.id));
      if (nodes.length < 2) return;

      pushUndo();

      switch(direction) {
        case 'left':
          const minLeft = Math.min(...nodes.map(n => n.position_x));
          nodes.forEach(n => n.position_x = minLeft);
          break;
        case 'right':
          const maxRight = Math.max(...nodes.map(n => n.position_x + 180));
          nodes.forEach(n => n.position_x = maxRight - 180);
          break;
        case 'top':
          const minTop = Math.min(...nodes.map(n => n.position_y));
          nodes.forEach(n => n.position_y = minTop);
          break;
        case 'bottom':
          const maxBottom = Math.max(...nodes.map(n => n.position_y + 80));
          nodes.forEach(n => n.position_y = maxBottom - 80);
          break;
        case 'center-h':
          const avgX = nodes.reduce((sum, n) => sum + n.position_x + 90, 0) / nodes.length;
          nodes.forEach(n => n.position_x = avgX - 90);
          break;
        case 'center-v':
          const avgY = nodes.reduce((sum, n) => sum + n.position_y + 40, 0) / nodes.length;
          nodes.forEach(n => n.position_y = avgY - 40);
          break;
      }

      renderCanvas();
      markDirty();
      showToast('success', '节点已对齐');
    }

    // 节点分布
    function distributeNodes(direction) {
      if (state.selectedNodes.size < 3) {
        showToast('warn', '请至少选中3个节点');
        return;
      }

      const nodes = state.currentWorkflow.nodes.filter(n => state.selectedNodes.has(n.id));
      if (nodes.length < 3) return;

      pushUndo();

      if (direction === 'horizontal') {
        nodes.sort((a, b) => a.position_x - b.position_x);
        const minX = nodes[0].position_x;
        const maxX = nodes[nodes.length - 1].position_x + 180;
        const spacing = (maxX - minX - 180 * nodes.length) / (nodes.length - 1);

        nodes.forEach((node, index) => {
          node.position_x = minX + index * (180 + spacing);
        });
      } else {
        nodes.sort((a, b) => a.position_y - b.position_y);
        const minY = nodes[0].position_y;
        const maxY = nodes[nodes.length - 1].position_y + 80;
        const spacing = (maxY - minY - 80 * nodes.length) / (nodes.length - 1);

        nodes.forEach((node, index) => {
          node.position_y = minY + index * (80 + spacing);
        });
      }

      renderCanvas();
      markDirty();
      showToast('success', '节点已均匀分布');
    }

    // 节点颜色设置
    function setNodeColor(color) {
      const nodeIds = [];
      if (state.selectedNode) {
        nodeIds.push(state.selectedNode.id);
      } else if (state.selectedNodes.size > 0) {
        nodeIds.push(...state.selectedNodes);
      }

      if (nodeIds.length === 0) return;

      pushUndo();

      nodeIds.forEach(nodeId => {
        const node = state.currentWorkflow.nodes.find(n => n.id === nodeId);
        if (node) {
          if (!node.config) node.config = {};
          try {
            const config = typeof node.config === 'string' ? JSON.parse(node.config) : node.config;
            config.color = color === 'none' ? undefined : color;
            node.config = JSON.stringify(config);
          } catch (e) {
            node.config = JSON.stringify({ color: color === 'none' ? undefined : color });
          }
        }
      });

      renderCanvas();
      markDirty();
      showToast('success', '节点颜色已更新');
    }

    // 增强的右键菜单动作
    function contextMenuAction(action) {
      const nodeId = state.contextMenuNode;
      closeContextMenu();

      if (!nodeId && !['select-all', 'paste'].includes(action)) return;

      switch(action) {
        case 'edit':
          selectNode(nodeId);
          break;
        case 'duplicate':
          selectNode(nodeId);
          duplicateNodes();
          break;
        case 'copy':
          selectNode(nodeId);
          copyNodes();
          break;
        case 'delete':
          selectNode(nodeId);
          deleteSelectedNodes();
          break;
        case 'run-from':
          if (state.currentWorkflow) {
            confirmAsync('是否从此节点开始执行？').then(confirmed => {
              if (confirmed) executeWorkflow(nodeId);
            });
          }
          break;
        case 'select-all':
          selectAllNodes();
          break;
        case 'invert-selection':
          invertSelection();
          break;
        // 对齐
        case 'align-left':
          alignNodes('left');
          break;
        case 'align-center-h':
          alignNodes('center-h');
          break;
        case 'align-right':
          alignNodes('right');
          break;
        case 'align-top':
          alignNodes('top');
          break;
        case 'align-center-v':
          alignNodes('center-v');
          break;
        case 'align-bottom':
          alignNodes('bottom');
          break;
        // 分布
        case 'distribute-h':
          distributeNodes('horizontal');
          break;
        case 'distribute-v':
          distributeNodes('vertical');
          break;
        // 颜色
        case 'color-red':
        case 'color-orange':
        case 'color-yellow':
        case 'color-green':
        case 'color-cyan':
        case 'color-blue':
        case 'color-purple':
        case 'color-pink':
        case 'color-gray':
        case 'color-none':
          setNodeColor(action.replace('color-', ''));
          break;
      }
    }

    // 画布右键菜单动作
    function canvasContextMenuAction(action) {
      closeContextMenu();

      switch(action) {
        case 'paste':
          pasteNodes();
          break;
        case 'select-all':
          selectAllNodes();
          break;
        case 'toggle-grid':
          state.showGrid = !state.showGrid;
          document.querySelector('.canvas-grid').style.display = state.showGrid ? 'block' : 'none';
          showToast('info', state.showGrid ? '网格已显示' : '网格已隐藏');
          break;
        case 'toggle-snap':
          state.gridSnap = !state.gridSnap;
          showToast('info', state.gridSnap ? '网格吸附已开启' : '网格吸附已关闭');
          break;
        case 'fit-canvas':
          fitCanvas();
          break;
        case 'reset-view':
          zoomReset();
          break;
        case 'export-png':
          exportCanvasAsImage();
          break;
        case 'export-json':
          exportWorkflowAsJson();
          break;
      }
    }

    // 连线右键菜单动作
    let currentEdgeId = null;

    function showEdgeContextMenu(event, edgeId) {
      event.preventDefault();
      currentEdgeId = edgeId;
      const menu = document.getElementById('edgeContextMenu');
      menu.style.left = event.clientX + 'px';
      menu.style.top = event.clientY + 'px';
      menu.classList.add('show');
    }

    function edgeContextMenuAction(action) {
      closeContextMenu();

      if (!currentEdgeId) return;

      switch(action) {
        case 'edit-label':
          const label = prompt('输入连线标签:');
          if (label !== null) {
            updateEdgeLabel(currentEdgeId, label);
          }
          break;
        case 'toggle-type-success':
          updateEdgeType(currentEdgeId, 'success');
          break;
        case 'toggle-type-failure':
          updateEdgeType(currentEdgeId, 'failure');
          break;
        case 'toggle-type-default':
          updateEdgeType(currentEdgeId, 'default');
          break;
        case 'delete':
          deleteEdgeById(currentEdgeId);
          break;
      }

      currentEdgeId = null;
    }

    async function updateEdgeLabel(edgeId, label) {
      if (!state.currentWorkflow || !state.currentWorkflow.edges) return;

      const edge = state.currentWorkflow.edges.find(e =>
        `${(e.sourceNodeId || e.source_node_id)}-${(e.targetNodeId || e.target_node_id)}` === edgeId || e.id === edgeId
      );
      if (edge) {
        // TODO: 调用后端API更新连线标签
        edge.label = label;
        await selectWorkflow(state.currentWorkflow.id);
      }
    }

    function updateEdgeType(edgeId, type) {
      if (!state.currentWorkflow || !state.currentWorkflow.edges) return;

      const edge = state.currentWorkflow.edges.find(e =>
        `${(e.sourceNodeId || e.source_node_id)}-${(e.targetNodeId || e.target_node_id)}` === edgeId || e.id === edgeId
      );
      if (edge) {
        // 更新需要调用后端API
        // TODO: 实现更新边的API
        edge.edge_type = type;
        renderCanvas();
      }
    }

    // 导出功能
    function exportCanvasAsImage() {
      showToast('info', '正在导出图片...');

      // 使用html2canvas或svg转图片
      const svg = document.getElementById('edgesSvg');
      const content = document.getElementById('canvasContent');

      // 简单实现：创建截图
      const canvas = document.createElement('canvas');
      const rect = content.getBoundingClientRect();
      canvas.width = rect.width;
      canvas.height = rect.height;

      // 这里需要更复杂的实现来捕获SVG和HTML节点
      // 暂时提示用户
      showToast('warn', '图片导出功能开发中，请使用浏览器截图');
    }

    function exportWorkflowAsJson() {
      if (!state.currentWorkflow) return;

      const dataStr = JSON.stringify(state.currentWorkflow, null, 2);
      const blob = new Blob([dataStr], { type: 'application/json' });
      const url = URL.createObjectURL(blob);

      const a = document.createElement('a');
      a.href = url;
      a.download = `${state.currentWorkflow.name || 'workflow'}.json`;
      a.click();

      URL.revokeObjectURL(url);
      showToast('success', '工作流已导出');
    }

    // 关闭所有右键菜单
    function closeContextMenu() {
      document.querySelectorAll('.context-menu').forEach(menu => {
        menu.classList.remove('show');
      });
      state.contextMenuNode = null;
    }

    // 点击其他地方关闭菜单
    document.addEventListener('click', (e) => {
      if (!e.target.closest('.context-menu')) {
        closeContextMenu();
      }
    });

    function showToast(type, msg) {
      const container = document.getElementById('toastContainer');
      const toast = document.createElement('div');
      toast.className = `toast ${type}`;
      toast.innerHTML = `<span style="margin-right:8px;">${type === 'success' ? '✓' : type === 'error' ? '✕' : type === 'warn' ? '⚠' : 'ℹ'}</span>${msg}`;
      container.appendChild(toast);
      setTimeout(() => toast.remove(), 3000);
    }

    // 确认对话框（替代confirm）
    function showConfirm(message, onConfirm, onCancel) {
      const modalHtml = `
        <div class="modal-overlay show" id="confirmModal" onclick="if(event.target===this){this.remove();if(onCancel)onCancel();}">
          <div class="modal" style="background:linear-gradient(145deg, #2d2d2d, #252525);border:1px solid #444;border-radius:16px;box-shadow:0 20px 60px rgba(0,0,0,0.5),0 0 0 1px rgba(255,255,255,0.05);min-width:360px;overflow:hidden;" onclick="event.stopPropagation()">
            <div style="padding:32px;text-align:center;">
              <div style="width:64px;height:64px;margin:0 auto 20px;background:linear-gradient(135deg, #fbbf24, #f59e0b);border-radius:50%;display:flex;align-items:center;justify-content:center;box-shadow:0 4px 20px rgba(251,191,36,0.3);">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 9v4M12 17h.01"/>
                  <circle cx="12" cy="12" r="10"/>
                </svg>
              </div>
              <div style="font-size:15px;color:#e0e0e0;white-space:pre-wrap;line-height:1.6;">${message}</div>
            </div>
            <div style="padding:0 24px 24px;display:flex;gap:12px;justify-content:center;">
              <button onclick="this.closest('.modal-overlay').remove();if(onCancel)onCancel();" style="flex:1;padding:12px 24px;background:#333;border:1px solid #444;border-radius:10px;color:#aaa;font-size:14px;cursor:pointer;transition:all 0.2s;" onmouseover="this.style.background='#3a3a3a';this.style.color='#fff';" onmouseout="this.style.background='#333';this.style.color='#aaa';">取消</button>
              <button id="confirmBtn" style="flex:1;padding:12px 24px;background:linear-gradient(135deg, #6366f1, #4f46e5);border:none;border-radius:10px;color:#fff;font-size:14px;font-weight:500;cursor:pointer;transition:all 0.2s;box-shadow:0 4px 15px rgba(99,102,241,0.3);" onmouseover="this.style.transform='translateY(-1px)';this.style.boxShadow='0 6px 20px rgba(99,102,241,0.4)';" onmouseout="this.style.transform='translateY(0)';this.style.boxShadow='0 4px 15px rgba(99,102,241,0.3)';">确定</button>
            </div>
          </div>
        </div>
      `;
      document.body.insertAdjacentHTML('beforeend', modalHtml);
      document.getElementById('confirmBtn').onclick = function() {
        document.getElementById('confirmModal').remove();
        if (onConfirm) onConfirm();
      };
    }

    // 异步确认（返回Promise）
