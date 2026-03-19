<template>
  <div class="execution-list-page">
    <el-header class="page-header">
      <h1>执行记录</h1>
      <el-button type="primary" @click="loadExecutions">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </el-header>

    <el-main class="page-main">
      <el-table :data="executions" v-loading="loading" stripe>
        <el-table-column prop="id" label="执行ID" width="180" />
        <el-table-column prop="workflowId" label="工作流ID" width="180">
          <template #default="{ row }">
            <router-link :to="`/workflows/${row.workflowId}`" class="workflow-link">
              {{ row.workflowId }}
            </router-link>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" :status="row.progress === 100 ? 'success' : ''" />
          </template>
        </el-table-column>
        <el-table-column prop="triggerMode" label="触发方式" width="100">
          <template #default="{ row }">
            {{ row.triggerMode === 'manual' ? '手动' : '定时' }}
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template #default="{ row }">
            {{ row.startTime ? formatDate(row.startTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="180">
          <template #default="{ row }">
            {{ row.endTime ? formatDate(row.endTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button
                size="small"
                @click="viewLogs(row.id)"
                :disabled="row.status === 'pending'"
              >
                日志
              </el-button>
              <el-button
                size="small"
                @click="pauseExecution(row.id)"
                :disabled="row.status !== 'running'"
              >
                暂停
              </el-button>
              <el-button
                size="small"
                type="danger"
                @click="stopExecution(row.id)"
                :disabled="row.status !== 'running' && row.status !== 'paused'"
              >
                停止
              </el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadExecutions"
        @current-change="loadExecutions"
        class="pagination"
      />
    </el-main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { executionApi } from '@/api'
import type { ExecutionRecord, ExecutionStatus } from '@/types'

const router = useRouter()

const loading = ref(false)
const executions = ref<ExecutionRecord[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const statusMap: Record<ExecutionStatus, { label: string; type: string }> = {
  pending: { label: '待执行', type: 'info' },
  running: { label: '执行中', type: 'primary' },
  paused: { label: '已暂停', type: 'warning' },
  stopped: { label: '已停止', type: 'danger' },
  completed: { label: '已完成', type: 'success' },
  failed: { label: '失败', type: 'danger' }
}

function getStatusLabel(status: ExecutionStatus) {
  return statusMap[status]?.label || status
}

function getStatusType(status: ExecutionStatus) {
  return statusMap[status]?.type || 'info'
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function loadExecutions() {
  loading.value = true
  try {
    const res = await executionApi.list({
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    executions.value = res.data
    pagination.total = res.total
  } catch (error) {
    console.error('Failed to load executions:', error)
    ElMessage.error('加载执行记录失败')
  } finally {
    loading.value = false
  }
}

function viewLogs(executionId: string) {
  router.push(`/executions/${executionId}`)
}

async function pauseExecution(executionId: string) {
  try {
    await ElMessageBox.confirm('确定要暂停此执行吗？', '确认暂停', {
      type: 'warning'
    })
    await executionApi.pause(executionId)
    ElMessage.success('已暂停')
    loadExecutions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to pause execution:', error)
      ElMessage.error('暂停失败')
    }
  }
}

async function stopExecution(executionId: string) {
  try {
    await ElMessageBox.confirm('确定要停止此执行吗？此操作不可恢复。', '确认停止', {
      type: 'warning'
    })
    await executionApi.stop(executionId)
    ElMessage.success('已停止')
    loadExecutions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to stop execution:', error)
      ElMessage.error('停止失败')
    }
  }
}

onMounted(() => {
  loadExecutions()
})
</script>

<style lang="scss" scoped>
.execution-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #e4e7ed;

  h1 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
  }
}

.page-main {
  flex: 1;
  padding: 24px;
  overflow: auto;
}

.workflow-link {
  color: #409eff;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>