<template>
  <div class="left-panel" :class="{ expanded }">
    <div class="panel-header">
      <h3 v-if="expanded">节点组件</h3>
      <button class="toggle-btn" @click="$emit('toggle')" :title="expanded ? '收起面板' : '展开面板'">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline :points="expanded ? '15 18 9 12 15 6' : '9 18 15 12 9 6'"/>
        </svg>
      </button>
    </div>
    <div class="panel-content" v-show="expanded">
      <!-- 基础节点 -->
      <div class="node-section">
        <div class="section-title">基础节点</div>
        <div class="node-palette">
          <div
            class="palette-item"
            draggable="true"
            @dragstart="onDragStart($event, 'start')"
          >
            <div class="node-icon start">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <circle cx="12" cy="12" r="10"/>
              </svg>
            </div>
            <div class="node-info">
              <span class="node-name">开始节点</span>
              <span class="node-desc">工作流起始点</span>
            </div>
          </div>

          <div
            class="palette-item"
            draggable="true"
            @dragstart="onDragStart($event, 'finish')"
          >
            <div class="node-icon finish">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <circle cx="12" cy="12" r="10"/>
                <rect x="8" y="8" width="8" height="8" fill="white"/>
              </svg>
            </div>
            <div class="node-info">
              <span class="node-name">结束节点</span>
              <span class="node-desc">工作流结束点</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 执行节点 -->
      <div class="node-section">
        <div class="section-title">执行节点</div>
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
            @dragstart="onDragStart($event, 'human_review')"
          >
            <div class="node-icon review">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="7" r="4"/>
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              </svg>
            </div>
            <div class="node-info">
              <span class="node-name">人工审核</span>
              <span class="node-desc">等待人工确认</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 控制节点 -->
      <div class="node-section">
        <div class="section-title">控制节点</div>
        <div class="node-palette">
          <div
            class="palette-item"
            draggable="true"
            @dragstart="onDragStart($event, 'condition')"
          >
            <div class="node-icon condition">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2l10 10-10 10L2 12z"/>
              </svg>
            </div>
            <div class="node-info">
              <span class="node-name">条件判断</span>
              <span class="node-desc">根据条件分支</span>
            </div>
          </div>

          <div
            class="palette-item"
            draggable="true"
            @dragstart="onDragStart($event, 'parallel')"
          >
            <div class="node-icon parallel">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="2" x2="12" y2="22"/>
                <line x1="6" y1="6" x2="6" y2="18"/>
                <line x1="18" y1="6" x2="18" y2="18"/>
              </svg>
            </div>
            <div class="node-info">
              <span class="node-name">并行执行</span>
              <span class="node-desc">多分支并行处理</span>
            </div>
          </div>

          <div
            class="palette-item"
            draggable="true"
            @dragstart="onDragStart($event, 'loop')"
          >
            <div class="node-icon loop">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="17 1 21 5 17 9"/>
                <path d="M3 11V9a4 4 0 0 1 4-4h14"/>
                <polyline points="7 23 3 19 7 15"/>
                <path d="M21 13v2a4 4 0 0 1-4 4H3"/>
              </svg>
            </div>
            <div class="node-info">
              <span class="node-name">循环</span>
              <span class="node-desc">重复执行任务</span>
            </div>
          </div>

          <div
            class="palette-item"
            draggable="true"
            @dragstart="onDragStart($event, 'wait')"
          >
            <div class="node-icon wait">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
            </div>
            <div class="node-info">
              <span class="node-name">等待</span>
              <span class="node-desc">延时或等待条件</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 快捷键提示 -->
      <div class="help-section">
        <div class="help-title">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="4" width="20" height="16" rx="2"/>
            <path d="M6 8h.001M10 8h.001M14 8h.001M18 8h.001M8 12h8M6 16h.001M18 16h.001"/>
          </svg>
          <span>快捷操作</span>
        </div>
        <div class="shortcut-grid">
          <div class="shortcut-item">
            <kbd>Ctrl+C</kbd>
            <span>复制</span>
          </div>
          <div class="shortcut-item">
            <kbd>Ctrl+V</kbd>
            <span>粘贴</span>
          </div>
          <div class="shortcut-item">
            <kbd>Ctrl+Z</kbd>
            <span>撤销</span>
          </div>
          <div class="shortcut-item">
            <kbd>Delete</kbd>
            <span>删除</span>
          </div>
          <div class="shortcut-item">
            <kbd>Ctrl+F</kbd>
            <span>搜索</span>
          </div>
          <div class="shortcut-item">
            <kbd>?</kbd>
            <span>帮助</span>
          </div>
        </div>
      </div>

      <!-- 使用提示 -->
      <div class="tips-section">
        <div class="tips-title">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
            <line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
          <span>使用提示</span>
        </div>
        <ul class="tips-list">
          <li>拖拽组件到画布创建节点</li>
          <li>从节点端口拖出连线</li>
          <li>点击节点编辑属性</li>
          <li>右键打开更多操作</li>
          <li>框选可批量操作节点</li>
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
  width: 280px;
  background: #11111b;
  border-right: 1px solid #313244;
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
  border-bottom: 1px solid #313244;

  h3 {
    font-size: 14px;
    font-weight: 600;
    color: #cdd6f4;
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
  color: #6c7086;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &:hover {
    background: #313244;
    color: #cdd6f4;
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

.node-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 11px;
  font-weight: 600;
  color: #6c7086;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 10px;
  padding-left: 4px;
}

