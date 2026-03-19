import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { WorkflowNode, WorkflowEdge, GlobalConfig, NodeType } from '@/types'
import { nodeApi, edgeApi } from '@/api'

// 生成唯一ID
const generateId = (prefix: string = 'node') => {
  return `${prefix}_${Date.now().toString(16)}_${Math.random().toString(16).substring(2, 6)}`
}

export const useWorkflowStore = defineStore('workflow', () => {
  // 状态
  const workflowId = ref<string>('')
  const name = ref<string>('')
  const description = ref<string>('')
  const status = ref<'draft' | 'published'>('draft')
  const version = ref<string>('1.0')
  const nodes = ref<WorkflowNode[]>([])
  const edges = ref<WorkflowEdge[]>([])
  const globalConfig = ref<GlobalConfig>({})

  // 当前选中的节点ID
  const selectedNodeId = ref<string | null>(null)

  // 计算属性
  const selectedNode = computed(() => {
    if (!selectedNodeId.value) return null
    return nodes.value.find(n => n.id === selectedNodeId.value) || null
  })

  const hasStartNode = computed(() => {
    return nodes.value.some(n => n.type === 'start')
  })

  const hasFinishNode = computed(() => {
    return nodes.value.some(n => n.type === 'finish')
  })

  // Actions
  function setWorkflow(data: {
    id: string
    name: string
    description: string
    status: 'draft' | 'published'
    version: string
    global_config: string | null
    nodes: WorkflowNode[]
    edges: WorkflowEdge[]
  }) {
    workflowId.value = data.id
    name.value = data.name
    description.value = data.description || ''
    status.value = data.status
    version.value = data.version
    nodes.value = data.nodes || []
    edges.value = data.edges || []
    if (data.global_config) {
      try {
        globalConfig.value = JSON.parse(data.global_config)
      } catch (e) {
        globalConfig.value = {}
      }
    }
  }

  function resetWorkflow() {
    workflowId.value = ''
    name.value = ''
    description.value = ''
    status.value = 'draft'
    version.value = '1.0'
    nodes.value = []
    edges.value = []
    selectedNodeId.value = null
    globalConfig.value = {}
  }

  function updateBasicInfo(data: { name?: string; description?: string }) {
    if (data.name !== undefined) name.value = data.name
    if (data.description !== undefined) description.value = data.description
  }

  function updateGlobalConfig(config: Partial<GlobalConfig>) {
    globalConfig.value = { ...globalConfig.value, ...config }
  }

  async function addNode(type: NodeType, position: { x: number; y: number }) {
    const nodeNames: Record<string, string> = {
      start: '开始',
      finish: '结束',
      agent_execution: 'Agent执行',
      api_call: 'API调用',
      condition: '条件判断',
      human_review: '人工审核',
      parallel: '并行',
      loop: '循环',
      wait: '等待'
    }

    try {
      const response = await nodeApi.create(workflowId.value, {
        type,
        name: nodeNames[type] || '新节点',
        positionX: position.x,
        positionY: position.y
      })

      if (response.success && response.data) {
        nodes.value.push(response.data)
        return response.data
      }
    } catch (e) {
      console.error('创建节点失败:', e)
    }
    return null
  }

  async function updateNode(nodeId: string, updates: Partial<WorkflowNode>) {
    try {
      const response = await nodeApi.update(workflowId.value, nodeId, {
        name: updates.name,
        positionX: updates.position_x,
        positionY: updates.position_y,
        config: updates.config
      })

      if (response.success && response.data) {
        const index = nodes.value.findIndex(n => n.id === nodeId)
        if (index !== -1) {
          nodes.value[index] = response.data
        }
      }
    } catch (e) {
      console.error('更新节点失败:', e)
    }
  }

  function updateNodePosition(nodeId: string, x: number, y: number) {
    const index = nodes.value.findIndex(n => n.id === nodeId)
    if (index !== -1) {
      nodes.value[index].position_x = x
      nodes.value[index].position_y = y
    }
    // 异步保存到后端
    nodeApi.updatePosition(workflowId.value, nodeId, x, y).catch(console.error)
  }

  async function deleteNode(nodeId: string) {
    try {
      await nodeApi.delete(workflowId.value, nodeId)
      // 删除相关的连线
      edges.value = edges.value.filter(e =>
        e.source_node_id !== nodeId && e.target_node_id !== nodeId
      )
      // 删除节点
      nodes.value = nodes.value.filter(n => n.id !== nodeId)
      if (selectedNodeId.value === nodeId) {
        selectedNodeId.value = null
      }
    } catch (e) {
      console.error('删除节点失败:', e)
    }
  }

  function selectNode(nodeId: string | null) {
    selectedNodeId.value = nodeId
  }

  async function addEdge(sourceNodeId: string, targetNodeId: string, type: 'success' | 'fail') {
    // 检查是否已存在相同连线
    const exists = edges.value.some(e =>
      e.source_node_id === sourceNodeId &&
      e.target_node_id === targetNodeId &&
      e.edge_type === type
    )
    if (exists) return null

    try {
      const response = await edgeApi.create(workflowId.value, {
        source: sourceNodeId,
        target: targetNodeId,
        type
      })

      if (response.success && response.data) {
        edges.value.push(response.data)
        return response.data
      }
    } catch (e) {
      console.error('创建连线失败:', e)
    }
    return null
  }

  async function deleteEdge(edgeId: string) {
    try {
      await edgeApi.delete(workflowId.value, edgeId)
      edges.value = edges.value.filter(e => e.id !== edgeId)
    } catch (e) {
      console.error('删除连线失败:', e)
    }
  }

  function getEdgesByNodeId(nodeId: string) {
    return edges.value.filter(e => e.source_node_id === nodeId || e.target_node_id === nodeId)
  }

  function getSuccessTarget(nodeId: string): string | null {
    const edge = edges.value.find(e => e.source_node_id === nodeId && e.edge_type === 'success')
    return edge?.target_node_id || null
  }

  function getFailTarget(nodeId: string): string | null {
    const edge = edges.value.find(e => e.source_node_id === nodeId && e.edge_type === 'fail')
    return edge?.target_node_id || null
  }

  return {
    // State
    workflowId,
    name,
    description,
    status,
    version,
    nodes,
    edges,
    globalConfig,
    selectedNodeId,
    // Computed
    selectedNode,
    hasStartNode,
    hasFinishNode,
    // Actions
    setWorkflow,
    resetWorkflow,
    updateBasicInfo,
    updateGlobalConfig,
    addNode,
    updateNode,
    updateNodePosition,
    deleteNode,
    selectNode,
    addEdge,
    deleteEdge,
    getEdgesByNodeId,
    getSuccessTarget,
    getFailTarget
  }
})