<template>
  <div class="workflow-editor-page">
    <!-- 顶部工具栏 -->
    <header class="editor-toolbar">
      <div class="toolbar-left">
        <button class="btn-icon" @click="goBack" title="返回 (Esc)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
        </button>
        <div class="divider"></div>
        <input
          v-model="workflowStore.name"
          placeholder="工作流名称"
          class="workflow-name-input"
          @change="handleNameChange"
        />
        <span class="status-badge" :class="workflowStore.status === 'published' ? 'published' : 'draft'">
          {{ workflowStore.status === 'published' ? '已发布' : '草稿' }}
        </span>
        <span v-if="editorStore.hasUnsavedChanges" class="unsaved-badge">未保存</span>
      </div>
      <div class="toolbar-center">
        <!-- 撤销/重做 -->
        <div class="toolbar-group">
          <button
            class="btn-icon"
            :disabled="!canUndo"
            @click="handleUndo"
            title="撤销 (Ctrl+Z)"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 7v6h6"/>
              <path d="M21 17a9 9 0 0 0-9-9 9 9 0 0 0-6 2.3L3 13"/>
            </svg>
          </button>
          <button
            class="btn-icon"
            :disabled="!canRedo"
            @click="handleRedo"
            title="重做 (Ctrl+Y)"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 7v6h-6"/>
              <path d="M3 17a9 9 0 0 1 9-9 9 9 0 0 1 6 2.3L21 13"/>
            </svg>
          </button>
        </div>
        <div class="divider"></div>
        <!-- 操作按钮 -->
        <button class="btn-action save" @click="handleSave" :disabled="editorStore.saving">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
            <polyline points="17 21 17 13 7 13 7 21"/>
            <polyline points="7 3 7 8 15 8"/>
          </svg>
          <span>{{ editorStore.saving ? '保存中...' : '保存' }}</span>
        </button>
        <button class="btn-action" @click="handleExport">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          <span>导出</span>
        </button>
        <button class="btn-action" @click="handleExportImage">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
            <circle cx="8.5" cy="8.5" r="1.5"/>
            <polyline points="21 15 16 10 5 21"/>
          </svg>
          <span>导出图片</span>
        </button>
        <div class="divider"></div>
        <button class="btn-action run" @click="handleRun" :disabled="!canRun">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="5 3 19 12 5 21 5 3"/>
          </svg>
          <span>执行</span>
        </button>
      </div>
      <div class="toolbar-right">
        <!-- 搜索 -->
        <button class="btn-icon" @click="openSearch" title="搜索节点 (Ctrl+F)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
        </button>
        <!-- 网格设置 -->
        <button
          class="btn-icon"
          :class="{ active: editorStore.snapToGrid }"
          @click="editorStore.toggleSnapToGrid"
          :title="editorStore.snapToGrid ? '关闭网格对齐' : '开启网格对齐'"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7"/>
            <rect x="14" y="3" width="7" height="7"/>
            <rect x="14" y="14" width="7" height="7"/>
            <rect x="3" y="14" width="7" height="7"/>
          </svg>
        </button>
        <!-- 小地图 -->
        <button
          class="btn-icon"
          :class="{ active: editorStore.showMiniMap }"
          @click="editorStore.toggleMiniMap"
          title="小地图"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"/>
            <line x1="8" y1="2" x2="8" y2="18"/>
            <line x1="16" y1="6" x2="16" y2="22"/>
          </svg>
        </button>
        <!-- 画布锁定 -->
        <button
          class="btn-icon"
          :class="{ active: editorStore.canvasLocked }"
          @click="editorStore.toggleCanvasLock"
          :title="editorStore.canvasLocked ? '解锁画布' : '锁定画布'"
        >
          <svg v-if="editorStore.canvasLocked" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
            <path d="M7 11V7a5 5 0 0 1 9.9-1"/>
          </svg>
        </button>
        <div class="divider"></div>
        <!-- 全局配置 -->
        <button class="btn-icon" @click="editorStore.openGlobalConfigDialog" title="全局配置">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
          </svg>
        </button>
        <!-- 帮助 -->
        <button class="btn-icon" @click="showHelpModal = true" title="快捷键帮助">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
            <line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
        </button>
      </div>
    </header>

    <!-- 主内容区 -->
    <div class="editor-main">
      <!-- 左侧组件面板 -->
      <LeftPanel :expanded="editorStore.leftPanelExpanded" @toggle="editorStore.toggleLeftPanel" />

      <!-- 画布区域 -->
      <div class="canvas-container">
        <Canvas
          ref="canvasRef"
          @node-click="handleNodeClick"
          @node-drag="handleNodeDrag"
          @connect="handleConnect"
          @canvas-click="handleCanvasClick"
        />
        <!-- 画布工具栏 -->
        <div class="canvas-tools">
          <button class="canvas-tool-btn" @click="zoomIn" title="放大 (Ctrl++)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              <line x1="11" y1="8" x2="11" y2="14"/>
              <line x1="8" y1="11" x2="14" y2="11"/>
            </svg>
          </button>
          <div class="zoom-display">{{ Math.round(editorStore.zoom * 100) }}%</div>
          <button class="canvas-tool-btn" @click="zoomOut" title="缩小 (Ctrl+-)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              <line x1="8" y1="11" x2="14" y2="11"/>
            </svg>
          </button>
          <button class="canvas-tool-btn" @click="fitView" title="适应画布 (Ctrl+0)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/>
            </svg>
          </button>
          <div class="tool-divider"></div>
          <button class="canvas-tool-btn" @click="resetView" title="重置视图">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 6v6l4 2"/>
            </svg>
          </button>
        </div>
        <!-- 节点统计 -->
        <div class="canvas-stats">
          <span>节点: {{ workflowStore.nodes.length }}</span>
          <span>连线: {{ workflowStore.edges.length }}</span>
        </div>
      </div>

      <!-- 右侧属性面板 -->
      <RightPanel
        :expanded="editorStore.rightPanelExpanded"
        @toggle="editorStore.toggleRightPanel"
      />
    </div>

    <!-- 底部日志面板 -->
    <LogPanel
      :expanded="editorStore.logPanelExpanded"
      :height="editorStore.logPanelHeight"
      @toggle="editorStore.toggleLogPanel"
      @resize="editorStore.setLogPanelHeight"
    />

    <!-- 全局配置对话框 -->
    <GlobalConfigDialog
      v-model="editorStore.showGlobalConfigDialog"
      :config="workflowStore.globalConfig"
      @save="handleGlobalConfigSave"
    />

    <!-- 帮助对话框 -->
    <Teleport to="body">
      <div v-if="showHelpModal" class="modal-overlay" @click.self="showHelpModal = false">
        <div class="modal-content help-modal">
          <div class="modal-header">
            <h3>快捷键帮助</h3>
            <button class="btn-close" @click="showHelpModal = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="help-section">
              <h4>编辑操作</h4>
              <div class="shortcut-list">
                <div class="shortcut-item">
                  <span class="keys"><kbd>Ctrl</kbd>+<kbd>C</kbd></span>
                  <span class="desc">复制节点</span>
                </div>
                <div class="shortcut-item">
                  <span class="keys"><kbd>Ctrl</kbd>+<kbd>V</kbd></span>
                  <span class="desc">粘贴节点</span>
                </div>
                <div class="shortcut-item">
                  <span class="keys"><kbd>Ctrl</kbd>+<kbd>D</kbd></span>
                  <span class="desc">复制为副本</span>
                </div>
                <div class="shortcut-item">
                  <span class="keys"><kbd>Ctrl</kbd>+<kbd>Z</kbd></span>
                  <span class="desc">撤销</span>
                </div>
                <div class="shortcut-item">
                  <span class="keys"><kbd>Ctrl</kbd>+<kbd>Y</kbd></span>
                  <span class="desc">重做</span>
                </div>
                <div class="shortcut-item">
                  <span class="keys"><kbd>Ctrl</kbd>+<kbd>A</kbd></span>
                  <span class="desc">全选节点</span>
                </div>
                <div class="shortcut-item">
                  <span class="keys"><kbd>Delete</kbd></span>
                  <span class="desc">删除选中</span>
                </div>
              </div>
            </div>
            <div class="help-section">
              <h4>画布操作</h4>
              <div class="shortcut-list">
                <div class="shortcut-item">
                  <span class="keys"><kbd>Ctrl</kbd>+<kbd>F</kbd></span>
                  <span class="desc">搜索节点</span>
                </div>
                <div class="shortcut-item">
                  <span class="keys"><kbd>Ctrl</kbd>+<kbd>+</kbd></span>
                  <span class="desc">放大</span>
                </div>
                <div class="shortcut-item">
                  <span class="keys"><kbd>Ctrl</kbd>+<kbd>-</kbd></span>
                  <span class="desc">缩小</span>
                </div>
                <div class="shortcut-item">
                  <span class="keys"><kbd>Ctrl</kbd>+<kbd>0</kbd></span>
                  <span class="desc">适应画布</span>
                </div>
                <div class="shortcut-item">
                  <span class="keys"><kbd>Ctrl</kbd>+<鼠标拖动></span>
                  <span class="desc">平移画布</span>
                </div>
                <div class="shortcut-item">
                  <span class="keys"><kbd>滚轮</kbd></span>
                  <span class="desc">缩放画布</span>
                </div>
              </div>
            </div>
            <div class="help-section">
              <h4>其他操作</h4>
              <div class="shortcut-list">
                <div class="shortcut-item">
                  <span class="keys"><kbd>Ctrl</kbd>+<kbd>S</kbd></span>
                  <span class="desc">保存工作流</span>
                </div>
                <div class="shortcut-item">
                  <span class="keys"><kbd>Esc</kbd></span>
                  <span class="desc">取消选择/关闭</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 执行对话框 -->
    <Teleport to="body">
      <div v-if="showRunDialog" class="modal-overlay" @click.self="showRunDialog = false">
        <div class="modal-content">
          <div class="modal-header">
            <h3>执行工作流</h3>
            <button class="btn-close" @click="showRunDialog = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>任务描述</label>
              <textarea
                v-model="runForm.taskDescription"
                placeholder="请输入任务描述..."
                rows="4"
              ></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" @click="showRunDialog = false">取消</button>
            <button class="btn-primary" @click="startExecution" :disabled="starting">
              {{ starting ? '启动中...' : '开始执行' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useWorkflowStore, useEditorStore, useExecutionStore } from '@/stores'
import { workflowApi, executionApi } from '@/api'
import type { EdgeType } from '@/types'
import Canvas from '@/components/workflow-editor/Canvas.vue'
import LeftPanel from '@/components/panels/LeftPanel.vue'
import RightPanel from '@/components/panels/RightPanel.vue'
import LogPanel from '@/components/panels/LogPanel.vue'
import GlobalConfigDialog from '@/components/dialogs/GlobalConfigDialog.vue'

const router = useRouter()
const route = useRoute()
const workflowStore = useWorkflowStore()
const editorStore = useEditorStore()
const executionStore = useExecutionStore()

const canvasRef = ref<InstanceType<typeof Canvas> | null>(null)
const showRunDialog = ref(false)
const showHelpModal = ref(false)
const starting = ref(false)
const runForm = reactive({
  taskDescription: ''
})

// 撤销重做状态
const canUndo = ref(false)
const canRedo = ref(false)

const canRun = computed(() => {
  return workflowStore.nodes.length > 0
})

function goBack() {
  if (editorStore.hasUnsavedChanges) {
    if (!confirm('有未保存的更改，确定要离开吗？')) {
      return
    }
  }
  router.push('/workflows')
}

async function loadWorkflow() {
  const workflowId = route.params.id as string
  try {
    const res = await workflowApi.get(workflowId)
    workflowStore.setWorkflow({
      id: res.data.id,
      name: res.data.name,
      description: res.data.description || '',
      status: res.data.status,
      version: res.data.version,
      global_config: res.data.global_config,
      nodes: res.data.nodes || [],
      edges: res.data.edges || []
    })
  } catch (error) {
    console.error('Failed to load workflow:', error)
  }
}

async function handleSave() {
  editorStore.setSaving(true)
  try {
    await workflowApi.update(workflowStore.workflowId, {
      name: workflowStore.name,
      description: workflowStore.description,
      globalConfig: workflowStore.globalConfig
    })
    editorStore.markSaved()
  } catch (error) {
    console.error('Failed to save workflow:', error)
  } finally {
    editorStore.setSaving(false)
  }
}

async function handleExport() {
  try {
    const res = await workflowApi.exportYaml(workflowStore.workflowId)
    const blob = new Blob([res.data], { type: 'text/yaml' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${workflowStore.name}.yaml`
    a.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('Failed to export workflow:', error)
  }
}

function handleExportImage() {
  canvasRef.value?.exportAsImage()
}

function handleRun() {
  runForm.taskDescription = ''
  showRunDialog.value = true
}

async function startExecution() {
  if (!runForm.taskDescription.trim()) {
    return
  }

  starting.value = true
  try {
    const res = await executionApi.start(workflowStore.workflowId, {
      task_description: runForm.taskDescription
    })
    showRunDialog.value = false
    executionStore.setCurrentExecution(res.data)
    executionStore.connectWebSocket(res.data.id)
    editorStore.toggleLogPanel()
  } catch (error) {
    console.error('Failed to start execution:', error)
  } finally {
    starting.value = false
  }
}

function handleNameChange() {
  editorStore.markUnsaved()
}

function handleNodeClick(nodeId: string) {
  workflowStore.selectNode(nodeId)
}

function handleNodeDrag(nodeId: string, x: number, y: number) {
  workflowStore.updateNodePosition(nodeId, x, y)
  editorStore.markUnsaved()
}

function handleConnect(sourceId: string, targetId: string, type: EdgeType) {
  workflowStore.addEdge(sourceId, targetId, type)
  editorStore.markUnsaved()
}

function handleCanvasClick() {
  workflowStore.selectNode(null)
}

async function handleGlobalConfigSave(config: typeof workflowStore.globalConfig) {
  workflowStore.updateGlobalConfig(config)
  editorStore.markUnsaved()
  editorStore.closeGlobalConfigDialog()
}

function zoomIn() {
  canvasRef.value?.zoomIn()
  editorStore.setZoom(editorStore.zoom * 1.2)
}

function zoomOut() {
  canvasRef.value?.zoomOut()
  editorStore.setZoom(editorStore.zoom / 1.2)
}

function fitView() {
  canvasRef.value?.fitView()
  editorStore.setZoom(1)
}

function resetView() {
  canvasRef.value?.fitView()
  editorStore.setZoom(1)
}

function openSearch() {
  // 搜索功能在 Canvas 组件内部实现
}

function handleUndo() {
  canvasRef.value?.undo()
}

function handleRedo() {
  canvasRef.value?.redo()
}

// 键盘快捷键
function onKeyDown(event: KeyboardEvent) {
  // Ctrl/Cmd + S: 保存
  if ((event.ctrlKey || event.metaKey) && event.key === 's') {
    event.preventDefault()
    handleSave()
  }
  // Ctrl/Cmd + F: 搜索
  else if ((event.ctrlKey || event.metaKey) && event.key === 'f') {
    event.preventDefault()
    openSearch()
  }
  // Ctrl/Cmd + +: 放大
  else if ((event.ctrlKey || event.metaKey) && event.key === '=') {
    event.preventDefault()
    zoomIn()
  }
  // Ctrl/Cmd + -: 缩小
  else if ((event.ctrlKey || event.metaKey) && event.key === '-') {
    event.preventDefault()
    zoomOut()
  }
  // Ctrl/Cmd + 0: 适应画布
  else if ((event.ctrlKey || event.metaKey) && event.key === '0') {
    event.preventDefault()
    fitView()
  }
  // ?: 帮助
  else if (event.key === '?' || (event.shiftKey && event.key === '/')) {
    showHelpModal.value = true
  }
}

onMounted(() => {
  loadWorkflow()
  document.addEventListener('keydown', onKeyDown)
})

onUnmounted(() => {
  workflowStore.resetWorkflow()
  editorStore.resetEditor()
  document.removeEventListener('keydown', onKeyDown)
})
</script>

<style lang="scss" scoped>
.workflow-editor-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-tertiary);
}

.editor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  height: 56px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-center {
  display: flex;
  align-items: center;
  gap: 6px;
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 2px;
  background: var(--bg-card);
  border-radius: 8px;
  padding: 2px;
}

.btn-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  &.active {
    background: rgba(203, 166, 247, 0.2);
    color: #cba6f7;
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  svg {
    width: 18px;
    height: 18px;
  }
}

.divider {
  width: 1px;
  height: 24px;
  background: var(--border-color);
  margin: 0 4px;
}

.workflow-name-input {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 8px 14px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  width: 180px;
  outline: none;
  transition: all 0.2s;

  &:focus {
    border-color: #cba6f7;
    box-shadow: 0 0 0 3px rgba(203, 166, 247, 0.1);
  }

  &::placeholder {
    color: var(--text-muted);
  }
}

.status-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 500;

  &.draft {
    background: rgba(251, 191, 36, 0.1);
    color: #fbbf24;
    border: 1px solid rgba(251, 191, 36, 0.3);
  }

  &.published {
    background: rgba(166, 227, 161, 0.1);
    color: #a6e3a1;
    border: 1px solid rgba(166, 227, 161, 0.3);
  }
}

.unsaved-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 500;
  background: rgba(243, 139, 168, 0.1);
  color: #f38ba8;
  border: 1px solid rgba(243, 139, 168, 0.3);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.btn-action {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  background: var(--bg-card);
  color: var(--text-primary);

  svg {
    width: 16px;
    height: 16px;
  }

  &:hover:not(:disabled) {
    background: var(--bg-hover);
  }

  &.save {
    border: 1px solid var(--border-color);
  }

  &.run {
    background: linear-gradient(135deg, #cba6f7 0%, #f5c2e7 100%);
    color: #1e1e2e;

    &:hover:not(:disabled) {
      opacity: 0.9;
      transform: translateY(-1px);
    }
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.editor-main {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

.canvas-container {
  flex: 1;
  position: relative;
  background: var(--bg-tertiary);
  background-image:
    radial-gradient(circle at 1px 1px, var(--border-color) 1px, transparent 0);
  background-size: 20px 20px;
}

.canvas-tools {
  position: absolute;
  bottom: 20px;
  right: 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  z-index: 10;
  background: var(--bg-card);
  border-radius: 10px;
  padding: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.canvas-tool-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  svg {
    width: 18px;
    height: 18px;
  }
}

.zoom-display {
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
  padding: 4px 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
}

.tool-divider {
  height: 1px;
  background: var(--border-color);
  margin: 4px 0;
}

.canvas-stats {
  position: absolute;
  bottom: 20px;
  left: 20px;
  display: flex;
  gap: 16px;
  background: var(--bg-card);
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 12px;
  color: var(--text-muted);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  width: 480px;
  max-width: 90%;
  box-shadow: var(--shadow-lg);
}

.help-modal {
  width: 560px;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);

  h3 {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
  }
}

.btn-close {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  svg {
    width: 18px;
    height: 18px;
  }
}

.modal-body {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.form-group {
  label {
    display: block;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    margin-bottom: 8px;
  }

  textarea {
    width: 100%;
    background: var(--bg-card);
    border: 1px solid var(--border-color);
    border-radius: 10px;
    padding: 12px 16px;
    color: var(--text-primary);
    font-size: 14px;
    resize: vertical;
    outline: none;
    transition: all 0.2s;

    &:focus {
      border-color: #cba6f7;
      box-shadow: 0 0 0 3px rgba(203, 166, 247, 0.1);
    }

    &::placeholder {
      color: var(--text-muted);
    }
  }
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
}

.btn-secondary {
  padding: 10px 20px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--bg-hover);
  }
}

.btn-primary {
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #cba6f7 0%, #f5c2e7 100%);
  color: #1e1e2e;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    opacity: 0.9;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

/* Help Modal */
.help-section {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }

  h4 {
    font-size: 14px;
    font-weight: 600;
    color: #cba6f7;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--border-color);
  }
}

.shortcut-list {
  display: grid;
  gap: 8px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--bg-card);
  border-radius: 8px;

  .keys {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
  }

  kbd {
    background: var(--bg-tertiary);
    border: 1px solid var(--border-color);
    border-radius: 4px;
    padding: 2px 8px;
    font-family: inherit;
    font-size: 11px;
    color: var(--text-primary);
  }

  .desc {
    font-size: 13px;
    color: var(--text-secondary);
  }
}
</style>