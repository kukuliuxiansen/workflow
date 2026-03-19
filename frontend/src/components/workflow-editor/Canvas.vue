<template>
  <div
    class="workflow-canvas"
    @dragover.prevent
    @drop="onDrop"
    @contextmenu.prevent="onContextMenu"
    @keydown="onKeyDown"
    tabindex="0"
  >
    <VueFlow
      ref="vueFlowRef"
      v-model:nodes="flowNodes"
      v-model:edges="flowEdges"
      :default-viewport="{ zoom: 1, x: 0, y: 0 }"
      :min-zoom="0.1"
      :max-zoom="4"
      :snap-to-grid="snapToGrid"
      :snap-grid="[gridSize, gridSize]"
      :pan-on-drag="panOnDrag"
      :selection-on-drag="selectionOnDrag"
      :pan-on-scroll="panOnScroll"
      :selection-mode="selectionMode"
      :multi-selection-active="multiSelectionActive"
      :delete-key-code="false"
      fit-view-on-init
      @node-click="onNodeClick"
      @node-double-click="onNodeDoubleClick"
      @node-drag-start="onNodeDragStart"
      @node-drag-stop="onNodeDragStop"
      @node-context="onNodeContextMenu"
      @connect="onConnect"
      @edge-click="onEdgeClick"
      @edge-context="onEdgeContextMenu"
      @pane-click="onPaneClick"
      @pane-context="onPaneContextMenu"
      @selection-change="onSelectionChange"
      @move="onMove"
      @zoom="onZoom"
    >
      <!-- 背景 -->
      <Background
        :gap="gridSize"
        :size="1"
        :pattern-color="gridColor"
        :variant="backgroundVariant"
      />

      <!-- 控制面板 -->
      <Controls position="bottom-right" :show-interactive="false">
        <template #top-center>
          <div class="zoom-indicator">{{ Math.round(zoom * 100) }}%</div>
        </template>
      </Controls>

      <!-- 小地图 -->
      <MiniMap
        :node-color="miniMapNodeColor"
        :node-stroke-width="3"
        pannable
        zoomable
      />

      <!-- 自定义节点 -->
      <template #node-start="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="start" :color="getNodeColor(nodeProps.id)" />
      </template>
      <template #node-finish="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="finish" :color="getNodeColor(nodeProps.id)" />
      </template>
      <template #node-agent_execution="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="agent_execution" :color="getNodeColor(nodeProps.id)" />
      </template>
      <template #node-api_call="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="api_call" :color="getNodeColor(nodeProps.id)" />
      </template>
      <template #node-condition="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="condition" :color="getNodeColor(nodeProps.id)" />
      </template>
      <template #node-human_review="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="human_review" :color="getNodeColor(nodeProps.id)" />
      </template>
      <template #node-parallel="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="parallel" :color="getNodeColor(nodeProps.id)" />
      </template>
      <template #node-loop="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="loop" :color="getNodeColor(nodeProps.id)" />
      </template>
      <template #node-wait="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="wait" :color="getNodeColor(nodeProps.id)" />
      </template>

      <!-- 自定义连线 -->
      <template #edge-success="edgeProps">
        <WorkflowEdge v-bind="edgeProps" type="success" @delete="onDeleteEdge" />
      </template>
      <template #edge-fail="edgeProps">
        <WorkflowEdge v-bind="edgeProps" type="fail" @delete="onDeleteEdge" />
      </template>

      <!-- 对齐辅助线 -->
      <template v-if="showAlignmentGuides">
        <div
          v-for="guide in alignmentGuides"
          :key="guide.id"
          class="alignment-guide"
          :class="guide.orientation"
          :style="getGuideStyle(guide)"
        />
      </template>
    </VueFlow>

    <!-- 右键菜单 -->
    <Teleport to="body">
      <div
        v-if="contextMenu.visible"
        class="context-menu"
        :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
        @click.stop
      >
        <template v-if="contextMenu.type === 'node'">
          <div class="menu-item" @click="copySelectedNodes">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
            </svg>
            <span>复制节点</span>
            <span class="shortcut">Ctrl+C</span>
          </div>
          <div class="menu-item" @click="pasteNodes">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/>
              <rect x="8" y="2" width="8" height="4" rx="1" ry="1"/>
            </svg>
            <span>粘贴节点</span>
            <span class="shortcut">Ctrl+V</span>
          </div>
          <div class="menu-item" @click="duplicateSelectedNodes">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="8" y="8" width="12" height="12" rx="2"/>
              <path d="M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2"/>
            </svg>
            <span>复制为副本</span>
            <span class="shortcut">Ctrl+D</span>
          </div>
          <div class="menu-divider"></div>
          <div class="menu-item" @click="selectAllNodes">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <path d="M8 12h8"/>
            </svg>
            <span>全选节点</span>
            <span class="shortcut">Ctrl+A</span>
          </div>
          <div class="menu-item" @click="invertSelection">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 2a10 10 0 0 0 0 20"/>
            </svg>
            <span>反选节点</span>
          </div>
          <div class="menu-divider"></div>
          <div class="menu-item" @click="alignNodes('left')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="4" y1="4" x2="4" y2="20"/>
              <rect x="8" y="6" width="12" height="4"/>
              <rect x="8" y="14" width="8" height="4"/>
            </svg>
            <span>左对齐</span>
          </div>
          <div class="menu-item" @click="alignNodes('center')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="4" x2="12" y2="20"/>
              <rect x="6" y="6" width="12" height="4"/>
              <rect x="8" y="14" width="8" height="4"/>
            </svg>
            <span>居中对齐</span>
          </div>
          <div class="menu-item" @click="alignNodes('right')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="20" y1="4" x2="20" y2="20"/>
              <rect x="4" y="6" width="12" height="4"/>
              <rect x="8" y="14" width="8" height="4"/>
            </svg>
            <span>右对齐</span>
          </div>
          <div class="menu-divider"></div>
          <div class="menu-item" @click="distributeNodes('horizontal')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="6" width="6" height="12"/>
              <rect x="16" y="6" width="6" height="12"/>
              <path d="M9 12h6"/>
            </svg>
            <span>水平分布</span>
          </div>
          <div class="menu-item" @click="distributeNodes('vertical')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="6" y="2" width="12" height="6"/>
              <rect x="6" y="16" width="12" height="6"/>
              <path d="M12 9v6"/>
            </svg>
            <span>垂直分布</span>
          </div>
          <div class="menu-divider"></div>
          <div class="menu-item" @click="bringToFront">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="8" y="8" width="12" height="12" rx="1"/>
              <rect x="4" y="4" width="12" height="12" rx="1"/>
            </svg>
            <span>置于顶层</span>
          </div>
          <div class="menu-item" @click="sendToBack">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="4" y="4" width="12" height="12" rx="1"/>
              <rect x="8" y="8" width="12" height="12" rx="1"/>
            </svg>
            <span>置于底层</span>
          </div>
          <div class="menu-divider"></div>
          <div class="menu-item submenu-trigger">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 2v4M12 18v4M2 12h4M18 12h4"/>
            </svg>
            <span>节点颜色</span>
            <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
            <div class="submenu color-submenu">
              <div class="color-grid">
                <div
                  v-for="color in nodeColors"
                  :key="color"
                  class="color-option"
                  :style="{ background: color }"
                  @click="setNodeColor(color)"
                />
              </div>
            </div>
          </div>
          <div class="menu-divider"></div>
          <div class="menu-item danger" @click="deleteSelectedNodes">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"/>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
            </svg>
            <span>删除节点</span>
            <span class="shortcut">Delete</span>
          </div>
        </template>
        <template v-else-if="contextMenu.type === 'edge'">
          <div class="menu-item" @click="editEdgeLabel">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
            <span>编辑标签</span>
          </div>
          <div class="menu-item" @click="flipEdgeType">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="17 1 21 5 17 9"/>
              <path d="M3 11V9a4 4 0 0 1 4-4h14"/>
              <polyline points="7 23 3 19 7 15"/>
              <path d="M21 13v2a4 4 0 0 1-4 4H3"/>
            </svg>
            <span>切换类型</span>
          </div>
          <div class="menu-divider"></div>
          <div class="menu-item danger" @click="deleteEdge(contextMenu.targetId)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"/>
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
            </svg>
            <span>删除连线</span>
          </div>
        </template>
        <template v-else>
          <div class="menu-item" @click="pasteNodes">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/>
              <rect x="8" y="2" width="8" height="4" rx="1" ry="1"/>
            </svg>
            <span>粘贴节点</span>
            <span class="shortcut">Ctrl+V</span>
          </div>
          <div class="menu-divider"></div>
          <div class="menu-item" @click="selectAllNodes">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <path d="M8 12h8"/>
            </svg>
            <span>全选节点</span>
            <span class="shortcut">Ctrl+A</span>
          </div>
          <div class="menu-divider"></div>
          <div class="menu-item" @click="toggleSnapToGrid">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="7"/>
              <rect x="14" y="3" width="7" height="7"/>
              <rect x="14" y="14" width="7" height="7"/>
              <rect x="3" y="14" width="7" height="7"/>
            </svg>
            <span>{{ snapToGrid ? '关闭网格对齐' : '开启网格对齐' }}</span>
          </div>
          <div class="menu-item" @click="toggleLockCanvas">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            <span>{{ canvasLocked ? '解锁画布' : '锁定画布' }}</span>
          </div>
          <div class="menu-divider"></div>
          <div class="menu-item" @click="fitView">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"/>
            </svg>
            <span>适应画布</span>
          </div>
          <div class="menu-item" @click="resetView">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 6v6l4 2"/>
            </svg>
            <span>重置视图</span>
          </div>
          <div class="menu-divider"></div>
          <div class="menu-item" @click="exportAsImage">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
              <circle cx="8.5" cy="8.5" r="1.5"/>
              <polyline points="21 15 16 10 5 21"/>
            </svg>
            <span>导出为图片</span>
          </div>
        </template>
      </div>
    </Teleport>

    <!-- 搜索面板 -->
    <Transition name="slide-fade">
      <div v-if="showSearch" class="search-panel">
        <div class="search-input-wrapper">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input
            ref="searchInputRef"
            v-model="searchQuery"
            type="text"
            placeholder="搜索节点..."
            @input="onSearchInput"
            @keydown.esc="closeSearch"
          />
          <button class="search-close" @click="closeSearch">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div v-if="searchResults.length > 0" class="search-results">
          <div
            v-for="result in searchResults"
            :key="result.id"
            class="search-result-item"
            :class="{ active: result.id === selectedNodeId }"
            @click="focusNode(result.id)"
          >
            <span class="result-type" :class="result.type">{{ getNodeTypeName(result.type) }}</span>
            <span class="result-name">{{ result.name }}</span>
          </div>
        </div>
        <div v-else-if="searchQuery" class="search-empty">
          未找到匹配的节点
        </div>
      </div>
    </Transition>

    <!-- 工具提示 -->
    <Teleport to="body">
      <div
        v-if="tooltip.visible"
        class="node-tooltip"
        :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }"
      >
        <div class="tooltip-title">{{ tooltip.title }}</div>
        <div v-if="tooltip.description" class="tooltip-description">{{ tooltip.description }}</div>
        <div class="tooltip-meta">
          <span v-if="tooltip.type">类型: {{ getNodeTypeName(tooltip.type) }}</span>
          <span v-if="tooltip.connections">连接: {{ tooltip.connections }}</span>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { VueFlow, useVueFlow, SelectionMode } from '@vue-flow/core'
