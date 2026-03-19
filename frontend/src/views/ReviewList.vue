<template>
  <div class="review-list-page">
    <el-header class="page-header">
      <h1>人工核查</h1>
      <el-radio-group v-model="statusFilter" @change="loadReviews">
        <el-radio-button label="pending">待审核</el-radio-button>
        <el-radio-button label="">全部</el-radio-button>
      </el-radio-group>
    </el-header>

    <el-main class="page-main">
      <el-table :data="reviews" v-loading="loading" stripe>
        <el-table-column prop="id" label="审核ID" width="180" />
        <el-table-column prop="workflowId" label="工作流ID" width="180" />
        <el-table-column prop="nodeId" label="节点ID" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="180">
          <template #default="{ row }">
            {{ row.submitTime ? formatDate(row.submitTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="responseTime" label="响应时间" width="180">
          <template #default="{ row }">
            {{ row.responseTime ? formatDate(row.responseTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="decision" label="决策" width="100">
          <template #default="{ row }">
            <template v-if="row.decision">
              <el-tag :type="row.decision === 'approve' ? 'success' : 'danger'">
                {{ row.decision === 'approve' ? '通过' : '拒绝' }}
              </el-tag>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-button size="small" type="success" @click="handleReview(row, 'approve')">
                通过
              </el-button>
              <el-button size="small" type="danger" @click="showRejectDialog(row)">
                拒绝
              </el-button>
            </template>
            <span v-else class="text-secondary">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadReviews"
        @current-change="loadReviews"
        class="pagination"
      />
    </el-main>

    <!-- 拒绝对话框 -->
    <el-dialog v-model="showRejectReasonDialog" title="拒绝原因" width="500px">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        placeholder="请输入拒绝原因"
      />
      <template #footer>
        <el-button @click="showRejectReasonDialog = false">取消</el-button>
        <el-button type="danger" @click="submitReject" :loading="submitting">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reviewApi } from '@/api'
import type { ReviewRecord, ReviewStatus, ReviewDecision } from '@/types'

const loading = ref(false)
const reviews = ref<ReviewRecord[]>([])
const statusFilter = ref<'pending' | ''>('pending')
const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const showRejectReasonDialog = ref(false)
const rejectingReview = ref<ReviewRecord | null>(null)
const rejectReason = ref('')
const submitting = ref(false)

const statusMap: Record<ReviewStatus, { label: string; type: string }> = {
  pending: { label: '待审核', type: 'warning' },
  approved: { label: '已通过', type: 'success' },
  rejected: { label: '已拒绝', type: 'danger' },
  timeout: { label: '已超时', type: 'info' }
}

function getStatusLabel(status: ReviewStatus) {
  return statusMap[status]?.label || status
}

function getStatusType(status: ReviewStatus) {
  return statusMap[status]?.type || 'info'
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function loadReviews() {
  loading.value = true
  try {
    const res = await reviewApi.listPending({
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    reviews.value = res.data
    pagination.total = res.total
  } catch (error) {
    console.error('Failed to load reviews:', error)
    ElMessage.error('加载审核列表失败')
  } finally {
    loading.value = false
  }
}

async function handleReview(review: ReviewRecord, decision: ReviewDecision) {
  try {
    await ElMessageBox.confirm(
      decision === 'approve' ? '确定要通过此审核吗？' : '确定要拒绝此审核吗？',
      '确认操作',
      { type: 'warning' }
    )
    await reviewApi.respond(review.id, decision)
    ElMessage.success(decision === 'approve' ? '已通过' : '已拒绝')
    loadReviews()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to respond review:', error)
      ElMessage.error('操作失败')
    }
  }
}

function showRejectDialog(review: ReviewRecord) {
  rejectingReview.value = review
  rejectReason.value = ''
  showRejectReasonDialog.value = true
}

async function submitReject() {
  if (!rejectingReview.value) return

  submitting.value = true
  try {
    await reviewApi.respond(rejectingReview.value.id, 'reject', rejectReason.value)
    showRejectReasonDialog.value = false
    ElMessage.success('已拒绝')
    loadReviews()
  } catch (error) {
    console.error('Failed to reject:', error)
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadReviews()
})
</script>

<style lang="scss" scoped>
.review-list-page {
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

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.text-secondary {
  color: #909399;
}
</style>