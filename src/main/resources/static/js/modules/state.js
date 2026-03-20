// 全局错误捕获
window.onerror = function(msg, url, lineNo, columnNo, error) {
  console.error('全局错误:', msg, 'at line', lineNo);
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

// API 基础路径
const API = '/api';

// 全局状态
const state = {
  workflows: [],
  folders: [],
  expandedFolders: new Set(),
  draggedWorkflowId: null,
  currentWorkflow: null,
  selectedNode: null,
  selectedNodeType: 'agent_execution',
  execution: null,
  logs: {
    execution: [],
    agent: [],
    api: []
  },
  operationLogs: [],
  currentLogTab: 'execution',
  ws: null,
  nodeStatus: new Map(),
  zoom: 1,
  panX: 0,
  panY: 0,
  pathSelect: {
    mode: 'file',
    targetId: '',
    callback: null,
    currentPath: '/',
    filters: []
  },
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
  isBoxSelecting: false,
  boxSelectStart: { x: 0, y: 0 },
  boxSelectEnd: { x: 0, y: 0 },
  selectedNodes: new Set(),
  showAlignGuides: true,
  alignGuides: [],
  isCanvasLocked: false,
  touchStartDistance: 0,
  touchStartZoom: 1
};

// 任务配置
let taskConfig = {
  executionId: null,
  name: '',
  description: '',
  projectPath: '',
  globalPrompt: ''
};

// 偏好设置键名
const PREF_KEY = 'openclaw_workflow_prefs';