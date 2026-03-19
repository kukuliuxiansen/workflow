<template>
  <div
    class="workflow-canvas"
    @dragover.prevent
    @drop="onDrop"
  >
    <VueFlow
      ref="vueFlowRef"
      v-model:nodes="flowNodes"
      v-model:edges="flowEdges"
      :default-viewport="{ zoom: 1, x: 0, y: 0 }"
      :min-zoom="0.25"
      :max-zoom="2"
      :snap-to-grid="true"
      :snap-grid="[20, 20]"
      fit-view-on-init
      @node-click="onNodeClick"
      @node-drag-stop="onNodeDragStop"
      @connect="onConnect"
      @edge-click="onEdgeClick"
      @pane-click="onPaneClick"
    >
      <!-- 背景 -->
      <Background :gap="20" :size="1" pattern-color="#313244" />

      <!-- 控制面板 -->
      <Controls />

      <!-- 小地图 -->
      <MiniMap />

      <!-- 自定义节点 -->
      <template #node-start="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="start" />
      </template>
      <template #node-finish="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="finish" />
      </template>
      <template #node-agent_execution="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="agent_execution" />
      </template>
      <template #node-api_call="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="api_call" />
      </template>
      <template #node-condition="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="condition" />
      </template>
      <template #node-human_review="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="human_review" />
      </template>
      <template #node-parallel="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="parallel" />
      </template>
      <template #node-loop="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="loop" />
      </template>
      <template #node-wait="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="wait" />
      </template>

      <!-- 自定义连线 -->
      <template #edge-success="edgeProps">
        <WorkflowEdge v-bind="edgeProps" type="success" @delete="onDeleteEdge" />
      </template>
      <template #edge-fail="edgeProps">
        <WorkflowEdge v-bind="edgeProps" type="fail" @delete="onDeleteEdge" />
      </template>
    </VueFlow>
  </div>
</template>

<script setup lang="ts">
import { computed, watch, ref } from 'vue'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'
import { useWorkflowStore, useEditorStore } from '@/stores'
import type { EdgeType, FlowNode, FlowEdge, NodeType } from '@/types'
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

const { fitView: fitViewFlow, zoomIn, zoomOut } = useVueFlow()

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

// 监听编辑器添加节点模式
watch(() => editorStore.mode, async (newMode) => {
  if (newMode === 'add-node' && editorStore.addingNodeType) {
    const position = { x: 400, y: 300 }
    const node = await workflowStore.addNode(editorStore.addingNodeType as NodeType, position)
    if (node) {
      emit('node-click', node.id)
    }
    editorStore.setMode('select')
  }
})

// 节点点击事件
function onNodeClick(event: { node: FlowNode }) {
  workflowStore.selectNode(event.node.id)
  emit('node-click', event.node.id)
}

// 节点拖拽结束事件
function onNodeDragStop(event: { node: FlowNode }) {
  workflowStore.updateNodePosition(
    event.node.id,
    event.node.position.x,
    event.node.position.y
  )
  emit('node-drag', event.node.id, event.node.position.x, event.node.position.y)
}

// 连接事件
async function onConnect(params: { source: string; target: string }) {
  const edge = await workflowStore.addEdge(params.source, params.target, 'success')
  if (edge) {
    emit('connect', params.source, params.target, 'success')
  }
}

// 连线点击事件
function onEdgeClick(event: { edge: FlowEdge }) {
  console.log('Edge clicked:', event.edge.id)
}

// 删除连线
function onDeleteEdge(edgeId: string) {
  workflowStore.deleteEdge(edgeId)
}

// 画布点击事件
function onPaneClick() {
  workflowStore.selectNode(null)
  emit('canvas-click')
}

// 拖放事件处理
async function onDrop(event: DragEvent) {
  event.preventDefault()
  const nodeType = event.dataTransfer?.getData('nodeType') as NodeType | undefined
  if (!nodeType) return

  const bounds = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const x = event.clientX - bounds.left
  const y = event.clientY - bounds.top

  await workflowStore.addNode(nodeType, { x, y })
}

// 暴露方法供父组件调用
defineExpose({
  addNode: (type: NodeType, position: { x: number; y: number }) => {
    return workflowStore.addNode(type, position)
  },
  zoomIn: () => zoomIn(),
  zoomOut: () => zoomOut(),
  fitView: () => fitViewFlow({ padding: 0.2 })
})
</script>

<style lang="scss" scoped>
.workflow-canvas {
  width: 100%;
  height: 100%;
}

:deep(.vue-flow) {
  background: #11111b;
}

:deep(.vue-flow__node) {
  cursor: pointer;
}
</style>