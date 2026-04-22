import request from '@/utils/request'
import type { SearchRequest, SearchResponse, PageResult, SearchHistory } from '@/types'

type RawPageResult<T> = Partial<PageResult<T>> & {
  content?: T[]
  page?: {
    size?: number
    number?: number
    totalElements?: number
    totalPages?: number
  }
}

const normalizePageResult = <T>(raw: RawPageResult<T> | null | undefined): PageResult<T> => {
  const content = Array.isArray(raw?.content) ? raw!.content : []
  const page = raw?.page

  const size = Number(raw?.size ?? page?.size ?? 0)
  const number = Number(raw?.number ?? page?.number ?? 0)
  const totalElements = Number(raw?.totalElements ?? page?.totalElements ?? content.length)
  const totalPages = Number(
    raw?.totalPages
      ?? page?.totalPages
      ?? (size > 0 ? Math.ceil(totalElements / size) : (totalElements > 0 ? 1 : 0))
  )
  const first = typeof raw?.first === 'boolean' ? raw.first : number <= 0
  const last = typeof raw?.last === 'boolean' ? raw.last : (totalPages === 0 ? true : number >= totalPages - 1)
  const empty = typeof raw?.empty === 'boolean' ? raw.empty : content.length === 0

  return {
    content,
    totalElements,
    totalPages,
    size,
    number,
    first,
    last,
    empty
  }
}

export const keywordSearch = async (params: SearchRequest): Promise<PageResult<SearchResponse>> => {
  const raw = await request.post<RawPageResult<SearchResponse>>('/search/keyword', params)
  return normalizePageResult(raw)
}

export const semanticSearch = async (params: SearchRequest): Promise<PageResult<SearchResponse>> => {
  const raw = await request.post<RawPageResult<SearchResponse>>('/search/semantic', params)
  return normalizePageResult(raw)
}

export const rebuildSemanticIndex = (): Promise<boolean> => {
  return request.post<boolean>('/search/semantic/rebuild')
}

export const getHotKeywords = (): Promise<string[]> => {
  return request.get<string[]>('/search/hot-keywords')
}

export const getSearchHistory = (userId: string = 'anonymous'): Promise<SearchHistory[]> => {
  return request.get<SearchHistory[]>(`/search/history?userId=${userId}`)
}

export const searchWithFilters = async (
  keyword: string,
  options: {
    languageType?: string
    tag?: string
    exactMatch?: boolean
    page?: number
    size?: number
  } = {}
): Promise<PageResult<SearchResponse>> => {
  const searchRequest: SearchRequest = {
    keyword,
    searchType: 'keyword',
    languageType: options.languageType,
    tag: options.tag,
    exactMatch: options.exactMatch || false,
    page: options.page || 0,
    size: options.size || 10
  }
  return keywordSearch(searchRequest)
}

export const quickSearch = async (keyword: string): Promise<SearchResponse[]> => {
  const result = await searchWithFilters(keyword, { size: 5 })
  return result.content || []
}

export const getRecentSearches = async (limit: number = 10): Promise<SearchHistory[]> => {
  const history = await getSearchHistory()
  return history.slice(0, limit)
}

export const searchByLanguage = async (
  keyword: string,
  languageType: string,
  options: {
    tag?: string
    exactMatch?: boolean
    page?: number
    size?: number
  } = {}
): Promise<PageResult<SearchResponse>> => {
  return searchWithFilters(keyword, { ...options, languageType })
}

export const searchByTag = async (
  keyword: string,
  tag: string,
  options: {
    languageType?: string
    exactMatch?: boolean
    page?: number
    size?: number
  } = {}
): Promise<PageResult<SearchResponse>> => {
  return searchWithFilters(keyword, { ...options, tag })
}

export const exactSearch = async (
  keyword: string,
  options: {
    languageType?: string
    tag?: string
    page?: number
    size?: number
  } = {}
): Promise<PageResult<SearchResponse>> => {
  return searchWithFilters(keyword, { ...options, exactMatch: true })
}

export const searchAllPages = async (
  keyword: string,
  options: {
    languageType?: string
    tag?: string
    exactMatch?: boolean
    pageSize?: number
  } = {}
): Promise<SearchResponse[]> => {
  const allResults: SearchResponse[] = []
  let page = 0
  const size = options.pageSize || 50
  
  while (true) {
    const result = await searchWithFilters(keyword, { ...options, page, size })
    allResults.push(...(result.content || []))
    
    if (result.last || result.empty) {
      break
    }
    
    page++
  }
  
  return allResults
}
