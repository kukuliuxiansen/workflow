// ========== 偏好设置 ==========

function getPrefs() {
  try {
    const saved = localStorage.getItem(PREF_KEY);
    return saved ? JSON.parse(saved) : getDefaultPrefs();
  } catch (e) {
    return getDefaultPrefs();
  }
}

function getDefaultPrefs() {
  return {
    panels: { sidebar: true, rightPanel: true, logPanel: true },
    lastWorkflowId: null,
    logPanelHeight: 200,
    sidebarWidth: 280,
    rightPanelWidth: 320
  };
}

function savePrefs(prefs) {
  try {
    localStorage.setItem(PREF_KEY, JSON.stringify(prefs));
  } catch (e) {}
}