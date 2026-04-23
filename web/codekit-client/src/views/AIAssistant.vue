<template>
  <div class="ai-assistant">
    <div class="ai-container">
      <!-- 标题区域 -->
      <div class="ai-header">
        <h1 class="title">AI 助手</h1>
        <p class="subtitle">智能代码辅助，提升开发效率</p>
      </div>

      <!-- 输入区域 -->
      <div class="input-section">
        <div class="input-row">
          <div class="input-group" v-if="mode === 'explain'">
            <label class="input-label">编程语言</label>
            <el-select v-model="form.languageType" placeholder="选择语言" clearable style="width: 160px">
              <el-option label="Java" value="Java" />
              <el-option label="Python" value="Python" />
              <el-option label="JavaScript" value="JavaScript" />
              <el-option label="TypeScript" value="TypeScript" />
              <el-option label="Go" value="Go" />
            </el-select>
          </div>

          <div class="input-group mode-toggle">
            <label class="input-label">模式</label>
            <el-radio-group v-model="mode" size="default">
              <el-radio-button value="chat">自由对话</el-radio-button>
              <el-radio-button value="explain">代码解释</el-radio-button>
              <el-radio-button value="agent">Agent 编排</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <div class="input-group temperature-group">
          <label class="input-label">温度（Temperature）: {{ temperature.toFixed(1) }}</label>
          <div class="temperature-row">
            <el-slider
              v-model="temperature"
              :min="0"
              :max="2"
              :step="0.1"
              style="flex: 1"
            />
            <el-button :loading="temperatureSaving" @click="handleSaveTemperature">保存温度</el-button>
          </div>
        </div>

        <!-- 代码输入区（代码解释模式显示） -->
        <div class="input-group code-group" v-if="mode === 'explain'">
          <label class="input-label">代码内容</label>
          <el-input
            v-model="form.code"
            type="textarea"
            :rows="8"
            placeholder="请粘贴需要解释的代码..."
            class="code-textarea"
          />
        </div>

        <!-- 问题输入区 -->
        <div class="input-group">
          <label class="input-label">
            {{ mode === 'explain' ? '问题描述（可选）' : (mode === 'agent' ? 'Agent 指令' : '问题') }}
          </label>
          <el-input
            v-model="form.question"
            type="textarea"
            :rows="3"
            :placeholder="mode === 'explain' ? '有什么想问的？' : (mode === 'agent' ? '例如：帮我搜索缓存代码并解释风险，再查询 snippetId=1 的版本历史' : '输入你的问题...')"
          />
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button type="primary" size="large" @click="handleSubmit" :loading="loading">
            <el-icon><ChatDotRound /></el-icon>
            {{ mode === 'explain' ? '解释代码' : (mode === 'agent' ? '执行 Agent' : '发送') }}
          </el-button>
          <el-button size="large" @click="handleNewChat">
            新对话
          </el-button>
          <el-button size="large" @click="handleClear">
            <el-icon><Delete /></el-icon>
            清空
          </el-button>
        </div>
      </div>

      <!-- 对话历史 -->
      <div class="conversation-section" v-if="conversation.length">
        <div
          v-for="(msg, index) in conversation"
          :key="`${msg.role}-${index}`"
          class="message-item"
          :class="msg.role === 'user' ? 'message-user' : 'message-ai'"
        >
          <div class="message-role">
            {{ msg.role === 'user' ? '你' : 'AI' }}
          </div>
          <div class="message-content">{{ msg.content }}</div>
        </div>
      </div>

      <!-- 响应区域 -->
      <div class="response-section" v-if="hasResponse">
        <div class="response-content" v-loading="loading">
          <el-tag v-if="errorMessage" type="danger" size="small" style="margin-bottom: 12px;">错误</el-tag>
          <!-- 错误信息 -->
          <div class="error-block" v-if="errorMessage">
            <el-icon><WarningFilled /></el-icon>
            <span>{{ errorMessage }}</span>
          </div>

          <!-- 回答内容 -->
          <div class="answer-block" v-if="response?.answer">
            <pre>{{ response.answer }}</pre>
          </div>

          <div class="agent-block" v-if="agentResponse">
            <pre class="agent-summary">{{ agentResponse.summary }}</pre>

            <div class="block-title" v-if="agentSearchItems.length">检索命中</div>
            <div class="agent-result-list" v-if="agentSearchItems.length">
              <div
                class="agent-result-item"
                v-for="(item, index) in agentSearchItems"
                :key="`${item.id ?? index}-${item.filePath ?? ''}`"
                @click="viewAgentSearchResult(item)"
              >
                <div class="result-header">
                  <div class="result-title">
                    <el-icon class="result-icon"><Document /></el-icon>
                    <span class="result-name">{{ item.fileName || item.filePath || '未命名代码片段' }}</span>
                  </div>
                  <span
                    class="language-tag"
                    :style="{ backgroundColor: getLanguageColor(item.languageType || '') + '20', color: getLanguageColor(item.languageType || '') }"
                  >
                    {{ item.languageType || 'Unknown' }}
                  </span>
                </div>
                <div class="result-path">{{ item.filePath || '-' }}</div>
                <div class="result-footer">
                  <div class="result-meta">
                    <span class="meta-item" v-if="item.createTime">
                      <el-icon><Clock /></el-icon>
                      {{ formatRelativeTime(item.createTime) }}
                    </span>
                    <span class="meta-item" v-if="item.tags?.length">
                      <el-icon><PriceTag /></el-icon>
                      {{ item.tags.slice(0, 3).join(', ') }}
                    </span>
                  </div>
                  <el-button text size="small" type="primary">
                    查看详情
                    <el-icon><ArrowRight /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>

            <div class="block-title" v-if="agentVersionItems.length">版本历史</div>
            <div class="code-block-item" v-for="(item, index) in agentVersionItems" :key="`${item.id ?? index}-version`">
              <div class="code-block-header">
                <span class="code-language">snippetId={{ item.snippetId ?? '-' }}</span>
                <span class="code-description">{{ item.versionName || `版本 #${index + 1}` }}</span>
              </div>
              <pre class="code-content"><code>创建时间: {{ item.createTime || '-' }}