import { Background, BackgroundVariant } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import { toPng } from 'html-to-image'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'
import { useWorkflowStore, useEditorStore } from '@/stores'
import type { EdgeType, FlowNode, FlowEdge, NodeType, WorkflowNode } from '@/types'
import WorkflowNode from './Node.vue'
import WorkflowEdge from './Edge.vue'

const emit = defineEmits<{
  (e: 'node-click', nodeId: string): void
  (e: 'node-drag', nodeId: string, x: number, y: number): void
  (e: 'connect', sourceId: string, targetId: string, type: EdgeType): void
  (e: 'canvas-click'): void
}>()

const workflowStore = useWorkflowStore()
const editorStore = useEditorStore()
const vueFlowRef = ref<any>(null)

// Vue Flow 实例方法
const {
  fitView: fitViewFlow,
  zoomIn,
  zoomOut,
  setCenter,
  getNodes,
  getEdges,
  addSelectedNodes,
  removeSelectedNodes,
  getSelectedNodes,
  getSelectedEdges,
  project
} = useVueFlow()

// ==================== 状态管理 ====================

// 画布状态
const zoom = ref(1)
const pan = ref({ x: 0, y: 0 })
const snapToGrid = ref(true)
const gridSize = ref(20)
const gridColor = ref('#313244')
const backgroundVariant = ref(BackgroundVariant.Dots)
const canvasLocked = ref(false)

