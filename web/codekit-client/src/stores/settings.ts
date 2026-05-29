import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type { AISettings, ProviderInfo } from '@/types'

export const useSettingsStore = defineStore('settings', () => {
  const settings = reactive<AISettings>({
    temperature: 0.7,
    provider: 'doubao',
    model: 'doubao-seed-2-0-pro-260215',
    apiKey: '',
    baseUrl: 'https://ark.cn-beijing.volces.com/api/v3',
    editorTheme: 'vs-dark',
    fontSize: 14,
    autoSave: true,
    pageSize: 20,
    maxContextTokens: 4096,
    contextWindowRounds: 4,
    embeddingApiKey: '',
    theme: 'light'
  })

  const providers = ref<ProviderInfo[]>([])
  const showApiKey = ref(false)
  const apiKeySavedHint = ref('')
  const loading = ref(false)

  const modelOptions = computed(() => {
    const provider = providers.value.find(p => p.code === settings.provider)
    if (!provider) return []
    return provider.defaultModels.map(m => ({
      label: m,
      value: m
    }))
  })

  function updateProvider(code: string) {
    const provider = providers.value.find(p => p.code === code)
    if (provider) {
      settings.provider = code
      settings.model = provider.defaultModels[0]
      settings.baseUrl = provider.defaultBaseUrl
    }
  }

  function updateApiKey(key: string) {
    settings.apiKey = key
  }

  function resetSettings() {
    settings.temperature = 0.7
    settings.provider = 'doubao'
    settings.model = 'doubao-seed-2-0-pro-260215'
    settings.apiKey = ''
    settings.baseUrl = 'https://ark.cn-beijing.volces.com/api/v3'
    settings.editorTheme = 'vs-dark'
    settings.fontSize = 14
    settings.autoSave = true
    settings.pageSize = 20
    settings.maxContextTokens = 4096
    settings.contextWindowRounds = 4
    settings.embeddingApiKey = ''
    settings.theme = 'light'
    apiKeySavedHint.value = ''
  }

  return {
    settings,
    providers,
    showApiKey,
    apiKeySavedHint,
    loading,
    modelOptions,
    updateProvider,
    updateApiKey,
    resetSettings
  }
})
