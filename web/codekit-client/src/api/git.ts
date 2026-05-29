import request from '@/utils/request'

export interface GitStatus {
  initialized: boolean
  repoPath: string
  currentBranch: string
  stagedCount: number
  modifiedCount: number
  untrackedCount: number
  changedFiles: string[]
}

export interface GitCommit {
  shortHash: string
  fullHash: string
  message: string
  author: string
  commitTime: string
  changedFiles: string[]
}

/**
 * 初始化 Git 仓库
 */
export const initGitRepo = (path: string): Promise<boolean> => {
  return request.post<boolean>('/code/git/init', undefined, { params: { path } })
}

/**
 * 获取 Git 状态
 */
export const getGitStatus = (path: string): Promise<GitStatus> => {
  return request.get<GitStatus>('/code/git/status', { params: { path } })
}

/**
 * 提交变更
 */
export const gitCommit = (path: string, message: string): Promise<string> => {
  return request.post<string>('/code/git/commit', undefined, { params: { path, message } })
}

/**
 * 获取提交历史
 */
export const getGitHistory = (path: string, maxCount = 20): Promise<GitCommit[]> => {
  return request.get<GitCommit[]>('/code/git/history', { params: { path, maxCount } })
}

/**
 * 获取差异
 */
export const getGitDiff = (path: string, oldRef = 'HEAD~1', newRef = 'HEAD'): Promise<string> => {
  return request.get<string>('/code/git/diff', { params: { path, oldRef, newRef } })
}
