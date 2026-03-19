<template>
  <div
    class="log-panel"
    :class="{ expanded }"
    :style="{ height: expanded ? `${height}px` : '48px' }"
  >
    <div class="panel-header" @click="$emit('toggle')">
      <div class="header-left">
        <svg class="toggle-icon" :class="{ rotated: expanded }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="18 15 12 9 6 15"/>
        </svg>
        <span class="panel-title">执行日志</span>
        <span v-if="executionStore.currentExecution" class="status-badge" :class="executionStore.currentExecution.status.toLowerCase()">
          {{ statusLabel }}
        </span>
      </div>
      <div class="header-right">
        <button class="clear-btn" @click.stop="clearLogs">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"/>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
          </svg>
          清空
        </button>
      </div>
    </div>
    <div v-if="expanded" class="panel-body">
      <div v-if="executionStore.currentExecution" class="execution-info">
        <div class="info-item">
          <span class="info-label">执行ID</span>
          <span class="info-value">{{ executionStore.currentExecution.id }}</span>
        </div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: `${executionStore.currentExecution.progress}%` }"></div>
        </div>
        <span class="progress-text">{{ executionStore.currentExecution.progress }}%</span>
      </div>
      <div class="log-list" ref="logListRef">
        <div
          v-for="log in executionStore.logs"
          :key="log.id"
          class="log-item"
          :class="`log-${log.level}`"
        >
          <span class="log-time">{{ formatTime(log.timestamp) }}</span>
          <span class="log-level">{{ log.level.toUpperCase() }}</span>
          <span v-if="log.nodeId" class="log-node">[{{ log.nodeId }}]</span>
          <span class="log-message">{{ log.message }}</span>
        </div>
        <div v-if="executionStore.logs.length === 0" class="empty-logs">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
          </svg>
          <p>等待执行日志...</p>
        </div>
      </div>
    </div>
    <div v-if="expanded" class="resize-handle" @mousedown="startResize" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useExecutionStore } from '@/stores'
import type { ExecutionStatus } from '@/types'

interface Props {
  expanded: boolean
  height: number
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'toggle'): void
  (e: 'resize', height: number): void
}>()

const executionStore = useExecutionStore()
const logListRef = ref<HTMLElement | null>(null)
const isResizing = ref(false)

const statusLabels: Record<ExecutionStatus, string> = {
  PENDING: '待执行',
  RUNNING: '执行中',
  PAUSED: '已暂停',
  STOPPED: '已停止',
  COMPLETED: '已完成',
  FAILED: '失败'
}

const statusLabel = computed(() => {
  return statusLabels[executionStore.currentExecution?.status || 'PENDING']
})

function formatTime(timestamp: string) {
  return new Date(timestamp).toLocaleTimeString('zh-CN')
}

function clearLogs() {
  executionStore.clearLogs()
}

watch(() => executionStore.logs.length, () => {
  nextTick(() => {
    if (logListRef.value) {
      logListRef.value.scrollTop = logListRef.value.scrollHeight
    }
  })
})

function startResize(e: MouseEvent) {
  isResizing.value = true
  const startY = e.clientY
  const startHeight = props.height

  const onMouseMove = (e: MouseEvent) => {
    if (!isResizing.value) return
    const diff = startY - e.clientY
    emit('resize', Math.max(100, Math.min(500, startHeight + diff)))
  }

  const onMouseUp = () => {
    isResizing.value = false
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}
</script>

<style lang="scss" scoped>
.log-panel {
  background: var(--bg-secondary);
  border-top: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: height 0.3s ease;
  position: relative;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  cursor: pointer;
  user-select: none;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);

  &:hover {
    background: var(--bg-hover);
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toggle-icon {
  width: 18px;
  height: 18px;
  color: var(--text-muted);
  transition: transform 0.3s;

  &.rotated {
    transform: rotate(180deg);
  }
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 500;

  &.pending {
    background: rgba(59, 130, 246, 0.1);
    color: #60a5fa;
    border: 1px solid rgba(59, 130, 246, 0.3);
  }

  &.running {
    background: rgba(99, 102, 241, 0.1);
    color: #818cf8;
    border: 1px solid rgba(99, 102, 241, 0.3);
  }

  &.completed {
    background: rgba(16, 185, 129, 0.1);
    color: #34d399;
    border: 1px solid rgba(16, 185, 129, 0.3);
  }

  &.failed, &.stopped {
    background: rgba(239, 68, 68, 0.1);
    color: #f87171;
    border: 1px solid rgba(239, 68, 68, 0.3);
  }

  &.paused {
    background: rgba(245, 158, 11, 0.1);
    color: #fbbf24;
    border: 1px solid rgba(245, 158, 11, 0.3);
  }
}

.clear-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
    border-color: var(--border-light);
  }

  svg {
    width: 14px;
    height: 14px;
  }
}

.panel-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.execution-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-label {
  font-size: 12px;
  color: var(--text-muted);
}

.info-value {
  font-size: 12px;
  color: var(--text-primary);
  font-family: 'SF Mono', Monaco, monospace;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: var(--bg-tertiary);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--accent-gradient);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}

.log-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 20px;
  font-family: 'SF Mono', Monaco, 'Ubuntu Mono', monospace;
  font-size: 12px;
  background: #0d0d1a;
  color: var(--text-secondary);
}

.log-item {
  padding: 4px 0;
  display: flex;
  gap: 12px;
  line-height: 1.5;

  &.log-info {
    color: var(--text-secondary);
  }

  &.log-warn {
    color: #fbbf24;
  }

  &.log-error {
    color: #f87171;
  }

  &.log-debug {
    color: var(--text-muted);
  }
}

.log-time {
  color: #6b7280;
  flex-shrink: 0;
}

.log-level {
  color: #818cf8;
  min-width: 50px;
  flex-shrink: 0;
}

.log-node {
  color: #fbbf24;
  flex-shrink: 0;
}

.log-message {
  flex: 1;
  white-space: pre-wrap;
  word-break: break-all;
}

.empty-logs {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--text-muted);

  svg {
    width: 48px;
    height: 48px;
    margin-bottom: 12px;
    opacity: 0.5;
  }

  p {
    margin: 0;
    font-size: 13px;
  }
}

.resize-handle {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  cursor: ns-resize;
  background: transparent;

  &:hover {
    background: var(--accent-primary);
  }
}
</style>