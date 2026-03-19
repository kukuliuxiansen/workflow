<template>
  <div class="workflow-list-page">
    <header class="page-header">
      <div class="header-content">
        <h1>工作流管理</h1>
        <p>创建和管理你的自动化工作流</p>
      </div>
      <button class="create-btn" @click="createWorkflow">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"/>
          <line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        <span>新建工作流</span>
      </button>
    </header>

    <main class="page-main">
      <div class="workflow-grid">
        <div
          v-for="workflow in workflows"
          :key="workflow.id"
          class="workflow-card"
          @click="editWorkflow(workflow.id)"
        >
          <div class="card-header">
            <div class="card-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <path d="M3 9h18M9 21V9"/>
              </svg>
            </div>
            <span class="status-badge" :class="workflow.status.toLowerCase()">
              {{ workflow.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </span>
          </div>
          <h3 class="card-title">{{ workflow.name }}</h3>
          <p class="card-desc">{{ workflow.description || '暂无描述' }}</p>
          <div class="card-footer">
            <span class="card-meta">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
              {{ formatDate(workflow.updatedAt) }}
            </span>
            <div class="card-actions" @click.stop>
              <button class="action-btn" @click="cloneWorkflow(workflow.id)" title="复制">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
                </svg>
              </button>
              <button class="action-btn delete" @click="deleteWorkflow(workflow.id)" title="删除">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!loading && workflows.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <path d="M3 9h18M9 21V9"/>
            </svg>
          </div>
          <h3>暂无工作流</h3>
          <p>点击上方按钮创建你的第一个工作流</p>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <!-- 分页 -->
      <div v-if="pagination.total > pagination.pageSize" class="pagination">
        <button
          class="page-btn"
          :disabled="pagination.page === 1"
          @click="pagination.page--; loadWorkflows()"
        >
          上一页
        </button>
        <span class="page-info">{{ pagination.page }} / {{ Math.ceil(pagination.total / pagination.pageSize) }}</span>
        <button
          class="page-btn"
          :disabled="pagination.page * pagination.pageSize >= pagination.total"
          @click="pagination.page++; loadWorkflows()"
        >
          下一页
        </button>
      </div>
    </main>

    <!-- 新建工作流对话框 -->
    <Teleport to="body">
      <div v-if="showCreateDialog" class="modal-overlay" @click.self="showCreateDialog = false">
        <div class="modal-content">
          <div class="modal-header">
            <h3>新建工作流</h3>
            <button class="btn-close" @click="showCreateDialog = false">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>工作流名称</label>
              <input
                v-model="newWorkflow.name"
                placeholder="输入工作流名称"
              />
            </div>
            <div class="form-group">
              <label>描述（可选）</label>
              <textarea
                v-model="newWorkflow.description"
                placeholder="描述这个工作流的用途..."
                rows="3"
              ></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" @click="showCreateDialog = false">取消</button>
            <button class="btn-primary" @click="submitCreate" :disabled="creating">
              {{ creating ? '创建中...' : '创建' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { workflowApi } from '@/api'
import type { WorkflowTemplate } from '@/types'

const router = useRouter()

const loading = ref(false)
const workflows = ref<WorkflowTemplate[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 12,
  total: 0
})

const showCreateDialog = ref(false)
const creating = ref(false)
const newWorkflow = reactive({
  name: '',
  description: ''
})

function formatDate(dateStr: string) {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

async function loadWorkflows() {
  loading.value = true
  try {
    const res = await workflowApi.list({
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    workflows.value = res.data
    pagination.total = res.total
  } catch (error) {
    console.error('Failed to load workflows:', error)
  } finally {
    loading.value = false
  }
}

function createWorkflow() {
  newWorkflow.name = ''
  newWorkflow.description = ''
  showCreateDialog.value = true
}

async function submitCreate() {
  if (!newWorkflow.name.trim()) return

  creating.value = true
  try {
    const res = await workflowApi.create({
      name: newWorkflow.name,
      description: newWorkflow.description,
      status: 'DRAFT',
      version: 1,
      globalConfig: {
        projectPath: '',
        feishuOpenId: '',
        maxGlobalLoop: 3
      }
    })
    showCreateDialog.value = false
    router.push(`/workflows/${res.data.id}`)
  } catch (error) {
    console.error('Failed to create workflow:', error)
  } finally {
    creating.value = false
  }
}

function editWorkflow(id: string) {
  router.push(`/workflows/${id}`)
}

async function cloneWorkflow(id: string) {
  try {
    await workflowApi.clone(id)
    loadWorkflows()
  } catch (error) {
    console.error('Failed to clone workflow:', error)
  }
}

async function deleteWorkflow(id: string) {
  try {
    await workflowApi.delete(id)
    loadWorkflows()
  } catch (error) {
    console.error('Failed to delete workflow:', error)
  }
}

onMounted(() => {
  loadWorkflows()
})
</script>

<style lang="scss" scoped>
.workflow-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-tertiary);
  overflow: hidden;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 32px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
}

.header-content {
  h1 {
    margin: 0;
    font-size: 24px;
    font-weight: 600;
    color: var(--text-primary);
  }

  p {
    margin: 4px 0 0;
    font-size: 14px;
    color: var(--text-muted);
  }
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: var(--accent-gradient);
  border: none;
  border-radius: 10px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: var(--shadow-glow);

  &:hover {
    transform: translateY(-2px);
    opacity: 0.9;
  }

  svg {
    width: 18px;
    height: 18px;
  }
}

.page-main {
  flex: 1;
  overflow: auto;
  padding: 24px;
}

.workflow-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.workflow-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    border-color: var(--accent-primary);
    transform: translateY(-4px);
    box-shadow: var(--shadow-lg);
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-icon {
  width: 40px;
  height: 40px;
  background: var(--accent-gradient);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 20px rgba(99, 102, 241, 0.3);

  svg {
    width: 20px;
    height: 20px;
    color: white;
  }
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 500;

  &.draft {
    background: rgba(251, 191, 36, 0.1);
    color: #fbbf24;
    border: 1px solid rgba(251, 191, 36, 0.3);
  }

  &.published {
    background: rgba(16, 185, 129, 0.1);
    color: #10b981;
    border: 1px solid rgba(16, 185, 129, 0.3);
  }
}

.card-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.card-desc {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);

  svg {
    width: 14px;
    height: 14px;
  }
}

.card-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-hover);
  color: var(--text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &:hover {
    color: var(--text-primary);
    border-color: var(--border-light);
  }

  &.delete:hover {
    color: #f87171;
    border-color: rgba(239, 68, 68, 0.5);
  }

  svg {
    width: 16px;
    height: 16px;
  }
}

.empty-state, .loading-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.empty-icon {
  width: 80px;
  height: 80px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;

  svg {
    width: 40px;
    height: 40px;
    color: var(--text-muted);
  }
}

.empty-state h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.empty-state p, .loading-state p {
  margin: 0;
  font-size: 14px;
  color: var(--text-muted);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top-color: var(--accent-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
  padding: 16px;
}

.page-btn {
  padding: 8px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    border-color: var(--accent-primary);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.page-info {
  font-size: 13px;
  color: var(--text-secondary);
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  width: 480px;
  max-width: 90%;
  box-shadow: var(--shadow-lg);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);

  h3 {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }
}

.btn-close {
  width: 32px;
  height: 32px;
  border-radius: 8px;
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
    width: 18px;
    height: 18px;
  }
}

.modal-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }

  label {
    display: block;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    margin-bottom: 8px;
  }

  input, textarea {
    width: 100%;
    background: var(--bg-card);
    border: 1px solid var(--border-color);
    border-radius: 10px;
    padding: 12px 16px;
    color: var(--text-primary);
    font-size: 14px;
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
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
}

.btn-secondary {
  padding: 10px 20px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--bg-hover);
    border-color: var(--border-light);
  }
}

.btn-primary {
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  background: var(--accent-gradient);
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    opacity: 0.9;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}
</style>