<template>
  <div class="version-control">
    <div class="vc-header">
      <div class="header-left">
        <div class="select-group">
          <span class="select-label">代码片段</span>
          <el-select
            v-model="selectedSnippetId"
            placeholder="选择代码片段"
            clearable
            filterable
            @change="handleSnippetChange"
            style="width: 280px"
          >
            <el-option
              v-for="snippet in snippetsList"
              :key="snippet.id"
              :label="snippet.fileName"
              :value="snippet.id"
            >
              <div class="snippet-option">
                <span class="snippet-name">{{ snippet.fileName }}</span>
                <span class="snippet-lang">{{ snippet.languageType }}</span>
              </div>
            </el-option>
          </el-select>
        </div>

        <div class="select-group" v-if="selectedSnippetId">
          <span class="select-label">版本 A</span>
          <el-select
            v-model="versionA"
            placeholder="选择版本"
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="v in versionsList"
              :key="v.id"
              :label="v.versionName || `v${v.id}`"
              :value="v.id"
            >
              <div class="version-option">
                <span class="version-name">{{ v.versionName || `v${v.id}` }}</span>
                <span class="version-time">{{ formatRelativeTime(v.createTime) }}</span>
              </div>
            </el-option>
          </el-select>
        </div>

        <div class="select-group" v-if="selectedSnippetId">
          <span class="select-label">版本 B</span>
          <el-select
            v-model="versionB"
            placeholder="选择版本"
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="v in versionsList"
              :key="v.id"
              :label="v.versionName || `v${v.id}`"
              :value="v.id"
            >
              <div class="version-option">
                <span class="version-name">{{ v.versionName || `v${v.id}` }}</span>
                <span class="version-time">{{ formatRelativeTime(v.createTime) }}</span>
              </div>
            </el-option>
          </el-select>
        </div>
      </div>

      <div class="header-right">
        <el-button-group v-if="versionA && versionB">
          <el-button :type="renderSideBySide ? 'primary' : ''" @click="renderSideBySide = true">
            并排视图
          </el-button>
          <el-button :type="!renderSideBySide ? 'primary' : ''" @click="renderSideBySide = false">
            内联视图
          </el-button>
        </el-button-group>
        <el-button @click="handleSwapVersions" v-if="versionA && versionB">
          <el-icon><RefreshRight /></el-icon>
          交换版本
        </el-button>
        <el-button type="primary" @click="showCreateDialog = true" v-if="selectedSnippetId">
          <el-icon><Plus /></el-icon>
          创建版本
        </el-button>
      </div>
    </div>

    <div class="vc-content" v-loading="loading">
      <div class="diff-wrapper" v-if="originalVersion && modifiedVersion">
        <div class="diff-header">
          <div class="version-badge original">
            <span class="badge-label">原始版本</span>
            <span class="badge-name">{{ originalVersion.versionName || `v${originalVersion.id}` }}</span>
            <span class="badge-time">{{ formatRelativeTime(originalVersion.createTime) }}</span>
          </div>
          <div class="diff-stats" v-if="diffResult">
            <span class="stat added">+{{ diffResult.added }}</span>
            <span class="stat removed">-{{ diffResult.removed }}</span>
            <span class="stat modified">~{{ diffResult.modifications }}</span>
          </div>
          <div class="version-badge modified">
            <span class="badge-label">对比版本</span>
            <span class="badge-name">{{ modifiedVersion.versionName || `v${modifiedVersion.id}` }}</span>
            <span class="badge-time">{{ formatRelativeTime(modifiedVersion.createTime) }}</span>
          </div>
        </div>
        <div class="diff-editor">
          <DiffEditor
            ref="diffEditorRef"
            :original-content="originalVersion.codeContent"
            :modified-content="modifiedVersion.codeContent"
            :original-language="detectLanguage(originalVersion)"
            :modified-language="detectLanguage(modifiedVersion)"
            theme="vs-light"
            :render-side-by-side="renderSideBySide"
          />
        </div>
      </div>

      <div class="empty-state" v-else>
        <el-icon class="empty-icon"><Switch /></el-icon>
        <h3 class="empty-title">版本对比</h3>
        <p class="empty-hint">选择代码片段和两个版本进行对比</p>
        <div class="empty-steps">
          <div class="step">
            <span class="step-num">1</span>
            <span class="step-text">选择代码片段</span>
          </div>
          <div class="step-arrow">→</div>
          <div class="step">
            <span class="step-num">2</span>
            <span class="step-text">选择版本 A</span>
          </div>
          <div class="step-arrow">→</div>
          <div class="step">
            <span class="step-num">3</span>
            <span class="step-text">选择版本 B</span>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showCreateDialog"
      title="创建版本"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form :model="createVersionForm" label-width="80px" label-position="left">
        <el-form-item label="版本名称" required>
          <el-input
            v-model="createVersionForm.versionName"
            placeholder="输入版本名称"
          />
        </el-form-item>
        <el-form-item label="版本描述">
          <el-input
            v-model="createVersionForm.description"
            type="textarea"
            :rows="3"
            placeholder="可选的版本描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateVersion" :loading="creating">
          创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshRight, Switch, Plus } from '@element-plus/icons-vue'
import { listVersions, createVersion } from '@/api/version'
import type { VersionInfo, CodeSnippet } from '@/types'
import { formatRelativeTime } from '@/utils/helpers'
import DiffEditor from '@/components/DiffEditor.vue'

