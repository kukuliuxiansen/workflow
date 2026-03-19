<template>
  <div class="workflow-editor-page">
    <!-- 顶部工具栏 -->
    <header class="editor-toolbar">
      <div class="toolbar-left">
        <button class="btn-icon" @click="goBack" title="返回">
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
        <span class="status-badge" :class="workflowStore.status === 'PUBLISHED' ? 'published' : 'draft'">
          {{ workflowStore.status === 'PUBLISHED' ? '已发布' : '草稿' }}
        </span>
      </div>
      <div class="toolbar-center">
        <button class="btn-action save" @click="handleSave" :disabled="editorStore.saving">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
            <polyline points="17 21 17 13 7 13 7 21"/>
            <polyline points="7 3 7 8 15 8"/>
          </svg>
          <span>{{ editorStore.saving ? '保存中...' : '保存' }}</span>
        </button>
        <button class="btn-action export" @click="handleExport">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          <span>导出YAML</span>
        </button>
        <button class="btn-action run" @click="handleRun" :disabled="!canRun">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="5 3 19 12 5 21 5 3"/>
          </svg>
          <span>执行</span>
        </button>
      </div>
      <div class="toolbar-right">
        <button class="btn-icon" @click="editorStore.openGlobalConfigDialog" title="全局配置">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
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
          <button class="canvas-tool-btn" @click="zoomIn" title="放大">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              <line x1="11" y1="8" x2="11" y2="14"/>
              <line x1="8" y1="11" x2="14" y2="11"/>
            </svg>
          </button>
          <button class="canvas-tool-btn" @click="zoomOut" title="缩小">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              <line x1="8" y1="11" x2="14" y2="11"/>
            </svg>
          </button>
          <button class="canvas-tool-btn" @click="fitView" title="适应画布">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/>
            </svg>
          </button>
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
const starting = ref(false)
const runForm = reactive({
  taskDescription: ''
})

const canRun = computed(() => {
  return workflowStore.nodes.length > 0
})

function goBack() {
  router.push('/workflows')
}

async function loadWorkflow() {
  const workflowId = route.params.id as string
  try {
    const res = await workflowApi.get(workflowId)
    workflowStore.setWorkflow({
      id: res.data.id,
      name: res.data.name,
      description: res.data.description,
      status: res.data.status,
      version: res.data.version,
      globalConfig: res.data.globalConfig,
      nodes: res.data.nodes,
      edges: res.data.edges
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
}

function zoomOut() {
  canvasRef.value?.zoomOut()
}

function fitView() {
  canvasRef.value?.fitView()
}

onMounted(() => {
  loadWorkflow()
})

onUnmounted(() => {
  workflowStore.resetWorkflow()
  editorStore.resetEditor()
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
  padding: 12px 20px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  height: 60px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-center {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
    border-color: var(--border-light);
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
}

.workflow-name-input {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 8px 16px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  width: 200px;
  outline: none;
  transition: all 0.2s;

  &:focus {
    border-color: var(--accent-primary);
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
  }

  &::placeholder {
    color: var(--text-muted);
  }
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;

  &.draft {
    background: rgba(251, 191, 36, 0.1);
    color: #fbbf24;
    border: 1px solid rgba(251, 191, 36, 0.3);
  }

  &.published {
    background: rgba(16, 185, 129, 0.1);
    color: #10b981;
    border: 1px solid rgba(16, 185, 129, 0.3);
  }
}

.btn-action {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;

  svg {
    width: 16px;
    height: 16px;
  }

  &.save {
    background: var(--bg-card);
    border: 1px solid var(--border-color);
    color: var(--text-primary);

    &:hover:not(:disabled) {
      background: var(--bg-hover);
      border-color: var(--border-light);
    }
  }

  &.export {
    background: var(--bg-card);
    border: 1px solid var(--border-color);
    color: var(--text-primary);

    &:hover:not(:disabled) {
      background: var(--bg-hover);
      border-color: var(--border-light);
    }
  }

  &.run {
    background: var(--accent-gradient);
    color: white;
    box-shadow: var(--shadow-glow);

    &:hover:not(:disabled) {
      opacity: 0.9;
      transform: translateY(-1px);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
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
  gap: 8px;
  z-index: 10;
}

.canvas-tool-btn {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  box-shadow: var(--shadow-md);

  &:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
    border-color: var(--accent-primary);
  }

  svg {
    width: 20px;
    height: 20px;
  }
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
      border-color: var(--accent-primary);
      box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
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
    border-color: var(--border-light);
  }
}

.btn-primary {
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  background: var(--accent-gradient);
  color: white;
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
</style>