描述: {{ item.description || '暂无描述' }}</code></pre>
            </div>

            <div class="block-title">任务拆解</div>
            <ul class="suggestions-list">
              <li v-for="(task, index) in agentResponse.tasks" :key="`${task.skillName}-${index}`">
                {{ index + 1 }}. {{ task.taskName }}（{{ task.skillName }}）
              </li>
            </ul>

            <div class="block-title">执行结果</div>
            <ul class="suggestions-list">
              <li v-for="(result, index) in agentResponse.results" :key="`${result.skillName}-${index}`">
                {{ index + 1 }}. {{ result.skillName }}：{{ result.success ? '成功' : `失败（${result.error || '未知错误'}）` }}
              </li>
            </ul>
          </div>

          <!-- 建议列表 -->
          <div class="suggestions-block" v-if="response?.suggestions?.length">
            <div class="block-title">
              <el-icon><InfoFilled /></el-icon>
              建议
            </div>
            <ul class="suggestions-list">
              <li v-for="(suggestion, index) in response.suggestions" :key="index">
                {{ suggestion }}
              </li>
            </ul>
          </div>

          <!-- 代码块 -->
          <div class="code-blocks" v-if="response?.codeBlocks?.length">
            <div class="block-title">
              <el-icon><Document /></el-icon>
              代码示例
            </div>
            <div
              v-for="(block, index) in response.codeBlocks"
              :key="index"
              class="code-block-item"
            >
              <div class="code-block-header">
                <span class="code-language">{{ block.language }}</span>
                <span class="code-description" v-if="block.description">{{ block.description }}</span>
                <el-button size="small" text @click="copyCode(block.code)">
                  <el-icon><DocumentCopy /></el-icon>
                  复制
                </el-button>
              </div>
              <pre class="code-content"><code>{{ block.code }}</code></pre>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态提示 -->
      <div class="empty-state" v-if="!hasResponse && !loading">
        <el-icon class="empty-icon"><ChatDotRound /></el-icon>
        <p>输入问题或代码，让 AI 帮助你</p>
      </div>

      <el-drawer
        v-model="showDetailDrawer"
        title="代码详情"
        size="640px"
        :close-on-click-modal="false"
      >
        <div class="detail-content" v-if="selectedSearchItem">
          <div class="detail-header">
            <div class="detail-title">
              <el-icon class="detail-icon"><Document /></el-icon>
              <span class="detail-name">{{ selectedSearchItem.fileName || selectedSearchItem.filePath || '未命名代码片段' }}</span>
            </div>
            <div class="detail-tags">
              <span
                class="language-tag"
                :style="{ backgroundColor: getLanguageColor(selectedSearchItem.languageType || '') + '20', color: getLanguageColor(selectedSearchItem.languageType || '') }"
              >
                {{ selectedSearchItem.languageType || 'Unknown' }}
              </span>
              <el-tag v-for="tag in selectedSearchItem.tags || []" :key="tag" size="small">{{ tag }}</el-tag>
            </div>
          </div>

          <div class="detail-meta">
            <div class="meta-row">
              <span class="meta-label">路径</span>
              <span class="meta-value mono">{{ selectedSearchItem.filePath || '-' }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">创建</span>
              <span class="meta-value">{{ selectedSearchItem.createTime ? formatRelativeTime(selectedSearchItem.createTime) : '-' }}</span>
            </div>
          </div>

          <div class="detail-code">
            <div class="code-header">
              <span class="code-title">代码内容</span>
              <el-button size="small" @click="copyDetailCode" :loading="copying">
                <el-icon><DocumentCopy /></el-icon>
                复制
              </el-button>
            </div>
            <div class="code-content" v-loading="loadingFullCode">
              <pre><code>{{ fullCodeContent }}</code></pre>
            </div>
          </div>
        </div>
      </el-drawer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ArrowRight, ChatDotRound, Clock, Delete, WarningFilled, InfoFilled, Document, DocumentCopy, PriceTag
} from '@element-plus/icons-vue'
import {
  aiAgentExecute,
  aiChatStream,
  aiExplain,
  clearAiSession,
  getAiSessionMessages,
  getAiTemperature,
  setAiTemperature
} from '@/api/ai'
import { getCodeSnippet } from '@/api/code'
import type { AgentExecuteResponse } from '@/api/ai'
import type { AIChatRequest, AIChatResponse, AIMessage } from '@/types'
import { copyToClipboard, extractErrorMessage, formatRelativeTime, getLanguageColor } from '@/utils/helpers'

