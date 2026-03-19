import axios from 'axios'
import type {
  WorkflowTemplate,
  WorkflowNode,
  WorkflowEdge,
  ExecutionRecord,
  ReviewRecord,
  Agent,
  LogEntry,
  PageRequest,
  PageResponse,
  ApiResponse,
  GlobalConfig
} from '@/types'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

// ============ 工作流 API ============

export const workflowApi = {
  // 获取工作流列表
  list(params: PageRequest & { status?: string }): Promise<PageResponse<WorkflowTemplate>> {
    return api.get('/workflows', { params })
  },

  // 获取工作流详情
  get(id: string): Promise<ApiResponse<WorkflowTemplate & { nodes: WorkflowNode[]; edges: WorkflowEdge[] }>> {
    return api.get(`/workflows/${id}`)
  },

  // 创建工作流
  create(data: Partial<WorkflowTemplate>): Promise<ApiResponse<WorkflowTemplate>> {
    return api.post('/workflows', data)
  },

  // 更新工作流
  update(id: string, data: Partial<WorkflowTemplate>): Promise<ApiResponse<WorkflowTemplate>> {
    return api.put(`/workflows/${id}`, data)
  },

  // 删除工作流
  delete(id: string): Promise<ApiResponse<void>> {
    return api.delete(`/workflows/${id}`)
  },

  // 复制工作流
  clone(id: string): Promise<ApiResponse<WorkflowTemplate>> {
    return api.post(`/workflows/${id}/clone`)
  },

  // 导出YAML
  exportYaml(id: string): Promise<ApiResponse<string>> {
    return api.get(`/workflows/${id}/export`)
  },

  // 导入YAML
  importYaml(yamlContent: string): Promise<ApiResponse<WorkflowTemplate>> {
    return api.post('/workflows/import', { yaml: yamlContent })
  },

  // 更新全局配置
  updateGlobalConfig(id: string, config: GlobalConfig): Promise<ApiResponse<void>> {
    return api.put(`/workflows/${id}/global-config`, config)
  }
}

// ============ 节点 API ============

export const nodeApi = {
  // 获取节点列表
  list(workflowId: string): Promise<ApiResponse<WorkflowNode[]>> {
    return api.get(`/workflows/${workflowId}/nodes`)
  },

  // 创建节点
  create(workflowId: string, data: Partial<WorkflowNode>): Promise<ApiResponse<WorkflowNode>> {
    return api.post(`/workflows/${workflowId}/nodes`, data)
  },

  // 更新节点
  update(workflowId: string, nodeId: string, data: Partial<WorkflowNode>): Promise<ApiResponse<WorkflowNode>> {
    return api.put(`/workflows/${workflowId}/nodes/${nodeId}`, data)
  },

  // 删除节点
  delete(workflowId: string, nodeId: string): Promise<ApiResponse<void>> {
    return api.delete(`/workflows/${workflowId}/nodes/${nodeId}`)
  },

  // 更新节点位置
  updatePosition(workflowId: string, nodeId: string, x: number, y: number): Promise<ApiResponse<void>> {
    return api.patch(`/workflows/${workflowId}/nodes/${nodeId}/position`, { positionX: x, positionY: y })
  }
}

// ============ 连线 API ============

export const edgeApi = {
  // 创建连线
  create(workflowId: string, data: Partial<WorkflowEdge>): Promise<ApiResponse<WorkflowEdge>> {
    return api.post(`/workflows/${workflowId}/edges`, data)
  },

  // 删除连线
  delete(workflowId: string, edgeId: string): Promise<ApiResponse<void>> {
    return api.delete(`/workflows/${workflowId}/edges/${edgeId}`)
  }
}

// ============ 执行 API ============

export const executionApi = {
  // 启动执行
  start(workflowId: string, inputData: Record<string, unknown>): Promise<ApiResponse<ExecutionRecord>> {
    return api.post(`/workflows/${workflowId}/executions`, { inputData })
  },

  // 获取执行状态
  get(executionId: string): Promise<ApiResponse<ExecutionRecord>> {
    return api.get(`/executions/${executionId}`)
  },

  // 暂停执行
  pause(executionId: string): Promise<ApiResponse<void>> {
    return api.post(`/executions/${executionId}/pause`)
  },

  // 停止执行
  stop(executionId: string): Promise<ApiResponse<void>> {
    return api.post(`/executions/${executionId}/stop`)
  },

  // 获取执行日志
  getLogs(executionId: string, params?: { nodeId?: string; level?: string }): Promise<ApiResponse<LogEntry[]>> {
    return api.get(`/executions/${executionId}/logs`, { params })
  },

  // 获取执行列表
  list(params: PageRequest & { workflowId?: string; status?: string }): Promise<PageResponse<ExecutionRecord>> {
    return api.get('/executions', { params })
  }
}

// ============ 审核 API ============

export const reviewApi = {
  // 获取待审核列表
  listPending(params: PageRequest): Promise<PageResponse<ReviewRecord>> {
    return api.get('/reviews/pending', { params })
  },

  // 提交审核结果
  respond(reviewId: string, decision: 'approve' | 'reject', comment?: string): Promise<ApiResponse<ReviewRecord>> {
    return api.post(`/reviews/${reviewId}/response`, { decision, comment })
  }
}

// ============ Agent API ============

export const agentApi = {
  // 获取Agent列表
  list(): Promise<ApiResponse<Agent[]>> {
    return api.get('/agents')
  }
}

// ============ AI API ============

export const aiApi = {
  // AI生成工作流
  generateWorkflow(prompt: string): Promise<ApiResponse<WorkflowTemplate>> {
    return api.post('/ai/generate-workflow', { prompt })
  }
}

export default api