// 选择状态
const selectedNodeIds = ref<string[]>([])
const selectedEdgeIds = ref<string[]>([])
const multiSelectionActive = ref(false)

// 平移和选择控制
const panOnDrag = computed(() => editorStore.isPanning || canvasLocked.value ? [1, 2] : false)
const panOnScroll = ref(false)
const selectionOnDrag = computed(() => !editorStore.isPanning && !canvasLocked.value)
const selectionMode = ref(SelectionMode.Partial)

// 右键菜单
const contextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  type: 'canvas' as 'node' | 'edge' | 'canvas',
  targetId: ''
})

// 复制粘贴
const clipboard = ref<WorkflowNode[]>([])

// 撤销重做
interface HistoryState {
  nodes: WorkflowNode[]
  edges: any[]
}
const history = ref<HistoryState[]>([])
const historyIndex = ref(-1)
const maxHistory = 50

// 搜索
const showSearch = ref(false)
const searchQuery = ref('')
const searchInputRef = ref<HTMLInputElement | null>(null)
const searchResults = ref<{ id: string; name: string; type: string }[]>([])

// 工具提示
const tooltip = ref({
  visible: false,
  x: 0,
  y: 0,
  title: '',
  description: '',
  type: '',
  connections: 0
})

// 对齐辅助线
const showAlignmentGuides = ref(false)
const alignmentGuides = ref<{ id: string; orientation: 'horizontal' | 'vertical'; position: number }[]>([])

