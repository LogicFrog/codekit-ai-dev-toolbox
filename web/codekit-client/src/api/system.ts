import request from '@/utils/request'
import type { FsItem } from '@/types'

export const listFs = (path: string): Promise<FsItem[]> => {
  return request.get<FsItem[]>(`/fs/list?path=${encodeURIComponent(path)}`)
}
