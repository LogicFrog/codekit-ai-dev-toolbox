<template>
  <div class="file-explorer">
    <div class="explorer-header">
      <div class="current-path">
        <el-icon class="path-icon"><FolderOpened /></el-icon>
        <span class="path-text">{{ currentPath }}</span>
      </div>
      <div class="path-actions">
        <el-button text size="small" @click="handleGoUp" :disabled="!canGoUp">
          <el-icon><ArrowUp /></el-icon>
          上级
        </el-button>
        <el-button text size="small" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <div class="explorer-content" v-loading="loading">
      <div v-if="fileList.length === 0" class="empty-state">
        <el-icon class="empty-icon"><Folder /></el-icon>
        <p class="empty-text">该目录为空</p>
      </div>

      <div v-else class="file-list">
        <div
          v-for="item in fileList"
          :key="item.path"
          class="file-item"
          :class="{ directory: item.isDirectory }"
          @dblclick="handleItemDblClick(item)"
        >
          <el-icon class="file-icon" :class="{ 'folder-icon': item.isDirectory }">
            <Folder v-if="item.isDirectory" />
            <Document v-else />
          </el-icon>
          <span class="file-name">{{ item.name }}</span>
          <div class="file-actions" v-if="!readonly">
            <el-button
              v-if="item.isDirectory"
              text
              size="small"
              type="primary"
              @click.stop="handleEnter(item)"
            >
              进入
            </el-button>
            <el-button
              v-else
              text
              size="small"
              type="primary"
              @click.stop="handleSelect(item)"
            >
              选择
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Folder, FolderOpened, Document, ArrowUp, Refresh } from '@element-plus/icons-vue'
import type { FsItem } from '@/types'
import { listFs } from '@/api/system'

interface Props {
  modelValue?: string
  readonly?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '/',
  readonly: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'select', item: FsItem): void
  (e: 'enter', item: FsItem): void
}>()

const loading = ref(false)
const fileList = ref<FsItem[]>([])
const currentPath = ref(props.modelValue || '/')

const canGoUp = computed(() => {
  const parts = currentPath.value.split('/').filter(Boolean)
  return parts.length > 0
})

const loadDirectory = async (path: string) => {
  loading.value = true
  try {
    const items = await listFs(path)
    fileList.value = items || []
    currentPath.value = path
    emit('update:modelValue', path)
  } catch (error) {
    console.error('加载目录失败:', error)
    fileList.value = []
  } finally {
    loading.value = false
  }
}

const handleGoUp = () => {
  const parts = currentPath.value.split('/').filter(Boolean)
  parts.pop()
  const parentPath = '/' + parts.join('/')
  loadDirectory(parentPath || '/')
}

const handleRefresh = () => {
  loadDirectory(currentPath.value)
}

const handleItemDblClick = (item: FsItem) => {
  if (item.isDirectory) {
    handleEnter(item)
  } else {
    handleSelect(item)
  }
}

const handleEnter = (item: FsItem) => {
  loadDirectory(item.path)
  emit('enter', item)
}

const handleSelect = (item: FsItem) => {
  emit('select', item)
}

defineExpose({
  loadDirectory,
  refresh: handleRefresh
})
</script>

<style scoped>
.file-explorer {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.explorer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--color-bg-sunken);
  border-bottom: 1px solid var(--color-border-muted);
}

.current-path {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  min-width: 0;
  flex: 1;
}

.path-icon {
  font-size: 16px;
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.path-text {
  font-size: var(--text-sm);
  font-family: var(--font-mono);
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.path-actions {
  display: flex;
  gap: var(--spacing-xs);
  flex-shrink: 0;
}

.explorer-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-sm);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-4xl);
  color: var(--color-text-muted);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-md);
}

.empty-text {
  font-size: var(--text-sm);
  margin: 0;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background-color var(--transition-fast);
}

.file-item:hover {
  background: var(--color-bg-muted);
}

.file-icon {
  font-size: 16px;
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.file-icon.folder-icon {
  color: var(--color-warning);
}

.file-name {
  flex: 1;
  font-size: var(--text-sm);
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-actions {
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.file-item:hover .file-actions {
  opacity: 1;
}
</style>