// 节点颜色
const nodeColors = ref([
  '#f38ba8', '#fab387', '#f9e2af', '#a6e3a1', '#89dceb',
  '#89b4fa', '#cba6f7', '#f5c2e7', '#94e2d5', '#74c7ec'
])
const nodeColorMap = ref<Record<string, string>>({})

// 拖拽状态
const isDragging = ref(false)
const dragStartPosition = ref({ x: 0, y: 0 })

// ==================== 计算属性 ====================

// 转换节点数据
const flowNodes = computed<FlowNode[]>({
  get: () => {
    return workflowStore.nodes.map(node => ({
      id: node.id,
      type: node.type,
      position: { x: node.position_x, y: node.position_y },
      data: {
        label: node.name,
        type: node.type,
        config: node.config
      }
    }))
  },
  set: () => {}
})

// 转换连线数据
const flowEdges = computed<FlowEdge[]>({
  get: () => {
    return workflowStore.edges.map(edge => ({
      id: edge.id,
      source: edge.source_node_id,
      target: edge.target_node_id,
      type: edge.edge_type,
      animated: true,
      style: edge.edge_type === 'success' ? { stroke: '#a6e3a1' } : { stroke: '#f38ba8' }
    }))
  },
  set: () => {}
})

const selectedNodeId = computed(() => workflowStore.selectedNodeId)

// ==================== 节点颜色 ====================

function getNodeColor(nodeId: string): string {
  return nodeColorMap.value[nodeId] || ''
}

function setNodeColor(color: string) {
  selectedNodeIds.value.forEach(id => {
    nodeColorMap.value[id] = color
  })
  hideContextMenu()
}

// ==================== 右键菜单 ====================

function onContextMenu(event: MouseEvent) {
  event.preventDefault()
}

function onNodeContextMenu(event: { node: FlowNode; event: MouseEvent }) {
  event.event.preventDefault()
  const node = workflowStore.nodes.find(n => n.id === event.node.id)
  if (!node) return

  // 如果节点未选中，选中它
  if (!selectedNodeIds.value.includes(node.id)) {
    selectedNodeIds.value = [node.id]
    workflowStore.selectNode(node.id)
  }

  contextMenu.value = {
    visible: true,
    x: event.event.clientX,
    y: event.event.clientY,
    type: 'node',
    targetId: node.id
  }
}

function onEdgeContextMenu(event: { edge: FlowEdge; event: MouseEvent }) {
  event.event.preventDefault()
  contextMenu.value = {
    visible: true,
    x: event.event.clientX,
    y: event.event.clientY,
    type: 'edge',
    targetId: event.edge.id
  }
}

function onPaneContextMenu(event: MouseEvent) {
  contextMenu.value = {
    visible: true,
    x: event.clientX,
    y: event.clientY,
    type: 'canvas',
    targetId: ''
  }
}

function hideContextMenu() {
  contextMenu.value.visible = false
}

// ==================== 复制粘贴 ====================

function copySelectedNodes() {
  const nodes = workflowStore.nodes.filter(n => selectedNodeIds.value.includes(n.id))
  clipboard.value = JSON.parse(JSON.stringify(nodes))
  hideContextMenu()
}

function pasteNodes() {
  if (clipboard.value.length === 0) return

  const offset = 30
  clipboard.value.forEach(async (node, index) => {
    const newNode = await workflowStore.addNode(node.type as NodeType, {
      x: node.position_x + offset,
      y: node.position_y + offset + (index * 50)
    })
    if (newNode) {
      // 复制节点配置
      await workflowStore.updateNode(newNode.id, {
        name: node.name + ' (副本)',
        config: node.config
      })
    }
  })
  hideContextMenu()
}

function duplicateSelectedNodes() {
  copySelectedNodes()
  nextTick(() => {
    pasteNodes()
  })
}

// ==================== 删除操作 ====================

function deleteSelectedNodes() {
  selectedNodeIds.value.forEach(id => {
    workflowStore.deleteNode(id)
  })
  selectedNodeIds.value = []
  hideContextMenu()
}

function deleteEdge(edgeId: string) {
  workflowStore.deleteEdge(edgeId)
  hideContextMenu()
}

// ==================== 选择操作 ====================

function selectAllNodes() {
  selectedNodeIds.value = workflowStore.nodes.map(n => n.id)
  hideContextMenu()
}

function invertSelection() {
  const allIds = workflowStore.nodes.map(n => n.id)
  selectedNodeIds.value = allIds.filter(id => !selectedNodeIds.value.includes(id))
  hideContextMenu()
}

function onSelectionChange({ nodes, edges }: { nodes: FlowNode[]; edges: FlowEdge[] }) {
  selectedNodeIds.value = nodes.map(n => n.id)
  selectedEdgeIds.value = edges.map(e => e.id)
}

// ==================== 对齐操作 ====================

