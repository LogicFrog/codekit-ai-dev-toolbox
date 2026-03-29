import type { VersionInfo, SearchRequest, CreateVersionRequest } from '@/types'

export const formatDate = (date: string | Date, format: string = 'YYYY-MM-DD HH:mm:ss'): string => {
  if (!date) return '-'
  
  const d = typeof date === 'string' ? new Date(date) : date
  if (isNaN(d.getTime())) return '-'
  
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  
  return format
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

export const formatRelativeTime = (date: string | Date): string => {
  if (!date) return '-'
  
  const d = typeof date === 'string' ? new Date(date) : date
  if (isNaN(d.getTime())) return '-'
  
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  const months = Math.floor(days / 30)
  const years = Math.floor(days / 365)
  
  if (years > 0) return `${years}年前`
  if (months > 0) return `${months}个月前`
  if (days > 0) return `${days}天前`
  if (hours > 0) return `${hours}小时前`
  if (minutes > 0) return `${minutes}分钟前`
  return '刚刚'
}

export const copyToClipboard = async (text: string): Promise<boolean> => {
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text)
      return true
    }
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    const successful = document.execCommand('copy')
    document.body.removeChild(textarea)
    return successful
  } catch (error) {
    console.error('复制失败:', error)
    return false
  }
}

export const debounce = <T extends (...args: any[]) => any>(
  func: T,
  wait: number = 300
): ((...args: Parameters<T>) => void) => {
  let timeout: number | null = null
  
  return function executedFunction(...args: Parameters<T>) {
    const later = () => {
      timeout = null
      func(...args)
    }
    
    if (timeout) clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}

export const truncate = (text: string, maxLength: number = 100, suffix: string = '...'): string => {
  if (!text || text.length <= maxLength) return text
  return text.substring(0, maxLength) + suffix
}

export const getLanguageColor = (language: string): string => {
  const colors: Record<string, string> = {
    Java: '#E76F00',
    JavaScript: '#F7DF1E',
    TypeScript: '#3178C6',
    Python: '#3776AB',
    Go: '#00ADD8',
    Rust: '#DEA584',
    C: '#555555',
    'C++': '#F34B7D',
    'C#': '#239120',
    PHP: '#777BB4',
    Ruby: '#CC342D',
    Swift: '#F05138',
    Kotlin: '#A97BFF',
    Scala: '#DC322F',
  }
  return colors[language] || '#6B7280'
}

export const getLanguageTagType = (language: string): 'default' | 'primary' | 'success' | 'warning' | 'error' | 'info' => {
  const typeMap: Record<string, 'default' | 'primary' | 'success' | 'warning' | 'error' | 'info'> = {
    Java: 'success',
    Python: 'warning',
    JavaScript: 'primary',
    TypeScript: 'info',
    Go: 'success',
    Rust: 'error'
  }
  return typeMap[language] || 'default'
}

export const getCodePreview = (code: string, lines: number = 5): string => {
  if (!code) return ''
  const codeLines = code.split('\n')
  const preview = codeLines.slice(0, lines)
  return preview.join('\n') + (codeLines.length > lines ? '\n...' : '')
}

export const extractFileName = (filePath: string): string => {
  const parts = filePath.split('/')
  return parts[parts.length - 1] || filePath
}

export const extractFileExtension = (fileName: string): string => {
  const parts = fileName.split('.')
  return parts.length > 1 ? parts[parts.length - 1].toLowerCase() : ''
}

export const detectLanguageType = (fileName: string): string => {
  const extension = extractFileExtension(fileName)
  const languageMap: Record<string, string> = {
    java: 'Java',
    js: 'JavaScript',
    ts: 'TypeScript',
    py: 'Python',
    go: 'Go',
    rs: 'Rust',
    c: 'C',
    cpp: 'C++',
    cs: 'C#',
    php: 'PHP',
    rb: 'Ruby',
    swift: 'Swift',
    kt: 'Kotlin',
    scala: 'Scala',
    html: 'HTML',
    css: 'CSS',
    json: 'JSON',
    xml: 'XML',
    sql: 'SQL',
    md: 'Markdown',
    vue: 'Vue',
    jsx: 'React',
    tsx: 'React'
  }
  return languageMap[extension] || 'Unknown'
}

export const normalizeLanguageType = (language: string): string => {
  const languageMap: Record<string, string> = {
    'java': 'Java',
    'javascript': 'JavaScript',
    'typescript': 'TypeScript',
    'python': 'Python',
    'go': 'Go',
    'rust': 'Rust',
    'c': 'C',
    'c++': 'C++',
    'csharp': 'C#',
    'php': 'PHP',
    'ruby': 'Ruby',
    'swift': 'Swift',
    'kotlin': 'Kotlin',
    'scala': 'Scala'
  }
  
  return languageMap[language.toLowerCase()] || language
}

export const normalizeFilePath = (filePath: string): string => {
  return filePath.replace(/\\/g, '/')
}

export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

export const formatCodePreview = (code: string, maxLength: number = 200): string => {
  if (!code) return ''
  
  const lines = code.split('\n')
  let preview = ''
  let totalLength = 0
  
  for (const line of lines) {
    if (totalLength + line.length > maxLength) {
      preview += line.substring(0, maxLength - totalLength) + '...'
      break
    }
    preview += line + '\n'
    totalLength += line.length + 1
  }
  
  return preview.trim()
}

export const compareVersions = (version1: VersionInfo, version2: VersionInfo): number => {
  const time1 = new Date(version1.createTime).getTime()
  const time2 = new Date(version2.createTime).getTime()
  return time1 - time2
}

export const groupVersionsBySnippet = (versions: VersionInfo[]): Record<number, VersionInfo[]> => {
  return versions.reduce((result, version) => {
    const snippetId = version.snippetId
    if (!result[snippetId]) {
      result[snippetId] = []
    }
    result[snippetId].push(version)
    return result
  }, {} as Record<number, VersionInfo[]>)
}

export const getLatestVersion = (versions: VersionInfo[]): VersionInfo | null => {
  if (versions.length === 0) return null
  
  return versions.reduce((latest, current) => {
    return compareVersions(current, latest) > 0 ? current : latest
  })
}

export const getOldestVersion = (versions: VersionInfo[]): VersionInfo | null => {
  if (versions.length === 0) return null
  
  return versions.reduce((oldest, current) => {
    return compareVersions(current, oldest) < 0 ? current : oldest
  })
}

export const buildSearchRequest = (params: Partial<SearchRequest>): SearchRequest => {
  return {
    keyword: params.keyword || '',
    searchType: params.searchType || 'keyword',
    languageType: params.languageType,
    tag: params.tag,
    exactMatch: params.exactMatch || false,
    page: params.page || 0,
    size: params.size || 10
  }
}

export const buildCreateVersionRequest = (params: Partial<CreateVersionRequest>): CreateVersionRequest => {
  return {
    versionName: params.versionName || '',
    description: params.description
  }
}

export const buildScanRequest = (scanDir: string): { scanDir: string } => {
  return { scanDir }
}

export const sleep = (ms: number): Promise<void> => {
  return new Promise(resolve => setTimeout(resolve, ms))
}

export const isEmpty = (value: any): boolean => {
  if (value === null || value === undefined) return true
  if (typeof value === 'string') return value.trim().length === 0
  if (Array.isArray(value)) return value.length === 0
  if (typeof value === 'object') return Object.keys(value).length === 0
  return false
}

export const isNotEmpty = (value: any): boolean => {
  return !isEmpty(value)
}

export const unique = <T>(array: T[]): T[] => {
  return Array.from(new Set(array))
}

export const groupBy = <T>(array: T[], key: keyof T): Record<string, T[]> => {
  return array.reduce((result, item) => {
    const groupKey = String(item[key])
    if (!result[groupKey]) {
      result[groupKey] = []
    }
    result[groupKey].push(item)
    return result
  }, {} as Record<string, T[]>)
}
