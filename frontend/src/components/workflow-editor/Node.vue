<template>
  <div
    class="workflow-node"
    :class="[
      `node-type-${type}`,
      { selected, disabled, running, completed, error }
    ]"
    :style="nodeStyle"
    @mouseenter="onMouseEnter"
    @mouseleave="onMouseLeave"
  >
    <!-- 节点头部 -->
    <div class="node-header">
      <div class="node-icon" :class="type" :style="iconStyle">
        <svg v-if="type === 'start'" viewBox="0 0 24 24" fill="currentColor">
          <circle cx="12" cy="12" r="10"/>
        </svg>
        <svg v-else-if="type === 'finish'" viewBox="0 0 24 24" fill="currentColor">
          <circle cx="12" cy="12" r="10"/>
          <rect x="8" y="8" width="8" height="8" fill="white"/>
        </svg>
        <svg v-else-if="type === 'agent_execution'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <path d="M8 14s1.5 2 4 2 4-2 4-2"/>
          <line x1="9" y1="9" x2="9.01" y2="9"/>
          <line x1="15" y1="9" x2="15.01" y2="9"/>
        </svg>
        <svg v-else-if="type === 'api_call'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
          <polyline points="22,6 12,13 2,6"/>
        </svg>
        <svg v-else-if="type === 'condition'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 2l10 10-10 10L2 12z"/>
        </svg>
        <svg v-else-if="type === 'human_review'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="7" r="4"/>
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
        </svg>
        <svg v-else-if="type === 'parallel'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="2" x2="12" y2="22"/>
          <line x1="6" y1="6" x2="6" y2="18"/>
          <line x1="18" y1="6" x2="18" y2="18"/>
        </svg>
        <svg v-else-if="type === 'loop'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="17 1 21 5 17 9"/>
          <path d="M3 11V9a4 4 0 0 1 4-4h14"/>
          <polyline points="7 23 3 19 7 15"/>
          <path d="M21 13v2a4 4 0 0 1-4 4H3"/>
        </svg>
        <svg v-else-if="type === 'wait'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <polyline points="12 6 12 12 16 14"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
        </svg>
      </div>
      <span class="node-label">{{ data.label }}</span>

      <!-- 状态指示器 -->
      <div v-if="running" class="status-indicator running">
        <span class="spinner"></span>
      </div>
      <div v-else-if="completed" class="status-indicator completed">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
      </div>
      <div v-else-if="error" class="status-indicator error">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
          <line x1="18" y1="6" x2="6" y2="18"/>
          <line x1="6" y1="6" x2="18" y2="18"/>
        </svg>
      </div>
    </div>

    <!-- 节点内容 -->
    <div v-if="showBody" class="node-body">
      <div v-if="description" class="node-description">
        {{ description }}
      </div>
      <div class="node-meta">
        <span v-if="data.config?.timeout" class="meta-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
          {{ data.config.timeout }}s
        </span>
      </div>
    </div>

    <!-- 折叠按钮 -->
    <button v-if="hasBody" class="collapse-btn" @click.stop="toggleCollapse">
      <svg :class="{ collapsed: isCollapsed }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="6 9 12 15 18 9"/>
      </svg>
    </button>

    <!-- 连接点 -->
    <Handle v-if="type !== 'start'" type="target" :position="Position.Left" class="handle-target" />
    <Handle v-if="type !== 'finish'" type="source" :position="Position.Right" class="handle-source" id="success" />
    <Handle v-if="type !== 'finish' && type !== 'start'" type="source" :position="Position.Bottom" class="handle-fail" id="fail" />
    <Handle v-if="type === 'condition'" type="source" :position="Position.Top" class="handle-condition" id="condition" />

    <!-- 工具栏 -->
    <Transition name="fade">
      <div v-if="showToolbar" class="node-toolbar">
        <button class="toolbar-btn" @click.stop="editNode" title="编辑">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
          </svg>
        </button>
        <button class="toolbar-btn" @click.stop="duplicateNode" title="复制">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="8" y="8" width="12" height="12" rx="2"/>
            <path d="M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2"/>
          </svg>
        </button>
        <button class="toolbar-btn danger" @click.stop="deleteNode" title="删除">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"/>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
          </svg>
        </button>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { Handle, Position } from '@vue-flow/core'
