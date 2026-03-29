<template>
  <div class="code-manager">
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="scan-input-group">
          <el-input
            v-model="scanDir"
            placeholder="输入本地代码目录路径..."
            size="default"
            clearable
            class="scan-input"
            @keyup.enter="handleScan"
          >
            <template #prefix>
              <el-icon><Folder /></el-icon>
            </template>
            <template #append>
              <el-button @click="handleOpenScanExplorer">
                <el-icon><FolderOpened /></el-icon>
                浏览
              </el-button>
            </template>
          </el-input>
          <el-button type="primary" @click="handleScan" :loading="scanning">
            <el-icon><Search /></el-icon>
            扫描目录
          </el-button>
        </div>
      </div>
      <div class="toolbar-right">
        <el-button @click="showImportDialog = true">
          <el-icon><Upload /></el-icon>
          导入文件
        </el-button>
        <el-button @click="showAIDrawer = true" disabled>
          <el-icon><ChatDotRound /></el-icon>
          AI 助手
          <span class="preview-tag">预留</span>
        </el-button>
      </div>
    </div>

    <div class="main-content">
      <div class="list-panel">
        <div class="panel-header">
          <div class="panel-title">
            <span class="title-text">代码片段</span>
            <span class="count-badge">{{ total }}</span>
          </div>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索..."
            size="small"
            clearable
            class="search-input"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div class="list-content" v-loading="loading.list">
          <div
            v-for="code in codeList"
            :key="code.id"
            class="code-item"
            :class="{ active: currentCode?.id === code.id }"
            @click="handleSelectCode(code)"
          >
            <div class="item-header">
              <div class="item-title">
                <el-icon class="file-icon"><Document /></el-icon>
                <span class="file-name">{{ code.fileName }}</span>
              </div>
              <span 
                class="language-tag"
                :style="{ backgroundColor: getLanguageColor(code.languageType) + '20', color: getLanguageColor(code.languageType) }"
              >
                {{ code.languageType }}
              </span>
            </div>
            <div class="item-preview">
              <code>{{ getCodePreview(code.codeContent, 2) }}</code>
            </div>
            <div class="item-footer">
              <span class="meta-item">
                <el-icon><Clock /></el-icon>
                {{ formatRelativeTime(code.updateTime) }}
              </span>
              <span class="meta-item" v-if="code.tags?.length">
                <el-icon><PriceTag /></el-icon>
                {{ code.tags.slice(0, 2).join(', ') }}
              </span>
            </div>
          </div>

          <div class="empty-state" v-if="!loading.list && codeList.length === 0">
            <el-icon class="empty-icon"><Document /></el-icon>
            <p class="empty-text">暂无代码片段</p>
            <p class="empty-hint">扫描目录或导入文件开始管理代码</p>
          </div>
        </div>

        <div class="panel-footer" v-if="total > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            size="small"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>

      <div class="detail-panel" v-if="currentCode">
        <div class="detail-header">
          <div class="detail-title">
            <el-icon class="detail-icon"><Document /></el-icon>
            <span class="detail-name">{{ currentCode.fileName }}</span>
          </div>
          <div class="detail-actions">
            <el-button size="small" @click="handleCreateVersion">
              <el-icon><Clock /></el-icon>
              创建版本
            </el-button>
            <el-button size="small" type="danger" @click="handleDeleteCode">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
            <el-button size="small" type="primary" @click="handleSaveCode" :loading="loading.save">
              <el-icon><Check /></el-icon>
              保存
            </el-button>
          </div>
        </div>

        <el-collapse v-model="activeDetails" class="detail-collapse">
          <el-collapse-item title="详情信息" name="info">
            <div class="detail-info">
              <div class="info-row">
                <span class="info-label">路径</span>
                <span class="info-value mono">{{ currentCode.filePath }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">语言</span>
                <span 
                  class="language-tag"
                  :style="{ backgroundColor: getLanguageColor(currentCode.languageType) + '20', color: getLanguageColor(currentCode.languageType) }"
                >
                  {{ currentCode.languageType }}
                </span>
              </div>
              <div class="info-row">
                <span class="info-label">标签</span>
                <div class="tags-container">
                  <el-tag
                    v-for="tag in currentCode.tags"
                    :key="tag"
                    size="small"
                    closable
                    @close="handleRemoveTag(tag)"
                  >
                    {{ tag }}
                  </el-tag>
                  <el-input
                    v-if="showTagInput"
                    v-model="newTag"
                    size="small"
                    class="tag-input"
                    @keyup.enter="handleAddTag"
                    @blur="handleAddTag"
                    ref="tagInputRef"
                  />
                  <el-button v-else size="small" text @click="showTagInput = true">
                    <el-icon><Plus /></el-icon>
                    添加
                  </el-button>
                </div>
              </div>
            </div>
          </el-collapse-item>

          <el-collapse-item title="依赖关系" name="deps" v-if="currentCode.dependencies?.length">
            <div class="dependencies-list">
              <span 
                v-for="dep in currentCode.dependencies" 
                :key="dep.id" 
                class="dependency-item"
              >
                {{ dep.dependName }}
                <span v-if="dep.dependType" class="dep-type">({{ dep.dependType }})</span>
              </span>
            </div>
          </el-collapse-item>
        </el-collapse>

        <div class="detail-editor">
          <div class="editor-content">
            <CodeEditor
              :key="currentCode.id"
              v-model="currentCode.codeContent"
              :language="currentCode.languageType"
              :theme="editorTheme"
            />
          </div>
        </div>
      </div>

      <div class="empty-detail" v-else>
        <el-icon class="empty-icon"><Document /></el-icon>
        <p class="empty-title">选择代码片段</p>
        <p class="empty-hint">从左侧列表中选择一个代码片段查看详情</p>
      </div>
    </div>

    <el-dialog 
      v-model="showImportDialog" 
      title="导入文件" 
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form :model="importForm" label-width="80px" label-position="left">
        <el-form-item label="文件路径">
          <el-input
            v-model="importForm.filePath"
            placeholder="选择或输入文件路径"
            readonly
          >
            <template #append>
              <el-button @click="handleOpenFileExplorer">浏览</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="语言类型">
          <el-select v-model="importForm.languageType" placeholder="自动检测" clearable style="width: 100%">
            <el-option label="Java" value="Java" />
            <el-option label="Python" value="Python" />
            <el-option label="JavaScript" value="JavaScript" />
            <el-option label="TypeScript" value="TypeScript" />
            <el-option label="Go" value="Go" />
            <el-option label="Rust" value="Rust" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="importForm.tag" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" @click="handleImport" :loading="loading.import">导入</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showFileExplorer"
      title="选择文件"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="file-explorer">
        <div class="current-path">
          <el-icon><FolderOpened /></el-icon>
          <span class="path-text">{{ currentPath }}</span>
          <el-button text size="small" @click="handleGoUp" :disabled="!canGoUp">
            <el-icon><ArrowUp /></el-icon>
            上级
          </el-button>
        </div>
        <el-table :data="fileList" height="360px" v-loading="loading.files" @row-dblclick="handleRowDblClick">
          <el-table-column prop="name" label="名称" min-width="200">
            <template #default="{ row }">
              <div class="file-name-cell">
                <el-icon v-if="row.isDirectory" class="folder-icon"><Folder /></el-icon>
                <el-icon v-else class="file-icon"><Document /></el-icon>
                <span>{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.isDirectory ? 'primary' : 'info'">
                {{ row.isDirectory ? '目录' : '文件' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button 
                v-if="row.isDirectory"
                text 
                size="small" 
                @click="loadDirectory(row.path)"
              >
                进入
              </el-button>
              <el-button 
                v-else 
                text 
                size="small" 
                type="primary"
                @click="handleSelectFile(row)"
              >
                选择
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <el-dialog
      v-model="showScanExplorer"
      title="选择扫描目录"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="file-explorer">
        <div class="current-path">
          <el-icon><FolderOpened /></el-icon>
          <span class="path-text">{{ scanPath }}</span>
          <el-button text size="small" @click="handleScanGoUp" :disabled="scanPath === '/'">
            <el-icon><ArrowUp /></el-icon>
            上级
          </el-button>
          <el-button type="primary" size="small" @click="handleConfirmScanDir">
            选择此目录
          </el-button>
        </div>
        <el-table :data="scanFileList" height="360px" v-loading="loading.scanFiles" @row-dblclick="handleScanRowDblClick">
          <el-table-column prop="name" label="名称" min-width="200">
            <template #default="{ row }">
              <div class="file-name-cell">
                <el-icon v-if="row.isDirectory" class="folder-icon"><Folder /></el-icon>
                <el-icon v-else class="file-icon"><Document /></el-icon>
                <span>{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.isDirectory ? 'primary' : 'info'">
                {{ row.isDirectory ? '目录' : '文件' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.isDirectory"
                text
                size="small"
                @click="loadScanDirectory(row.path)"
              >
                进入
              </el-button>
              <el-tag v-else size="small" type="info">
                文件
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <el-drawer
      v-model="showAIDrawer"
      title="AI 助手"
      size="400px"
      :close-on-click-modal="false"
    >
      <div class="ai-placeholder">
        <el-icon class="ai-icon"><ChatDotRound /></el-icon>
        <h3>AI 助手功能开发中</h3>
        <p>该功能正在积极开发中，敬请期待</p>
        <div class="ai-features">
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>代码解释与优化建议</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>智能代码补全</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>Bug 检测与修复建议</span>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Folder, Search, Upload, ChatDotRound, Document, Clock, 
  Delete, Check, PriceTag, Plus, FolderOpened, ArrowUp
} from '@element-plus/icons-vue'
import { 
  listCodeByPage, scanLocalCode, getScanStatus, saveCodeSnippetByPath,
  deleteCodeSnippet, saveCodeSnippet, listCodeDependencies, createVersion
} from '@/api/code'
import { listFs } from '@/api/system'
import type { CodeSnippet, FsItem } from '@/types'
import { formatRelativeTime, getLanguageColor, getCodePreview } from '@/utils/helpers'
import CodeEditor from '@/components/CodeEditor.vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const scanDir = ref('')
const scanPath = ref('/')
const searchKeyword = ref('')
const scanning = ref(false)
const editorTheme = ref<'vs-dark' | 'vs-light'>('vs-light')
const showImportDialog = ref(false)
const showFileExplorer = ref(false)
const showScanExplorer = ref(false)
const showAIDrawer = ref(false)
const showTagInput = ref(false)
const newTag = ref('')
const activeDetails = ref<string[]>([])

const loading = reactive({
  list: false,
  save: false,
  import: false,
  files: false,
  scanFiles: false
})

const codeList = ref<CodeSnippet[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const currentCode = ref<CodeSnippet | null>(null)

const importForm = reactive({
  filePath: '',
  languageType: '',
  tag: ''
})

const fileList = ref<FsItem[]>([])
const scanFileList = ref<FsItem[]>([])
const currentPath = ref('/')

const canGoUp = computed(() => currentPath.value !== '/' && currentPath.value !== '')

const handleRefreshList = async () => {
  loading.list = true
  try {
    const result = await listCodeByPage(currentPage.value - 1, pageSize.value)
    codeList.value = result?.content || []
    total.value = result?.totalElements || 0
  } catch (error) {
    console.error('加载代码列表失败:', error)
    ElMessage.error('加载代码列表失败')
    codeList.value = []
    total.value = 0
  } finally {
    loading.list = false
  }
}

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({
      path: '/search-center',
      query: { keyword: searchKeyword.value }
    })
  }
}

const handleSelectCode = async (code: CodeSnippet) => {
  currentCode.value = { ...code }
  if (!currentCode.value.tags) {
    currentCode.value.tags = []
  }
  try {
    const dependencies = await listCodeDependencies(code.id)
    currentCode.value.dependencies = dependencies
  } catch (error) {
    console.error('加载依赖失败:', error)
    currentCode.value.dependencies = []
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  handleRefreshList()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  handleRefreshList()
}

const handleScan = async () => {
  if (!scanDir.value) {
    ElMessage.warning('请输入扫描目录')
    return
  }
  
  scanning.value = true
  try {
    await scanLocalCode(scanDir.value)
    ElMessage.info('扫描任务已启动')
    
    const pollInterval = setInterval(async () => {
      try {
        const status = await getScanStatus(scanDir.value)
        if (status.status === 'COMPLETED') {
          clearInterval(pollInterval)
          ElMessage.success('扫描完成')
          scanning.value = false
          handleRefreshList()
        } else if (status.status === 'FAILED') {
          clearInterval(pollInterval)
          ElMessage.error('扫描失败')
          scanning.value = false
        }
      } catch (error) {
        clearInterval(pollInterval)
        scanning.value = false
      }
    }, 2000)
  } catch (error) {
    console.error('扫描失败:', error)
    ElMessage.error('扫描失败')
    scanning.value = false
  }
}

const handleOpenFileExplorer = () => {
  currentPath.value = '/'
  loadDirectory('/')
  showFileExplorer.value = true
}

const handleOpenScanExplorer = () => {
  scanPath.value = '/'
  loadScanDirectory('/')
  showScanExplorer.value = true
}

const loadScanDirectory = async (path: string) => {
  loading.scanFiles = true
  try {
    scanFileList.value = await listFs(path)
    scanPath.value = path
  } catch (error) {
    console.error('加载目录失败:', error)
    ElMessage.error('加载目录失败')
  } finally {
    loading.scanFiles = false
  }
}

const handleScanGoUp = () => {
  const parts = scanPath.value.split('/').filter(Boolean)
  parts.pop()
  const parentPath = '/' + parts.join('/')
  loadScanDirectory(parentPath || '/')
}

const handleScanRowDblClick = (row: FsItem) => {
  if (row.isDirectory) {
    loadScanDirectory(row.path)
  }
}

const handleConfirmScanDir = () => {
  scanDir.value = scanPath.value
  showScanExplorer.value = false
}

const loadDirectory = async (path: string) => {
  loading.files = true
  try {
    fileList.value = await listFs(path)
    currentPath.value = path
  } catch (error) {
    console.error('加载目录失败:', error)
    ElMessage.error('加载目录失败')
  } finally {
    loading.files = false
  }
}

const handleGoUp = () => {
  const parts = currentPath.value.split('/').filter(Boolean)
  parts.pop()
  const parentPath = '/' + parts.join('/')
  loadDirectory(parentPath || '/')
}

const handleRowDblClick = (row: FsItem) => {
  if (row.isDirectory) {
    loadDirectory(row.path)
  } else {
    handleSelectFile(row)
  }
}

const handleSelectFile = (row: FsItem) => {
  importForm.filePath = row.path
  showFileExplorer.value = false
}

const handleImport = async () => {
  if (!importForm.filePath) {
    ElMessage.warning('请选择文件')
    return
  }
  
  loading.import = true
  try {
    await saveCodeSnippetByPath(
      importForm.filePath,
      importForm.languageType,
      importForm.tag
    )
    ElMessage.success('导入成功')
    showImportDialog.value = false
    importForm.filePath = ''
    importForm.languageType = ''
    importForm.tag = ''
    handleRefreshList()
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error('导入失败')
  } finally {
    loading.import = false
  }
}

const handleSaveCode = async () => {
  if (!currentCode.value) return
  
  loading.save = true
  try {
    await saveCodeSnippet(currentCode.value)
    ElMessage.success('保存成功')
    handleRefreshList()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    loading.save = false
  }
}

const handleDeleteCode = async () => {
  if (!currentCode.value) return
  
  try {
    await ElMessageBox.confirm('确定要删除该代码片段吗？', '确认删除', {
      type: 'warning'
    })
    await deleteCodeSnippet(currentCode.value.id)
    ElMessage.success('删除成功')
    currentCode.value = null
    handleRefreshList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleCreateVersion = async () => {
  if (!currentCode.value) return
  
  try {
    await createVersion(currentCode.value.id, {
      versionName: `v${Date.now()}`,
      description: '手动创建版本'
    })
    ElMessage.success('版本创建成功')
  } catch (error) {
    console.error('创建版本失败:', error)
    ElMessage.error('创建版本失败')
  }
}

const handleAddTag = () => {
  if (newTag.value.trim() && currentCode.value) {
    if (!currentCode.value.tags) {
      currentCode.value.tags = []
    }
    if (!currentCode.value.tags.includes(newTag.value.trim())) {
      currentCode.value.tags.push(newTag.value.trim())
    }
    newTag.value = ''
  }
  showTagInput.value = false
}

const handleRemoveTag = (tag: string) => {
  if (currentCode.value?.tags) {
    const index = currentCode.value.tags.indexOf(tag)
    if (index > -1) {
      currentCode.value.tags.splice(index, 1)
    }
  }
}

onMounted(() => {
  handleRefreshList()
})
</script>

<style scoped>
.code-manager {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-lg);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.scan-input-group {
  display: flex;
  gap: var(--spacing-sm);
}

.scan-input {
  width: 400px;
}

.toolbar-right {
  display: flex;
  gap: var(--spacing-sm);
}

.preview-tag {
  font-size: 10px;
  padding: 2px 6px;
  background: var(--color-warning-muted);
  color: var(--color-warning);
  border-radius: var(--radius-sm);
  margin-left: var(--spacing-xs);
}

.main-content {
  flex: 1;
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: var(--spacing-lg);
  min-height: 0;
}

.list-panel {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-muted);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.title-text {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-text-primary);
}

.count-badge {
  font-size: var(--text-xs);
  font-weight: 600;
  padding: 2px 8px;
  background: var(--color-bg-muted);
  color: var(--color-text-secondary);
  border-radius: var(--radius-full);
}

.search-input {
  width: 160px;
}

.list-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-sm);
}

.code-item {
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 1px solid transparent;
  margin-bottom: var(--spacing-xs);
}

.code-item:hover {
  background: var(--color-bg-muted);
}

.code-item.active {
  background: var(--color-accent-subtle);
  border-color: var(--color-accent-primary);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
}

.item-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  min-width: 0;
}

.file-icon {
  font-size: 16px;
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.file-name {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.language-tag {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.item-preview {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--color-text-tertiary);
  background: var(--color-bg-sunken);
  padding: var(--spacing-sm);
  border-radius: var(--radius-sm);
  margin-bottom: var(--spacing-sm);
  overflow: hidden;
}

.item-preview code {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-footer {
  display: flex;
  gap: var(--spacing-lg);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 11px;
  color: var(--color-text-muted);
}

.panel-footer {
  padding: var(--spacing-md);
  border-top: 1px solid var(--color-border-muted);
  display: flex;
  justify-content: center;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-4xl);
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  color: var(--color-text-muted);
  margin-bottom: var(--spacing-lg);
}

.empty-text {
  font-size: var(--text-base);
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-xs);
}

.empty-hint {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

.detail-panel {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.detail-header {
  flex-shrink: 0;
}

.detail-info {
  flex-shrink: 0;
}

.detail-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.editor-content {
  flex: 1;
  overflow: hidden;
  min-height: 0;
}

.detail-collapse {
  flex-shrink: 0;
}

.detail-collapse :deep(.el-collapse-item__header) {
  padding-left: var(--spacing-lg);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.detail-collapse :deep(.el-collapse-item__content) {
  padding: 0 var(--spacing-lg) var(--spacing-md);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-muted);
}

.detail-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.detail-icon {
  font-size: 20px;
  color: var(--color-accent-primary);
}

.detail-name {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
}

.detail-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.detail-info {
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-muted);
}

.info-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-md);
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-label {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-tertiary);
  min-width: 48px;
}

.info-value {
  font-size: var(--text-sm);
  color: var(--color-text-primary);
}

.info-value.mono {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
  align-items: center;
}

.tag-input {
  width: 100px;
}

.detail-editor {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.editor-header {
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-muted);
}

.editor-title {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-secondary);
}

.editor-content {
  flex: 1;
  overflow: hidden;
}

.code-textarea {
  width: 100%;
  height: 100%;
  padding: var(--spacing-lg);
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  line-height: 1.6;
  color: var(--color-text-primary);
  background: var(--color-bg-sunken);
  border: none;
  resize: none;
  outline: none;
}

.code-textarea:focus {
  background: var(--color-bg-base);
}

.detail-dependencies {
  padding: var(--spacing-lg);
  border-top: 1px solid var(--color-border-muted);
  background: var(--color-bg-sunken);
}

.dependencies-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-md);
}

.dependencies-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.dependency-item {
  font-size: var(--text-xs);
  padding: var(--spacing-xs) var(--spacing-sm);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
}

.dep-type {
  color: var(--color-text-muted);
}

.dep-count {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  margin-left: var(--spacing-xs);
}

.empty-detail {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.empty-detail .empty-icon {
  font-size: 64px;
  color: var(--color-text-hint);
  margin-bottom: var(--spacing-xl);
}

.empty-detail .empty-title {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-sm);
}

.empty-detail .empty-hint {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

.file-explorer {
  padding: var(--spacing-md) 0;
}

.current-path {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  background: var(--color-bg-sunken);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-md);
}

.path-text {
  flex: 1;
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  color: var(--color-text-primary);
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.folder-icon {
  color: var(--color-warning);
}

.ai-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-4xl);
  text-align: center;
}

.ai-icon {
  font-size: 64px;
  color: var(--color-accent-primary);
  margin-bottom: var(--spacing-xl);
}

.ai-placeholder h3 {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-sm);
}

.ai-placeholder p {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  margin-bottom: var(--spacing-xl);
}

.ai-features {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.feature-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.feature-item .el-icon {
  color: var(--color-success);
}
</style>
