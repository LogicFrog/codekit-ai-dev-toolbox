import request from '@/utils/request'
import type { VersionInfo, CreateVersionRequest } from '@/types'

export const listVersions = (snippetId: number): Promise<VersionInfo[]> => {
  return request.get<VersionInfo[]>(`/code/${snippetId}/versions`)
}

export const createVersion = (snippetId: number, versionReq: CreateVersionRequest): Promise<VersionInfo> => {
  return request.post<VersionInfo>(`/code/${snippetId}/create-version`, versionReq)
}

export const batchCreateVersions = async (
  snippetId: number,
  requests: CreateVersionRequest[]
): Promise<VersionInfo[]> => {
  const results = await Promise.allSettled(
    requests.map(req => createVersion(snippetId, req))
  )
  return results
    .filter((result): result is PromiseFulfilledResult<VersionInfo> => result.status === 'fulfilled')
    .map(result => result.value)
}

export const getAllVersions = async (snippetIds: number[]): Promise<Record<number, VersionInfo[]>> => {
  const results = await Promise.allSettled(
    snippetIds.map(id => listVersions(id))
  )
  
  const versionsMap: Record<number, VersionInfo[]> = {}
  
  snippetIds.forEach((snippetId, index) => {
    const result = results[index]
    if (result.status === 'fulfilled') {
      versionsMap[snippetId] = result.value
    } else {
      versionsMap[snippetId] = []
    }
  })
  
  return versionsMap
}

export const getLatestVersion = async (snippetId: number): Promise<VersionInfo | null> => {
  const versions = await listVersions(snippetId)
  if (versions.length === 0) return null
  
  return versions.reduce((latest, current) => {
    const latestTime = new Date(latest.createTime).getTime()
    const currentTime = new Date(current.createTime).getTime()
    return currentTime > latestTime ? current : latest
  })
}

export const getOldestVersion = async (snippetId: number): Promise<VersionInfo | null> => {
  const versions = await listVersions(snippetId)
  if (versions.length === 0) return null
  
  return versions.reduce((oldest, current) => {
    const oldestTime = new Date(oldest.createTime).getTime()
    const currentTime = new Date(current.createTime).getTime()
    return currentTime < oldestTime ? current : oldest
  })
}

export const getVersionCount = async (snippetId: number): Promise<number> => {
  const versions = await listVersions(snippetId)
  return versions.length
}

export const hasVersions = async (snippetId: number): Promise<boolean> => {
  const count = await getVersionCount(snippetId)
  return count > 0
}

export const searchVersions = async (snippetId: number, keyword: string): Promise<VersionInfo[]> => {
  const versions = await listVersions(snippetId)
  return versions.filter(version => 
    version.versionName?.toLowerCase().includes(keyword.toLowerCase()) ||
    version.description?.toLowerCase().includes(keyword.toLowerCase())
  )
}

export const exportVersion = async (versionId: number, snippetId: number): Promise<string> => {
  const versions = await listVersions(snippetId)
  const version = versions.find(v => v.id === versionId)
  if (!version) throw new Error('版本不存在')
  
  return version.codeContent
}

export const importVersion = async (
  snippetId: number,
  _codeContent: string,
  versionName: string,
  description?: string
): Promise<VersionInfo> => {
  const versionReq: CreateVersionRequest = {
    versionName,
    description
  }
  
  return createVersion(snippetId, versionReq)
}
