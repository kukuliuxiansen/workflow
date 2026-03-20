    // 全局错误捕获
    window.onerror = function(msg, url, lineNo, columnNo, error) {
      console.error('全局错误:', msg, 'at line', lineNo);
      // showToast会在后面定义，函数声明会被提升
      if (typeof showToast === 'function') {
        showToast('error', '错误: ' + msg + ' (行号: ' + lineNo + ')');
      }
      return false;
    };
    window.addEventListener('unhandledrejection', function(event) {
      console.error('Promise错误:', event.reason);
      if (typeof showToast === 'function') {
        showToast('error', 'Promise错误: ' + event.reason);
      }
    });

    // 面板拖拽调整大小
    function startResize(e, panelId) {
      e.preventDefault();
      const panel = document.getElementById(panelId);
      const startX = e.clientX;
      const startY = e.clientY;
      const startWidth = panel.offsetWidth;
      const startHeight = panel.offsetHeight;

      function onMouseMove(e) {
        if (panelId === 'sidebar') {
          panel.style.width = Math.max(150, startWidth + e.clientX - startX) + 'px';
        } else if (panelId === 'rightPanel') {
          panel.style.width = Math.max(200, startWidth - e.clientX + startX) + 'px';
        } else if (panelId === 'logPanel') {
          panel.style.height = Math.max(100, startHeight - e.clientY + startY) + 'px';
        }
      }

      function onMouseUp() {
        document.removeEventListener('mousemove', onMouseMove);
        document.removeEventListener('mouseup', onMouseUp);
        // 保存面板大小
        const prefs = getPrefs();
        if (panelId === 'sidebar') prefs.sidebarWidth = panel.offsetWidth;
        if (panelId === 'rightPanel') prefs.rightPanelWidth = panel.offsetWidth;
        if (panelId === 'logPanel') prefs.logPanelHeight = panel.offsetHeight;
        savePrefs(prefs);
      }

      document.addEventListener('mousemove', onMouseMove);
      document.addEventListener('mouseup', onMouseUp);
    }

    const API = '/api';
    const state = {
      workflows: [],
      folders: [],           // 文件夹列表
      expandedFolders: new Set(), // 展开的文件夹ID
      draggedWorkflowId: null,    // 正在拖拽的工作流ID
      currentWorkflow: null,
      selectedNode: null,
      selectedNodeType: 'agent_execution',
      execution: null,
      logs: {
        execution: [],  // 执行日志
        agent: [],       // Agent交互日志
        api: []          // API日志
      },
      operationLogs: [],      // 操作日志
      currentLogTab: 'execution',  // 当前显示的日志tab
      ws: null,
      nodeStatus: new Map(),
      zoom: 1,
      panX: 0,
      panY: 0,
      // 路径选择状态
      pathSelect: {
        mode: 'file',        // 'file' 或 'directory'
        targetId: '',        // 目标输入框ID
        callback: null,      // 选择后的回调
        currentPath: '/',    // 当前浏览路径
        filters: []          // 文件过滤器
      },
      // UX改进状态
      autoSaveTimer: null,
      lastSaveTime: null,
      isDirty: false,
      undoStack: [],
      redoStack: [],
      maxUndoSize: 50,
      searchQuery: '',
      gridSnap: true,
      gridSize: 20,
      showMinimap: true,
      showGrid: true,
      clipboard: null,
      contextMenuNode: null,
      // 框选状态
      isBoxSelecting: false,
      boxSelectStart: { x: 0, y: 0 },
      boxSelectEnd: { x: 0, y: 0 },
      selectedNodes: new Set(),  // 多选节点集合
      // 对齐辅助线
      showAlignGuides: true,
      alignGuides: [],
      // 画布锁定
      isCanvasLocked: false,
      // 触摸板支持
      touchStartDistance: 0,
      touchStartZoom: 1
    };

    // 用户偏好设置 (localStorage缓存)
    const PREF_KEY = 'openclaw_workflow_prefs';

