<template>
  <div class="code-manager">
    <section class="toolbar">
      <div class="toolbar-primary">
        <div class="scan-input-group">
          <el-input
            v-model="scanDir"
            placeholder="输入本地代码目录路径..."
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

      <div class="toolbar-actions">
        <el-button @click="showAIDrawer = true" disabled>
          <el-icon><ChatDotRound /></el-icon>
          AI 助手
          <span class="preview-tag">预留</span>
        </el-button>
      </div>
    </section>

    <div class="main-content">
      <section class="list-panel">
        <div class="panel-header">
          <div class="panel-tools">
            <el-tooltip content="新建分类" placement="top">
              <el-button circle size="small" class="panel-icon-btn" @click="openCreateCategoryDialog">
                <el-icon><FolderAdd /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="导入文件" placement="top">
              <el-button circle size="small" class="panel-icon-btn" @click="openImportDialog()">
                <el-icon><Upload /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
          <div class="panel-tools panel-tools-right">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索代码内容..."
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
        </div>

        <div class="list-content" v-loading="loading.list">
          <template v-for="folder in categoryFolders" :key="`folder-${folder.category.id}`">
            <div class="tree-node folder-node" :class="{ expanded: isFolderExpanded(folder.category.id) }">
              <button class="folder-row" @click="toggleFolder(folder.category.id)">
                <div class="folder-row-main">
                  <el-icon class="folder-caret" :class="{ expanded: isFolderExpanded(folder.category.id) }">
                    <ArrowRightBold />
                  </el-icon>
                  <el-icon class="folder-row-icon"><Folder /></el-icon>
                  <span class="folder-row-name">{{ folder.category.categoryName }}</span>
                </div>
              </button>

              <div class="node-actions">
                <el-button
                  text
                  size="small"
                  class="node-action"
                  @click.stop="openImportDialog(folder.category.id)"
                >
                  <el-icon><Upload /></el-icon>
                </el-button>
                <el-button
                  text
                  size="small"
                  class="node-action"
                  @click.stop="openRenameCategoryDialog(folder.category)"
                >
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button
                  v-if="folder.category.categoryName !== '未分类'"
                  text
                  size="small"
                  class="node-action danger"
                  @click.stop="handleDeleteCategory(folder.category)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>

            <div v-if="isFolderExpanded(folder.category.id)" class="folder-children">
              <div v-if="folder.items.length === 0" class="folder-empty">空文件夹</div>
              <div
                v-for="code in folder.items"
                :key="code.id"
                class="tree-node file-node nested"
                :class="{ active: currentCode?.id === code.id }"
                @click="handleSelectCode(code)"
              >
                <div class="file-row-main">
                  <el-icon class="file-icon"><Document /></el-icon>
                  <span class="file-name">{{ code.fileName }}</span>
                </div>
                <div class="file-row-meta">
                  <span
                    class="language-tag"
                    :style="{ backgroundColor: getLanguageColor(code.languageType) + '20', color: getLanguageColor(code.languageType) }"
                  >
                    {{ code.languageType }}
                  </span>
                </div>
              </div>
            </div>
          </template>

          <div
            v-for="code in uncategorizedSnippets"
            :key="code.id"
            class="tree-node file-node"
            :class="{ active: currentCode?.id === code.id }"
            @click="handleSelectCode(code)"
          >
            <div class="file-row-main">
              <el-icon class="file-icon"><Document /></el-icon>
              <span class="file-name">{{ code.fileName }}</span>
            </div>
            <div class="file-row-meta">
              <span class="category-badge uncategorized-badge">未分类</span>
              <span
                class="language-tag"
                :style="{ backgroundColor: getLanguageColor(code.languageType) + '20', color: getLanguageColor(code.languageType) }"
              >
                {{ code.languageType }}
              </span>
            </div>
          </div>

          <div v-if="!loading.list && total === 0 && categories.length === 0" class="empty-state">
            <el-icon class="empty-icon"><Document /></el-icon>
            <p class="empty-text">暂无代码片段</p>
          </div>
        </div>
      </section>

      <section class="detail-panel" v-if="currentCode">
        <div class="detail-header">
          <div class="detail-title-wrap">
            <div class="detail-title">
              <el-icon class="detail-icon"><Document /></el-icon>
              <span class="detail-name">{{ currentCode.fileName }}</span>
            </div>
            <div class="detail-meta">
              <span class="category-badge detail-badge">{{ getCategoryName(currentCode) }}</span>
              <span class="detail-updated">更新于 {{ formatRelativeTime(currentCode.updateTime) }}</span>
            </div>
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
              <div class="info-grid">
                <div class="info-card info-card-wide">
                  <span class="info-label">路径</span>
                  <span class="info-value mono">{{ currentCode.filePath }}</span>
                </div>

                <div class="info-card">
                  <span class="info-label">语言</span>
                  <span
                    class="language-tag"
                    :style="{ backgroundColor: getLanguageColor(currentCode.languageType) + '20', color: getLanguageColor(currentCode.languageType) }"
                  >
                    {{ currentCode.languageType }}
                  </span>
                </div>

                <div class="info-card">
                  <span class="info-label">分类</span>
                  <el-select
                    v-model="selectedDetailCategoryId"
                    placeholder="不指定则归入未分类"
                    clearable
                    class="category-select"
                    @change="handleDetailCategoryChange"
                  >
                    <el-option
                      v-for="category in categories"
                      :key="category.id"
                      :label="category.categoryName"
                      :value="category.id"
                    />
                  </el-select>
                </div>
              </div>

              <div class="info-row tags-row">
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
                  />
                  <el-button v-else size="small" text @click="showTagInput = true">
                    <el-icon><Plus /></el-icon>
                    添加标签
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
          <CodeEditor
            :key="currentCode.id"
            v-model="currentCode.codeContent"
            :language="currentCode.languageType"
            :theme="editorTheme"
          />
        </div>
      </section>

      <div class="empty-detail" v-else>
        <el-icon class="empty-icon"><Document /></el-icon>
        <p class="empty-title">选择一个代码片段</p>
        <p class="empty-hint">左侧支持按分类查看，右侧可直接调整分类与标签</p>
      </div>
    </div>

    <el-dialog
      v-model="showImportDialog"
      title="导入文件"
      width="520px"
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
        <el-form-item label="代码分类">
          <el-select
            v-model="importForm.categoryId"
            placeholder="可选，不选则归入未分类"
            :disabled="importCategoryLocked"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.categoryName"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="importForm.tag" placeholder="可选，多个标签用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" @click="handleImport" :loading="loading.import">导入</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showCreateCategoryDialog"
      :title="editingCategory ? '重命名分类' : '新建分类'"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form label-width="72px" label-position="left">
        <el-form-item label="分类名称">
          <el-input
            v-model="newCategoryName"
            placeholder="例如：后端、数据库、工具类"
            maxlength="100"
            show-word-limit
            @keyup.enter="handleCreateCategory"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeCategoryDialog">取消</el-button>
        <el-button type="primary" :loading="loading.categories" @click="handleCreateCategory">
          {{ editingCategory ? '保存' : '创建' }}
        </el-button>
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
              <el-button v-if="row.isDirectory" text size="small" @click="loadDirectory(row.path)">进入</el-button>
              <el-button v-else text size="small" type="primary" @click="handleSelectFile(row)">选择</el-button>
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
              <el-button v-if="row.isDirectory" text size="small" @click="loadScanDirectory(row.path)">进入</el-button>
              <el-tag v-else size="small" type="info">文件</el-tag>
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
        <p>后续这里会优先补上未分类代码自动归类能力。</p>
        <div class="ai-features">
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>未分类代码智能归类</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>代码解释与优化建议</span>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowUp,
  ArrowRightBold,
  ChatDotRound,
  Check,
  Delete,
  Document,
  Edit,
  Folder,
  FolderAdd,
  FolderOpened,
  Plus,
  Search,
  Upload,
  Clock
} from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import CodeEditor from '@/components/CodeEditor.vue'
import { listFs } from '@/api/system'
import {
  createCategory,
  createVersion,
  deleteCategory,
  deleteCodeSnippet,
  getAllCodeSnippets,
  getScanStatus,
  listCategories,
  listCodeDependencies,
  renameCategory,
  saveCodeSnippet,
  saveCodeSnippetByPath,
  scanLocalCode
} from '@/api/code'
import type { CodeCategory, CodeSnippet, FsItem } from '@/types'
import {
  extractErrorMessage,
  formatRelativeTime,
  getLanguageColor
} from '@/utils/helpers'