import { ref, computed, inject } from 'vue'
import type { NodeType, FlowNodeData, NodeConfig } from '@/types'

interface Props {
  id: string
  data: FlowNodeData
  selected?: boolean
  type: NodeType
  color?: string
  disabled?: boolean
  running?: boolean
  completed?: boolean
  error?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  selected: false,
  disabled: false,
  running: false,
  completed: false,
  error: false,
  color: ''
})

const emit = defineEmits<{
  (e: 'edit', id: string): void
  (e: 'duplicate', id: string): void
  (e: 'delete', id: string): void
}>()

// 获取节点配置
const config = computed(() => {
  if (!props.data.config) return null
  try {
    return typeof props.data.config === 'string'
      ? JSON.parse(props.data.config)
      : props.data.config
  } catch {
    return null
  }
})

const description = computed(() => config.value?.description || '')

// 节点样式
const nodeStyle = computed(() => {
  const style: Record<string, string> = {}
  if (props.color) {
    style['--node-color'] = props.color
    style['border-color'] = props.color
  }
  return style
})

const iconStyle = computed(() => {
  if (props.color) {
    return { background: `linear-gradient(135deg, ${props.color} 0%, ${adjustColor(props.color, 20)} 100%)` }
  }
  return {}
})

// 辅助函数：调整颜色亮度
function adjustColor(color: string, amount: number): string {
  const hex = color.replace('#', '')
  const num = parseInt(hex, 16)
  const r = Math.min(255, Math.max(0, (num >> 16) + amount))
  const g = Math.min(255, Math.max(0, ((num >> 8) & 0x00FF) + amount))
  const b = Math.min(255, Math.max(0, (num & 0x0000FF) + amount))
  return `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`
}

// 折叠状态
const isCollapsed = ref(false)
const hasBody = computed(() => {
  return props.type !== 'start' && props.type !== 'finish'
})
const showBody = computed(() => hasBody.value && !isCollapsed.value)

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}

// 工具栏
const showToolbar = ref(false)

function onMouseEnter() {
  showToolbar.value = true
}

function onMouseLeave() {
  showToolbar.value = false
}

function editNode() {
  emit('edit', props.id)
}

function duplicateNode() {
  emit('duplicate', props.id)
}

function deleteNode() {
  emit('delete', props.id)
}
</script>

