<template>
  <div
    class="workflow-node"
    :class="[
      `node-type-${type}`,
      { selected }
    ]"
  >
    <div class="node-header">
      <div class="node-icon" :class="type">
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
    </div>

    <!-- 连接点 -->
    <Handle v-if="type !== 'start'" type="target" :position="Position.Left" class="handle-target" />
    <Handle v-if="type !== 'finish'" type="source" :position="Position.Right" class="handle-source" id="success" />
    <Handle v-if="type !== 'finish' && type !== 'start'" type="source" :position="Position.Bottom" class="handle-fail" id="fail" />
  </div>
</template>

<script setup lang="ts">
import { Handle, Position } from '@vue-flow/core'
import type { NodeType, FlowNodeData } from '@/types'

interface Props {
  id: string
  data: FlowNodeData
  selected?: boolean
  type: NodeType
}

defineProps<Props>()
</script>

<style lang="scss" scoped>
.workflow-node {
  min-width: 160px;
  background: #1e1e2e;
  border: 2px solid #313244;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  transition: all 0.2s ease;

  &.node-type-start {
    border-color: #a6e3a1;
    .node-icon { background: linear-gradient(135deg, #a6e3a1 0%, #94e2d5 100%); }
  }

  &.node-type-finish {
    border-color: #f9e2af;
    .node-icon { background: linear-gradient(135deg, #f9e2af 0%, #fab387 100%); }
  }

  &.node-type-agent_execution {
    border-color: #cba6f7;
    .node-icon { background: linear-gradient(135deg, #cba6f7 0%, #f5c2e7 100%); }
  }

  &.node-type-api_call {
    border-color: #89b4fa;
    .node-icon { background: linear-gradient(135deg, #89b4fa 0%, #74c7ec 100%); }
  }

  &.node-type-condition {
    border-color: #f9e2af;
    .node-icon { background: linear-gradient(135deg, #f9e2af 0%, #f5a97f 100%); }
  }

  &.node-type-human_review {
    border-color: #f38ba8;
    .node-icon { background: linear-gradient(135deg, #f38ba8 0%, #eba0ac 100%); }
  }

  &.node-type-parallel,
  &.node-type-loop,
  &.node-type-wait {
    border-color: #89dceb;
    .node-icon { background: linear-gradient(135deg, #89dceb 0%, #94e2d5 100%); }
  }

  &.selected {
    box-shadow: 0 0 0 2px #cba6f7, 0 0 20px rgba(203, 166, 247, 0.4);
    transform: scale(1.02);
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4);
  }
}

.node-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #181825;
  border-radius: 10px 10px 0 0;
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
  font-weight: 600;
  font-size: 14px;
  color: #cdd6f4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.handle-target,
.handle-source,
.handle-fail {
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
</style>