const router = useRouter()

const scanDir = ref('')
const scanPath = ref('/')
const searchKeyword = ref('')
const scanning = ref(false)
const editorTheme = ref<'vs-dark' | 'vs-light'>('vs-light')
const showImportDialog = ref(false)
const showCreateCategoryDialog = ref(false)
const showFileExplorer = ref(false)
const showScanExplorer = ref(false)
const showAIDrawer = ref(false)
const showTagInput = ref(false)
const newTag = ref('')
const newCategoryName = ref('')
const editingCategory = ref<CodeCategory | null>(null)
const activeDetails = ref<string[]>(['info'])
const currentPath = ref('/')
const selectedDetailCategoryId = ref<number | null>(null)
const expandedCategoryIds = ref<number[]>([])
const lockImportCategory = ref(false)

const loading = reactive({
  list: false,
  save: false,
  import: false,
  files: false,
  scanFiles: false,
  categories: false
})

const codeList = ref<CodeSnippet[]>([])
const categories = ref<CodeCategory[]>([])
const fileList = ref<FsItem[]>([])
const scanFileList = ref<FsItem[]>([])
const currentCode = ref<CodeSnippet | null>(null)
const total = ref(0)

const importForm = reactive({
  filePath: '',
  languageType: '',
  tag: '',
  categoryId: undefined as number | undefined
})