const loading = ref(false)
const snippetsList = ref<CodeSnippet[]>([])
const versionsList = ref<VersionInfo[]>([])
const selectedSnippetId = ref<number | null>(null)
const versionA = ref<number | null>(null)
const versionB = ref<number | null>(null)
const renderSideBySide = ref(true)
const diffResult = ref<{ added: number; removed: number; modifications: number } | null>(null)
const diffEditorRef = ref()
const showCreateDialog = ref(false)
const createVersionForm = ref({
  versionName: '',
  description: ''
})
const creating = ref(false)

const originalVersion = computed(() => {
  if (!versionA.value) return null
  return versionsList.value.find(v => v.id === versionA.value) || null
})

const modifiedVersion = computed(() => {
  if (!versionB.value) return null
  return versionsList.value.find(v => v.id === versionB.value) || null
})

const detectLanguage = (version: VersionInfo): string => {
  const snippet = snippetsList.value.find(s => s.id === version.snippetId)
  return snippet?.languageType || 'plaintext'
}

const fetchSnippets = async () => {
  loading.value = true
  try {
    const { getAllCodeSnippets } = await import('@/api/code')
    const allSnippets = await getAllCodeSnippets()
    snippetsList.value = allSnippets
  } catch (error) {
    console.error('获取代码片段列表失败:', error)
    ElMessage.error('加载代码片段失败')
  } finally {
    loading.value = false
  }
}

const fetchVersions = async (snippetId: number) => {
  loading.value = true
  versionsList.value = []
  versionA.value = null
  versionB.value = null
  
  try {
    const versions = await listVersions(snippetId)
    versionsList.value = versions || []
    
    if (versions.length >= 2) {
      versionA.value = versions[versions.length - 2].id
      versionB.value = versions[versions.length - 1].id
    } else if (versions.length === 1) {
      versionA.value = versions[0].id
    }
  } catch (error) {
    console.error('获取版本列表失败:', error)
    ElMessage.error('加载版本列表失败')
  } finally {
    loading.value = false
  }
}

const handleSnippetChange = (snippetId: number | null) => {
  if (snippetId) {
    fetchVersions(snippetId)
  } else {
    versionsList.value = []
    versionA.value = null
    versionB.value = null
  }
}

const handleSwapVersions = () => {
  const temp = versionA.value
  versionA.value = versionB.value
  versionB.value = temp
}

watch([versionA, versionB], () => {
  if (versionA.value && versionB.value) {
    setTimeout(() => {
      const diff = diffEditorRef.value?.getDiff?.()
      if (diff) {
        diffResult.value = diff
      }
    }, 500)
  } else {
    diffResult.value = null
  }
})

const handleCreateVersion = async () => {
  if (!selectedSnippetId.value || !createVersionForm.value.versionName.trim()) {
    ElMessage.warning('请输入版本名称')
    return
  }
  
  creating.value = true
  try {
    await createVersion(selectedSnippetId.value, {
      versionName: createVersionForm.value.versionName,
      description: createVersionForm.value.description
    })
    ElMessage.success('版本创建成功')
    showCreateDialog.value = false
    createVersionForm.value.versionName = ''
    createVersionForm.value.description = ''
    fetchVersions(selectedSnippetId.value)
  } catch (error) {
    console.error('创建版本失败:', error)
    ElMessage.error('创建版本失败')
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  fetchSnippets()
})
</script>

<style scoped>
.version-control {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-bg-base);
}

.vc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg) var(--spacing-xl);
  background: var(--color-bg-elevated);
  border-bottom: 1px solid var(--color-border-muted);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-xl);
}

.select-group {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.select-label {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.snippet-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.snippet-name {
  font-size: var(--text-sm);
  color: var(--color-text-primary);
}

.snippet-lang {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  background: var(--color-bg-muted);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.version-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.version-name {
  font-size: var(--text-sm);
  color: var(--color-text-primary);
}

.version-time {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.vc-content {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.diff-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.diff-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-xl);
  background: var(--color-bg-sunken);
  border-bottom: 1px solid var(--color-border-muted);
}

.version-badge {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--color-bg-elevated);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-muted);
}

.version-badge.original {
  border-left: 3px solid var(--color-error);
}

.version-badge.modified {
  border-left: 3px solid var(--color-success);
}

.badge-label {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  color: var(--color-text-muted);
  letter-spacing: 0.5px;
}

.badge-name {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-primary);
}

.badge-time {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.diff-stats {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.stat {
  font-size: var(--text-sm);
  font-weight: 600;
  font-family: var(--font-mono);
}

.stat.added {
  color: var(--color-success);
}

.stat.removed {
  color: var(--color-error);
}

.stat.modified {
  color: var(--color-warning);
}

.diff-editor {
  flex: 1;
  min-height: 0;
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-4xl);
}

.empty-icon {
  font-size: 64px;
  color: var(--color-text-hint);
  margin-bottom: var(--spacing-xl);
}

.empty-title {
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-sm);
}

.empty-hint {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  margin: 0 0 var(--spacing-2xl);
}

.empty-steps {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.step {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-md);
}

.step-num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: var(--color-accent-primary);
  color: white;
  font-size: var(--text-xs);
  font-weight: 600;
  border-radius: 50%;
}

.step-text {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.step-arrow {
  font-size: var(--text-lg);
  color: var(--color-text-muted);
}
</style>
