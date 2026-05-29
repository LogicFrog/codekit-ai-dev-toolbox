import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AIChatRequest, AIChatResponse, AIMessage } from '@/types'
import type { AgentExecuteResponse, AgentTask, SkillResult } from '@/api/ai'

export interface AgentSearchItem {
  id: number
  fileName: string
  codePreview: string
  languageType: string
  relevanceScore?: number
  fullCode?: string
}

export interface AgentVersionItem {
  id: number
  snippetId: number
  versionName?: string
  description?: string
  createTime: string
}

export interface AgentCompareData {
  versionA?: { id: number; versionName?: string }
  versionB?: { id: number; versionName?: string }
  diff?: {
    addedLines: number
    removedLines: number
    modifiedBlocks: number
    changeRate: number
    summary: string
  }
  analysis?: {
    summary: string
    riskLevel: string
    risks: string[]
    suggestions: string[]
  }
}

export interface AgentOptimizeData {
  answer: string
  suggestions: string[]
  optimizeType?: string
}

export const useAIChatStore = defineStore('aiChat', () => {
  const mode = ref<'chat' | 'explain' | 'agent'>('chat')
  const loading = ref(false)
  const response = ref<AIChatResponse | null>(null)
  const agentResponse = ref<AgentExecuteResponse | null>(null)
  const hasResponse = ref(false)
  const errorMessage = ref('')
  const conversation = ref<AIMessage[]>([])
  const temperature = ref(0.7)
  const temperatureSaving = ref(false)
  const sessionId = ref<string | undefined>(undefined)

  const agentSearchItems = computed<AgentSearchItem[]>(() => {
    if (!agentResponse.value) return []
    const items: AgentSearchItem[] = []
    for (const result of agentResponse.value.results) {
      if (result.data && typeof result.data === 'object') {
        const data = result.data as Record<string, unknown>
        if (data.items && Array.isArray(data.items)) {
          items.push(...(data.items as AgentSearchItem[]))
        }
      }
    }
    return items
  })

  const agentVersionItems = computed<AgentVersionItem[]>(() => {
    if (!agentResponse.value) return []
    for (const result of agentResponse.value.results) {
      if (result.data && typeof result.data === 'object') {
        const data = result.data as Record<string, unknown>
        if (data.items && Array.isArray(data.items)) {
          return data.items as AgentVersionItem[]
        }
      }
    }
    return []
  })

  const agentCompareData = computed<AgentCompareData | null>(() => {
    if (!agentResponse.value) return null
    for (const result of agentResponse.value.results) {
      if (result.skillName === 'git_compare' && result.data && typeof result.data === 'object') {
        const data = result.data as Record<string, unknown>
        return {
          diff: data.diff as AgentCompareData['diff'],
          analysis: data.analysis as AgentCompareData['analysis']
        }
      }
    }
    return null
  })

  const agentOptimizeData = computed<AgentOptimizeData | null>(() => {
    if (!agentResponse.value) return null
    for (const result of agentResponse.value.results) {
      if (result.skillName === 'code_optimize' && result.data && typeof result.data === 'object') {
        const data = result.data as Record<string, unknown>
        return {
          answer: (data.answer as string) || '',
          suggestions: (data.suggestions as string[]) || [],
          optimizeType: data.optimizeType as string | undefined
        }
      }
    }
    return null
  })

  function resetChat() {
    loading.value = false
    response.value = null
    agentResponse.value = null
    hasResponse.value = false
    errorMessage.value = ''
    conversation.value = []
  }

  function newChat() {
    resetChat()
    sessionId.value = undefined
  }

  function handleAgentResponse(resp: AgentExecuteResponse) {
    agentResponse.value = resp
    hasResponse.value = true
  }

  return {
    mode,
    loading,
    response,
    agentResponse,
    hasResponse,
    errorMessage,
    conversation,
    temperature,
    temperatureSaving,
    sessionId,
    agentSearchItems,
    agentVersionItems,
    agentCompareData,
    agentOptimizeData,
    resetChat,
    newChat,
    handleAgentResponse
  }
})