const canGoUp = computed(() => currentPath.value !== '/' && currentPath.value !== '')
const importCategoryLocked = computed(() => lockImportCategory.value)

const getCategoryName = (snippet: CodeSnippet) => snippet.category?.categoryName || '未分类'
const isUncategorizedSnippet = (snippet: CodeSnippet) => !snippet.category?.id || snippet.category?.categoryName === '未分类'

const categoryFolders = computed(() => {
  return categories.value
    .filter(category => category.categoryName !== '未分类')
    .map(category => ({
      category,
      items: codeList.value.filter(item => item.category?.id === category.id)
    }))
})

const uncategorizedSnippets = computed(() => {
  return codeList.value.filter(isUncategorizedSnippet)
})

const isFolderExpanded = (categoryId: number) => expandedCategoryIds.value.includes(categoryId)

const toggleFolder = (categoryId: number) => {
  if (isFolderExpanded(categoryId)) {
    expandedCategoryIds.value = expandedCategoryIds.value.filter(id => id !== categoryId)
  } else {
    expandedCategoryIds.value = [...expandedCategoryIds.value, categoryId]
  }
}

const handleRefreshList = async () => {
  loading.list = true
  try {
    const result = await getAllCodeSnippets()
    codeList.value = [...result].sort((a, b) => {
      const left = new Date(b.updateTime || b.createTime || 0).getTime()
      const right = new Date(a.updateTime || a.createTime || 0).getTime()
      return left - right
    })
    total.value = codeList.value.length

    if (currentCode.value) {
      const matched = codeList.value.find(item => item.id === currentCode.value?.id)
      if (matched) {
        currentCode.value = { ...matched, dependencies: currentCode.value.dependencies || [] }
        selectedDetailCategoryId.value = matched.category?.id ?? null
      }
    }
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '加载代码列表失败'))
    codeList.value = []
    total.value = 0
  } finally {
    loading.list = false
  }
}

const handleRefreshCategories = async () => {
  loading.categories = true
  try {
    categories.value = await listCategories()
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '加载分类失败'))
  } finally {
    loading.categories = false
  }
}

const openCreateCategoryDialog = () => {
  editingCategory.value = null
  newCategoryName.value = ''
  showCreateCategoryDialog.value = true
}

const openRenameCategoryDialog = (category: CodeCategory) => {
  editingCategory.value = category
  newCategoryName.value = category.categoryName
  showCreateCategoryDialog.value = true
}

const closeCategoryDialog = () => {
  showCreateCategoryDialog.value = false
  editingCategory.value = null
  newCategoryName.value = ''
}

const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    return
  }
  router.push({
    path: '/search-center',
    query: { keyword: searchKeyword.value }
  })
}

const handleSelectCode = async (code: CodeSnippet) => {
  currentCode.value = {
    ...code,
    tags: [...(code.tags || [])]
  }
  selectedDetailCategoryId.value = code.category?.id ?? null
  try {
    currentCode.value.dependencies = await listCodeDependencies(code.id)
  } catch {
    currentCode.value.dependencies = []
  }
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
          scanning.value = false
          ElMessage.success('扫描完成')
          await Promise.all([handleRefreshCategories(), handleRefreshList()])
        } else if (status.status === 'FAILED') {
          clearInterval(pollInterval)
          scanning.value = false
          ElMessage.error('扫描失败')
        }
      } catch {
        clearInterval(pollInterval)
        scanning.value = false
      }
    }, 2000)
  } catch (error) {
    scanning.value = false
    ElMessage.error(extractErrorMessage(error, '扫描失败'))
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
    ElMessage.error(extractErrorMessage(error, '加载目录失败'))
  } finally {
    loading.scanFiles = false
  }
}

