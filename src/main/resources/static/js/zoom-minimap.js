
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
