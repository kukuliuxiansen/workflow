import yaml from 'js-yaml'
import type { WorkflowTemplate, WorkflowNode, WorkflowEdge, GlobalConfig } from '@/types'

interface YamlWorkflow {
  workflow: {
    id: string
    name: string
    version: string
    description: string
  }
  globals: {
    project_path: string
    feishu_open_id: string
    max_global_loop: number
    timeout?: number
    retry_interval?: number
    output_format?: string
  }
  nodes: YamlNode[]
}

interface YamlNode {
  id: string
  type: string
  name: string
  description?: string
  agent_id?: string
  prompt?: string
  timeout?: number
  retry_interval?: number
  on_success?: string | null
  on_fail?: string | null
  input?: YamlInputField[]
  output?: YamlOutputField[]
  api?: YamlApiConfig
}

interface YamlInputField {
  name: string
  type: string
  required?: boolean
  default?: string
  description?: string
}

interface YamlOutputField {
  name: string
  type: string
  description?: string
}

interface YamlApiConfig {
  url: string
  method: string
  headers?: Record<string, string>
  body?: string
  timeout?: number
}

/**
 * 根据节点ID和连线列表获取成功目标
 */
function getSuccessTarget(nodeId: string, edges: WorkflowEdge[]): string | null {
  const edge = edges.find(e => e.sourceNodeId === nodeId && e.type === 'success')
  return edge?.targetNodeId || null
}

/**
 * 根据节点ID和连线列表获取失败目标
 */
function getFailTarget(nodeId: string, edges: WorkflowEdge[]): string | null {
  const edge = edges.find(e => e.sourceNodeId === nodeId && e.type === 'fail')
  return edge?.targetNodeId || null
}

/**
 * 转换节点为YAML格式
 */
function convertNodeToYaml(
  node: WorkflowNode,
  edges: WorkflowEdge[]
): YamlNode {
  const yamlNode: YamlNode = {
    id: node.id,
    type: node.type,
    name: node.name,
    timeout: node.config.timeout,
    retry_interval: node.config.retryInterval,
    on_success: getSuccessTarget(node.id, edges),
    on_fail: getFailTarget(node.id, edges)
  }

  // Agent执行节点
  if (node.type === 'agent_execution') {
    yamlNode.agent_id = node.config.agentId
    yamlNode.prompt = node.config.prompt
    yamlNode.description = node.config.prompt
  }

  // API调用节点
  if (node.type === 'api_call' && node.config.apiConfig) {
    yamlNode.api = {
      url: node.config.apiConfig.url,
      method: node.config.apiConfig.method,
      headers: node.config.apiConfig.headers,
      body: node.config.apiConfig.body,
      timeout: node.config.apiConfig.timeout
    }
  }

  // 输入字段
  if (node.config.inputFields && node.config.inputFields.length > 0) {
    yamlNode.input = node.config.inputFields.map(field => ({
      name: field.name,
      type: field.type,
      required: field.required,
      default: field.defaultValue,
      description: field.description
    }))
  }

  // 输出字段
  if (node.config.outputFields && node.config.outputFields.length > 0) {
    yamlNode.output = node.config.outputFields.map(field => ({
      name: field.name,
      type: field.type,
      description: field.description
    }))
  }

  return yamlNode
}

/**
 * 转换全局配置为YAML格式
 */
function convertGlobalConfigToYaml(config: GlobalConfig): YamlWorkflow['globals'] {
  return {
    project_path: config.projectPath,
    feishu_open_id: config.feishuOpenId,
    max_global_loop: config.maxGlobalLoop,
    timeout: config.timeout,
    retry_interval: config.retryInterval,
    output_format: config.outputFormat
  }
}

/**
 * 生成YAML工作流配置
 */
export function generateYaml(
  workflow: Pick<WorkflowTemplate, 'id' | 'name' | 'description' | 'version'> & {
    globalConfig: GlobalConfig
    nodes: WorkflowNode[]
    edges: WorkflowEdge[]
  }
): string {
  const yamlWorkflow: YamlWorkflow = {
    workflow: {
      id: workflow.id,
      name: workflow.name,
      version: `1.${workflow.version}`,
      description: workflow.description
    },
    globals: convertGlobalConfigToYaml(workflow.globalConfig),
    nodes: workflow.nodes.map(node => convertNodeToYaml(node, workflow.edges))
  }

  return yaml.dump(yamlWorkflow, {
    indent: 2,
    lineWidth: -1,
    noRefs: true,
    sortKeys: false
  })
}

/**
 * 解析YAML工作流配置
 */
export function parseYaml(yamlContent: string): YamlWorkflow {
  return yaml.load(yamlContent) as YamlWorkflow
}

/**
 * 验证工作流配置
 */
export function validateWorkflow(
  nodes: WorkflowNode[],
  edges: WorkflowEdge[]
): { valid: boolean; errors: string[] } {
  const errors: string[] = []

  // 检查是否有节点
  if (nodes.length === 0) {
    errors.push('工作流必须至少包含一个节点')
  }

  // 检查是否有结束节点
  const hasFinishNode = nodes.some(n => n.type === 'finish')
  if (!hasFinishNode) {
    errors.push('工作流必须包含一个结束节点')
  }

  // 检查节点连线
  for (const node of nodes) {
    if (node.type === 'finish') continue

    const outgoingEdges = edges.filter(e => e.sourceNodeId === node.id)
    if (outgoingEdges.length === 0) {
      errors.push(`节点 "${node.name}" 没有配置任何连线`)
    }
  }

  // 检查Agent执行节点是否选择了Agent
  for (const node of nodes) {
    if (node.type === 'agent_execution' && !node.config.agentId) {
      errors.push(`节点 "${node.name}" 未选择Agent`)
    }
  }

  // 检查API调用节点是否配置了URL
  for (const node of nodes) {
    if (node.type === 'api_call') {
      if (!node.config.apiConfig?.url) {
        errors.push(`节点 "${node.name}" 未配置API URL`)
      }
    }
  }

  return {
    valid: errors.length === 0,
    errors
  }
}

export type { YamlWorkflow, YamlNode }