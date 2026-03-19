<template>
  <div class="workflow-canvas">
    <VueFlow
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
      @pane-click="onPaneClick"
    >
      <!-- 背景 -->
      <Background :gap="20" :size="1" pattern-color="#e4e7ed" />

      <!-- 控制面板 -->
      <Controls />

      <!-- 小地图 -->
      <MiniMap />

      <!-- 自定义节点 -->
      <template #node-agent_execution="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="agent_execution" />
      </template>
      <template #node-api_call="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="api_call" />
      </template>
      <template #node-finish="nodeProps">
        <WorkflowNode v-bind="nodeProps" type="finish" />
      </template>

      <!-- 自定义连线 -->
      <template #edge-success="edgeProps">
        <WorkflowEdge v-bind="edgeProps" type="success" />
      </template>
      <template #edge-fail="edgeProps">
        <WorkflowEdge v-bind="edgeProps" type="fail" />
      </template>
    </VueFlow>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'
import { useWorkflowStore, useEditorStore } from '@/stores'
import type { NodeType, EdgeType, FlowNode, FlowEdge } from '@/types'
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

// 转换节点数据
const flowNodes = computed<FlowNode[]>({
  get: () => {
    return workflowStore.nodes.map(node => ({
      id: node.id,
      type: node.type,
      position: { x: node.positionX, y: node.positionY },
      data: {
        label: node.name,
        type: node.type,
        config: node.config
      }
    }))
  },
  set: (_nodes) => {
    // Vue Flow 会尝试更新这个，但我们通过其他方式管理状态
  }
})

// 转换连线数据
const flowEdges = computed<FlowEdge[]>({
  get: () => {
    return workflowStore.edges.map(edge => ({
      id: edge.id,
      source: edge.sourceNodeId,
      target: edge.targetNodeId,
      type: edge.type,
      animated: true,
      style: edge.type === 'success' ? { stroke: '#67C23A' } : { stroke: '#F56C6C' }
    }))
  },
  set: (_edges) => {
    // Vue Flow 会尝试更新这个，但我们通过其他方式管理状态
  }
})

// 监听编辑器添加节点模式
watch(() => editorStore.mode, (newMode) => {
  if (newMode === 'add-node' && editorStore.addingNodeType) {
    // 在画布中心添加节点
    const position = { x: 400, y: 300 }
    const node = workflowStore.addNode(editorStore.addingNodeType, position)
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
function onConnect(params: { source: string; target: string }) {
  // 默认为成功连线
  const edge = workflowStore.addEdge(params.source, params.target, 'success')
  if (edge) {
    emit('connect', params.source, params.target, 'success')
  }
}

// 画布点击事件
function onPaneClick() {
  workflowStore.selectNode(null)
  emit('canvas-click')
}

// 暴露方法供父组件调用
defineExpose({
  addNode: (type: NodeType, position: { x: number; y: number }) => {
    return workflowStore.addNode(type, position)
  }
})
</script>

<style lang="scss" scoped>
.workflow-canvas {
  width: 100%;
  height: 100%;
}

:deep(.vue-flow) {
  background: #f5f7fa;
}

:deep(.vue-flow__node) {
  cursor: pointer;
}
</style>