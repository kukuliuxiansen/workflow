<template>
  <div class="right-panel" :class="{ expanded }">
    <div class="panel-header">
      <h3>属性配置</h3>
      <button class="toggle-btn" @click="$emit('toggle')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline :points="expanded ? '9 18 15 12 9 6' : '15 18 9 12 15 6'"/>
        </svg>
      </button>
    </div>
    <div class="panel-content" v-show="expanded">
      <template v-if="selectedNode">
        <div class="config-section">
          <div class="section-header">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <path d="M3 9h18"/>
            </svg>
            <span>基本信息</span>
          </div>
          <div class="form-group">
            <label>节点名称</label>
            <input
              v-model="nodeName"
              placeholder="输入节点名称"
              @change="handleNameChange"
            />
          </div>
          <div class="form-group">
            <label>节点类型</label>
            <div class="type-badge" :class="selectedNode.type">
              {{ getNodeTypeLabel(selectedNode.type) }}
            </div>
          </div>
        </div>

        <!-- Agent执行配置 -->
        <template v-if="selectedNode.type === 'agent_execution'">
          <div class="config-section">
            <div class="section-header">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <path d="M8 14s1.5 2 4 2 4-2 4-2"/>
                <line x1="9" y1="9" x2="9.01" y2="9"/>
                <line x1="15" y1="9" x2="15.01" y2="9"/>
              </svg>
              <span>Agent配置</span>
            </div>
            <div class="form-group">
              <label>选择Agent</label>
              <select v-model="agentId" @change="handleAgentChange">
                <option value="">请选择Agent</option>
                <option v-for="agent in agents" :key="agent.id" :value="agent.id">
                  {{ agent.name }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>提示词</label>
              <textarea
                v-model="prompt"
                placeholder="输入执行提示词..."
                rows="4"
                @change="handlePromptChange"
              ></textarea>
            </div>
          </div>
        </template>

        <!-- API调用配置 -->
        <template v-if="selectedNode.type === 'api_call'">
          <div class="config-section">
            <div class="section-header">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                <polyline points="22,6 12,13 2,6"/>
              </svg>
              <span>API配置</span>
            </div>
            <div class="form-group">
              <label>请求URL</label>
              <input
                v-model="apiUrl"
                placeholder="https://api.example.com"
                @change="handleApiConfigChange"
              />
            </div>
            <div class="form-group">
              <label>请求方法</label>
              <select v-model="apiMethod" @change="handleApiConfigChange">
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="DELETE">DELETE</option>
              </select>
            </div>
            <div class="form-group">
              <label>请求头 (JSON)</label>
              <textarea
                v-model="apiHeaders"
                placeholder='{"Content-Type": "application/json"}'
                rows="2"
                @change="handleApiConfigChange"
              ></textarea>
            </div>
            <div class="form-group">
              <label>请求体</label>
              <textarea
                v-model="apiBody"
                placeholder='{"key": "value"}'
                rows="4"
                @change="handleApiConfigChange"
              ></textarea>
            </div>
          </div>
        </template>

        <!-- 执行配置 -->
        <div class="config-section">
          <div class="section-header">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
            <span>执行配置</span>
          </div>
          <div class="form-row">
            <div class="form-group half">
              <label>超时时间</label>
              <div class="input-with-unit">
                <input type="number" v-model.number="timeout" min="1" max="3600" @change="handleTimeoutChange" />
                <span class="unit">秒</span>
              </div>
            </div>
            <div class="form-group half">
              <label>重试间隔</label>
              <div class="input-with-unit">
                <input type="number" v-model.number="retryInterval" min="1" max="600" @change="handleRetryIntervalChange" />
                <span class="unit">秒</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 删除按钮 -->
        <button class="delete-btn" @click="handleDeleteNode">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"/>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
          </svg>
          删除节点
        </button>
      </template>

      <template v-else>
        <div class="empty-state">
          <div class="empty-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <path d="M3 9h18M9 21V9"/>
            </svg>
          </div>
          <p>选择一个节点来编辑属性</p>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useWorkflowStore, useEditorStore } from '@/stores'
import { agentApi, nodeApi } from '@/api'
import type { Agent, NodeType } from '@/types'

interface Props {
  expanded: boolean
}

defineProps<Props>()
defineEmits<{
  (e: 'toggle'): void
}>()

const workflowStore = useWorkflowStore()
const editorStore = useEditorStore()

const agents = ref<Agent[]>([])

const selectedNode = computed(() => workflowStore.selectedNode)

const nodeName = ref('')
const agentId = ref('')
const prompt = ref('')
const timeout = ref(300)
const retryInterval = ref(60)

const apiUrl = ref('')
const apiMethod = ref<'GET' | 'POST' | 'PUT' | 'DELETE'>('POST')
const apiHeaders = ref('')
const apiBody = ref('')