.node-palette {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: #1e1e2e;
  border: 1px solid #313244;
  border-radius: 10px;
  cursor: grab;
  transition: all 0.2s;

  &:hover {
    background: #313244;
    border-color: #45475a;
    transform: translateX(4px);
  }

  &:active {
    cursor: grabbing;
    transform: scale(0.98);
  }
}

.node-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.start {
    background: linear-gradient(135deg, #a6e3a1 0%, #94e2d5 100%);
  }

  &.finish {
    background: linear-gradient(135deg, #f9e2af 0%, #fab387 100%);
  }

  &.agent {
    background: linear-gradient(135deg, #cba6f7 0%, #f5c2e7 100%);
  }

  &.api {
    background: linear-gradient(135deg, #89b4fa 0%, #74c7ec 100%);
  }

  &.review {
    background: linear-gradient(135deg, #f38ba8 0%, #eba0ac 100%);
  }

  &.condition {
    background: linear-gradient(135deg, #f9e2af 0%, #f5a97f 100%);
  }

  &.parallel, &.loop, &.wait {
    background: linear-gradient(135deg, #89dceb 0%, #94e2d5 100%);
  }

  svg {
    width: 18px;
    height: 18px;
    color: #1e1e2e;
  }
}

.node-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.node-name {
  font-size: 13px;
  font-weight: 500;
  color: #cdd6f4;
}

.node-desc {
  font-size: 11px;
  color: #6c7086;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.help-section,
.tips-section {
  margin-top: 16px;
  padding: 12px;
  background: #1e1e2e;
  border: 1px solid #313244;
  border-radius: 10px;
}

.help-title,
.tips-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 12px;
  font-weight: 500;
  color: #a6adc8;

  svg {
    width: 14px;
    height: 14px;
    color: #cba6f7;
  }
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #6c7086;

  kbd {
    background: #11111b;
    border: 1px solid #45475a;
    border-radius: 4px;
    padding: 2px 6px;
    font-family: inherit;
    font-size: 10px;
    color: #cdd6f4;
  }
}

.tips-list {
  list-style: none;
  padding: 0;
  margin: 0;

  li {
    font-size: 11px;
    color: #6c7086;
    padding: 4px 0;
    padding-left: 12px;
    position: relative;

    &::before {
      content: '•';
      position: absolute;
      left: 0;
      color: #cba6f7;
    }
  }
}
</style>