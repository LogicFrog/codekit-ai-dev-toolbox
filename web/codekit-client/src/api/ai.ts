import request from '@/utils/request'
import type { AIChatRequest, AIChatResponse, AIMessage } from '@/types'

/**
 * AI 对话接口
 */
export const aiChat = (data: AIChatRequest): Promise<AIChatResponse> => {
  return request.post<AIChatResponse>('/ai/chat', data, { timeout: 120000 })
}

/**
 * 代码解释接口
 */
export const aiExplain = (data: AIChatRequest): Promise<AIChatResponse> => {
  return request.post<AIChatResponse>('/ai/explain', data, { timeout: 120000 })
}

/**
 * 清空会话
 */
export const clearAiSession = (sessionId: string): Promise<boolean> => {
  return request.delete<boolean>(`/ai/session/${sessionId}`)
}

/**
 * 查询会话历史
 */
export const getAiSessionMessages = (sessionId: string, maxRounds = 10): Promise<AIMessage[]> => {
  return request.get<AIMessage[]>(`/ai/session/${sessionId}/messages`, {
    params: { maxRounds }
  })
}

/**
 * 获取当前温度
 */
export const getAiTemperature = (): Promise<number> => {
  return request.get<number>('/ai/settings/temperature')
}

/**
 * 设置温度
 */
export const setAiTemperature = (value: number): Promise<number> => {
  return request.put<number>('/ai/settings/temperature', undefined, {
    params: { value }
  })
}

interface ChatStreamCallbacks {
  onChunk: (chunk: string, sessionId?: string) => void
  onDone: (sessionId?: string, answer?: string) => void
  onError: (message: string) => void
}

/**
 * AI 对话流式接口（SSE over fetch）
 */
export const aiChatStream = async (data: AIChatRequest, callbacks: ChatStreamCallbacks): Promise<void> => {
  const response = await fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  })

  if (!response.ok || !response.body) {
    callbacks.onError(`流式请求失败: ${response.status}`)
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    buffer = buffer.replace(/\r/g, '')

    let separatorIndex = buffer.indexOf('\n\n')
    while (separatorIndex !== -1) {
      const block = buffer.slice(0, separatorIndex).trim()
      buffer = buffer.slice(separatorIndex + 2)
      separatorIndex = buffer.indexOf('\n\n')

      if (!block) continue

      let eventName = 'message'
      const dataLines: string[] = []
      block.split('\n').forEach((line) => {
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          dataLines.push(line.slice(5).trim())
        }
      })

      const dataStr = dataLines.join('\n')
      if (!dataStr) continue

      try {
        const payload = JSON.parse(dataStr)
        if (eventName === 'chunk') {
          callbacks.onChunk(payload.content || '', payload.sessionId)
        } else if (eventName === 'done') {
          callbacks.onDone(payload.sessionId, payload.answer)
        } else if (eventName === 'error') {
          callbacks.onError(payload.message || '流式响应异常')
        }
      } catch {
        // ignore malformed chunk
      }
    }
  }
}