<style lang="scss" scoped>
.workflow-node {
  min-width: 160px;
  max-width: 280px;
  background: #1e1e2e;
  border: 2px solid var(--node-color, #313244);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  transition: all 0.2s ease;
  position: relative;
  --node-color: #313244;

  // 节点类型默认颜色
  &.node-type-start {
    --node-color: #a6e3a1;
    .node-icon { background: linear-gradient(135deg, #a6e3a1 0%, #94e2d5 100%); }
  }

  &.node-type-finish {
    --node-color: #f9e2af;
    .node-icon { background: linear-gradient(135deg, #f9e2af 0%, #fab387 100%); }
  }

  &.node-type-agent_execution {
    --node-color: #cba6f7;
    .node-icon { background: linear-gradient(135deg, #cba6f7 0%, #f5c2e7 100%); }
  }

  &.node-type-api_call {
    --node-color: #89b4fa;
    .node-icon { background: linear-gradient(135deg, #89b4fa 0%, #74c7ec 100%); }
  }

  &.node-type-condition {
    --node-color: #f9e2af;
    .node-icon { background: linear-gradient(135deg, #f9e2af 0%, #f5a97f 100%); }
  }

  &.node-type-human_review {
    --node-color: #f38ba8;
    .node-icon { background: linear-gradient(135deg, #f38ba8 0%, #eba0ac 100%); }
  }

  &.node-type-parallel,
  &.node-type-loop,
  &.node-type-wait {
    --node-color: #89dceb;
    .node-icon { background: linear-gradient(135deg, #89dceb 0%, #94e2d5 100%); }
  }

  // 状态
  &.selected {
    box-shadow: 0 0 0 2px var(--node-color), 0 0 20px color-mix(in srgb, var(--node-color) 40%, transparent);
    transform: scale(1.02);
  }

  &.disabled {
    opacity: 0.5;
    pointer-events: none;
  }

  &.running {
    border-color: #89b4fa;
    animation: pulse 1.5s ease-in-out infinite;
  }

  &.completed {
    border-color: #a6e3a1;
  }

  &.error {
    border-color: #f38ba8;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4);
  }
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 2px #89b4fa, 0 0 10px rgba(137, 180, 250, 0.3);
  }
  50% {
    box-shadow: 0 0 0 4px #89b4fa, 0 0 20px rgba(137, 180, 250, 0.5);
  }
}

.node-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #181825;
  border-radius: 10px 10px 0 0;
  position: relative;
}

.node-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  svg {
    width: 18px;
    height: 18px;
    color: #1e1e2e;
  }
}

.node-label {
  flex: 1;
  font-weight: 600;
  font-size: 14px;
  color: #cdd6f4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-body {
  padding: 10px 16px;
  border-top: 1px solid #313244;
}

.node-description {
  font-size: 12px;
  color: #a6adc8;
  line-height: 1.4;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.node-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #6c7086;
  background: #313244;
  padding: 2px 8px;
  border-radius: 4px;

  svg {
    width: 12px;
    height: 12px;
  }
}

// 折叠按钮
.collapse-btn {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 24px;
  height: 24px;
  background: #313244;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.15s ease;

  svg {
    width: 14px;
    height: 14px;
    color: #6c7086;
    transition: transform 0.2s ease;

    &.collapsed {
      transform: rotate(-90deg);
    }
  }

  .workflow-node:hover & {
    opacity: 1;
  }

  &:hover {
    background: #45475a;
    svg { color: #cdd6f4; }
  }
}

// 状态指示器
.status-indicator {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: auto;

  &.running {
    background: rgba(137, 180, 250, 0.2);

    .spinner {
      width: 12px;
      height: 12px;
      border: 2px solid #89b4fa;
      border-top-color: transparent;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
  }

  &.completed {
    background: rgba(166, 227, 161, 0.2);
    svg {
      width: 12px;
      height: 12px;
      color: #a6e3a1;
    }
  }

  &.error {
    background: rgba(243, 139, 168, 0.2);
    svg {
      width: 12px;
      height: 12px;
      color: #f38ba8;
    }
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

// 连接点
.handle-target,
.handle-source,
.handle-fail,
.handle-condition {
  width: 14px !important;
  height: 14px !important;
  border: 2px solid !important;
  transition: all 0.2s;
}

.handle-target {
  border-color: #cba6f7 !important;
  background: #1e1e2e !important;
  left: -7px !important;

  &:hover {
    transform: scale(1.3);
    box-shadow: 0 0 10px rgba(203, 166, 247, 0.5);
  }
}

.handle-source {
  border-color: #a6e3a1 !important;
  background: #1e1e2e !important;
  right: -7px !important;

  &:hover {
    transform: scale(1.3);
    box-shadow: 0 0 10px rgba(166, 227, 161, 0.5);
  }
}

.handle-fail {
  border-color: #f38ba8 !important;
  background: #1e1e2e !important;
  bottom: -7px !important;

  &:hover {
    transform: scale(1.3);
    box-shadow: 0 0 10px rgba(243, 139, 168, 0.5);
  }
}

.handle-condition {
  border-color: #f9e2af !important;
  background: #1e1e2e !important;
  top: -7px !important;

  &:hover {
    transform: scale(1.3);
    box-shadow: 0 0 10px rgba(249, 226, 175, 0.5);
  }
}

// 节点工具栏
.node-toolbar {
  position: absolute;
  top: -36px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 4px;
  background: #1e1e2e;
  border: 1px solid #313244;
  border-radius: 8px;
  padding: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  z-index: 10;
}

.toolbar-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;

  svg {
    width: 14px;
    height: 14px;
    color: #6c7086;
  }

  &:hover {
    background: #313244;
    svg { color: #cdd6f4; }
  }

  &.danger:hover {
    background: rgba(243, 139, 168, 0.2);
    svg { color: #f38ba8; }
  }
}

// 动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>