function alignNodes(direction: 'left' | 'center' | 'right' | 'top' | 'bottom') {
  const nodes = workflowStore.nodes.filter(n => selectedNodeIds.value.includes(n.id))
  if (nodes.length < 2) return

  let targetPosition: number
  if (direction === 'left') {
    targetPosition = Math.min(...nodes.map(n => n.position_x))
    nodes.forEach(n => {
      workflowStore.updateNodePosition(n.id, targetPosition, n.position_y)
    })
  } else if (direction === 'right') {
    targetPosition = Math.max(...nodes.map(n => n.position_x + 160))
    nodes.forEach(n => {
      workflowStore.updateNodePosition(n.id, targetPosition - 160, n.position_y)
    })
  } else if (direction === 'center') {
    const avgX = nodes.reduce((sum, n) => sum + n.position_x + 80, 0) / nodes.length
    nodes.forEach(n => {
      workflowStore.updateNodePosition(n.id, avgX - 80, n.position_y)
    })
  } else if (direction === 'top') {
    targetPosition = Math.min(...nodes.map(n => n.position_y))
    nodes.forEach(n => {
      workflowStore.updateNodePosition(n.id, n.position_x, targetPosition)
    })
  } else if (direction === 'bottom') {
    targetPosition = Math.max(...nodes.map(n => n.position_y + 60))
    nodes.forEach(n => {
      workflowStore.updateNodePosition(n.id, n.position_x, targetPosition - 60)
    })
  }
  hideContextMenu()
}

function distributeNodes(direction: 'horizontal' | 'vertical') {
  const nodes = workflowStore.nodes.filter(n => selectedNodeIds.value.includes(n.id))
  if (nodes.length < 3) return

  if (direction === 'horizontal') {
    const sorted = [...nodes].sort((a, b) => a.position_x - b.position_x)
    const minX = sorted[0].position_x
    const maxX = sorted[sorted.length - 1].position_x
    const step = (maxX - minX) / (sorted.length - 1)
    sorted.forEach((n, i) => {
      workflowStore.updateNodePosition(n.id, minX + step * i, n.position_y)
    })
  } else {
    const sorted = [...nodes].sort((a, b) => a.position_y - b.position_y)
    const minY = sorted[0].position_y
    const maxY = sorted[sorted.length - 1].position_y
    const step = (maxY - minY) / (sorted.length - 1)
    sorted.forEach((n, i) => {
      workflowStore.updateNodePosition(n.id, n.position_x, minY + step * i)
    })
  }
  hideContextMenu()
}

// ==================== 层级操作 ====================

function bringToFront() {
  // 在 Vue Flow 中，节点顺序由数组顺序决定
  selectedNodeIds.value.forEach(id => {
    const index = workflowStore.nodes.findIndex(n => n.id === id)
    if (index !== -1) {
      const node = workflowStore.nodes.splice(index, 1)[0]
      workflowStore.nodes.push(node)
    }
  })
  hideContextMenu()
}

function sendToBack() {
  selectedNodeIds.value.forEach(id => {
    const index = workflowStore.nodes.findIndex(n => n.id === id)
    if (index !== -1) {
      const node = workflowStore.nodes.splice(index, 1)[0]
      workflowStore.nodes.unshift(node)
    }
  })
  hideContextMenu()
}

// ==================== 连线操作 ====================

function editEdgeLabel() {
  // TODO: 实现连线标签编辑
  hideContextMenu()
}

function flipEdgeType() {
  const edge = workflowStore.edges.find(e => e.id === contextMenu.value.targetId)
  if (edge) {
    const newType = edge.edge_type === 'success' ? 'fail' : 'success'
    // 删除旧连线，创建新连线
    workflowStore.deleteEdge(edge.id)
    workflowStore.addEdge(edge.source_node_id, edge.target_node_id, newType)
  }
  hideContextMenu()
}

// ==================== 画布操作 ====================

function toggleSnapToGrid() {
  snapToGrid.value = !snapToGrid.value
  hideContextMenu()
}

function toggleLockCanvas() {
  canvasLocked.value = !canvasLocked.value
  hideContextMenu()
}

function fitView() {
  fitViewFlow({ padding: 0.2 })
  hideContextMenu()
}

function resetView() {
  setCenter(0, 0, { zoom: 1 })
  hideContextMenu()
}

async function exportAsImage() {
  if (!vueFlowRef.value) return
  try {
    const dataUrl = await toPng(vueFlowRef.value.$el)
    const link = document.createElement('a')
    link.download = `${workflowStore.name || 'workflow'}.png`
    link.href = dataUrl
    link.click()
  } catch (error) {
    console.error('Failed to export image:', error)
  }
  hideContextMenu()
}

// ==================== 搜索功能 ====================

function openSearch() {
  showSearch.value = true
  nextTick(() => {
    searchInputRef.value?.focus()
  })
}

function closeSearch() {
  showSearch.value = false
  searchQuery.value = ''
  searchResults.value = []
}

