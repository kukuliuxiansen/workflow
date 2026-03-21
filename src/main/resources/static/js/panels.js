
    function togglePanel(panelId) {
      const panel = document.getElementById(panelId);
      if (!panel) return;

      const isCollapsed = panel.classList.toggle('collapsed');

      // 保存偏好
      const prefs = getPrefs();
      if (!prefs.panels) prefs.panels = {};
      prefs.panels[panelId] = !isCollapsed;
      savePrefs(prefs);

      // 更新按钮位置和文本
      updateButtonPositions();

      // 重新渲染画布连线（确保连线位置正确）
      setTimeout(() => renderEdges(), 50);
    }

    function updateButtonPositions() {
      const sidebar = document.getElementById('sidebar');
      const rightPanel = document.getElementById('rightPanel');
      const logPanel = document.getElementById('logPanel');
      const sidebarBtn = document.getElementById('toggleSidebarBtn');
      const rightBtn = document.getElementById('toggleRightPanelBtn');
      const logBtn = document.getElementById('toggleLogPanelBtn');

      if (!sidebar || !rightPanel || !logPanel) return;

      // 更新log-panel位置
      const sidebarCollapsed = sidebar.classList.contains('collapsed');
      const rightPanelCollapsed = rightPanel.classList.contains('collapsed');
      logPanel.classList.toggle('sidebar-collapsed', sidebarCollapsed);
      logPanel.classList.toggle('right-panel-collapsed', rightPanelCollapsed);
      logPanel.style.left = sidebarCollapsed ? '0' : sidebar.offsetWidth + 'px';
      logPanel.style.right = rightPanelCollapsed ? '0' : rightPanel.offsetWidth + 'px';

      // 更新侧边栏按钮位置
      if (sidebarBtn) {
        if (sidebarCollapsed) {
          sidebarBtn.style.left = '10px';
          sidebarBtn.textContent = '▶';
        } else {
          sidebarBtn.style.left = sidebar.offsetWidth + 'px';
          sidebarBtn.textContent = '◀';
        }
      }

      // 更新右侧面板按钮位置
      if (rightBtn) {
        if (rightPanelCollapsed) {
          rightBtn.style.right = '10px';
          rightBtn.textContent = '◀';
        } else {
          rightBtn.style.right = rightPanel.offsetWidth + 'px';
          rightBtn.textContent = '▶';
        }
      }

      // 更新日志面板按钮位置
      if (logBtn) {
        if (logPanel.classList.contains('collapsed')) {
          logBtn.style.bottom = '10px';
          logBtn.textContent = '▲';
        } else {
          logBtn.style.bottom = logPanel.offsetHeight + 'px';
          logBtn.textContent = '▼';
        }
      }
    }

    function loadPanelStates() {
      const prefs = getPrefs();
      const panels = prefs.panels || {};
      Object.entries(panels).forEach(([panelId, isOpen]) => {
        const panel = document.getElementById(panelId);
        if (panel && !isOpen) {
          panel.classList.add('collapsed');
        }
      });

      // 恢复面板大小
      const sidebar = document.getElementById('sidebar');
      const rightPanel = document.getElementById('rightPanel');
      const logPanel = document.getElementById('logPanel');

      if (prefs.sidebarWidth && sidebar) {
        sidebar.style.width = prefs.sidebarWidth + 'px';
      }
      if (prefs.rightPanelWidth && rightPanel) {
        rightPanel.style.width = prefs.rightPanelWidth + 'px';
      }
      if (prefs.logPanelHeight && logPanel) {
        logPanel.style.height = prefs.logPanelHeight + 'px';
      }

      // 初始化按钮位置
      updateButtonPositions();

      // 初始化画布缩放
      initCanvasZoom();
    }

    // 画布缩放功能