// 表单数据
const form = reactive<AIChatRequest>({
  question: '',
  code: '',
  languageType: ''
})

// 模式：chat 对话 / explain 代码解释 / agent 编排
const mode = ref<'chat' | 'explain' | 'agent'>('chat')

// 加载状态
const loading = ref(false)

// 响应数据
const response = ref<AIChatResponse | null>(null)
const agentResponse = ref<AgentExecuteResponse | null>(null)
const hasResponse = ref(false)
const errorMessage = ref('')
const conversation = ref<AIMessage[]>([])
const temperature = ref(1.0)
const temperatureSaving = ref(false)
const showDetailDrawer = ref(false)
const selectedSearchItem = ref<AgentSearchItem | null>(null)
const fullCodeContent = ref('')
const loadingFullCode = ref(false)
const copying = ref(false)

const SESSION_STORAGE_KEY = 'codekit-ai-session-id'
const sessionId = ref<string | undefined>(undefined)

interface AgentSearchItem {
  id?: number
  fileName?: string
  filePath?: string
  languageType?: string
  codePreview?: string
  createTime?: string
  tags?: string[]
}

interface AgentVersionItem {
  id?: number
  snippetId?: number
  versionName?: string
  description?: string
  createTime?: string
}

const isRecord = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null
}

const extractAgentExplainAnswer = (payload: AgentExecuteResponse | null): string => {
  const explainResult = payload?.results?.find((item) => item.skillName === 'ai_explain')
  if (!explainResult || !explainResult.success || !isRecord(explainResult.data)) {
    return ''
  }
  const answer = explainResult.data.answer
  return typeof answer === 'string' ? answer.trim() : ''
}

