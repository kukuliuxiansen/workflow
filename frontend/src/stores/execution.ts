import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ExecutionRecord, LogEntry, ExecutionStatus } from '@/types'

export type WebSocketStatus = 'connecting' | 'connected' | 'disconnected' | 'error'

export const useExecutionStore = defineStore('execution', () => {
  // 当前执行记录
  const currentExecution = ref<ExecutionRecord | null>(null)

  // 执行日志
  const logs = ref<LogEntry[]>([])

  // WebSocket 连接状态
  const wsStatus = ref<WebSocketStatus>('disconnected')

  // WebSocket 实例
  let ws: WebSocket | null = null

  // 计算属性
  const isRunning = computed(() => currentExecution.value?.status === 'running')
  const isPaused = computed(() => currentExecution.value?.status === 'paused')
  const isCompleted = computed(() => {
    const status = currentExecution.value?.status
    return status === 'completed' || status === 'failed' || status === 'stopped'
  })

  // Actions
  function setCurrentExecution(execution: ExecutionRecord | null) {
    currentExecution.value = execution
    if (execution) {
      logs.value = []
    }
  }

  function updateExecutionStatus(status: ExecutionStatus, progress?: number) {
    if (currentExecution.value) {
      currentExecution.value.status = status
      if (progress !== undefined) {
        currentExecution.value.progress = progress
      }
    }
  }

  function addLog(log: LogEntry) {
    logs.value.push(log)
  }

  function clearLogs() {
    logs.value = []
  }

  function connectWebSocket(executionId: string) {
    if (ws) {
      ws.close()
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/ws/executions/${executionId}/logs`

    wsStatus.value = 'connecting'
    ws = new WebSocket(wsUrl)

    ws.onopen = () => {
      wsStatus.value = 'connected'
      console.log('WebSocket connected')
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'log') {
          addLog(data.data)
        } else if (data.type === 'status') {
          updateExecutionStatus(data.data.status, data.data.progress)
        }
      } catch (e) {
        console.error('Failed to parse WebSocket message:', e)
      }
    }

    ws.onerror = (error) => {
      wsStatus.value = 'error'
      console.error('WebSocket error:', error)
    }

    ws.onclose = () => {
      wsStatus.value = 'disconnected'
      console.log('WebSocket disconnected')
    }
  }

  function disconnectWebSocket() {
    if (ws) {
      ws.close()
      ws = null
    }
    wsStatus.value = 'disconnected'
  }

  function reset() {
    currentExecution.value = null
    logs.value = []
    disconnectWebSocket()
  }

  return {
    // State
    currentExecution,
    logs,
    wsStatus,
    // Computed
    isRunning,
    isPaused,
    isCompleted,
    // Actions
    setCurrentExecution,
    updateExecutionStatus,
    addLog,
    clearLogs,
    connectWebSocket,
    disconnectWebSocket,
    reset
  }
})