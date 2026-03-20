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