function onSearchInput() {
  if (!searchQuery.value.trim()) {
    searchResults.value = []
    return
  }
  const query = searchQuery.value.toLowerCase()
  searchResults.value = workflowStore.nodes
    .filter(n => n.name.toLowerCase().includes(query))
    .map(n => ({ id: n.id, name: n.name, type: n.type }))
}

function focusNode(nodeId: string) {
  const node = workflowStore.nodes.find(n => n.id === nodeId)
  if (node) {
    setCenter(node.position_x + 80, node.position_y + 30, { zoom: 1.5 })
    workflowStore.selectNode(nodeId)
    closeSearch()
  }
}

function getNodeTypeName(type: string): string {
  const names: Record<string, string> = {
    start: '开始节点',
    finish: '结束节点',
    agent_execution: 'Agent执行',
    api_call: 'API调用',
    condition: '条件判断',
    human_review: '人工审核',
    parallel: '并行',
    loop: '循环',
    wait: '等待'
  }
  return names[type] || type
}

// ==================== 撤销重做 ====================

function saveHistory() {
  // 删除当前位置之后的历史
  history.value = history.value.slice(0, historyIndex.value + 1)

  // 保存当前状态
  history.value.push({
    nodes: JSON.parse(JSON.stringify(workflowStore.nodes)),
    edges: JSON.parse(JSON.stringify(workflowStore.edges))
  })

  // 限制历史长度
  if (history.value.length > maxHistory) {
    history.value.shift()
  } else {
    historyIndex.value++
  }
}

function undo() {
  if (historyIndex.value > 0) {
    historyIndex.value--
    const state = history.value[historyIndex.value]
    workflowStore.nodes = JSON.parse(JSON.stringify(state.nodes))
    workflowStore.edges = JSON.parse(JSON.stringify(state.edges))
  }
}

function redo() {
  if (historyIndex.value < history.value.length - 1) {
    historyIndex.value++
    const state = history.value[historyIndex.value]
    workflowStore.nodes = JSON.parse(JSON.stringify(state.nodes))
    workflowStore.edges = JSON.parse(JSON.stringify(state.edges))
  }
}

// ==================== 工具提示 ====================

function showNodeTooltip(event: MouseEvent, node: WorkflowNode) {
  const connections = workflowStore.edges.filter(
    e => e.source_node_id === node.id || e.target_node_id === node.id
  ).length

  tooltip.value = {
    visible: true,
    x: event.clientX + 10,
    y: event.clientY + 10,
    title: node.name,
    description: '',
    type: node.type,
    connections
  }
}

function hideTooltip() {
  tooltip.value.visible = false
}

// ==================== 对齐辅助线 ====================

function calculateAlignmentGuides(nodeId: string, position: { x: number; y: number }) {
  const node = workflowStore.nodes.find(n => n.id === nodeId)
  if (!node) return

  const guides: typeof alignmentGuides.value = []
  const threshold = 5

  workflowStore.nodes.forEach(other => {
    if (other.id === nodeId) return

    // 水平居中对齐
    if (Math.abs(position.x + 80 - (other.position_x + 80)) < threshold) {
      guides.push({
        id: `v-center-${other.id}`,
        orientation: 'vertical',
        position: other.position_x + 80
      })
    }

    // 垂直居中对齐
    if (Math.abs(position.y + 30 - (other.position_y + 30)) < threshold) {
      guides.push({
        id: `h-center-${other.id}`,
        orientation: 'horizontal',
        position: other.position_y + 30
      })
    }

    // 左对齐
    if (Math.abs(position.x - other.position_x) < threshold) {
      guides.push({
        id: `v-left-${other.id}`,
        orientation: 'vertical',
        position: other.position_x
      })
    }

    // 右对齐
    if (Math.abs(position.x + 160 - (other.position_x + 160)) < threshold) {
      guides.push({
        id: `v-right-${other.id}`,
        orientation: 'vertical',
        position: other.position_x + 160
      })
    }
  })

  alignmentGuides.value = guides
}

function getGuideStyle(guide: typeof alignmentGuides.value[0]) {
  if (guide.orientation === 'vertical') {
    return { left: guide.position + 'px', top: '0', bottom: '0' }
  }
  return { top: guide.position + 'px', left: '0', right: '0' }
}

// ==================== 事件处理 ====================

function onNodeClick(event: { node: FlowNode }) {
  workflowStore.selectNode(event.node.id)
  emit('node-click', event.node.id)
}

function onNodeDoubleClick(event: { node: FlowNode }) {
  // 双击进入编辑模式
  workflowStore.selectNode(event.node.id)
}

function onNodeDragStart(event: { node: FlowNode }) {
  isDragging.value = true
  dragStartPosition.value = { x: event.node.position.x, y: event.node.position.y }
  showAlignmentGuides.value = true
  saveHistory()
}

function onNodeDragStop(event: { node: FlowNode }) {
  isDragging.value = false
  showAlignmentGuides.value = false
  alignmentGuides.value = []

  workflowStore.updateNodePosition(
    event.node.id,
    event.node.position.x,
    event.node.position.y
  )
  emit('node-drag', event.node.id, event.node.position.x, event.node.position.y)
}