const agentSearchItems = computed<AgentSearchItem[]>(() => {
  const target = agentResponse.value?.results?.find((item) => item.skillName === 'code_search')
  if (!target || !target.success || !isRecord(target.data)) {
    return []
  }
  const items = target.data.items
  if (!Array.isArray(items)) {
    return []
  }
  return items.filter(isRecord).map((item) => ({
    id: typeof item.id === 'number' ? item.id : undefined,
    fileName: typeof item.fileName === 'string' ? item.fileName : undefined,
    filePath: typeof item.filePath === 'string' ? item.filePath : undefined,
    languageType: typeof item.languageType === 'string' ? item.languageType : undefined,
    codePreview: typeof item.codePreview === 'string' ? item.codePreview : undefined,
    createTime: typeof item.createTime === 'string' ? item.createTime : undefined,
    tags: Array.isArray(item.tags) ? item.tags.filter((tag): tag is string => typeof tag === 'string') : []
  }))
})

const agentVersionItems = computed<AgentVersionItem[]>(() => {
  const target = agentResponse.value?.results?.find((item) => item.skillName === 'version_list')
  if (!target || !target.success || !isRecord(target.data)) {
    return []
  }
  const items = target.data.items
  if (!Array.isArray(items)) {
    return []
  }
  return items.filter(isRecord).map((item) => ({
    id: typeof item.id === 'number' ? item.id : undefined,
    snippetId: typeof item.snippetId === 'number' ? item.snippetId : undefined,
    versionName: typeof item.versionName === 'string' ? item.versionName : undefined,
    description: typeof item.description === 'string' ? item.description : undefined,
    createTime: typeof item.createTime === 'string' ? item.createTime : undefined
  }))
})

onMounted(async () => {
  const cachedSessionId = localStorage.getItem(SESSION_STORAGE_KEY) || undefined
  if (cachedSessionId) {
    sessionId.value = cachedSessionId
    try {
      const messages = await getAiSessionMessages(cachedSessionId, 10)
      conversation.value = messages
    } catch (error) {
      console.warn('加载会话历史失败', error)
    }
  }

  try {
    const currentTemperature = await getAiTemperature()
    if (typeof currentTemperature === 'number') {
      temperature.value = Number(currentTemperature.toFixed(1))
    }
  } catch (error) {
    console.warn('加载温度失败，使用默认值', error)
  }
})

const handleSaveTemperature = async () => {
  temperatureSaving.value = true
  try {
    const saved = await setAiTemperature(Number(temperature.value.toFixed(1)))
    temperature.value = Number(saved.toFixed(1))
    ElMessage.success(`温度已更新为 ${temperature.value.toFixed(1)}`)
  } catch (error: any) {
    ElMessage.error(extractErrorMessage(error, '温度设置失败'))
  } finally {
    temperatureSaving.value = false
  }
}

