<template>
  <div class="settings">
    <div class="settings-content">
      <div class="settings-section">
        <h2 class="section-title">基本设置</h2>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">主题模式</span>
            <span class="setting-desc">选择界面主题风格</span>
          </div>
          <el-radio-group v-model="settings.theme" size="default" @change="onSettingChanged">
            <el-radio-button value="light">浅色</el-radio-button>
            <el-radio-button value="dark">深色</el-radio-button>
            <el-radio-button value="auto">跟随系统</el-radio-button>
          </el-radio-group>
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">编辑器主题</span>
            <span class="setting-desc">代码编辑器的配色方案</span>
          </div>
          <el-select v-model="settings.editorTheme" style="width: 200px" @change="onSettingChanged">
            <el-option label="VS Light" value="vs-light" />
            <el-option label="VS Dark" value="vs-dark" />
            <el-option label="High Contrast" value="hc-black" />
          </el-select>
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">字体大小</span>
            <span class="setting-desc">编辑器字体大小</span>
          </div>
          <el-input-number v-model="settings.fontSize" :min="12" :max="24" @change="onSettingChanged" />
        </div>
      </div>

      <div class="settings-section">
        <h2 class="section-title">代码管理</h2>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">默认分页大小</span>
            <span class="setting-desc">代码列表每页显示数量</span>
          </div>
          <el-select v-model="settings.pageSize" style="width: 200px" @change="onSettingChanged">
            <el-option :value="10" label="10 条" />
            <el-option :value="20" label="20 条" />
            <el-option :value="50" label="50 条" />
            <el-option :value="100" label="100 条" />
          </el-select>
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">自动保存</span>
            <span class="setting-desc">编辑代码时自动保存</span>
          </div>
          <el-switch v-model="settings.autoSave" @change="onSettingChanged" />
        </div>
      </div>

      <div class="settings-section">
        <h2 class="section-title">AI 设置</h2>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">LLM 提供商</span>
            <span class="setting-desc">选择 AI 大语言模型服务商</span>
          </div>
          <el-select
            v-model="settings.provider"
            style="width: 320px"
            @change="onProviderChanged"
          >
            <el-option
              v-for="p in store.providers"
              :key="p.code"
              :label="p.displayName"
              :value="p.code"
            />
          </el-select>
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">AI 模型</span>
            <span class="setting-desc">选择模型或输入自定义模型名</span>
          </div>
          <div class="setting-control-row">
            <el-select
              v-model="settings.model"
              style="width: 320px"
              filterable
              allow-create
              @change="onSettingChanged"
            >
              <el-option
                v-for="opt in store.modelOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </div>
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">API 地址</span>
            <span class="setting-desc">API 端点地址（为空则使用提供商默认）</span>
          </div>
          <el-input
            v-model="settings.baseUrl"
            placeholder="自定义 API 地址（可选）"
            style="width: 320px"
            @change="onSettingChanged"
          />
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">API Key</span>
            <span class="setting-desc">配置 LLM 服务的 API 密钥</span>
          </div>
          <div class="setting-control-row">
            <el-input
              v-model="settings.apiKey"
              :type="store.showApiKey ? 'text' : 'password'"
              placeholder="请输入 API Key"
              style="width: 320px"
              @change="onKeyChanged"
              @focus="onApiKeyFocus"
            >
              <template #suffix>
                <el-icon
                  class="api-key-toggle"
                  @click.stop="store.showApiKey = !store.showApiKey"
                >
                  <View v-if="!store.showApiKey" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
            <span v-if="store.apiKeySavedHint" class="save-hint">{{ store.apiKeySavedHint }}</span>
          </div>
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">Embedding API Key</span>
            <span class="setting-desc">用于语义检索的向量化 API 密钥（为空则复用上方主 Key）</span>
          </div>
          <div class="setting-control-row">
            <el-input
              v-model="settings.embeddingApiKey"
              :type="showEmbeddingKey ? 'text' : 'password'"
              placeholder="可选，不填则使用主 API Key"
              style="width: 320px"
              @change="onEmbeddingKeyChanged"
              @focus="onEmbeddingKeyFocus"
            >
              <template #suffix>
                <el-icon class="api-key-toggle" @click.stop="showEmbeddingKey = !showEmbeddingKey">
                  <View v-if="!showEmbeddingKey" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
          </div>
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">温度参数</span>
            <span class="setting-desc">控制 AI 回答的随机性 (0=确定, 2=最随机)</span>
          </div>
          <div class="setting-control-row">
            <el-slider
              v-model="settings.temperature"
              :min="0"
              :max="2"
              :step="0.1"
              :show-input="true"
              style="width: 280px"
              @change="onSettingChanged"
            />
          </div>
        </div>
      </div>

      <div class="settings-section">
        <h2 class="section-title">关于</h2>

        <div class="about-info">
          <div class="about-item">
            <span class="about-label">版本</span>
            <span class="about-value">1.0.0</span>
          </div>
          <div class="about-item">
            <span class="about-label">项目地址</span>
            <a href="#" class="about-link">GitHub</a>
          </div>
          <div class="about-item">
            <span class="about-label">技术栈</span>
            <span class="about-value">Vue 3 + TypeScript + Element Plus + Spring Boot 3</span>
          </div>
          <div class="about-item">
            <span class="about-label">AI 提供商</span>
            <span class="about-value">火山方舟 (Doubao)</span>
          </div>
        </div>
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">上下文 Token 预算</span>
            <span class="setting-desc">每次对话注入上下文的最大 Token 数 (512~16384)</span>
          </div>
          <el-input-number
            v-model="settings.maxContextTokens"
            :min="512"
            :max="16384"
            :step="512"
            @change="onSettingChanged"
          />
        </div>

        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">上下文历史轮数</span>
            <span class="setting-desc">保留最近 N 轮对话作为上下文 (1~20)</span>
          </div>
          <el-input-number
            v-model="settings.contextWindowRounds"
            :min="1"
            :max="20"
            @change="onSettingChanged"
          />
        </div>
      </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { View, Hide } from '@element-plus/icons-vue'
