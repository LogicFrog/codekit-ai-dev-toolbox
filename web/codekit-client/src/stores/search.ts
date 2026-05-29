import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import type { SearchResponse, SearchHistory, CodeSnippet } from '@/types'
import { keywordSearch, semanticSearch, getHotKeywords, getSearchHistory } from '@/api/search'
import { getAllCodeSnippets, getCodeSnippet } from '@/api/code'
import { ElMessage } from 'element-plus'
import { extractErrorMessage, copyToClipboard } from '@/utils/helpers'

export const useSearchStore = defineStore('search', () => {
  const searchForm = reactive({
    keyword: '',
    searchType: 'keyword' as string,
    language: '',
    tag: '',
    exactMatch: false
  })

  const searching = ref(false)
  const hasSearched = ref(false)
  const searchResults = ref<SearchResponse[]>([])
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(10)
  const showDetailDrawer = ref(false)
  const selectedResult = ref<SearchResponse | null>(null)
  const fullCodeContent = ref('')
  const loadingFullCode = ref(false)
  const copying = ref(false)

  const hotKeywords = ref<string[]>([])
  const recentSearches = ref<SearchHistory[]>([])
  const availableTags = ref<string[]>([])

  async function doSearch() {
    if (!searchForm.keyword.trim() && !searchForm.language && !searchForm.tag) {
      ElMessage.warning('请输入搜索关键词或选择语言/标签')
      return
    }
    currentPage.value = 1
    searching.value = true
    hasSearched.value = true

    try {
      const api = searchForm.searchType === 'semantic' ? semanticSearch : keywordSearch
      const result = await api({
        keyword: searchForm.keyword || undefined,
        languageType: searchForm.language || undefined,
        tag: searchForm.tag || undefined,
        exactMatch: searchForm.exactMatch,
        page: currentPage.value - 1,
        size: pageSize.value
      })
      searchResults.value = result.content || []
      total.value = result.totalElements || 0
    } catch (error) {
      ElMessage.error(extractErrorMessage(error, '搜索失败'))
      searchResults.value = []
      total.value = 0
    } finally {
      searching.value = false
    }
  }

  function quickSearch(keyword: string) {
    searchForm.keyword = keyword
    currentPage.value = 1
    doSearch()
  }

  function resetSearch() {
    searchForm.keyword = ''
    searchForm.language = ''
    searchForm.tag = ''
    searchForm.exactMatch = false
    hasSearched.value = false
    searchResults.value = []
    total.value = 0
  }

  async function viewResult(result: SearchResponse) {
    selectedResult.value = result
    showDetailDrawer.value = true
    loadingFullCode.value = true
    try {
      const snippet = await getCodeSnippet(result.id)
      fullCodeContent.value = snippet.codeContent
    } catch (error) {
      fullCodeContent.value = result.codePreview || ''
    } finally {
      loadingFullCode.value = false
    }
  }

  async function copyResultCode() {
    if (!selectedResult.value) return
    copying.value = true
    try {
      const code = fullCodeContent.value || selectedResult.value.codePreview || ''
      const ok = await copyToClipboard(code)
      if (ok) {
        ElMessage.success('代码已复制')
      } else {
        ElMessage.error('复制失败')
      }
    } finally {
      copying.value = false
    }
  }

  async function loadInitialData() {
    try {
      const [hot, history, allSnippets] = await Promise.all([
        getHotKeywords().catch(() => [] as string[]),
        getSearchHistory().catch(() => [] as SearchHistory[]),
        getAllCodeSnippets().catch(() => [] as CodeSnippet[])
      ])
      hotKeywords.value = (Array.isArray(hot) ? hot : []).slice(0, 6)
      recentSearches.value = (Array.isArray(history) ? history : []).slice(0, 5)
      const tags = new Set<string>()
      ;(Array.isArray(allSnippets) ? allSnippets : []).forEach((s: CodeSnippet) => {
        s.tags?.forEach(t => tags.add(t))
      })
      availableTags.value = Array.from(tags)
    } catch {
      // ignore
    }
  }

  return {
    searchForm, searching, hasSearched, searchResults, total,
    currentPage, pageSize, showDetailDrawer, selectedResult,
    fullCodeContent, loadingFullCode, copying,
    hotKeywords, recentSearches, availableTags,
    doSearch, quickSearch, resetSearch, viewResult, copyResultCode, loadInitialData
  }
})
