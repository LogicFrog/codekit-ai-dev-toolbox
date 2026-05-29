import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAIChatStore } from '@/stores/aiChat'

describe('useAIChatStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('初始 mode 为 chat', () => {
    const store = useAIChatStore()
    expect(store.mode).toBe('chat')
  })

  it('初始 loading 为 false', () => {
    const store = useAIChatStore()
    expect(store.loading).toBe(false)
  })

  it('初始 hasResponse 为 false', () => {
    const store = useAIChatStore()
    expect(store.hasResponse).toBe(false)
  })

  it('初始 errorMessage 为空字符串', () => {
    const store = useAIChatStore()
    expect(store.errorMessage).toBe('')
  })

  it('初始 conversation 为空数组', () => {
    const store = useAIChatStore()
    expect(store.conversation).toEqual([])
  })

  it('初始 temperature 为 0.7', () => {
    const store = useAIChatStore()
    expect(store.temperature).toBe(0.7)
  })

  it('初始 sessionId 为 undefined', () => {
    const store = useAIChatStore()
    expect(store.sessionId).toBeUndefined()
  })

  it('resetChat 清空所有状态', () => {
    const store = useAIChatStore()
    store.loading = true
    store.hasResponse = true
    store.errorMessage = 'test error'
    store.conversation = [{ role: 'user', content: 'hello' }]

    store.resetChat()

    expect(store.loading).toBe(false)
    expect(store.hasResponse).toBe(false)
    expect(store.errorMessage).toBe('')
    expect(store.conversation).toEqual([])
  })

  it('newChat 重置所有状态并清空 sessionId', () => {
    const store = useAIChatStore()
    store.sessionId = 'session-123'
    store.conversation = [{ role: 'user', content: 'hello' }]
    store.hasResponse = true

    store.newChat()

    expect(store.sessionId).toBeUndefined()
    expect(store.conversation).toEqual([])
    expect(store.hasResponse).toBe(false)
  })

  it('agentSearchItems 计算属性从 agentResponse 正确提取搜索结果', () => {
    const store = useAIChatStore()
    store.agentResponse = {
      instruction: 'search code',
      tasks: [],
      results: [
        {
          success: true,
          skillName: 'code_search',
          data: {
            items: [
              { id: 1, fileName: 'Test.java', codePreview: 'code...', languageType: 'Java' },
              { id: 2, fileName: 'Main.java', codePreview: 'main...', languageType: 'Java' }
            ],
            total: 2
          }
        }
      ],
      summary: 'ok'
    }

    expect(store.agentSearchItems).toHaveLength(2)
    expect(store.agentSearchItems[0].fileName).toBe('Test.java')
  })

  it('agentSearchItems 无 data.items 时返回空数组', () => {
    const store = useAIChatStore()
    store.agentResponse = {
      instruction: 'search',
      tasks: [],
      results: [{ success: false, skillName: 'code_search', error: 'not found' }],
      summary: 'failed'
    }

    expect(store.agentSearchItems).toEqual([])
  })

  it('agentVersionItems 从版本结果提取', () => {
    const store = useAIChatStore()
    store.agentResponse = {
      instruction: 'list versions',
      tasks: [],
      results: [
        {
          success: true,
          skillName: 'version_list',
          data: {
            snippetId: 5,
            count: 2,
            items: [
              { id: 1, snippetId: 5, versionName: 'v1.0', description: '初始', createTime: '2025-01-01' },
              { id: 2, snippetId: 5, versionName: 'v2.0', description: '更新', createTime: '2025-01-02' }
            ]
          }
        }
      ],
      summary: 'ok'
    }

    expect(store.agentVersionItems).toHaveLength(2)
  })

  it('agentCompareData 从 git_compare 结果提取', () => {
    const store = useAIChatStore()
    store.agentResponse = {
      instruction: 'compare',
      tasks: [],
      results: [
        {
          success: true,
          skillName: 'git_compare',
          data: {
            snippetId: 1,
            versionAId: 10,
            versionBId: 11,
            diff: { addedLines: 5, removedLines: 3, modifiedBlocks: 2, changeRate: 0.3, summary: 'changed' },
            analysis: { summary: '低风险', riskLevel: 'low', risks: [], suggestions: [] }
          }
        }
      ],
      summary: 'ok'
    }

    expect(store.agentCompareData).not.toBeNull()
    expect(store.agentCompareData!.diff?.addedLines).toBe(5)
    expect(store.agentCompareData!.analysis?.riskLevel).toBe('low')
  })

  it('agentOptimizeData 从 code_optimize 结果提取', () => {
    const store = useAIChatStore()
    store.agentResponse = {
      instruction: 'optimize',
      tasks: [],
      results: [
        {
          success: true,
          skillName: 'code_optimize',
          data: {
            answer: '优化建议：使用 Stream API',
            suggestions: ['改成 lambda', '使用并行流'],
            optimizeType: 'performance'
          }
        }
      ],
      summary: 'ok'
    }

    expect(store.agentOptimizeData).not.toBeNull()
    expect(store.agentOptimizeData!.answer).toBe('优化建议：使用 Stream API')
    expect(store.agentOptimizeData!.suggestions).toHaveLength(2)
    expect(store.agentOptimizeData!.optimizeType).toBe('performance')
  })

  it('handleAgentResponse 设置 agentResponse 和 hasResponse', () => {
    const store = useAIChatStore()
    const resp = { instruction: 'test', tasks: [], results: [], summary: 'done' }

    store.handleAgentResponse(resp)

    expect(store.agentResponse).toStrictEqual(resp)
    expect(store.hasResponse).toBe(true)
  })

  it('mode 切换后相关计算属性更新来源不变', () => {
    const store = useAIChatStore()
    store.mode = 'agent'
    store.agentResponse = {
      instruction: 'x',
      tasks: [],
      results: [
        {
          success: true,
          skillName: 'code_search',
          data: { items: [{ id: 3, fileName: 'A.java', codePreview: 'a', languageType: 'Java' }], total: 1 }
        }
      ],
      summary: 'ok'
    }

    expect(store.agentSearchItems).toHaveLength(1)
    // mode 变化不影响计算属性
    store.mode = 'chat'
    expect(store.agentSearchItems).toHaveLength(1)
  })

  it('errorMessage 可以正常设置和读取', () => {
    const store = useAIChatStore()
    store.errorMessage = 'API 调用失败'

    expect(store.errorMessage).toBe('API 调用失败')
  })
})
