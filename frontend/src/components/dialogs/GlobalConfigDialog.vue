<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="全局配置"
    width="600px"
    destroy-on-close
  >
    <el-form :model="formData" label-width="120px">
      <el-form-item label="项目路径" required>
        <el-input
          v-model="formData.projectPath"
          placeholder="请输入项目路径"
        />
      </el-form-item>
      <el-form-item label="飞书 Open ID">
        <el-input
          v-model="formData.feishuOpenId"
          placeholder="请输入飞书 Open ID"
        />
      </el-form-item>
      <el-form-item label="最大全局循环">
        <el-input-number
          v-model="formData.maxGlobalLoop"
          :min="1"
          :max="10"
        />
      </el-form-item>
      <el-form-item label="默认超时时间">
        <el-input-number
          v-model="formData.timeout"
          :min="1"
          :max="3600"
        />
        <span class="unit">秒</span>
      </el-form-item>
      <el-form-item label="默认重试间隔">
        <el-input-number
          v-model="formData.retryInterval"
          :min="1"
          :max="600"
        />
        <span class="unit">秒</span>
      </el-form-item>
      <el-form-item label="输出格式">
        <el-select v-model="formData.outputFormat" placeholder="请选择输出格式">
          <el-option label="JSON" value="json" />
          <el-option label="文本" value="text" />
          <el-option label="Markdown" value="markdown" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" @click="handleSave">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { GlobalConfig } from '@/types'

interface Props {
  modelValue: boolean
  config: GlobalConfig
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', config: GlobalConfig): void
}>()

const formData = reactive<GlobalConfig>({
  projectPath: '',
  feishuOpenId: '',
  maxGlobalLoop: 3,
  timeout: 300,
  retryInterval: 60,
  outputFormat: 'json'
})

watch(() => props.config, (config) => {
  Object.assign(formData, config)
}, { immediate: true, deep: true })

function handleSave() {
  emit('save', { ...formData })
}
</script>

<style lang="scss" scoped>
.unit {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>