import request from '@/utils/request'
import type { AIChatRequest, AIChatResponse } from '@/types'

/**
 * AI 对话接口
 */
export const aiChat = (data: AIChatRequest): Promise<AIChatResponse> => {
  return request.post<AIChatResponse>('/ai/chat', data)
}

/**
 * 代码解释接口
 */
export const aiExplain = (data: AIChatRequest): Promise<AIChatResponse> => {
  return request.post<AIChatResponse>('/ai/explain', data)
}
