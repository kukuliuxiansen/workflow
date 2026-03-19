<template>
  <div class="workflow-toolbar">
    <el-button-group>
      <el-button @click="handleZoomIn" title="放大">
        <el-icon><ZoomIn /></el-icon>
      </el-button>
      <el-button @click="handleZoomOut" title="缩小">
        <el-icon><ZoomOut /></el-icon>
      </el-button>
      <el-button @click="handleFitView" title="适应画布">
        <el-icon><FullScreen /></el-icon>
      </el-button>
    </el-button-group>
    <span class="zoom-level">{{ Math.round(zoom * 100) }}%</span>
  </div>
</template>

<script setup lang="ts">
import { ZoomIn, ZoomOut, FullScreen } from '@element-plus/icons-vue'
import { useEditorStore } from '@/stores'

const editorStore = useEditorStore()

const zoom = editorStore.zoom

function handleZoomIn() {
  editorStore.setZoom(zoom + 0.1)
}

function handleZoomOut() {
  editorStore.setZoom(zoom - 0.1)
}

function handleFitView() {
  editorStore.setZoom(1)
}
</script>

<style lang="scss" scoped>
.workflow-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.zoom-level {
  font-size: 12px;
  color: #909399;
  min-width: 40px;
  text-align: center;
}
</style>