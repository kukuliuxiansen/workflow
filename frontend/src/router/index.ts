import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/workflows'
  },
  {
    path: '/workflows',
    name: 'WorkflowList',
    component: () => import('@/views/WorkflowList.vue'),
    meta: { title: '工作流列表' }
  },
  {
    path: '/workflows/:id',
    name: 'WorkflowEditor',
    component: () => import('@/views/WorkflowEditor.vue'),
    meta: { title: '工作流编辑' }
  },
  {
    path: '/executions',
    name: 'ExecutionList',
    component: () => import('@/views/ExecutionList.vue'),
    meta: { title: '执行记录' }
  },
  {
    path: '/reviews',
    name: 'ReviewList',
    component: () => import('@/views/ReviewList.vue'),
    meta: { title: '人工核查' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || 'OpenClaw'
  next()
})

export default router