import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useSettingsStore } from '@/stores/settings'

describe('useSettingsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('初始 settings 有默认值', () => {
    const store = useSettingsStore()
    expect(store.settings.temperature).toBe(0.7)
    expect(store.settings.provider).toBe('doubao')
    expect(store.settings.model).toBe('doubao-seed-2-0-pro-260215')
    expect(store.settings.editorTheme).toBe('vs-dark')
    expect(store.settings.fontSize).toBe(14)
    expect(store.settings.autoSave).toBe(true)
    expect(store.settings.pageSize).toBe(20)
    expect(store.settings.maxContextTokens).toBe(4096)
    expect(store.settings.contextWindowRounds).toBe(4)
  })

  it('初始 apiKey 为空字符串', () => {
    const store = useSettingsStore()
    expect(store.settings.apiKey).toBe('')
  })

  it('初始 providers 为空数组', () => {
    const store = useSettingsStore()
    expect(store.providers).toEqual([])
  })

  it('初始 loading 为 false', () => {
    const store = useSettingsStore()
    expect(store.loading).toBe(false)
  })

  it('初始 showApiKey 为 false', () => {
    const store = useSettingsStore()
    expect(store.showApiKey).toBe(false)
  })

  it('updateProvider 更新 provider、model 和 baseUrl', () => {
    const store = useSettingsStore()
    store.providers = [
      {
        code: 'qwen',
        displayName: '通义千问',
        defaultBaseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
        defaultModels: ['qwen-max', 'qwen-plus']
      },
      {
        code: 'openai',
        displayName: 'ChatGPT',
        defaultBaseUrl: 'https://api.openai.com/v1',
        defaultModels: ['gpt-4o']
      }
    ]

    store.updateProvider('qwen')

    expect(store.settings.provider).toBe('qwen')
    expect(store.settings.model).toBe('qwen-max')
    expect(store.settings.baseUrl).toBe('https://dashscope.aliyuncs.com/compatible-mode/v1')
  })

  it('updateProvider 不存在的 provider 不做任何变更', () => {
    const store = useSettingsStore()
    const originalProvider = store.settings.provider
    const originalModel = store.settings.model

    store.updateProvider('nonexistent')

    expect(store.settings.provider).toBe(originalProvider)
    expect(store.settings.model).toBe(originalModel)
  })

  it('updateApiKey 更新 apiKey', () => {
    const store = useSettingsStore()
    store.updateApiKey('sk-test-key-123')

    expect(store.settings.apiKey).toBe('sk-test-key-123')
  })

  it('resetSettings 恢复默认值', () => {
    const store = useSettingsStore()
    store.settings.temperature = 0.2
    store.settings.provider = 'openai'
    store.settings.apiKey = 'some-key'
    store.settings.editorTheme = 'vs'
    store.settings.fontSize = 20
    store.apiKeySavedHint = '已保存'

    store.resetSettings()

    expect(store.settings.temperature).toBe(0.7)
    expect(store.settings.provider).toBe('doubao')
    expect(store.settings.apiKey).toBe('')
    expect(store.settings.editorTheme).toBe('vs-dark')
    expect(store.settings.fontSize).toBe(14)
    expect(store.apiKeySavedHint).toBe('')
  })

  it('modelOptions 从 providers 推导当前 provider 的模型列表', () => {
    const store = useSettingsStore()
    store.providers = [
      {
        code: 'doubao',
        displayName: '豆包',
        defaultBaseUrl: 'https://ark.cn-beijing.volces.com/api/v3',
        defaultModels: ['doubao-pro', 'doubao-lite']
      },
      {
        code: 'deepseek',
        displayName: 'DeepSeek',
        defaultBaseUrl: 'https://api.deepseek.com/v1',
        defaultModels: ['deepseek-chat', 'deepseek-reasoner']
      }
    ]
    store.settings.provider = 'deepseek'

    const options = store.modelOptions
    expect(options).toHaveLength(2)
    expect(options[0].value).toBe('deepseek-chat')
    expect(options[1].value).toBe('deepseek-reasoner')
  })

  it('modelOptions 当前 provider 不在列表中返回空', () => {
    const store = useSettingsStore()
    store.providers = [{ code: 'openai', displayName: 'ChatGPT', defaultBaseUrl: 'https://api.openai.com/v1', defaultModels: ['gpt-4o'] }]
    store.settings.provider = 'doubao'

    expect(store.modelOptions).toEqual([])
  })

  it('modelOptions providers 为空返回空', () => {
    const store = useSettingsStore()
    expect(store.modelOptions).toEqual([])
  })

  it('settings 是 reactive 的，可以直接修改嵌套属性', () => {
    const store = useSettingsStore()
    store.settings.temperature = 0.5
    store.settings.model = 'custom-model'

    expect(store.settings.temperature).toBe(0.5)
    expect(store.settings.model).toBe('custom-model')
  })
})
