import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { WorkflowNode, WorkflowEdge, GlobalConfig, NodeType, NodeConfig } from '@/types'

// 生成唯一ID
const generateId = (prefix: string = 'node') => {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
}

export const useWorkflowStore = defineStore('workflow', () => {
  // 状态
  const workflowId = ref<string>('')
  const name = ref<string>('')
  const description = ref<string>('')
  const status = ref<'draft' | 'published'>('draft')
  const version = ref<number>(1)
  const nodes = ref<WorkflowNode[]>([])
  const edges = ref<WorkflowEdge[]>([])
  const globalConfig = ref<GlobalConfig>({
    projectPath: '',
    feishuOpenId: '',
    maxGlobalLoop: 3,
    timeout: 300,
    retryInterval: 60
  })

  // 当前选中的节点ID
  const selectedNodeId = ref<string | null>(null)

  // 计算属性
  const selectedNode = computed(() => {
    if (!selectedNodeId.value) return null
    return nodes.value.find(n => n.id === selectedNodeId.value) || null
  })

  const hasStartNode = computed(() => {
    return nodes.value.some(n => n.name === 'start' || nodes.value.findIndex(node => node.id === n.id) === 0)
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
    version: number
    globalConfig: GlobalConfig
    nodes: WorkflowNode[]
    edges: WorkflowEdge[]
  }) {
    workflowId.value = data.id
    name.value = data.name
    description.value = data.description
    status.value = data.status
    version.value = data.version
    globalConfig.value = data.globalConfig
    nodes.value = data.nodes
    edges.value = data.edges
  }

  function resetWorkflow() {
    workflowId.value = ''
    name.value = ''
    description.value = ''
    status.value = 'draft'
    version.value = 1
    nodes.value = []
    edges.value = []
    selectedNodeId.value = null
    globalConfig.value = {
      projectPath: '',
      feishuOpenId: '',
      maxGlobalLoop: 3,
      timeout: 300,
      retryInterval: 60
    }
  }

  function updateBasicInfo(data: { name?: string; description?: string }) {
    if (data.name !== undefined) name.value = data.name
    if (data.description !== undefined) description.value = data.description
  }

  function updateGlobalConfig(config: Partial<GlobalConfig>) {
    globalConfig.value = { ...globalConfig.value, ...config }
  }

  function addNode(type: NodeType, position: { x: number; y: number }, config?: Partial<NodeConfig>) {
    const nodeNames: Record<NodeType, string> = {
      agent_execution: 'Agent执行',
      api_call: 'API调用',
      finish: '结束'
    }

    const defaultConfig: NodeConfig = {
      timeout: 300,
      retryInterval: 60,
      inputFields: [],
      outputFields: [],
      ...config
    }

    const node: WorkflowNode = {
      id: generateId('node'),
      workflowId: workflowId.value,
      type,
      name: config?.agentId || nodeNames[type],
      positionX: position.x,
      positionY: position.y,
      config: defaultConfig,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }

    nodes.value.push(node)
    return node
  }

  function updateNode(nodeId: string, updates: Partial<WorkflowNode>) {
    const index = nodes.value.findIndex(n => n.id === nodeId)
    if (index !== -1) {
      nodes.value[index] = {
        ...nodes.value[index],
        ...updates,
        updatedAt: new Date().toISOString()
      }
    }
  }

  function updateNodeConfig(nodeId: string, configUpdates: Partial<NodeConfig>) {
    const index = nodes.value.findIndex(n => n.id === nodeId)
    if (index !== -1) {
      nodes.value[index] = {
        ...nodes.value[index],
        config: {
          ...nodes.value[index].config,
          ...configUpdates
        },
        updatedAt: new Date().toISOString()
      }
    }
  }

  function updateNodePosition(nodeId: string, x: number, y: number) {
    const index = nodes.value.findIndex(n => n.id === nodeId)
    if (index !== -1) {
      nodes.value[index].positionX = x
      nodes.value[index].positionY = y
    }
  }

  function deleteNode(nodeId: string) {
    // 删除相关的连线
    edges.value = edges.value.filter(e =>
      e.sourceNodeId !== nodeId && e.targetNodeId !== nodeId
    )
    // 删除节点
    nodes.value = nodes.value.filter(n => n.id !== nodeId)
    if (selectedNodeId.value === nodeId) {
      selectedNodeId.value = null
    }
  }

  function selectNode(nodeId: string | null) {
    selectedNodeId.value = nodeId
  }

  function addEdge(sourceNodeId: string, targetNodeId: string, type: 'success' | 'fail') {
    // 检查是否已存在相同连线
    const exists = edges.value.some(e =>
      e.sourceNodeId === sourceNodeId &&
      e.targetNodeId === targetNodeId &&
      e.type === type
    )
    if (exists) return null

    const edge: WorkflowEdge = {
      id: generateId('edge'),
      workflowId: workflowId.value,
      sourceNodeId,
      targetNodeId,
      type,
      createdAt: new Date().toISOString()
    }

    edges.value.push(edge)
    return edge
  }

  function deleteEdge(edgeId: string) {
    edges.value = edges.value.filter(e => e.id !== edgeId)
  }

  function getEdgesByNodeId(nodeId: string) {
    return edges.value.filter(e => e.sourceNodeId === nodeId || e.targetNodeId === nodeId)
  }

  function getSuccessTarget(nodeId: string): string | null {
    const edge = edges.value.find(e => e.sourceNodeId === nodeId && e.type === 'success')
    return edge?.targetNodeId || null
  }

  function getFailTarget(nodeId: string): string | null {
    const edge = edges.value.find(e => e.sourceNodeId === nodeId && e.type === 'fail')
    return edge?.targetNodeId || null
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
    updateNodeConfig,
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