watch(selectedNode, (node) => {
  if (node) {
    nodeName.value = node.name
    agentId.value = node.config?.agentId || ''
    prompt.value = node.config?.prompt || ''
    timeout.value = node.config?.timeout || 300
    retryInterval.value = node.config?.retryInterval || 60

    if (node.config?.apiConfig) {
      apiUrl.value = node.config.apiConfig.url || ''
      apiMethod.value = node.config.apiConfig.method || 'POST'
      apiHeaders.value = JSON.stringify(node.config.apiConfig.headers || {}, null, 2)
      apiBody.value = node.config.apiConfig.body || ''
    }
  }
}, { immediate: true })

async function loadAgents() {
  try {
    const res = await agentApi.list()
    agents.value = res.data
  } catch (error) {
    console.error('Failed to load agents:', error)
  }
}

function getNodeTypeLabel(type: NodeType): string {
  const labels: Record<NodeType, string> = {
    agent_execution: 'Agent执行',
    api_call: 'API调用',
    finish: '结束节点'
  }
  return labels[type]
}

function handleNameChange() {
  if (selectedNode.value) {
    workflowStore.updateNode(selectedNode.value.id, { name: nodeName.value })
    editorStore.markUnsaved()
  }
}

function handleAgentChange() {
  if (selectedNode.value) {
    workflowStore.updateNodeConfig(selectedNode.value.id, { agentId: agentId.value })
    editorStore.markUnsaved()
  }
}

function handlePromptChange() {
  if (selectedNode.value) {
    workflowStore.updateNodeConfig(selectedNode.value.id, { prompt: prompt.value })
    editorStore.markUnsaved()
  }
}

function handleTimeoutChange() {
  if (selectedNode.value) {
    workflowStore.updateNodeConfig(selectedNode.value.id, { timeout: timeout.value })
    editorStore.markUnsaved()
  }
}

function handleRetryIntervalChange() {
  if (selectedNode.value) {
    workflowStore.updateNodeConfig(selectedNode.value.id, { retryInterval: retryInterval.value })
    editorStore.markUnsaved()
  }
}

function handleApiConfigChange() {
  if (selectedNode.value) {
    try {
      let headers = {}
      if (apiHeaders.value) {
        headers = JSON.parse(apiHeaders.value)
      }
      workflowStore.updateNodeConfig(selectedNode.value.id, {
        apiConfig: {
          url: apiUrl.value,
          method: apiMethod.value,
          headers,
          body: apiBody.value,
          timeout: timeout.value
        }
      })
      editorStore.markUnsaved()
    } catch (e) {
      // JSON解析错误
    }
  }
}

async function handleDeleteNode() {
  if (!selectedNode.value) return

  try {
    await nodeApi.delete(workflowStore.workflowId, selectedNode.value.id)
    workflowStore.deleteNode(selectedNode.value.id)
    editorStore.markUnsaved()
  } catch (error) {
    console.error('Failed to delete node:', error)
  }
}

onMounted(() => {
  loadAgents()
})
</script>

<style lang="scss" scoped>
.right-panel {
  width: 300px;
  background: var(--bg-secondary);
  border-left: 1px solid var(--border-color);
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

  .right-panel:not(.expanded) & {
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

.config-section {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);

  svg {
    width: 16px;
    height: 16px;
    color: var(--accent-primary);
  }
}

.form-group {
  margin-bottom: 12px;

  label {
    display: block;
    font-size: 12px;
    font-weight: 500;
    color: var(--text-secondary);
    margin-bottom: 6px;
  }

  input, select, textarea {
    width: 100%;
    background: var(--bg-card);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    padding: 10px 12px;
    color: var(--text-primary);
    font-size: 13px;
    outline: none;
    transition: all 0.2s;

    &:focus {
      border-color: var(--accent-primary);
      box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
    }

    &::placeholder {
      color: var(--text-muted);
    }
  }

  textarea {
    resize: vertical;
    font-family: inherit;
  }

  select {
    cursor: pointer;
    appearance: none;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23a0a0b0' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 12px center;
    padding-right: 36px;
  }

  &.half {
    flex: 1;
  }
}

.form-row {
  display: flex;
  gap: 12px;
}

.input-with-unit {
  display: flex;
  align-items: center;
  gap: 8px;

  input {
    flex: 1;
  }

  .unit {
    font-size: 12px;
    color: var(--text-muted);
    white-space: nowrap;
  }
}

.type-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;

  &.agent_execution {
    background: rgba(99, 102, 241, 0.1);
    color: #818cf8;
    border: 1px solid rgba(99, 102, 241, 0.3);
  }

  &.api_call {
    background: rgba(16, 185, 129, 0.1);
    color: #34d399;
    border: 1px solid rgba(16, 185, 129, 0.3);
  }

  &.finish {
    background: rgba(245, 158, 11, 0.1);
    color: #fbbf24;
    border: 1px solid rgba(245, 158, 11, 0.3);
  }
}

.delete-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 8px;
  color: #f87171;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba(239, 68, 68, 0.2);
  }

  svg {
    width: 16px;
    height: 16px;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;

  svg {
    width: 32px;
    height: 32px;
    color: var(--text-muted);
  }
}

.empty-state p {
  font-size: 13px;
  color: var(--text-muted);
  text-align: center;
}
</style>