import { useSettingsStore } from '@/stores/settings'
import { getAiSettings, saveAiSettings, getAiProviders } from '@/api/ai'
import { debounce } from '@/utils/helpers'
import type { AISettings } from '@/types'

const store = useSettingsStore()
const settings = store.settings

let keyChanged = false
let embeddingKeyChanged = false
const showEmbeddingKey = ref(false)

function onProviderChanged() {
  store.updateProvider(settings.provider)
  onSettingChanged()
}

const debouncedSave = debounce(async () => {
  try {
    const payload: AISettings = {
      temperature: settings.temperature,
      provider: settings.provider,
      model: settings.model,
      apiKey: keyChanged ? settings.apiKey : '',
      baseUrl: settings.baseUrl,
      editorTheme: settings.editorTheme,
      fontSize: settings.fontSize,
      autoSave: settings.autoSave,
      pageSize: settings.pageSize,
      maxContextTokens: settings.maxContextTokens,
      contextWindowRounds: settings.contextWindowRounds,
      embeddingApiKey: embeddingKeyChanged ? settings.embeddingApiKey : ''
    }
    const result = await saveAiSettings(payload)
    if (keyChanged && result.apiKey) {
      settings.apiKey = result.apiKey
      store.apiKeySavedHint = '已保存'
      setTimeout(() => { store.apiKeySavedHint = '' }, 2000)
    }
    if (embeddingKeyChanged && result.embeddingApiKey) {
      settings.embeddingApiKey = result.embeddingApiKey
    }
    keyChanged = false
    embeddingKeyChanged = false
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    console.error('保存设置失败:', e)
    ElMessage.error('保存失败：' + msg)
  }
}, 400)

function onSettingChanged() {
  debouncedSave()
}

function onKeyChanged() {
  keyChanged = true
  if (settings.apiKey) {
    debouncedSave()
  }
}

function onApiKeyFocus() {
  if (!keyChanged && settings.apiKey && settings.apiKey.includes('****')) {
    settings.apiKey = ''
    keyChanged = true
  }
}

function onEmbeddingKeyChanged() {
  embeddingKeyChanged = true
  if (settings.embeddingApiKey) {
    debouncedSave()
  }
}

function onEmbeddingKeyFocus() {
  if (!embeddingKeyChanged && settings.embeddingApiKey && settings.embeddingApiKey.includes('****')) {
    settings.embeddingApiKey = ''
    embeddingKeyChanged = true
  }
}

onMounted(async () => {
  store.loading = true
  try {
    const [data, providerList] = await Promise.all([
      getAiSettings(),
      getAiProviders()
    ])
    store.providers = providerList
    settings.provider = data.provider || 'doubao'
    settings.model = data.model || ''
    settings.apiKey = data.apiKey || ''
    settings.baseUrl = data.baseUrl || ''
    settings.temperature = data.temperature
    settings.editorTheme = data.editorTheme || 'vs-dark'
    settings.fontSize = data.fontSize || 14
    settings.autoSave = data.autoSave ?? false
    settings.pageSize = data.pageSize || 20
    settings.maxContextTokens = data.maxContextTokens || 4096
    settings.contextWindowRounds = data.contextWindowRounds || 4
    settings.embeddingApiKey = data.embeddingApiKey || ''
  } catch (e) {
    console.error('加载设置失败:', e)
    ElMessage.warning('无法加载设置，请确认后端已启动')
  } finally {
    store.loading = false
  }
})
</script>

<style scoped>
.settings {
  height: 100%;
  overflow-y: auto;
}

.settings-content {
  max-width: 800px;
  margin: 0 auto;
}

.settings-section {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  margin-bottom: var(--spacing-lg);
}

.section-title {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-xl);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--color-border-muted);
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg) 0;
  border-bottom: 1px solid var(--color-border-muted);
}

.setting-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.setting-label {
  font-size: var(--text-base);
  font-weight: 500;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.setting-desc {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

.setting-control-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.save-hint {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  white-space: nowrap;
}

.api-key-toggle {
  cursor: pointer;
  color: var(--color-text-tertiary);
  transition: color 0.2s;
}

.api-key-toggle:hover {
  color: var(--color-text-primary);
}

.about-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.about-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) 0;
}

.about-label {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

.about-value {
  font-size: var(--text-sm);
  color: var(--color-text-primary);
  font-family: var(--font-mono);
}

.about-link {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  text-decoration: none;
  border-bottom: 1px solid var(--color-border-strong);
  padding-bottom: 1px;
}

.about-link:hover {
  color: var(--color-text-primary);
  border-bottom-color: var(--color-text-primary);
}
</style>
