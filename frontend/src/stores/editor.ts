import { defineStore } from 'pinia'
import { ref } from 'vue'

export type EditorMode = 'select' | 'add-node' | 'connect'

export const useEditorStore = defineStore('editor', () => {
  // 编辑器模式
  const mode = ref<EditorMode>('select')

  // 当前要添加的节点类型
  const addingNodeType = ref<'agent_execution' | 'api_call' | 'finish' | null>(null)

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

  // Actions
  function setMode(newMode: EditorMode) {
    mode.value = newMode
    if (newMode !== 'add-node') {
      addingNodeType.value = null
    }
  }

  function setAddingNodeType(type: 'agent_execution' | 'api_call' | 'finish' | null) {
    addingNodeType.value = type
    if (type) {
      mode.value = 'add-node'
    }
  }

  function setZoom(newZoom: number) {
    zoom.value = Math.max(0.25, Math.min(2, newZoom))
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

  function resetEditor() {
    mode.value = 'select'
    addingNodeType.value = null
    zoom.value = 1
    pan.value = { x: 0, y: 0 }
    saving.value = false
    hasUnsavedChanges.value = false
  }

  return {
    // State
    mode,
    addingNodeType,
    zoom,
    pan,
    leftPanelExpanded,
    rightPanelExpanded,
    logPanelExpanded,
    logPanelHeight,
    showGlobalConfigDialog,
    saving,
    hasUnsavedChanges,
    // Actions
    setMode,
    setAddingNodeType,
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
    resetEditor
  }
})