const handleScanGoUp = () => {
  const parts = scanPath.value.split('/').filter(Boolean)
  parts.pop()
  const parentPath = `/${parts.join('/')}`
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
    ElMessage.error(extractErrorMessage(error, '加载目录失败'))
  } finally {
    loading.files = false
  }
}

const handleGoUp = () => {
  const parts = currentPath.value.split('/').filter(Boolean)
  parts.pop()
  const parentPath = `/${parts.join('/')}`
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

const resetImportForm = () => {
  importForm.filePath = ''
  importForm.languageType = ''
  importForm.tag = ''
  importForm.categoryId = undefined
  lockImportCategory.value = false
}

const openImportDialog = (categoryId?: number) => {
  resetImportForm()
  if (typeof categoryId === 'number') {
    importForm.categoryId = categoryId
    lockImportCategory.value = true
  }
  showImportDialog.value = true
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
      importForm.languageType || undefined,
      importForm.tag || undefined,
      importForm.categoryId
    )
    ElMessage.success('导入成功')
    showImportDialog.value = false
    resetImportForm()
    await Promise.all([handleRefreshCategories(), handleRefreshList()])
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '导入失败'))
  } finally {
    loading.import = false
  }
}

const handleSaveCode = async () => {
  if (!currentCode.value) {
    return
  }

  loading.save = true
  try {
    const payload: Partial<CodeSnippet> = {
      ...currentCode.value,
      category: selectedDetailCategoryId.value
        ? { id: selectedDetailCategoryId.value, categoryName: '' }
        : undefined
    }
    const saved = await saveCodeSnippet(payload)
    currentCode.value = {
      ...saved,
      dependencies: currentCode.value.dependencies || []
    }
    selectedDetailCategoryId.value = saved.category?.id ?? null
    ElMessage.success('保存成功')
    await Promise.all([handleRefreshCategories(), handleRefreshList()])
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '保存失败'))
  } finally {
    loading.save = false
  }
}

const handleDeleteCode = async () => {
  if (!currentCode.value) {
    return
  }

  try {
    await ElMessageBox.confirm('确定要删除该代码片段吗？', '确认删除', { type: 'warning' })
    await deleteCodeSnippet(currentCode.value.id)
    currentCode.value = null
    ElMessage.success('删除成功')
    await Promise.all([handleRefreshCategories(), handleRefreshList()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(extractErrorMessage(error, '删除失败'))
    }
  }
}

const handleCreateVersion = async () => {
  if (!currentCode.value) {
    return
  }

  try {
    await createVersion(currentCode.value.id, {
      versionName: `v${Date.now()}`,
      description: '手动创建版本'
    })
    ElMessage.success('版本创建成功')
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '创建版本失败'))
  }
}

const handleAddTag = () => {
  const value = newTag.value.trim()
  if (value && currentCode.value) {
    currentCode.value.tags = [...new Set([...(currentCode.value.tags || []), value])]
  }
  newTag.value = ''
  showTagInput.value = false
}

const handleRemoveTag = (tag: string) => {
  if (!currentCode.value?.tags) {
    return
  }
  currentCode.value.tags = currentCode.value.tags.filter(item => item !== tag)
}

const handleDetailCategoryChange = (categoryId: number | null) => {
  if (!currentCode.value) {
    return
  }
  currentCode.value.category = categories.value.find(item => item.id === categoryId) || null
}

const handleCreateCategory = async () => {
  const categoryName = newCategoryName.value.trim()
  if (!categoryName) {
    ElMessage.warning('请输入分类名称')
    return
  }

  loading.categories = true
  const isEditing = !!editingCategory.value
  const editingCategoryId = editingCategory.value?.id
  try {
    const category = isEditing
      ? await renameCategory(editingCategoryId as number, categoryName)
      : await createCategory(categoryName)
    await handleRefreshCategories()
    ElMessage.success(isEditing ? '分类已重命名' : '分类创建成功')
    expandedCategoryIds.value = [...expandedCategoryIds.value, category.id]
    closeCategoryDialog()

    if (!isEditing && importForm.categoryId == null) {
      importForm.categoryId = category.id
    }
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, isEditing ? '重命名分类失败' : '创建分类失败'))
  } finally {
    loading.categories = false
  }
}

