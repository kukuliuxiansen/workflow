import { defineStore } from 'pinia'
import { ref } from 'vue'

export type EditorMode = 'select' | 'add-node' | 'connect' | 'pan'

export const useEditorStore = defineStore('editor', () => {
  // 编辑器模式
  const mode = ref<EditorMode>('select')

  // 当前要添加的节点类型
  const addingNodeType = ref<string | null>(null)

  // 是否处于平移模式 (Ctrl+拖动)
  const isPanning = ref<boolean>(false)

  // 画布缩放
  const zoom = ref<number>(1)

  // 画布偏移
  const pan = ref<{ x: number; y: number }>({ x: 0, y: 0 })

  // 左侧面板展开状态
  const leftPanelExpanded = ref<boolean>(true)

  // 右侧面板展开状态
  const rightPanelExpanded = ref<boolean>(true)

  // 底部日志面板展开状态
  const logPanelExpanded = ref<boolean>(false)

  // 日志面板高度
  const logPanelHeight = ref<number>(200)

  // 是否显示全局配置对话框
  const showGlobalConfigDialog = ref<boolean>(false)

  // 是否正在保存
  const saving = ref<boolean>(false)

  // 是否有未保存的更改
  const hasUnsavedChanges = ref<boolean>(false)

  // 网格设置
  const gridSize = ref<number>(20)
  const snapToGrid = ref<boolean>(true)
  const showGrid = ref<boolean>(true)

  // 画布锁定
  const canvasLocked = ref<boolean>(false)

  // 迷你地图显示
  const showMiniMap = ref<boolean>(true)

  // Actions
  function setMode(newMode: EditorMode) {
    mode.value = newMode
    if (newMode !== 'add-node') {
      addingNodeType.value = null
    }
  }

  function setAddingNodeType(type: string | null) {
    addingNodeType.value = type
    if (type) {
      mode.value = 'add-node'
    }
  }

  function setPanning(value: boolean) {
    isPanning.value = value
    mode.value = value ? 'pan' : 'select'
  }

  function setZoom(newZoom: number) {
    zoom.value = Math.max(0.1, Math.min(4, newZoom))
  }

  function setPan(x: number, y: number) {
    pan.value = { x, y }
  }

  function toggleLeftPanel() {
    leftPanelExpanded.value = !leftPanelExpanded.value
  }

  function toggleRightPanel() {
    rightPanelExpanded.value = !rightPanelExpanded.value
  }

  function toggleLogPanel() {
    logPanelExpanded.value = !logPanelExpanded.value
  }

  function setLogPanelHeight(height: number) {
    logPanelHeight.value = Math.max(100, Math.min(500, height))
  }

  function openGlobalConfigDialog() {
    showGlobalConfigDialog.value = true
  }

  function closeGlobalConfigDialog() {
    showGlobalConfigDialog.value = false
  }

  function setSaving(value: boolean) {
    saving.value = value
  }

  function markUnsaved() {
    hasUnsavedChanges.value = true
  }

  function markSaved() {
    hasUnsavedChanges.value = false
  }

  function toggleSnapToGrid() {
    snapToGrid.value = !snapToGrid.value
  }

  function toggleGrid() {
    showGrid.value = !showGrid.value
  }

  function toggleCanvasLock() {
    canvasLocked.value = !canvasLocked.value
  }

  function toggleMiniMap() {
    showMiniMap.value = !showMiniMap.value
  }

  function resetEditor() {
    mode.value = 'select'
    addingNodeType.value = null
    isPanning.value = false
    zoom.value = 1
    pan.value = { x: 0, y: 0 }
    saving.value = false
    hasUnsavedChanges.value = false
    canvasLocked.value = false
  }

  return {
    // State
    mode,
    addingNodeType,
    isPanning,
    zoom,
    pan,
    leftPanelExpanded,
    rightPanelExpanded,
    logPanelExpanded,
    logPanelHeight,
    showGlobalConfigDialog,
    saving,
    hasUnsavedChanges,
    gridSize,
    snapToGrid,
    showGrid,
    canvasLocked,
    showMiniMap,
    // Actions
    setMode,
    setAddingNodeType,
    setPanning,
    setZoom,
    setPan,
    toggleLeftPanel,
    toggleRightPanel,
    toggleLogPanel,
    setLogPanelHeight,
    openGlobalConfigDialog,
    closeGlobalConfigDialog,
    setSaving,
    markUnsaved,
    markSaved,
    toggleSnapToGrid,
    toggleGrid,
    toggleCanvasLock,
    toggleMiniMap,
    resetEditor
  }
})