import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import type { VersionInfo, CodeSnippet, VersionDiffResponse, VersionAnalyzeResponse } from '@/types'
import { listVersions, createVersion, rollbackVersion, compareVersionsApi, analyzeVersionsApi } from '@/api/version'
import { getAllCodeSnippets } from '@/api/code'
import { ElMessage, ElMessageBox } from 'element-plus'
import { extractErrorMessage } from '@/utils/helpers'

export const useVersionControlStore = defineStore('versionControl', () => {
  const loading = ref(false)
  const snippetsList = ref<CodeSnippet[]>([])
  const versionsList = ref<VersionInfo[]>([])
  const selectedSnippetId = ref<number | null>(null)
  const versionA = ref<number | null>(null)
  const versionB = ref<number | null>(null)
  const renderSideBySide = ref(true)
  const diffResult = ref<{ added: number; removed: number; modifications: number } | null>(null)
  const serverDiff = ref<VersionDiffResponse | null>(null)
  const diffEditorRef = ref<any>(null)
  const showCreateDialog = ref(false)
  const showAnalyzeDialog = ref(false)
  const analyzeResult = ref<VersionAnalyzeResponse | null>(null)
  const analyzing = ref(false)
  const creating = ref(false)
  const createVersionForm = ref({ versionName: '', description: '' })

  const originalVersion = computed(() => {
    if (!versionA.value) return null
    return versionsList.value.find(v => v.id === versionA.value) || null
  })

  const modifiedVersion = computed(() => {
    if (!versionB.value) return null
    return versionsList.value.find(v => v.id === versionB.value) || null
  })

  function detectLanguage(version: VersionInfo): string {
    const snippet = snippetsList.value.find(s => s.id === version.snippetId)
    return snippet?.languageType || 'plaintext'
  }

  async function fetchSnippets() {
    loading.value = true
    try {
      snippetsList.value = await getAllCodeSnippets()
    } catch (error) {
      ElMessage.error(extractErrorMessage(error, '加载代码片段失败'))
    } finally {
      loading.value = false
    }
  }

  async function fetchVersions(snippetId: number) {
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
      ElMessage.error(extractErrorMessage(error, '加载版本列表失败'))
    } finally {
      loading.value = false
    }
  }

  function handleSnippetChange(snippetId: number | null) {
    if (snippetId) {
      fetchVersions(snippetId)
    } else {
      versionsList.value = []
      versionA.value = null
      versionB.value = null
    }
  }

  function swapVersions() {
    const temp = versionA.value
    versionA.value = versionB.value
    versionB.value = temp
  }

  async function loadServerDiff() {
    if (!selectedSnippetId.value || !versionA.value || !versionB.value) {
      serverDiff.value = null
      return
    }
    try {
      serverDiff.value = await compareVersionsApi(selectedSnippetId.value, versionA.value, versionB.value)
    } catch (error) {
      ElMessage.error(extractErrorMessage(error, '后端差异分析失败'))
    }
  }

  async function doRollback() {
    if (!selectedSnippetId.value || !versionA.value) return
    try {
      await ElMessageBox.confirm(
        '回滚会覆盖当前代码内容，是否继续？',
        '确认回滚',
        { type: 'warning' }
      )
      await rollbackVersion(selectedSnippetId.value, versionA.value)
      ElMessage.success('回滚成功')
      await fetchVersions(selectedSnippetId.value)
    } catch (error) {
      if (error === 'cancel' || error === 'close') return
      ElMessage.error(extractErrorMessage(error, '回滚失败'))
    }
  }

  async function doAiAnalyze() {
    if (!selectedSnippetId.value || !versionA.value || !versionB.value) {
      ElMessage.warning('请先选择两个版本')
      return
    }
    analyzing.value = true
    try {
      analyzeResult.value = await analyzeVersionsApi(selectedSnippetId.value, {
        fromVersionId: versionA.value,
        toVersionId: versionB.value
      })
      showAnalyzeDialog.value = true
    } catch (error) {
      ElMessage.error(extractErrorMessage(error, 'AI 分析失败'))
    } finally {
      analyzing.value = false
    }
  }

  async function doCreateVersion() {
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
      ElMessage.error(extractErrorMessage(error, '创建版本失败'))
    } finally {
      creating.value = false
    }
  }

  return {
    loading, snippetsList, versionsList, selectedSnippetId,
    versionA, versionB, renderSideBySide, diffResult, serverDiff,
    diffEditorRef, showCreateDialog, showAnalyzeDialog,
    analyzeResult, analyzing, creating, createVersionForm,
    originalVersion, modifiedVersion,
    detectLanguage, fetchSnippets, fetchVersions,
    handleSnippetChange, swapVersions, loadServerDiff,
    doRollback, doAiAnalyze, doCreateVersion
  }
})