function onConnect(params: { source: string; target: string }) {
  saveHistory()
  workflowStore.addEdge(params.source, params.target, 'success')
  emit('connect', params.source, params.target, 'success')
}

function onEdgeClick(event: { edge: FlowEdge }) {
  console.log('Edge clicked:', event.edge.id)
}

function onPaneClick() {
  workflowStore.selectNode(null)
  hideContextMenu()
  emit('canvas-click')
}

function onMove(event: { x: number; y: number }) {
  pan.value = { x: event.x, y: event.y }
}

function onZoom(event: { zoom: number }) {
  zoom.value = event.zoom
}

function onDrop(event: DragEvent) {
  event.preventDefault()
  const nodeType = event.dataTransfer?.getData('nodeType') as NodeType | undefined
  if (!nodeType) return

  const bounds = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const x = event.clientX - bounds.left
  const y = event.clientY - bounds.top

  saveHistory()
  workflowStore.addNode(nodeType, { x, y })
}

function onKeyDown(event: KeyboardEvent) {
  // Ctrl/Cmd + C: 复制
  if ((event.ctrlKey || event.metaKey) && event.key === 'c') {
    copySelectedNodes()
  }
  // Ctrl/Cmd + V: 粘贴
  else if ((event.ctrlKey || event.metaKey) && event.key === 'v') {
    pasteNodes()
  }
  // Ctrl/Cmd + D: 复制为副本
  else if ((event.ctrlKey || event.metaKey) && event.key === 'd') {
    event.preventDefault()
    duplicateSelectedNodes()
  }
  // Ctrl/Cmd + A: 全选
  else if ((event.ctrlKey || event.metaKey) && event.key === 'a') {
    event.preventDefault()
    selectAllNodes()
  }
  // Ctrl/Cmd + Z: 撤销
  else if ((event.ctrlKey || event.metaKey) && event.key === 'z' && !event.shiftKey) {
    event.preventDefault()
    undo()
  }
  // Ctrl/Cmd + Shift + Z 或 Ctrl/Cmd + Y: 重做
  else if ((event.ctrlKey || event.metaKey) && (event.key === 'y' || (event.key === 'z' && event.shiftKey))) {
    event.preventDefault()
    redo()
  }
  // Ctrl/Cmd + F: 搜索
  else if ((event.ctrlKey || event.metaKey) && event.key === 'f') {
    event.preventDefault()
    openSearch()
  }
  // Delete/Backspace: 删除
  else if (event.key === 'Delete' || event.key === 'Backspace') {
    if (selectedNodeIds.value.length > 0) {
      deleteSelectedNodes()
    }
  }
  // Escape: 关闭菜单/取消选择
  else if (event.key === 'Escape') {
    hideContextMenu()
    closeSearch()
    selectedNodeIds.value = []
  }
}

// ==================== 小地图节点颜色 ====================

function miniMapNodeColor(node: FlowNode): string {
  const typeColors: Record<string, string> = {
    start: '#a6e3a1',
    finish: '#f9e2af',
    agent_execution: '#cba6f7',
    api_call: '#89b4fa',
    condition: '#f9e2af',
    human_review: '#f38ba8',
    parallel: '#89dceb',
    loop: '#89dceb',
    wait: '#89dceb'
  }
  return typeColors[node.type] || '#cdd6f4'
}

// ==================== 生命周期 ====================