const handleDeleteCategory = async (category: CodeCategory) => {
  try {
    await ElMessageBox.confirm(
      `确定删除分类“${category.categoryName}”吗？该分类下的代码会自动归入未分类。`,
      '确认删除',
      { type: 'warning' }
    )
    await deleteCategory(category.id)
    expandedCategoryIds.value = expandedCategoryIds.value.filter(id => id !== category.id)
    if (selectedDetailCategoryId.value === category.id) {
      selectedDetailCategoryId.value = null
      if (currentCode.value) {
        currentCode.value.category = null
      }
    }
    ElMessage.success('分类已删除')
    await Promise.all([handleRefreshCategories(), handleRefreshList()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(extractErrorMessage(error, '删除分类失败'))
    }
  }
}

onMounted(async () => {
  await Promise.all([handleRefreshCategories(), handleRefreshList()])
})
</script>

<style scoped>
.code-manager {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.toolbar,
.list-panel,
.detail-panel,
.empty-detail {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xs);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: var(--spacing-xl);
  padding: var(--spacing-xl);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--color-bg-elevated) 90%, #ececec 10%) 0%, var(--color-bg-elevated) 48%, color-mix(in srgb, var(--color-bg-elevated) 82%, #f3f3f3 18%) 100%);
}

.toolbar-primary {
  flex: 1;
  min-width: 0;
}

.toolbar-actions,
.scan-input-group {
  display: flex;
  gap: var(--spacing-sm);
}

.scan-input {
  width: min(520px, 100%);
}

.preview-tag {
  margin-left: var(--spacing-xs);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border-default);
  color: var(--color-text-secondary);
  font-size: 10px;
}

.main-content {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: var(--spacing-lg);
}

.list-panel,
.detail-panel {
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.panel-header,
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--spacing-md);
  padding: var(--spacing-lg) var(--spacing-xl);
  border-bottom: 1px solid var(--color-border-muted);
}

.panel-anchor,
.panel-tools {
  display: flex;
  align-items: center;
}

.panel-tools {
  gap: var(--spacing-sm);
}

.panel-tools-right {
  margin-left: auto;
}

.panel-icon-btn {
  border-color: var(--color-border-default);
  background: var(--color-bg-elevated);
  color: var(--color-text-secondary);
}

.panel-icon-btn:hover {
  border-color: var(--color-border-strong);
  color: var(--color-text-primary);
  background: var(--color-bg-muted);
}

.detail-name {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
}

.detail-updated {
  margin: 6px 0 0;
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

.search-input {
  width: 180px;
}

.list-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-sm);
}

.tree-node {
  position: relative;
  margin-bottom: 2px;
}

.file-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-sm);
  min-height: 38px;
  padding: 0 12px 0 14px;
  border: 1px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.file-node:hover {
  background: var(--color-bg-muted);
}

.file-node.active {
  background: color-mix(in srgb, var(--color-accent-subtle) 82%, #fff 18%);
  border-color: var(--color-accent-primary);
}

.file-node.nested {
  margin-left: 28px;
}

.file-node.nested::before {
  content: '';
  position: absolute;
  left: -14px;
  top: -2px;
  bottom: -2px;
  width: 1px;
  background: var(--color-border-muted);
}

.folder-row {
  width: 100%;
  min-height: 38px;
  padding: 0 12px 0 10px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.folder-row:hover {
  background: var(--color-bg-muted);
}

.folder-row:focus-visible {
  outline: 2px solid var(--color-accent-primary);
  outline-offset: 1px;
}

.folder-row-main {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  min-width: 0;
}

.folder-caret {
  font-size: 12px;
  color: var(--color-text-muted);
  transition: transform var(--transition-fast);
}

.folder-caret.expanded {
  transform: rotate(90deg);
}

.folder-row-icon {
  color: var(--color-warning);
}

.folder-row-name {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text-primary);
}

.folder-children {
  margin-bottom: 4px;
}

.folder-empty {
  margin: 2px 0 var(--spacing-sm) 28px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  color: var(--color-text-muted);
  font-size: 12px;
  background: color-mix(in srgb, var(--color-bg-sunken) 72%, var(--color-bg-elevated) 28%);
  border: 1px dashed var(--color-border-muted);
}

.folder-node {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.node-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.folder-node:hover .node-actions,
.folder-node.expanded .node-actions {
  opacity: 1;
}

.node-action {
  color: var(--color-text-muted);
}

.node-action:hover {
  color: var(--color-text-primary);
}

.node-action.danger:hover {
  color: #b42318;
}

.item-header,
.item-footer,
.item-category-row,
.item-title,
.detail-title,
.detail-meta,
.detail-actions,
.file-name-cell,
.current-path,
.dependencies-list,
.tags-container,
.feature-item {
  display: flex;
  align-items: center;
}

.item-title,
.detail-title,
.detail-meta,
.detail-actions,
.file-name-cell,
.current-path,
.dependencies-list,
.tags-container,
.feature-item {
  gap: var(--spacing-sm);
}

.item-path {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-muted);
  font-size: 11px;
}

.file-icon,
.detail-icon {
  flex-shrink: 0;
}

.file-icon {
  color: var(--color-text-tertiary);
}

.detail-icon {
  font-size: 18px;
  color: var(--color-accent-primary);
}

.file-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-primary);
  font-size: var(--text-sm);
  font-weight: 600;
}

