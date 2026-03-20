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
