import request from '@/utils/request'
import type {
  CodeCategory,
  CodeSnippet,
  CodeDependency,
  VersionInfo,
  ScanStatusDTO,
  CreateVersionRequest,
  PageResult
} from '@/types'

export const scanLocalCode = (scanDir: string): Promise<boolean> => {
  return request.post<boolean>('/code/scan', { scanDir })
}

export const getScanStatus = (scanDir: string): Promise<ScanStatusDTO> => {
  return request.get<ScanStatusDTO>(`/code/scan/status?scanDir=${encodeURIComponent(scanDir)}`)
}

export const saveCodeSnippetByPath = (
  filePath: string,
  languageType?: string,
  tag?: string,
  categoryId?: number
): Promise<CodeSnippet> => {
  const params = new URLSearchParams()
  params.append('filePath', filePath)
  if (languageType) params.append('languageType', languageType)
  if (tag) params.append('tag', tag)
  if (typeof categoryId === 'number') params.append('categoryId', String(categoryId))
  return request.post<CodeSnippet>(`/code/save-by-path?${params.toString()}`)
}

export const saveCodeSnippet = (codeSnippet: Partial<CodeSnippet>): Promise<CodeSnippet> => {
  return request.post<CodeSnippet>('/code/save', codeSnippet)
}

export const deleteCodeSnippet = (id: number): Promise<boolean> => {
  return request.delete<boolean>(`/code/delete/${id}`)
}

export const getCodeSnippet = (id: number): Promise<CodeSnippet> => {
  return request.get<CodeSnippet>(`/code/get/${id}`)
}

export const listCodeByTag = (tag: string): Promise<CodeSnippet[]> => {
  return request.get<CodeSnippet[]>(`/code/tag?tag=${encodeURIComponent(tag)}`)
}

export const listCodeByLanguage = (type: string): Promise<CodeSnippet[]> => {
  return request.get<CodeSnippet[]>(`/code/language?type=${encodeURIComponent(type)}`)
}

export const listCategories = (): Promise<CodeCategory[]> => {
  return request.get<CodeCategory[]>('/code/categories')
}

export const createCategory = (categoryName: string): Promise<CodeCategory> => {
  return request.post<CodeCategory>(`/code/categories?categoryName=${encodeURIComponent(categoryName)}`)
}

export const renameCategory = (categoryId: number, categoryName: string): Promise<CodeCategory> => {
  return request.put<CodeCategory>(`/code/categories/${categoryId}?categoryName=${encodeURIComponent(categoryName)}`)
}

export const deleteCategory = (categoryId: number): Promise<boolean> => {
  return request.delete<boolean>(`/code/categories/${categoryId}`)
}

export const listCodeByCategory = (categoryId: number): Promise<CodeSnippet[]> => {
  return request.get<CodeSnippet[]>(`/code/category/${categoryId}`)
}

export const assignSnippetCategory = (id: number, categoryId: number): Promise<CodeSnippet> => {
  return request.put<CodeSnippet>(`/code/${id}/category?categoryId=${categoryId}`)
}

export const listCodeByPage = (
  page: number = 0,
  size: number = 10,
  categoryId?: number
): Promise<PageResult<CodeSnippet>> => {
  const query = new URLSearchParams({
    page: String(page),
    size: String(size)
  })
  if (typeof categoryId === 'number') {
    query.append('categoryId', String(categoryId))
  }
  return request.get<PageResult<CodeSnippet>>(`/code/page?${query.toString()}`)
}

export const listCodeDependencies = (id: number): Promise<CodeDependency[]> => {
  return request.get<CodeDependency[]>(`/code/${id}/dependencies`)
}

export const createVersion = (snippetId: number, versionReq: CreateVersionRequest): Promise<VersionInfo> => {
  return request.post<VersionInfo>(`/code/${snippetId}/create-version`, versionReq)
}

export const listVersions = (id: number): Promise<VersionInfo[]> => {
  return request.get<VersionInfo[]>(`/code/${id}/versions`)
}

export const getAllCodeSnippets = async (): Promise<CodeSnippet[]> => {
  const allSnippets: CodeSnippet[] = []
  let page = 0
  const size = 100
  const maxPages = 100
  
  while (page < maxPages) {
    const result = await listCodeByPage(page, size)
    allSnippets.push(...(result.content || []))
    if (result.last || result.empty || !result.content?.length) break
    page++
  }
  
  return allSnippets
}

export const searchCodeSnippets = async (
  keyword: string,
  languageType?: string,
  tag?: string
): Promise<CodeSnippet[]> => {
  const allSnippets = await getAllCodeSnippets()
  
  return allSnippets.filter(snippet => {
    const matchKeyword = !keyword || 
      snippet.fileName.toLowerCase().includes(keyword.toLowerCase()) ||
      snippet.filePath.toLowerCase().includes(keyword.toLowerCase()) ||
      snippet.codeContent.toLowerCase().includes(keyword.toLowerCase())
    
    const matchLanguage = !languageType || snippet.languageType === languageType
    const matchTag = !tag || snippet.tags?.includes(tag)
    
    return matchKeyword && matchLanguage && matchTag
  })
}

export const getSnippetByFilePath = async (filePath: string): Promise<CodeSnippet | null> => {
  const allSnippets = await getAllCodeSnippets()
  return allSnippets.find(snippet => snippet.filePath === filePath) || null
}

export const batchDeleteSnippets = async (ids: number[]): Promise<boolean[]> => {
  const results = await Promise.allSettled(ids.map(id => deleteCodeSnippet(id)))
  return results.map(result => result.status === 'fulfilled' && result.value)
}

export const batchCreateVersions = async (
  snippetId: number,
  versionRequests: CreateVersionRequest[]
): Promise<VersionInfo[]> => {
  const results = await Promise.allSettled(
    versionRequests.map(req => createVersion(snippetId, req))
  )
  return results
    .filter((result): result is PromiseFulfilledResult<VersionInfo> => result.status === 'fulfilled')
    .map(result => result.value)
}
