// 工作流模板
export interface WorkflowTemplate {
  id: string
  name: string
  description: string
  status: 'draft' | 'published'
  version: string
  folder_id: string | null
  global_config: string | null
  created_at: string
  updated_at: string
}

// 全局配置
export interface GlobalConfig {
  projectPath?: string
  feishuOpenId?: string
  maxGlobalLoop?: number
  maxRetries?: number
  maxGlobalRetries?: number
  outputFormat?: string
  retryInterval?: number
  timeout?: number
}

// 节点类型
export type NodeType = 'start' | 'finish' | 'agent_execution' | 'api_call' | 'condition' | 'parallel' | 'loop' | 'wait' | 'subworkflow' | 'human_review'

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
  workflow_id: string
  type: NodeType
  name: string
  description?: string
  position_x: number
  position_y: number
  config: string | null
  created_at: string
  updated_at: string
}

// 连线类型
export type EdgeType = 'success' | 'fail'

// 工作流连线
export interface WorkflowEdge {
  id: string
  workflow_id: string
  source_node_id: string
  target_node_id: string
  edge_type: EdgeType
  condition_expr?: string
  label?: string
  created_at: string
}

// 执行状态
export type ExecutionStatus = 'pending' | 'running' | 'paused' | 'stopped' | 'completed' | 'failed'

// 日志条目
export interface LogEntry {
  timestamp: string
  level: 'info' | 'warn' | 'error' | 'debug'
  message: string
  nodeId?: string
  nodeName?: string
}

// 执行记录
export interface ExecutionRecord {
  executionId: string
  workflowId: string
  status: ExecutionStatus
  startTime: string | null
  endTime: string | null
  createdAt: string
  currentNodeId: string | null
  previousNodeId: string | null
  nodeRetryCount?: number
  globalRetryCount?: number
}

// API响应
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  success: boolean
}

// Vue Flow 节点数据
export interface FlowNodeData {
  label: string
  type: NodeType
  config: string | null
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

// 文件夹
export interface Folder {
  id: string
  name: string
  parent_id: string | null
  sort_order: number
  created_at: string
}

// 模板
export interface Template {
  id: string
  name: string
  description: string
  nodes?: string
  edges?: string
  builtin: boolean
  workflowId?: string
}