// 提交请求
const handleSubmit = async () => {
  // 验证输入
  if (mode.value === 'chat' && !form.question.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  if (mode.value === 'agent' && !form.question.trim()) {
    ElMessage.warning('请输入 Agent 指令')
    return
  }
  if (mode.value === 'explain' && !form.code?.trim()) {
    ElMessage.warning('请输入需要解释的代码')
    return
  }

  loading.value = true
  hasResponse.value = true
  response.value = null
  agentResponse.value = null
  errorMessage.value = ''

  try {
    const userContent = mode.value === 'chat'
      ? form.question.trim()
      : (mode.value === 'explain' ? (form.question?.trim() || '请解释我刚刚输入的代码') : form.question.trim())
    conversation.value.push({ role: 'user', content: userContent })

    const requestData: AIChatRequest = {
      question: userContent,
      code: mode.value === 'explain' ? form.code : undefined,
      languageType: mode.value === 'explain' ? form.languageType : undefined,
      sessionId: sessionId.value
    }

    if (mode.value === 'chat') {
      const assistantMessage: AIMessage = { role: 'assistant', content: '' }
      conversation.value.push(assistantMessage)
      await aiChatStream(requestData, {
        onChunk: (chunk, streamedSessionId) => {
          assistantMessage.content += chunk
          if (streamedSessionId) {
            sessionId.value = streamedSessionId
            localStorage.setItem(SESSION_STORAGE_KEY, streamedSessionId)
          }
        },
        onDone: (doneSessionId, answer) => {
          if (doneSessionId) {
            sessionId.value = doneSessionId
            localStorage.setItem(SESSION_STORAGE_KEY, doneSessionId)
          }
          response.value = {
            answer: answer || assistantMessage.content,
            sessionId: doneSessionId || sessionId.value
          }
        },
        onError: (message) => {
          throw new Error(message)
        }
      })
    } else if (mode.value === 'explain') {
      response.value = await aiExplain(requestData)
      if (response.value?.answer) {
        conversation.value.push({ role: 'assistant', content: response.value.answer })
      }
    } else {
      agentResponse.value = await aiAgentExecute({
        instruction: userContent,
        sessionId: sessionId.value
      })
      const explainAnswer = extractAgentExplainAnswer(agentResponse.value)
      conversation.value.push({
        role: 'assistant',
        content: explainAnswer || agentResponse.value.summary || 'Agent 执行完成'
      })
    }
  } catch (error: any) {
    console.error('AI 请求失败:', error)
    errorMessage.value = extractErrorMessage(error, '请求失败，请稍后重试')
    conversation.value.pop()
  } finally {
    loading.value = false
  }
}

// 清空表单
const handleClear = () => {
  form.question = ''
  form.code = ''
  form.languageType = ''
  response.value = null
  agentResponse.value = null
  errorMessage.value = ''
  hasResponse.value = false
}

const handleNewChat = async () => {
  const currentSessionId = sessionId.value
  if (currentSessionId) {
    try {
      await clearAiSession(currentSessionId)
    } catch (error) {
      console.warn('清空会话失败，将继续本地重置', error)
    }
  }
  sessionId.value = undefined
  localStorage.removeItem(SESSION_STORAGE_KEY)
  conversation.value = []
  response.value = null
  agentResponse.value = null
  errorMessage.value = ''
  hasResponse.value = false
  form.question = ''
  form.code = ''
}

// 复制代码
const copyCode = async (code: string) => {
  const success = await copyToClipboard(code)
  if (success) {
    ElMessage.success('代码已复制')
  } else {
    ElMessage.error('复制失败')
  }
}

const viewAgentSearchResult = async (item: AgentSearchItem) => {
  selectedSearchItem.value = item
  showDetailDrawer.value = true
  fullCodeContent.value = ''
  loadingFullCode.value = true
  try {
    if (typeof item.id === 'number') {
      const fullSnippet = await getCodeSnippet(item.id)
      fullCodeContent.value = fullSnippet.codeContent || item.codePreview || ''
    } else {
      fullCodeContent.value = item.codePreview || ''
    }
  } catch (error) {
    console.error('加载完整代码失败:', error)
    fullCodeContent.value = item.codePreview || ''
  } finally {
    loadingFullCode.value = false
  }
}

const copyDetailCode = async () => {
  if (!selectedSearchItem.value) return
  copying.value = true
  try {
    const codeToCopy = fullCodeContent.value || selectedSearchItem.value.codePreview || ''
    const success = await copyToClipboard(codeToCopy)
    if (success) {
      ElMessage.success('代码已复制')
    } else {
      ElMessage.error('复制失败')
    }
  } finally {
    copying.value = false
  }
}
</script>

<style scoped>
.ai-assistant {
  height: 100%;
  overflow-y: auto;
  padding: var(--spacing-xl);
}

.ai-container {
  max-width: 900px;
  margin: 0 auto;
}

.ai-header {
  text-align: center;
  margin-bottom: var(--spacing-2xl);
}

.title {
  font-size: var(--text-2xl);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-sm);
}

.subtitle {
  font-size: var(--text-base);
  color: var(--color-text-tertiary);
  margin: 0;
}

.input-section {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
  padding: var(--spacing-xl);
  margin-bottom: var(--spacing-xl);
}

