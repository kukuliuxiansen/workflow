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
        <svg v-if="type === 'agent_execution'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <path d="M8 14s1.5 2 4 2 4-2 4-2"/>
          <line x1="9" y1="9" x2="9.01" y2="9"/>
          <line x1="15" y1="9" x2="15.01" y2="9"/>
        </svg>
        <svg v-else-if="type === 'api_call'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
          <polyline points="22,6 12,13 2,6"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
          <polyline points="22 4 12 14.01 9 11.01"/>
        </svg>
      </div>
      <span class="node-label">{{ data.label }}</span>
    </div>
    <div class="node-body" v-if="type === 'agent_execution' || type === 'api_call'">
      <div v-if="type === 'agent_execution' && data.config?.agentId" class="node-info">
        <span class="info-label">Agent</span>
        <span class="info-value">{{ data.config.agentId }}</span>
      </div>
      <div v-if="data.config?.timeout" class="node-info">
        <span class="info-label">超时</span>
        <span class="info-value">{{ data.config.timeout }}s</span>
      </div>
    </div>
    <!-- 连接点 -->
    <Handle type="target" :position="Position.Left" class="handle-target" />
    <Handle type="source" :position="Position.Right" class="handle-source" id="success" />
    <Handle type="source" :position="Position.Bottom" class="handle-fail" id="fail" />
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
  min-width: 200px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: var(--shadow-md);
  transition: all 0.2s ease;
  overflow: hidden;

  &.node-type-agent_execution {
    border-color: rgba(99, 102, 241, 0.5);

    .node-icon {
      background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
      box-shadow: 0 0 20px rgba(99, 102, 241, 0.4);
    }
  }

  &.node-type-api_call {
    border-color: rgba(16, 185, 129, 0.5);

    .node-icon {
      background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
      box-shadow: 0 0 20px rgba(16, 185, 129, 0.4);
    }
  }

  &.node-type-finish {
    border-color: rgba(245, 158, 11, 0.5);

    .node-icon {
      background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
      box-shadow: 0 0 20px rgba(245, 158, 11, 0.4);
    }
  }

  &.selected {
    box-shadow: 0 0 0 2px var(--accent-primary), var(--shadow-glow);
    transform: scale(1.02);
  }

  &:hover {
    box-shadow: var(--shadow-lg);
    transform: translateY(-2px);
  }
}

.node-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: var(--bg-hover);
  border-bottom: 1px solid var(--border-color);
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
    color: white;
  }
}

.node-label {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-body {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.node-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.info-label {
  color: var(--text-muted);
  font-weight: 500;
}

.info-value {
  color: var(--text-secondary);
  background: var(--bg-tertiary);
  padding: 2px 8px;
  border-radius: 4px;
  font-family: 'SF Mono', Monaco, monospace;
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
  border-color: #6366f1 !important;
  background: var(--bg-card) !important;
  left: -7px !important;

  &:hover {
    transform: scale(1.3);
    box-shadow: 0 0 10px rgba(99, 102, 241, 0.5);
  }
}

.handle-source {
  border-color: #10b981 !important;
  background: var(--bg-card) !important;
  right: -7px !important;

  &:hover {
    transform: scale(1.3);
    box-shadow: 0 0 10px rgba(16, 185, 129, 0.5);
  }
}

.handle-fail {
  border-color: #ef4444 !important;
  background: var(--bg-card) !important;
  bottom: -7px !important;

  &:hover {
    transform: scale(1.3);
    box-shadow: 0 0 10px rgba(239, 68, 68, 0.5);
  }
}
</style>