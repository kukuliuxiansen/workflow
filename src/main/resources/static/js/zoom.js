function initCanvasZoom() {
  const canvas = document.getElementById('canvas');
  const content = document.getElementById('canvasContent');

  // 鼠标滚轮缩放
  canvas.addEventListener('wheel', (e) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -0.1 : 0.1;
    const newZoom = Math.max(0.1, Math.min(4, state.zoom + delta));

    // 以鼠标位置为中心缩放
    const rect = canvas.getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const mouseY = e.clientY - rect.top;

    const zoomRatio = newZoom / state.zoom;
    state.panX = mouseX - (mouseX - state.panX) * zoomRatio;
    state.panY = mouseY - (mouseY - state.panY) * zoomRatio;

    state.zoom = newZoom;
    updateCanvasTransform();
    updateMinimap();
    updateZoomLevel();
  });

  // 画布拖拽和框选
  let isPanning = false;
  let isBoxSelecting = false;
  let boxSelectStartX = 0;
  let boxSelectStartY = 0;
  let lastPanX = 0;
  let lastPanY = 0;

  canvas.addEventListener('mousedown', (e) => {
    // 中键或Ctrl+左键: 平移
    if (e.button === 1 || (e.button === 0 && e.ctrlKey)) {
      isPanning = true;
      lastPanX = e.clientX;
      lastPanY = e.clientY;
      canvas.style.cursor = 'grabbing';
      e.preventDefault();
      return;
    }

    // 左键在空白处: 框选
    if (e.button === 0 && !e.target.closest('.node') && !e.target.closest('.edge')) {
      isBoxSelecting = true;
      const rect = canvas.getBoundingClientRect();
      boxSelectStartX = e.clientX - rect.left;
      boxSelectStartY = e.clientY - rect.top;

      // 如果没有按Shift，清除之前的选择
      if (!e.shiftKey) {
        clearNodeSelection();
      }

      const selectionRect = document.getElementById('selectionRect');
      selectionRect.style.left = boxSelectStartX + 'px';
      selectionRect.style.top = boxSelectStartY + 'px';
      selectionRect.style.width = '0';
      selectionRect.style.height = '0';
      selectionRect.classList.add('active');
      e.preventDefault();
    }
  });

  document.addEventListener('mousemove', (e) => {
    // 平移
    if (isPanning) {
      state.panX += e.clientX - lastPanX;
      state.panY += e.clientY - lastPanY;
      lastPanX = e.clientX;
      lastPanY = e.clientY;
      updateCanvasTransform();
      updateMinimap();
    }

    // 框选
    if (isBoxSelecting) {
      const rect = canvas.getBoundingClientRect();
      const currentX = e.clientX - rect.left;
      const currentY = e.clientY - rect.top;

      const selectionRect = document.getElementById('selectionRect');
      const left = Math.min(boxSelectStartX, currentX);
      const top = Math.min(boxSelectStartY, currentY);
      const width = Math.abs(currentX - boxSelectStartX);
      const height = Math.abs(currentY - boxSelectStartY);

      selectionRect.style.left = left + 'px';
      selectionRect.style.top = top + 'px';
      selectionRect.style.width = width + 'px';
      selectionRect.style.height = height + 'px';
    }
  });

  document.addEventListener('mouseup', (e) => {
    if (isPanning) {
      isPanning = false;
      canvas.style.cursor = '';
    }

    if (isBoxSelecting) {
      isBoxSelecting = false;
      const selectionRect = document.getElementById('selectionRect');
      selectionRect.classList.remove('active');

      // 计算框选区域内的节点
      const rect = canvas.getBoundingClientRect();
      const endX = e.clientX - rect.left;
      const endY = e.clientY - rect.top;

      const boxLeft = Math.min(boxSelectStartX, endX);
      const boxTop = Math.min(boxSelectStartY, endY);
      const boxRight = Math.max(boxSelectStartX, endX);
      const boxBottom = Math.max(boxSelectStartY, endY);

      // 转换为画布坐标
      const canvasBoxLeft = (boxLeft - state.panX) / state.zoom;
      const canvasBoxTop = (boxTop - state.panY) / state.zoom;
