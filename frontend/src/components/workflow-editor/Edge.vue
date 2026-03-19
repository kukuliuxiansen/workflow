<template>
  <g class="workflow-edge" :class="`edge-type-${type}`">
    <!-- 边路径 -->
    <BaseEdge
      :id="id"
      :path="path"
      :style="edgeStyle"
      :marker-end="markerEnd"
    />

    <!-- 边标签 -->
    <EdgeLabelRenderer>
      <div
        v-if="label"
        class="edge-label"
        :style="labelStyle"
        :class="type"
      >
        {{ label }}
      </div>

      <!-- 删除按钮 -->
      <button
        v-if="showDeleteBtn"
        class="edge-delete-btn"
        :style="deleteBtnStyle"
        @click="handleDelete"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="6" x2="6" y2="18"/>
          <line x1="6" y1="6" x2="18" y2="18"/>
        </svg>
      </button>

      <!-- 类型切换按钮 -->
      <button
        v-if="showTypeBtn"
        class="edge-type-btn"
        :style="typeBtnStyle"
        @click="toggleType"
        :title="type === 'success' ? '切换为失败连线' : '切换为成功连线'"
      >
        <svg v-if="type === 'success'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="17 1 21 5 17 9"/>
          <path d="M3 11V9a4 4 0 0 1 4-4h14"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="7 23 3 19 7 15"/>
          <path d="M21 13v2a4 4 0 0 1-4 4H3"/>
        </svg>
      </button>
    </EdgeLabelRenderer>
  </g>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { BaseEdge, getBezierPath, EdgeLabelRenderer, useVueFlow } from '@vue-flow/core'
import type { EdgeType } from '@/types'

interface Props {
  id: string
  sourceX: number
  sourceY: number
  targetX: number
  targetY: number
  sourcePosition: any
  targetPosition: any
  type: EdgeType
  label?: string
  data?: any
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'delete', id: string): void
  (e: 'toggle-type', id: string): void
}>()

const { getEdge } = useVueFlow()

// 控制按钮显示
const showDeleteBtn = ref(false)
const showTypeBtn = ref(false)

// 路径计算
const path = computed(() => {
  const [pathStr] = getBezierPath({
    sourceX: props.sourceX,
    sourceY: props.sourceY,
    targetX: props.targetX,
    targetY: props.targetY,
    sourcePosition: props.sourcePosition,
    targetPosition: props.targetPosition
  })
  return pathStr
})

// 边样式
const edgeStyle = computed(() => ({
  stroke: props.type === 'success' ? '#a6e3a1' : '#f38ba8',
  strokeWidth: 2
}))

// 箭头标记
const markerEnd = computed(() => {
  const color = props.type === 'success' ? '#a6e3a1' : '#f38ba8'
  return `url(#arrow-${color.replace('#', '')})`
})

// 标签位置
const labelStyle = computed(() => {
  const centerX = (props.sourceX + props.targetX) / 2
  const centerY = (props.sourceY + props.targetY) / 2
  return {
    transform: `translate(-50%, -50%) translate(${centerX}px, ${centerY}px)`
  }
})

// 删除按钮位置
const deleteBtnStyle = computed(() => {
  const centerX = (props.sourceX + props.targetX) / 2
  const centerY = (props.sourceY + props.targetY) / 2 - 20
  return {
    transform: `translate(-50%, -50%) translate(${centerX}px, ${centerY}px)`
  }
})

// 类型按钮位置
const typeBtnStyle = computed(() => {
  const centerX = (props.sourceX + props.targetX) / 2
  const centerY = (props.sourceY + props.targetY) / 2 + 20
  return {
    transform: `translate(-50%, -50%) translate(${centerX}px, ${centerY}px)`
  }
})

function handleDelete() {
  emit('delete', props.id)
}

function toggleType() {
  emit('toggle-type', props.id)
}

// 显示控制按钮（悬停时）
function showControls() {
  showDeleteBtn.value = true
  showTypeBtn.value = true
}

function hideControls() {
  showDeleteBtn.value = false
  showTypeBtn.value = false
}
</script>

<style lang="scss" scoped>
.workflow-edge {
  &:hover {
    :deep(.vue-flow__edge-path) {
      stroke-width: 3;
      filter: drop-shadow(0 0 4px currentColor);
    }

    .edge-delete-btn,
    .edge-type-btn {
      opacity: 1;
      pointer-events: all;
    }
  }
}

.edge-label {
  position: absolute;
  background: #1e1e2e;
  border: 1px solid #313244;
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  pointer-events: none;

  &.success {
    color: #a6e3a1;
    border-color: rgba(166, 227, 161, 0.3);
  }

  &.fail {
    color: #f38ba8;
    border-color: rgba(243, 139, 168, 0.3);
  }
}

.edge-delete-btn,
.edge-type-btn {
  position: absolute;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  background: #1e1e2e;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  pointer-events: none;
  transition: all 0.15s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);

  svg {
    width: 12px;
    height: 12px;
  }

  &:hover {
    transform: scale(1.1);
  }
}

.edge-delete-btn {
  color: #f38ba8;
  border: 1px solid rgba(243, 139, 168, 0.3);

  &:hover {
    background: rgba(243, 139, 168, 0.2);
  }
}

.edge-type-btn {
  color: #89b4fa;
  border: 1px solid rgba(137, 180, 250, 0.3);

  &:hover {
    background: rgba(137, 180, 250, 0.2);
  }
}

// 动画效果
:deep(.vue-flow__edge-path) {
  stroke-width: 2;
  transition: stroke-width 0.15s ease;
}

// 流动动画
.edge-type-success :deep(.vue-flow__edge-path) {
  stroke-dasharray: 5;
  animation: flowSuccess 0.5s linear infinite;
}

.edge-type-fail :deep(.vue-flow__edge-path) {
  stroke-dasharray: 5;
  animation: flowFail 0.5s linear infinite;
}

@keyframes flowSuccess {
  to {
    stroke-dashoffset: -10;
  }
}

@keyframes flowFail {
  to {
    stroke-dashoffset: 10;
  }
}
</style>