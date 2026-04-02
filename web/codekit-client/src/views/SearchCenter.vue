<template>
  <div class="search-center">
    <div class="search-hero">
      <div class="search-container">
        <h1 class="search-title">代码检索</h1>
        <p class="search-subtitle">快速定位您的代码片段</p>
        
        <div class="search-box">
          <el-input
            v-model="searchForm.keyword"
            placeholder="输入关键词搜索代码..."
            size="large"
            clearable
            @keyup.enter="handleSearch"
            class="search-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" size="large" @click="handleSearch" :loading="searching">
            搜索
          </el-button>
        </div>

        <div class="search-options">
          <div class="option-group">
            <span class="option-label">搜索类型</span>
            <el-radio-group v-model="searchForm.searchType" size="default">
              <el-radio-button value="keyword">关键词</el-radio-button>
              <el-radio-button value="semantic" disabled>
                语义搜索
                <el-tooltip content="语义搜索功能开发中" placement="top">
                  <el-icon class="lock-icon"><Lock /></el-icon>
                </el-tooltip>
              </el-radio-button>
            </el-radio-group>
          </div>

          <div class="option-group">
            <span class="option-label">编程语言</span>
            <el-select
              v-model="searchForm.language"
              placeholder="全部语言"
              clearable
              size="default"
              style="width: 160px"
            >
              <el-option
                v-for="lang in languages"
                :key="lang.value"
                :label="lang.label"
                :value="lang.value"
              />
            </el-select>
          </div>

          <div class="option-group">
            <span class="option-label">标签筛选</span>
            <el-select
              v-model="searchForm.tag"
              placeholder="全部标签"
              clearable
              size="default"
              style="width: 160px"
            >
              <el-option
                v-for="tag in availableTags"
                :key="tag"
                :label="tag"
                :value="tag"
              />
            </el-select>
          </div>

          <div class="option-group">
            <span class="option-label">匹配方式</span>
            <el-switch
              v-model="searchForm.exactMatch"
              active-text="精确匹配"
              inactive-text="模糊匹配"
              inline-prompt
            />
          </div>
        </div>

        <div class="quick-access" v-if="!hasSearched">
          <div class="quick-section" v-if="hotKeywords.length">
            <span class="quick-label">热门搜索</span>
            <div class="quick-tags">
              <el-tag
                v-for="keyword in hotKeywords"
                :key="keyword"
                size="default"
                class="quick-tag"
                @click="handleQuickSearch(keyword)"
              >
                {{ keyword }}
              </el-tag>
            </div>
          </div>

          <div class="quick-section" v-if="recentSearches.length">
            <span class="quick-label">最近搜索</span>
            <div class="quick-tags">
              <el-tag
                v-for="item in recentSearches"
                :key="item.id"
                size="default"
                type="info"
                class="quick-tag"
                @click="handleQuickSearch(item.keyword)"
              >
                {{ item.keyword }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="results-section" v-if="hasSearched">
      <div class="results-header">
        <div class="results-info">
          <span class="results-count">
            找到 <strong>{{ total }}</strong> 个结果
          </span>
          <span class="results-keyword" v-if="searchForm.keyword">
            关键词: "{{ searchForm.keyword }}"
          </span>
        </div>
        <el-button text @click="handleReset">
          <el-icon><RefreshLeft /></el-icon>
          重置搜索
        </el-button>
      </div>

      <div class="results-content" v-loading="searching">
        <div class="results-list" v-if="searchResults.length > 0">
          <div
            v-for="result in searchResults"
            :key="result.id"
            class="result-item"
            @click="viewResult(result)"
          >
            <div class="result-header">
              <div class="result-title">
                <el-icon class="result-icon"><Document /></el-icon>
                <span class="result-name">{{ result.fileName }}</span>
              </div>
              <span 
                class="language-tag"
                :style="{ backgroundColor: getLanguageColor(result.languageType) + '20', color: getLanguageColor(result.languageType) }"
              >
                {{ result.languageType }}
              </span>
            </div>

            <div class="result-path">{{ result.filePath }}</div>

            <div class="result-preview">
              <code>{{ getCodePreview(result.codePreview, 4) }}</code>
            </div>

            <div class="result-footer">
              <div class="result-meta">
                <span class="meta-item">
                  <el-icon><Clock /></el-icon>
                  {{ formatRelativeTime(result.createTime) }}
                </span>
                <span class="meta-item" v-if="result.tags?.length">
                  <el-icon><PriceTag /></el-icon>
                  {{ result.tags.slice(0, 3).join(', ') }}
                </span>
              </div>
              <el-button text size="small" type="primary">
                查看详情
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </div>

        <div class="empty-results" v-else>
          <el-icon class="empty-icon"><Search /></el-icon>
          <h3>未找到匹配结果</h3>
          <p>尝试调整搜索条件或使用不同的关键词</p>
        </div>

        <div class="pagination" v-if="total > pageSize">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>

    <el-drawer
      v-model="showDetailDrawer"
      title="代码详情"
      size="640px"
      :close-on-click-modal="false"
    >
      <div class="detail-content" v-if="selectedResult">
        <div class="detail-header">
          <div class="detail-title">
            <el-icon class="detail-icon"><Document /></el-icon>
            <span class="detail-name">{{ selectedResult.fileName }}</span>
          </div>
          <div class="detail-tags">
            <span 
              class="language-tag"
              :style="{ backgroundColor: getLanguageColor(selectedResult.languageType) + '20', color: getLanguageColor(selectedResult.languageType) }"
            >
              {{ selectedResult.languageType }}
            </span>
            <el-tag
              v-for="tag in selectedResult.tags"
              :key="tag"
              size="small"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>

        <div class="detail-meta">
          <div class="meta-row">
            <span class="meta-label">路径</span>
            <span class="meta-value mono">{{ selectedResult.filePath }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">创建</span>
            <span class="meta-value">{{ formatRelativeTime(selectedResult.createTime) }}</span>
          </div>
        </div>

        <div class="detail-code">
          <div class="code-header">
            <span class="code-title">代码内容</span>
            <el-button size="small" @click="copyCode" :loading="copying">
              <el-icon><DocumentCopy /></el-icon>
              复制
            </el-button>
          </div>
          <div class="code-content" v-loading="loadingFullCode">
            <pre><code>{{ fullCodeContent }}</code></pre>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search, Lock, RefreshLeft, Document, Clock, PriceTag, ArrowRight, DocumentCopy
} from '@element-plus/icons-vue'
import { keywordSearch, getHotKeywords, getSearchHistory } from '@/api/search'
import { getAllCodeSnippets, getCodeSnippet } from '@/api/code'
import type { SearchResponse, SearchHistory, CodeSnippet } from '@/types'
import { formatRelativeTime, getLanguageColor, getCodePreview, copyToClipboard, extractErrorMessage } from '@/utils/helpers'

const route = useRoute()

const searchForm = reactive({
  keyword: '',
  searchType: 'keyword',
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

const languages = [
  { label: '全部', value: '' },
  { label: 'Java', value: 'Java' },
  { label: 'Python', value: 'Python' },
  { label: 'JavaScript', value: 'JavaScript' },
  { label: 'TypeScript', value: 'TypeScript' },
  { label: 'Go', value: 'Go' },
  { label: 'Rust', value: 'Rust' },
  { label: 'C++', value: 'C++' }
]

const handleSearch = async () => {
  if (!searchForm.keyword.trim() && !searchForm.language && !searchForm.tag) {
    ElMessage.warning('请输入搜索关键词或选择语言/标签')
    return
  }

  currentPage.value = 1
  searching.value = true
  hasSearched.value = true

  try {
    const result = await keywordSearch({
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
    console.error('搜索失败:', error)
    ElMessage.error(extractErrorMessage(error, '搜索失败'))
    searchResults.value = []
    total.value = 0
  } finally {
    searching.value = false
  }
}

const handleQuickSearch = (keyword: string) => {
  searchForm.keyword = keyword
  currentPage.value = 1
  handleSearch()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.language = ''
  searchForm.tag = ''
  searchForm.exactMatch = false
  hasSearched.value = false
  searchResults.value = []
  total.value = 0
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  handleSearch()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  handleSearch()
}

const viewResult = async (result: SearchResponse) => {
  selectedResult.value = result
  showDetailDrawer.value = true
  loadingFullCode.value = true
  try {
    const fullSnippet = await getCodeSnippet(result.id)
    fullCodeContent.value = fullSnippet.codeContent
  } catch (error) {
    console.error('加载完整代码失败:', error)
    fullCodeContent.value = result.codePreview || ''
  } finally {
    loadingFullCode.value = false
  }
}

const copyCode = async () => {
  if (!selectedResult.value) return
  
  copying.value = true
  try {
    const codeToCopy = fullCodeContent.value || selectedResult.value.codePreview || ''
    const success = await copyToClipboard(codeToCopy)
    if (success) {
      ElMessage.success('代码已复制')
    } else {
      ElMessage.error('复制失败')
    }
  } finally {
    copying.value = false
  }
}

const loadInitialData = async () => {
  try {
    const [hotResult, historyResult, allSnippets] = await Promise.all([
      getHotKeywords().catch(() => [] as string[]),
      getSearchHistory().catch(() => [] as SearchHistory[]),
      getAllCodeSnippets().catch(() => [] as CodeSnippet[])
    ])
    
    const hotKeywordsData = Array.isArray(hotResult) ? hotResult : []
    const historyData = Array.isArray(historyResult) ? historyResult : []
    const snippetsData = Array.isArray(allSnippets) ? allSnippets : []
    
    hotKeywords.value = hotKeywordsData.slice(0, 6)
    recentSearches.value = historyData.slice(0, 5)
    
    const allTags = new Set<string>()
    snippetsData.forEach((snippet: CodeSnippet) => {
      snippet.tags?.forEach(tag => allTags.add(tag))
    })
    availableTags.value = Array.from(allTags)
  } catch (error) {
    console.error('加载初始数据失败:', error)
  }
}

onMounted(() => {
  loadInitialData()
  const keyword = route.query.keyword as string
  if (keyword) {
    searchForm.keyword = keyword
    handleSearch()
  }
})
</script>

<style scoped>
.search-center {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.search-hero {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
  padding: var(--spacing-3xl);
  margin-bottom: var(--spacing-lg);
}

.search-container {
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
}

.search-title {
  font-size: var(--text-2xl);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-sm);
}

.search-subtitle {
  font-size: var(--text-base);
  color: var(--color-text-tertiary);
  margin: 0 0 var(--spacing-xl);
}

.search-box {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-xl);
}

.search-input {
  flex: 1;
}

.search-options {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: var(--spacing-2xl);
  margin-bottom: var(--spacing-xl);
}

.option-group {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.option-label {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

.lock-icon {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-left: var(--spacing-xs);
}

.quick-access {
  display: flex;
  justify-content: center;
  gap: var(--spacing-3xl);
}

.quick-section {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md);
}

.quick-label {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  padding-top: var(--spacing-xs);
}

.quick-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.quick-tag {
  cursor: pointer;
  transition: all var(--transition-fast);
}

.quick-tag:hover {
  background-color: var(--color-accent-muted);
  color: var(--color-accent-primary);
  border-color: var(--color-accent-primary);
}

.results-section {
  flex: 1;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg) var(--spacing-xl);
  border-bottom: 1px solid var(--color-border-muted);
}

.results-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.results-count {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.results-count strong {
  color: var(--color-accent-primary);
  font-weight: 600;
}

.results-keyword {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

.results-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-lg);
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.result-item {
  padding: var(--spacing-lg);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.result-item:hover {
  border-color: var(--color-accent-primary);
  background-color: var(--color-bg-sunken);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
}

.result-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  min-width: 0;
}

.result-icon {
  font-size: 18px;
  color: var(--color-accent-primary);
  flex-shrink: 0;
}

.result-name {
  font-size: var(--text-base);
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

.result-path {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  margin-bottom: var(--spacing-md);
}

.result-preview {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  background: var(--color-bg-sunken);
  padding: var(--spacing-md);
  border-radius: var(--radius-sm);
  margin-bottom: var(--spacing-md);
}

.result-preview code {
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: pre-wrap;
}

.result-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-meta {
  display: flex;
  gap: var(--spacing-lg);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.empty-results {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  min-height: 220px;
  padding: var(--spacing-4xl);
  border: 1px dashed var(--color-border-default);
  border-radius: var(--radius-lg);
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--color-bg-elevated) 90%, var(--color-bg-sunken) 10%) 0%,
    color-mix(in srgb, var(--color-bg-elevated) 78%, var(--color-bg-sunken) 22%) 100%
  );
}

.empty-results .empty-icon {
  font-size: 64px;
  color: var(--color-text-muted);
  opacity: 0.82;
}

.empty-results h3 {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.empty-results p {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  margin: 0;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: var(--spacing-lg);
  border-top: 1px solid var(--color-border-muted);
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.detail-icon {
  font-size: 24px;
  color: var(--color-accent-primary);
}

.detail-name {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
}

.detail-tags {
  display: flex;
  gap: var(--spacing-sm);
}

.detail-meta {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  background: var(--color-bg-sunken);
  border-radius: var(--radius-md);
}

.meta-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.meta-label {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  min-width: 48px;
}

.meta-value {
  font-size: var(--text-sm);
  color: var(--color-text-primary);
}

.meta-value.mono {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.detail-code {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
}

.code-title {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-secondary);
}

.code-content {
  flex: 1;
  background: var(--color-bg-sunken);
  border-radius: var(--radius-md);
  padding: var(--spacing-lg);
  overflow: auto;
  max-height: 400px;
}

.code-content pre {
  margin: 0;
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  line-height: 1.6;
  color: var(--color-text-primary);
}

.code-content code {
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
