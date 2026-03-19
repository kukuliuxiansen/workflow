// 工作流模板
export interface WorkflowTemplate {
  id: string
  name: string
  description: string
  status: 'draft' | 'published'
  version: number
  globalConfig: GlobalConfig
  createdAt: string
  updatedAt: string
}

// 全局配置
export interface GlobalConfig {
  projectPath: string
  feishuOpenId: string
  maxGlobalLoop: number
  outputFormat?: string
  retryInterval?: number
  timeout?: number
}

// 节点类型
export type NodeType = 'agent_execution' | 'api_call' | 'finish'

// 节点配置
export interface NodeConfig {
  agentId?: string
  prompt?: string
  timeout?: number
  retryInterval?: number
  inputFields?: InputField[]
  outputFields?: OutputField[]
  apiConfig?: ApiCallConfig
}

// 输入字段
export interface InputField {
  name: string
  type: 'string' | 'number' | 'object' | 'array'
  required: boolean
  defaultValue?: string
  description?: string
}

// 输出字段
export interface OutputField {
  name: string
  type: 'string' | 'number' | 'object' | 'array'
  description?: string
}

// API调用配置
export interface ApiCallConfig {
  url: string
  method: 'GET' | 'POST' | 'PUT' | 'DELETE'
  headers?: Record<string, string>
  body?: string
  timeout?: number
}

// 工作流节点
export interface WorkflowNode {
  id: string
  workflowId: string
  type: NodeType
  name: string
  positionX: number
  positionY: number
  config: NodeConfig
  createdAt: string
  updatedAt: string
}

// 连线类型
export type EdgeType = 'success' | 'fail'

// 工作流连线
export interface WorkflowEdge {
  id: string
  workflowId: string
  sourceNodeId: string
  targetNodeId: string
  type: EdgeType
  createdAt: string
}

// 执行状态
export type ExecutionStatus = 'pending' | 'running' | 'paused' | 'stopped' | 'completed' | 'failed'

// 触发模式
export type TriggerMode = 'manual' | 'scheduled'

// 执行记录
export interface ExecutionRecord {
  id: string
  workflowId: string
  status: ExecutionStatus
  progress: number
  inputData: Record<string, unknown>
  triggerMode: TriggerMode
  startTime: string | null
  endTime: string | null
  currentNodeId: string | null
  errorInfo: ErrorInfo | null
  createdAt: string
}

// 错误信息
export interface ErrorInfo {
  code: string
  message: string
  stack?: string
}

// 审核状态
export type ReviewStatus = 'pending' | 'approved' | 'rejected' | 'timeout'

// 审核决策
export type ReviewDecision = 'approve' | 'reject'

// 审核记录
export interface ReviewRecord {
  id: string
  executionId: string
  workflowId: string
  nodeId: string
  status: ReviewStatus
  reviewerAgentId: string | null
  submitTime: string | null
  timeoutSeconds: number
  responseTime: string | null
  decision: ReviewDecision | null
  comment: string | null
}

// Agent信息
export interface Agent {
  id: string
  name: string
  description: string
  type: string
  capabilities: string[]
}

// 日志条目
export interface LogEntry {
  id: string
  executionId: string
  timestamp: string
  level: 'info' | 'warn' | 'error' | 'debug'
  nodeId: string | null
  message: string
  data?: Record<string, unknown>
}

// 分页请求
export interface PageRequest {
  page: number
  pageSize: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

// 分页响应
export interface PageResponse<T> {
  data: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

// API响应
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// Vue Flow 节点数据
export interface FlowNodeData {
  label: string
  type: NodeType
  config: NodeConfig
}

// Vue Flow 节点
export interface FlowNode {
  id: string
  type: string
  position: { x: number; y: number }
  data: FlowNodeData
}

// Vue Flow 连线
export interface FlowEdge {
  id: string
  source: string
  target: string
  type: string
  animated?: boolean
  style?: Record<string, string>
  label?: string
}