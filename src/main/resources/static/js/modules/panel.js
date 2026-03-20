// ========== 面板切换功能 ==========

function togglePanel(panelId) {
  const panel = document.getElementById(panelId);
  const isCollapsed = panel.classList.toggle('collapsed');
  const prefs = getPrefs();
  prefs.panels[panelId] = !isCollapsed;
  savePrefs(prefs);
  updateButtonPositions();
  setTimeout(() => renderEdges(), 50);
}

function updateButtonPositions() {
  const sidebar = document.getElementById('sidebar');
  const rightPanel = document.getElementById('rightPanel');
  const logPanel = document.getElementById('logPanel');
  const sidebarBtn = document.getElementById('toggleSidebarBtn');
  const rightBtn = document.getElementById('toggleRightPanelBtn');
  const logBtn = document.getElementById('toggleLogPanelBtn');

  if (sidebar.classList.contains('collapsed')) {
    sidebarBtn.style.left = '10px';
    sidebarBtn.textContent = '▶';
  } else {
    sidebarBtn.style.left = sidebar.offsetWidth + 'px';
    sidebarBtn.textContent = '◀';
  }

  if (rightPanel.classList.contains('collapsed')) {
    rightBtn.style.right = '10px';
    rightBtn.textContent = '◀';
  } else {
    rightBtn.style.right = rightPanel.offsetWidth + 'px';
    rightBtn.textContent = '▶';
  }

  if (logPanel.classList.contains('collapsed')) {
    logBtn.style.bottom = '10px';
    logBtn.textContent = '▲';
  } else {
    logBtn.style.bottom = logPanel.offsetHeight + 'px';
    logBtn.textContent = '▼';
  }
}

function loadPanelStates() {
  const prefs = getPrefs();
  Object.entries(prefs.panels).forEach(([panelId, isOpen]) => {
    const panel = document.getElementById(panelId);
    if (!isOpen) {
      panel.classList.add('collapsed');
    }
  });

  if (prefs.sidebarWidth) {
    document.getElementById('sidebar').style.width = prefs.sidebarWidth + 'px';
  }
  if (prefs.rightPanelWidth) {
    document.getElementById('rightPanel').style.width = prefs.rightPanelWidth + 'px';
  }
  if (prefs.logPanelHeight) {
    document.getElementById('logPanel').style.height = prefs.logPanelHeight + 'px';
  }

  updateButtonPositions();
  initCanvasZoom();
}