onMounted(() => {
  document.addEventListener('click', hideContextMenu)
  document.addEventListener('keydown', onKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('click', hideContextMenu)
  document.removeEventListener('keydown', onKeyDown)
})

// 监听编辑器添加节点模式
watch(() => editorStore.mode, async (newMode) => {
  if (newMode === 'add-node' && editorStore.addingNodeType) {
    const position = { x: 400, y: 300 }
    saveHistory()
    const node = await workflowStore.addNode(editorStore.addingNodeType, position)
    if (node) {
      emit('node-click', node.id)
    }
    editorStore.setMode('select')
  }
})

// 暴露方法供父组件调用
defineExpose({
  addNode: (type: NodeType, position: { x: number; y: number }) => {
    saveHistory()
    return workflowStore.addNode(type, position)
  },
  zoomIn: () => zoomIn(),
  zoomOut: () => zoomOut(),
  fitView: () => fitViewFlow({ padding: 0.2 }),
  undo,
  redo,
  exportAsImage
})
</script>

<style lang="scss" scoped>
.workflow-canvas {
  width: 100%;
  height: 100%;
  position: relative;
  outline: none;

  &:focus {
    outline: none;
  }
}

:deep(.vue-flow) {
  background: #11111b;
}

:deep(.vue-flow__node) {
  cursor: pointer;
}

:deep(.vue-flow__node.selected) {
  z-index: 1000 !important;
}

:deep(.vue-flow__edge-path) {
  stroke-width: 2;
}

:deep(.vue-flow__edge.selected .vue-flow__edge-path) {
  stroke-width: 3;
}

:deep(.vue-flow__handle) {
  width: 12px !important;
  height: 12px !important;
}

// 缩放指示器
.zoom-indicator {
  background: #1e1e2e;
  color: #cdd6f4;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

// 右键菜单
.context-menu {
  position: fixed;
  background: #1e1e2e;
  border: 1px solid #313244;
  border-radius: 12px;
  padding: 8px 0;
  min-width: 200px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  z-index: 10000;
  animation: fadeIn 0.15s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  color: #cdd6f4;
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;

  svg {
    width: 16px;
    height: 16px;
    color: #6c7086;
  }

  span {
    flex: 1;
  }

  .shortcut {
    color: #45475a;
    font-size: 12px;
  }

  .chevron {
    width: 14px;
    height: 14px;
    color: #45475a;
  }

  &:hover {
    background: #313244;

    svg {
      color: #cdd6f4;
    }
  }

  &.danger {
    color: #f38ba8;

    svg {
      color: #f38ba8;
    }

    &:hover {
      background: rgba(243, 139, 168, 0.1);
    }
  }
}

.menu-divider {
  height: 1px;
  background: #313244;
  margin: 8px 0;
}

.submenu-trigger {
  .submenu {
    display: none;
    position: absolute;
    left: 100%;
    top: 0;
    background: #1e1e2e;
    border: 1px solid #313244;
    border-radius: 12px;
    padding: 12px;
    margin-left: 4px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  }

  &:hover .submenu {
    display: block;
  }
}

.color-submenu {
  .color-grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 8px;
  }

  .color-option {
    width: 28px;
    height: 28px;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.15s ease;
    border: 2px solid transparent;

    &:hover {
      transform: scale(1.1);
      border-color: #cdd6f4;
    }
  }
}

// 搜索面板
.search-panel {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: #1e1e2e;
  border: 1px solid #313244;
  border-radius: 12px;
  width: 400px;
  max-width: 90%;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  z-index: 100;
  overflow: hidden;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  gap: 12px;
  border-bottom: 1px solid #313244;

  svg {
    width: 18px;
    height: 18px;
    color: #6c7086;
  }

  input {
    flex: 1;
    background: transparent;
    border: none;
    color: #cdd6f4;
    font-size: 14px;
    outline: none;

    &::placeholder {
      color: #45475a;
    }
  }

  .search-close {
    background: transparent;
    border: none;
    padding: 4px;
    cursor: pointer;
    color: #6c7086;
    transition: color 0.15s;

    &:hover {
      color: #cdd6f4;
    }

    svg {
      width: 16px;
      height: 16px;
    }
  }
}

.search-results {
  max-height: 300px;
  overflow-y: auto;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover, &.active {
    background: #313244;
  }

  .result-type {
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 500;

    &.start { background: rgba(166, 227, 161, 0.2); color: #a6e3a1; }
    &.finish { background: rgba(249, 226, 175, 0.2); color: #f9e2af; }
    &.agent_execution { background: rgba(203, 166, 247, 0.2); color: #cba6f7; }
    &.api_call { background: rgba(137, 180, 250, 0.2); color: #89b4fa; }
    &.condition { background: rgba(249, 226, 175, 0.2); color: #f9e2af; }
    &.human_review { background: rgba(243, 139, 168, 0.2); color: #f38ba8; }
    &.parallel, &.loop, &.wait { background: rgba(137, 220, 235, 0.2); color: #89dceb; }
  }

  .result-name {
    color: #cdd6f4;
    font-size: 14px;
  }
}

.search-empty {
  padding: 20px;
  text-align: center;
  color: #6c7086;
  font-size: 14px;
}

// 工具提示
.node-tooltip {
  position: fixed;
  background: #1e1e2e;
  border: 1px solid #313244;
  border-radius: 8px;
  padding: 12px;
  min-width: 150px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
  z-index: 10000;
  pointer-events: none;

  .tooltip-title {
    font-weight: 600;
    color: #cdd6f4;
    margin-bottom: 4px;
  }

  .tooltip-description {
    color: #a6adc8;
    font-size: 12px;
    margin-bottom: 8px;
  }

  .tooltip-meta {
    display: flex;
    gap: 12px;
    color: #6c7086;
    font-size: 11px;
  }
}

// 对齐辅助线
.alignment-guide {
  position: absolute;
  z-index: 999;
  pointer-events: none;

  &.horizontal {
    height: 1px;
    background: #cba6f7;
    left: 0;
    right: 0;
  }

  &.vertical {
    width: 1px;
    background: #cba6f7;
    top: 0;
    bottom: 0;
  }
}

// 动画
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.2s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateX(-50%) translateY(-10px);
  opacity: 0;
}
</style>