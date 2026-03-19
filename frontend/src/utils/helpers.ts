import { v4 as uuidv4 } from 'uuid'

/**
 * 生成工作流ID
 */
export function generateWorkflowId(): string {
  const timestamp = Date.now()
  return `wf_${timestamp}`
}

/**
 * 生成节点ID
 */
export function generateNodeId(): string {
  const timestamp = Date.now()
  const random = Math.random().toString(36).substring(2, 9)
  return `node-${timestamp}-${random}`
}

/**
 * 生成连线ID
 */
export function generateEdgeId(): string {
  const timestamp = Date.now()
  const random = Math.random().toString(36).substring(2, 9)
  return `edge-${timestamp}-${random}`
}

/**
 * 生成执行ID
 */
export function generateExecutionId(): string {
  const timestamp = Date.now()
  return `exec_${timestamp}`
}

/**
 * 生成审核ID
 */
export function generateReviewId(): string {
  const timestamp = Date.now()
  return `review_${timestamp}`
}

/**
 * 生成日志ID
 */
export function generateLogId(): string {
  return uuidv4()
}

/**
 * 格式化日期时间
 */
export function formatDateTime(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date
  return d.toISOString().slice(0, 19).replace('T', ' ')
}

/**
 * 解析日期时间
 */
export function parseDateTime(dateStr: string): Date {
  return new Date(dateStr)
}

/**
 * 延迟执行
 */
export function delay(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * 深拷贝
 */
export function deepClone<T>(obj: T): T {
  return JSON.parse(JSON.stringify(obj))
}

/**
 * 防抖函数
 */
export function debounce<T extends (...args: unknown[]) => unknown>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timer: ReturnType<typeof setTimeout> | null = null
  return function (this: unknown, ...args: Parameters<T>) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn.apply(this, args), delay)
  }
}

/**
 * 节流函数
 */
export function throttle<T extends (...args: unknown[]) => unknown>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let lastTime = 0
  return function (this: unknown, ...args: Parameters<T>) {
    const now = Date.now()
    if (now - lastTime >= delay) {
      lastTime = now
      fn.apply(this, args)
    }
  }
}