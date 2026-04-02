export interface CodeCategory {
  id: number
  categoryName: string
  sortOrder?: number
  createTime?: string
  updateTime?: string
}

export interface CodeSnippet {
  id: number
  filePath: string
  fileName: string
  codeContent: string
  languageType: string
  className?: string
  packageName?: string
  fileMd5?: string
  tags?: string[]
  category?: CodeCategory | null
  createTime: string
  updateTime: string
  dependencies?: CodeDependency[]
}

export interface CodeDependency {
  id: number
  codeSnippetId: number
  dependName: string
  dependType?: string
}

export interface VersionInfo {
  id: number
  snippetId: number
  versionName?: string
  codeContent: string
  description?: string
  createTime: string
}

export interface ScanStatusDTO {
  status: string
  processedCount: number
  successCount: number
  skipCount: number
  failedCount: number
  message: string
}

export interface CreateVersionRequest {
  versionName: string
  description?: string
}

export interface SearchRequest {
  keyword?: string
  searchType?: string
  languageType?: string
  tag?: string
  exactMatch?: boolean
  page?: number
  size?: number
}

export interface SearchResponse {
  id: number
  filePath: string
  fileName: string
  codePreview: string
  languageType: string
  className?: string
  packageName?: string
  tags?: string[]
  relevanceScore?: number
  createTime: string
  highlight?: string
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface SearchHistory {
  id: number
  keyword: string
  searchType: number
  searchTime: string
}

export interface FsItem {
  name: string
  path: string
  isDirectory: boolean
}

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// ==================== AI 模块类型 ====================

export interface AIChatRequest {
  question: string
  code?: string
  languageType?: string
  sessionId?: string
}

export interface AIChatResponse {
  answer: string
  suggestions?: string[]
  codeBlocks?: CodeBlock[]
}

export interface CodeBlock {
  language: string
  code: string
  description?: string
}
