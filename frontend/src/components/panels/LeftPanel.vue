<template>
  <div class="left-panel" :class="{ expanded }">
    <div class="panel-header">
      <h3>节点组件</h3>
      <button class="toggle-btn" @click="$emit('toggle')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline :points="expanded ? '15 18 9 12 15 6' : '9 18 15 12 9 6'"/>
        </svg>
      </button>
    </div>
    <div class="panel-content" v-show="expanded">
      <div class="node-palette">
        <div
          class="palette-item"
          draggable="true"
          @dragstart="onDragStart($event, 'agent_execution')"
        >
          <div class="node-icon agent">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M8 14s1.5 2 4 2 4-2 4-2"/>
              <line x1="9" y1="9" x2="9.01" y2="9"/>
              <line x1="15" y1="9" x2="15.01" y2="9"/>
            </svg>
          </div>
          <div class="node-info">
            <span class="node-name">Agent 执行</span>
            <span class="node-desc">执行AI Agent任务</span>
          </div>
        </div>

        <div
          class="palette-item"
          draggable="true"
          @dragstart="onDragStart($event, 'api_call')"
        >
          <div class="node-icon api">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
              <polyline points="22,6 12,13 2,6"/>
            </svg>
          </div>
          <div class="node-info">
            <span class="node-name">API 调用</span>
            <span class="node-desc">调用外部API接口</span>
          </div>
        </div>

        <div
          class="palette-item"
          draggable="true"
          @dragstart="onDragStart($event, 'finish')"
        >
          <div class="node-icon finish">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
          <div class="node-info">
            <span class="node-name">结束节点</span>
            <span class="node-desc">工作流结束点</span>
          </div>
        </div>
      </div>

      <div class="help-section">
        <div class="help-title">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
            <line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
          <span>使用提示</span>
        </div>
        <ul class="help-list">
          <li>拖拽组件到画布创建节点</li>
          <li>从节点端口拖出连线</li>
          <li>点击节点编辑属性</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  expanded: boolean
}>()

defineEmits<{
  (e: 'toggle'): void
}>()

function onDragStart(event: DragEvent, nodeType: string) {
  if (event.dataTransfer) {
    event.dataTransfer.setData('nodeType', nodeType)
    event.dataTransfer.effectAllowed = 'copy'
  }
}
</script>

<style lang="scss" scoped>
.left-panel {
  width: 260px;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;

  &:not(.expanded) {
    width: 48px;
  }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--border-color);

  h3 {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .left-panel:not(.expanded) & {
    justify-content: center;
    padding: 12px;

    h3 {
      display: none;
    }
  }
}

.toggle-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
  }

  svg {
    width: 16px;
    height: 16px;
  }
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.node-palette {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  cursor: grab;
  transition: all 0.2s;

  &:hover {
    background: var(--bg-hover);
    border-color: var(--accent-primary);
    transform: translateX(4px);
  }

  &:active {
    cursor: grabbing;
  }
}

.node-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.agent {
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
    box-shadow: 0 0 20px rgba(99, 102, 241, 0.3);
  }

  &.api {
    background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
    box-shadow: 0 0 20px rgba(16, 185, 129, 0.3);
  }

  &.finish {
    background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
    box-shadow: 0 0 20px rgba(245, 158, 11, 0.3);
  }

  svg {
    width: 20px;
    height: 20px;
    color: white;
  }
}

.node-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.node-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.node-desc {
  font-size: 12px;
  color: var(--text-muted);
}

.help-section {
  margin-top: 24px;
  padding: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 10px;
}

.help-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);

  svg {
    width: 16px;
    height: 16px;
    color: var(--accent-primary);
  }
}

.help-list {
  list-style: none;
  padding: 0;

  li {
    font-size: 12px;
    color: var(--text-muted);
    padding: 4px 0;
    padding-left: 16px;
    position: relative;

    &::before {
      content: '•';
      position: absolute;
      left: 0;
      color: var(--accent-primary);
    }
  }
}
</style>