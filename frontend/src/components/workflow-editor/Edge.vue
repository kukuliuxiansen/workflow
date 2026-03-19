<template>
  <BaseEdge :id="id" :path="path" :style="edgeStyle" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { BaseEdge, getBezierPath } from '@vue-flow/core'
import type { EdgeType } from '@/types'

interface Props {
  id: string
  sourceX: number
  sourceY: number
  targetX: number
  targetY: number
  sourcePosition: any
  targetPosition: any
  type: EdgeType
}

const props = defineProps<Props>()

const path = computed(() => {
  const [pathStr] = getBezierPath({
    sourceX: props.sourceX,
    sourceY: props.sourceY,
    targetX: props.targetX,
    targetY: props.targetY,
    sourcePosition: props.sourcePosition,
    targetPosition: props.targetPosition
  })
  return pathStr
})

const edgeStyle = computed(() => ({
  stroke: props.type === 'success' ? '#67C23A' : '#F56C6C',
  strokeWidth: 2
}))
</script>

<style lang="scss" scoped>
:deep(.vue-flow__edge-path) {
  stroke-width: 2;
}
</style>