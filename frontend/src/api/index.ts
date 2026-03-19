import axios from 'axios'
import type {
  WorkflowTemplate,
  WorkflowNode,
  WorkflowEdge,
  ExecutionRecord,
  GlobalConfig,
  ApiResponse
} from '@/types'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

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
  list() {
    return api.get<any, ApiResponse<WorkflowTemplate[]>>('/workflows')
  },

  // 获取工作流详情
  get(id: string) {
    return api.get<any, ApiResponse<WorkflowTemplate & { nodes: WorkflowNode[]; edges: WorkflowEdge[] }>>(`/workflows/${id}`)
  },

  // 创建工作流
  create(data: { name: string; description?: string }) {
    return api.post<any, ApiResponse<WorkflowTemplate>>('/workflows', data)
  },

  // 更新工作流
  update(id: string, data: { name?: string; description?: string; globalConfig?: string }) {
    return api.put<any, ApiResponse<WorkflowTemplate>>(`/workflows/${id}`, data)
  },

  // 删除工作流
  delete(id: string) {
    return api.delete<any, ApiResponse<void>>(`/workflows/${id}`)
  },

  // 复制工作流
  clone(id: string) {
    return api.post<any, ApiResponse<WorkflowTemplate>>(`/workflows/${id}/clone`)
  },

  // 导出YAML
  exportYaml(id: string) {
    return api.get<any, ApiResponse<string>>(`/workflows/${id}/export`)
  }
}

// ============ 节点 API ============

export const nodeApi = {
  // 获取节点列表
  list(workflowId: string) {
    return api.get<any, ApiResponse<WorkflowNode[]>>(`/workflows/${workflowId}/nodes`)
  },

  // 创建节点
  create(workflowId: string, data: { type: string; name: string; positionX: number; positionY: number; config?: string }) {
    return api.post<any, ApiResponse<WorkflowNode>>(`/workflows/${workflowId}/nodes`, data)
  },

  // 更新节点
  update(workflowId: string, nodeId: string, data: { name?: string; positionX?: number; positionY?: number; config?: string }) {
    return api.put<any, ApiResponse<WorkflowNode>>(`/workflows/${workflowId}/nodes/${nodeId}`, data)
  },

  // 删除节点
  delete(workflowId: string, nodeId: string) {
    return api.delete<any, ApiResponse<void>>(`/workflows/${workflowId}/nodes/${nodeId}`)
  },

  // 更新节点位置
  updatePosition(workflowId: string, nodeId: string, x: number, y: number) {
    return api.patch<any, ApiResponse<void>>(`/workflows/${workflowId}/nodes/${nodeId}/position`, { x, y })
  }
}

// ============ 连线 API ============

export const edgeApi = {
  // 创建连线
  create(workflowId: string, data: { source: string; target: string; type: string }) {
    return api.post<any, ApiResponse<WorkflowEdge>>(`/workflows/${workflowId}/edges`, data)
  },

  // 删除连线
  delete(workflowId: string, edgeId: string) {
    return api.delete<any, ApiResponse<void>>(`/workflows/${workflowId}/edges/${edgeId}`)
  }
}

// ============ 执行 API ============

export const executionApi = {
  // 启动执行
  start(workflowId: string, inputData: Record<string, unknown>) {
    return api.post<any, ApiResponse<ExecutionRecord>>(`/workflows/${workflowId}/executions`, { inputData })
  },

  // 获取执行状态
  get(executionId: string) {
    return api.get<any, ApiResponse<ExecutionRecord>>(`/executions/${executionId}`)
  },

  // 暂停执行
  pause(executionId: string) {
    return api.post<any, ApiResponse<void>>(`/executions/${executionId}/pause`)
  },

  // 恢复执行
  resume(executionId: string) {
    return api.post<any, ApiResponse<void>>(`/executions/${executionId}/resume`)
  },

  // 停止执行
  stop(executionId: string) {
    return api.post<any, ApiResponse<void>>(`/executions/${executionId}/stop`)
  },

  // 获取执行记录列表
  listRecords(workflowId?: string, limit: number = 10) {
    const params = new URLSearchParams()
    if (workflowId) params.append('workflowId', workflowId)
    params.append('limit', String(limit))
    return api.get<any, ApiResponse<ExecutionRecord[]>>(`/executions/records?${params}`)
  },

  // 获取执行记录详情
  getRecord(executionId: string) {
    return api.get<any, ApiResponse<ExecutionRecord>>(`/executions/records/${executionId}`)
  }
}

// ============ 配置 API ============

export const configApi = {
  // 获取全局配置
  getGlobal() {
    return api.get<any, ApiResponse<GlobalConfig>>('/config/global')
  },

  // 保存全局配置
  saveGlobal(config: Partial<GlobalConfig>) {
    return api.post<any, ApiResponse<void>>('/config/global', config)
  },

  // 获取Agent列表
  getAgents() {
    return api.get<any, ApiResponse<any[]>>('/config/load-agents')
  }
}

// ============ 模板 API ============

export const templateApi = {
  // 获取模板列表
  list() {
    return api.get<any, ApiResponse<any[]>>('/templates')
  },

  // 从模板创建工作流
  createFromTemplate(templateId: string, name: string) {
    return api.post<any, ApiResponse<WorkflowTemplate>>(`/templates/${templateId}/create-workflow`, { name })
  }
}

// ============ 系统 API ============

export const systemApi = {
  // 浏览目录
  browseDirectory(path?: string) {
    return api.post<any, ApiResponse<{ path: string; directories: { name: string; path: string }[] }>>('/system/browse-directory', { path })
  }
}

// ============ AI API ============

export const aiApi = {
  // AI生成工作流
  generateWorkflow(description: string, name?: string) {
    return api.post<any, ApiResponse<WorkflowTemplate>>('/ai/generate-workflow', { description, name })
  }
}

// ============ 审核 API ============

export const reviewApi = {
  // 获取审核列表
  list(status?: string) {
    const params = status ? `?status=${status}` : ''
    return api.get<any, ApiResponse<any[]>>(`/reviews${params}`)
  },

  // 获取审核详情
  get(reviewId: string) {
    return api.get<any, ApiResponse<any>>(`/reviews/${reviewId}`)
  },

  // 提交审核决策
  submit(reviewId: string, decision: string, comment?: string) {
    return api.post<any, ApiResponse<void>>(`/reviews/${reviewId}/submit`, { decision, comment })
  }
}

export default api