.file-row-main,
.file-row-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  min-width: 0;
}

.file-row-main {
  flex: 1;
}

.file-row-meta {
  flex-shrink: 0;
}

.language-tag,
.category-badge {
  display: inline-flex;
  align-items: center;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
}

.language-tag {
  padding: 3px 10px;
}

.category-badge {
  padding: 4px 10px;
  background: var(--color-bg-muted);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border-default);
}

.uncategorized-badge {
  background: color-mix(in srgb, var(--color-bg-muted) 70%, #f7f7f7 30%);
}

.detail-badge {
  background: color-mix(in srgb, var(--color-accent-subtle) 70%, #fff 30%);
}

.detail-title-wrap {
  min-width: 0;
}

.detail-info {
  padding: var(--spacing-lg) var(--spacing-xl);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--spacing-md);
}

.info-card {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  border-radius: var(--radius-lg);
  background: var(--color-bg-sunken);
  border: 1px solid var(--color-border-muted);
}

.info-card-wide {
  grid-column: 1 / -1;
}

.info-label {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text-tertiary);
}

.info-value {
  color: var(--color-text-primary);
  font-size: var(--text-sm);
}

.mono {
  font-family: var(--font-mono);
  word-break: break-all;
}

.category-select {
  width: 100%;
}

.tags-row {
  margin-top: var(--spacing-lg);
  align-items: flex-start;
}

.tag-input {
  width: 140px;
}

.detail-collapse :deep(.el-collapse-item__header) {
  padding-left: var(--spacing-xl);
  color: var(--color-text-secondary);
}

.detail-collapse :deep(.el-collapse-item__content) {
  padding-bottom: var(--spacing-md);
}

.detail-editor {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  border-top: 1px solid var(--color-border-muted);
}

.dependency-item {
  padding: 6px 10px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-default);
  background: var(--color-bg-elevated);
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
}

.dep-type {
  color: var(--color-text-muted);
}

.empty-state,
.empty-detail,
.ai-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.empty-state {
  min-height: 260px;
  padding: var(--spacing-3xl);
}

.empty-detail {
  border-style: dashed;
  min-height: 460px;
}

.empty-icon,
.ai-icon {
  color: var(--color-text-hint);
}

.empty-icon {
  font-size: 54px;
}

.empty-title {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: var(--spacing-lg) 0 var(--spacing-sm);
}

.empty-text {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: var(--spacing-md) 0 0;
}

.ai-placeholder p {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

.file-explorer {
  padding: var(--spacing-sm) 0;
}

.current-path {
  margin-bottom: var(--spacing-md);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  background: var(--color-bg-sunken);
}

.path-text {
  flex: 1;
  min-width: 0;
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  color: var(--color-text-primary);
}

.folder-icon {
  color: var(--color-warning);
}

.ai-placeholder {
  height: 100%;
  padding: var(--spacing-4xl);
}

.ai-icon {
  font-size: 64px;
  margin-bottom: var(--spacing-xl);
}

.ai-placeholder h3 {
  margin: 0 0 var(--spacing-sm);
  color: var(--color-text-primary);
  font-size: var(--text-lg);
}

.ai-features {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  margin-top: var(--spacing-lg);
}

.feature-item {
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
}

.feature-item .el-icon {
  color: var(--color-success);
}

@media (max-width: 1120px) {
  .main-content {
    grid-template-columns: 1fr;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-actions {
    flex-wrap: wrap;
  }
}

@media (max-width: 720px) {
  .scan-input-group,
  .panel-header,
  .detail-header {
    flex-direction: column;
    align-items: stretch;
  }

  .panel-tools {
    width: 100%;
  }

  .scan-input,
  .search-input {
    width: 100%;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