.conversation-section {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

.message-item {
  margin-bottom: var(--spacing-md);
}

.message-item:last-child {
  margin-bottom: 0;
}

.message-role {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  margin-bottom: var(--spacing-2xs);
}

.message-content {
  border-radius: var(--radius-md);
  padding: var(--spacing-sm) var(--spacing-md);
  font-size: var(--text-sm);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-user .message-content {
  background: var(--color-primary-subtle);
  color: var(--color-primary);
}

.message-ai .message-content {
  background: var(--color-bg-sunken);
  color: var(--color-text-primary);
}

.input-row {
  display: flex;
  gap: var(--spacing-xl);
  margin-bottom: var(--spacing-lg);
  flex-wrap: wrap;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.temperature-group {
  margin-bottom: var(--spacing-md);
}

.temperature-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.input-group.code-group {
  width: 100%;
}

.input-label {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-secondary);
}

.mode-toggle {
  margin-left: auto;
}

.code-textarea :deep(textarea) {
  font-family: var(--font-mono);
}

.action-buttons {
  display: flex;
  gap: var(--spacing-sm);
  justify-content: flex-end;
  margin-top: var(--spacing-lg);
}

.response-section {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-default);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
  overflow: hidden;
}

.response-content {
  padding: var(--spacing-xl);
}

.error-block {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  background: var(--color-danger-subtle);
  border-radius: var(--radius-md);
  color: var(--color-danger);
  font-size: var(--text-sm);
}

.answer-block {
  margin-bottom: var(--spacing-lg);
}

.answer-block pre {
  margin: 0;
  padding: var(--spacing-lg);
  background: var(--color-bg-sunken);
  border-radius: var(--radius-md);
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.agent-block {
  margin-bottom: var(--spacing-lg);
}

.agent-summary {
  margin: 0 0 var(--spacing-lg);
  padding: var(--spacing-lg);
  background: var(--color-bg-sunken);
  border-radius: var(--radius-md);
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.block-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-md);
}

.suggestions-block {
  margin-bottom: var(--spacing-lg);
}

.suggestions-list {
  margin: 0;
  padding-left: var(--spacing-xl);
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  line-height: 1.8;
}

.suggestions-list li {
  margin-bottom: var(--spacing-xs);
}

.code-blocks {
  margin-bottom: var(--spacing-lg);
}

.code-block-item {
  margin-bottom: var(--spacing-md);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.code-block-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--color-bg-sunken);
  border-bottom: 1px solid var(--color-border-muted);
}

.code-language {
  font-size: var(--text-xs);
  font-weight: 500;
  padding: 2px 8px;
  background: var(--color-accent-subtle);
  color: var(--color-accent-primary);
  border-radius: var(--radius-sm);
}

.code-description {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

.code-content {
  margin: 0;
  padding: var(--spacing-md);
  background: var(--color-bg-sunken);
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  line-height: 1.5;
  overflow-x: auto;
}

.code-content code {
  white-space: pre;
}

.agent-result-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.agent-result-item {
  padding: var(--spacing-lg);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.agent-result-item:hover {
  border-color: var(--color-accent-primary);
  background-color: var(--color-bg-sunken);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
}

.result-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  min-width: 0;
}

.result-icon {
  font-size: 18px;
  color: var(--color-accent-primary);
  flex-shrink: 0;
}

.result-name {
  font-size: var(--text-base);
  font-weight: 500;
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.language-tag {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.result-path {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  margin-bottom: var(--spacing-md);
}

.result-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-meta {
  display: flex;
  gap: var(--spacing-lg);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.detail-icon {
  font-size: 24px;
  color: var(--color-accent-primary);
}

.detail-name {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
}

.detail-tags {
  display: flex;
  gap: var(--spacing-sm);
}

.detail-meta {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  background: var(--color-bg-sunken);
  border-radius: var(--radius-md);
}

.meta-row {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2xs);
}

.meta-label {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.meta-value {
  font-size: var(--text-sm);
  color: var(--color-text-primary);
}

.meta-value.mono {
  font-family: var(--font-mono);
  word-break: break-all;
}

.detail-code {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.empty-state {
  min-height: 240px;
}

.empty-state .empty-icon {
  font-size: 48px;
}

.empty-state p {
  font-size: var(--text-sm);
}
</style>
