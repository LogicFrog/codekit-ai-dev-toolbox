import { defineStore } from 'pinia'
import { ref, reactive, computed } from 'vue'
import type { CodeCategory, CodeSnippet, FsItem } from '@/types'
import {
  getAllCodeSnippets, listCategories, saveCodeSnippet, deleteCodeSnippet,
  saveCodeSnippetByPath, createCategory, renameCategory, deleteCategory,
  scanLocalCode, getScanStatus, createVersion, listCodeDependencies, assignCategory
} from '@/api/code'
import { listFs } from '@/api/system'
import { ElMessageBox } from 'element-plus'
import { extractErrorMessage } from '@/utils/helpers'

export const useCodeManagerStore = defineStore('codeManager', () => {
  const scanDir = ref('')
  const searchKeyword = ref('')
  const scanning = ref(false)
  const editorTheme = ref<'vs-dark' | 'vs-light'>('vs-light')
  const codeList = ref<CodeSnippet[]>([])
  const categories = ref<CodeCategory[]>([])
  const currentCode = ref<CodeSnippet | null>(null)
  const total = ref(0)
  const expandedCategoryIds = ref<number[]>([])
  const currentPath = ref('/')
  const scanPath = ref('/')
  const fileList = ref<FsItem[]>([])
  const scanFileList = ref<FsItem[]>([])
  const showImportDialog = ref(false)
  const showCreateCategoryDialog = ref(false)
  const showFileExplorer = ref(false)
  const showScanExplorer = ref(false)
  const showTagInput = ref(false)
  const newTag = ref('')
  const newCategoryName = ref('')
  const editingCategory = ref<CodeCategory | null>(null)
  const selectedDetailCategoryId = ref<number | null>(null)
  const lockImportCategory = ref(false)

  const loading = reactive({
    list: false,
    save: false,
    import: false,
    files: false,
    scanFiles: false,
    categories: false
  })

  const importForm = reactive({
    filePath: '',
    languageType: '',
    tag: '',
    categoryId: undefined as number | undefined
  })

  const canGoUp = computed(() => currentPath.value !== '/' && currentPath.value !== '')
  const importCategoryLocked = computed(() => lockImportCategory.value)

  const isUncategorizedSnippet = (snippet: CodeSnippet) =>
    !snippet.category?.id || snippet.category?.categoryName === '未分类'

  const categoryFolders = computed(() => {
    return categories.value
      .filter(c => c.categoryName !== '未分类')
      .map(category => ({
        category,
        items: codeList.value.filter(item => item.category?.id === category.id)
      }))
  })

  const uncategorizedSnippets = computed(() => codeList.value.filter(isUncategorizedSnippet))

  const isFolderExpanded = (categoryId: number) => expandedCategoryIds.value.includes(categoryId)

  function toggleFolder(categoryId: number) {
    if (isFolderExpanded(categoryId)) {
      expandedCategoryIds.value = expandedCategoryIds.value.filter(id => id !== categoryId)
    } else {
      expandedCategoryIds.value = [...expandedCategoryIds.value, categoryId]
    }
  }

  async function refreshList() {
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

  async function refreshCategories() {
    loading.categories = true
    try {
      categories.value = await listCategories()
    } catch (error) {
      ElMessage.error(extractErrorMessage(error, '加载分类失败'))
    } finally {
      loading.categories = false
    }
  }

  async function selectCode(code: CodeSnippet) {
    currentCode.value = { ...code, tags: [...(code.tags || [])] }
    selectedDetailCategoryId.value = code.category?.id ?? null
    try {
      currentCode.value.dependencies = await listCodeDependencies(code.id)
    } catch {
      currentCode.value.dependencies = []
    }
  }

  let pendingScanCategory: number | undefined

  async function doScan(categoryId?: number) {
    if (!scanDir.value) {
      return
    }
    scanning.value = true
    try {
      await scanLocalCode(scanDir.value)
      const pollInterval = setInterval(async () => {
        try {
          const status = await getScanStatus(scanDir.value)
          if (status.status === 'COMPLETED') {
            clearInterval(pollInterval)
            scanning.value = false
            await Promise.all([refreshCategories(), refreshList()])
            if (categoryId) {
              for (const item of codeList.value) {
                if (!item.category?.id) {
                  try { await assignCategory(item.id, categoryId) } catch { /* ignore */ }
                }
              }
              await refreshList()
            }
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

  async function doImport() {
    if (!importForm.filePath) {
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
      showImportDialog.value = false
      resetImportForm()
      await Promise.all([refreshCategories(), refreshList()])
    } catch (error) {
      ElMessage.error(extractErrorMessage(error, '导入失败'))
    } finally {
      loading.import = false
    }
  }

  async function saveCode() {
    if (!currentCode.value) return
    loading.save = true
    try {
      const payload: Partial<CodeSnippet> = {
        ...currentCode.value,
        category: selectedDetailCategoryId.value
          ? { id: selectedDetailCategoryId.value, categoryName: '' }
          : undefined
      }
      const saved = await saveCodeSnippet(payload)
      currentCode.value = { ...saved, dependencies: currentCode.value.dependencies || [] }
      selectedDetailCategoryId.value = saved.category?.id ?? null
      await Promise.all([refreshCategories(), refreshList()])
    } catch (error) {
      ElMessage.error(extractErrorMessage(error, '保存失败'))
    } finally {
      loading.save = false
    }
  }

  async function removeCode() {
    if (!currentCode.value) return
    try {
      await ElMessageBox.confirm('确定要删除该代码片段吗？', '确认删除', { type: 'warning' })
      await deleteCodeSnippet(currentCode.value.id)
      currentCode.value = null
      await Promise.all([refreshCategories(), refreshList()])
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error(extractErrorMessage(error, '删除失败'))
      }
    }
  }

  async function doCreateVersion() {
    if (!currentCode.value) return
    try {
      await createVersion(currentCode.value.id, {
        versionName: `v${Date.now()}`,
        description: '手动创建版本'
      })
    } catch (error) {
      ElMessage.error(extractErrorMessage(error, '创建版本失败'))
    }
  }

  async function doCreateCategory() {
    const name = newCategoryName.value.trim()
    if (!name) {
      return
    }
    loading.categories = true
    const isEditing = !!editingCategory.value
    const editingId = editingCategory.value?.id
    try {
      const category = isEditing
        ? await renameCategory(editingId as number, name)
        : await createCategory(name)
      await refreshCategories()
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

  async function doDeleteCategory(category: CodeCategory) {
    try {
      await ElMessageBox.confirm(
        `确定删除分类"${category.categoryName}"吗？`,
        '确认删除',
        { type: 'warning' }
      )
      await deleteCategory(category.id)
      expandedCategoryIds.value = expandedCategoryIds.value.filter(id => id !== category.id)
      if (selectedDetailCategoryId.value === category.id) {
        selectedDetailCategoryId.value = null
        if (currentCode.value) currentCode.value.category = null
      }
      await Promise.all([refreshCategories(), refreshList()])
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error(extractErrorMessage(error, '删除分类失败'))
      }
    }
  }

  async function loadDirectory(path: string) {
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

  async function loadScanDirectory(path: string) {
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

  function addTag() {
    const value = newTag.value.trim()
    if (value && currentCode.value) {
      currentCode.value.tags = [...new Set([...(currentCode.value.tags || []), value])]
    }
    newTag.value = ''
    showTagInput.value = false
  }

  function removeTag(tag: string) {
    if (currentCode.value?.tags) {
      currentCode.value.tags = currentCode.value.tags.filter(item => item !== tag)
    }
  }

  function resetImportForm() {
    importForm.filePath = ''
    importForm.languageType = ''
    importForm.tag = ''
    importForm.categoryId = undefined
    lockImportCategory.value = false
  }

  function openImportDialog(categoryId?: number) {
    resetImportForm()
    if (typeof categoryId === 'number') {
      importForm.categoryId = categoryId
      lockImportCategory.value = true
    }
    showImportDialog.value = true
  }

  function closeCategoryDialog() {
    showCreateCategoryDialog.value = false
    editingCategory.value = null
    newCategoryName.value = ''
  }

  return {
    scanDir, searchKeyword, scanning, editorTheme, codeList, categories,
    currentCode, total, expandedCategoryIds, currentPath, scanPath,
    fileList, scanFileList, showImportDialog, showCreateCategoryDialog,
    showFileExplorer, showScanExplorer, showTagInput, newTag, newCategoryName,
    editingCategory, selectedDetailCategoryId, lockImportCategory,
    loading, importForm, canGoUp, importCategoryLocked,
    categoryFolders, uncategorizedSnippets,
    isFolderExpanded, toggleFolder,
    refreshList, refreshCategories, selectCode,
    doScan, doImport, saveCode, removeCode, doCreateVersion,
    doCreateCategory, doDeleteCategory,
    loadDirectory, loadScanDirectory,
    addTag, removeTag,
    resetImportForm, openImportDialog, closeCategoryDialog
  }
})
