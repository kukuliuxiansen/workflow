// 画布缩放

    function initCanvasZoom() {
      const canvas = document.getElementById('canvas');

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

      // 初始化画布交互
      initCanvasInteract();
    }

    function updateCanvasTransform() {
      const content = document.getElementById('canvasContent');
      if (content) {
        content.style.transform = `translate(${state.panX}px, ${state.panY}px) scale(${state.zoom})`;
      }
      const zoomLevelEl = document.getElementById('zoomLevel');
      if (zoomLevelEl) {
        zoomLevelEl.textContent = Math.round(state.zoom * 100) + '%';
      }
    }

    function zoomIn() {
      state.zoom = Math.min(2, state.zoom + 0.1);
      updateCanvasTransform();
    }

    function zoomOut() {
      state.zoom = Math.max(0.25, state.zoom - 0.1);
      updateCanvasTransform();
    }

    function zoomReset() {
      state.zoom = 1;
      state.panX = 0;
      state.panY = 0;
      updateCanvasTransform();
    }