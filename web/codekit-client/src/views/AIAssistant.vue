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
          <div class="input-group">
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
            </el-radio-group>
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
            {{ mode === 'explain' ? '问题描述（可选）' : '问题' }}
          </label>
          <el-input
            v-model="form.question"
            type="textarea"
            :rows="3"
            :placeholder="mode === 'explain' ? '有什么想问的？' : '输入你的问题...'"
          />
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button type="primary" size="large" @click="handleSubmit" :loading="loading">
            <el-icon><ChatDotRound /></el-icon>
            {{ mode === 'explain' ? '解释代码' : '发送' }}
          </el-button>
          <el-button size="large" @click="handleClear">
            <el-icon><Delete /></el-icon>
            清空
          </el-button>
        </div>
      </div>

      <!-- 响应区域 -->
      <div class="response-section" v-if="hasResponse">
        <div class="response-header">
          <span class="response-title">AI 回复</span>
          <el-tag v-if="response?.error" type="danger" size="small">错误</el-tag>
        </div>

        <div class="response-content" v-loading="loading">
          <!-- 错误信息 -->
          <div class="error-block" v-if="response?.error">
            <el-icon><WarningFilled /></el-icon>
            <span>{{ response.error }}</span>
          </div>

          <!-- 回答内容 -->
          <div class="answer-block" v-if="response?.answer">
            <pre>{{ response.answer }}</pre>
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound, Delete, WarningFilled, InfoFilled, Document, DocumentCopy
} from '@element-plus/icons-vue'
import { aiChat, aiExplain } from '@/api/ai'
import type { AIChatRequest, AIChatResponse } from '@/types'

// 表单数据
const form = reactive<AIChatRequest>({
  question: '',
  code: '',
  languageType: ''
})

// 模式：chat 对话 / explain 代码解释
const mode = ref<'chat' | 'explain'>('chat')

// 加载状态
const loading = ref(false)

// 响应数据
const response = ref<AIChatResponse | null>(null)
const hasResponse = ref(false)

// 会话ID（简单生成）
const sessionId = ref(`session-${Date.now()}`)

// 提交请求
const handleSubmit = async () => {
  // 验证输入
  if (mode.value === 'chat' && !form.question.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  if (mode.value === 'explain' && !form.code.trim()) {
    ElMessage.warning('请输入需要解释的代码')
    return
  }

  loading.value = true
  hasResponse.value = true
  response.value = null

  try {
    const requestData: AIChatRequest = {
      question: form.question,
      code: form.code,
      languageType: form.languageType,
      sessionId: sessionId.value
    }

    if (mode.value === 'chat') {
      response.value = await aiChat(requestData)
    } else {
      response.value = await aiExplain(requestData)
    }
  } catch (error: any) {
    console.error('AI 请求失败:', error)
    response.value = {
      answer: '',
      error: error.message || '请求失败，请稍后重试'
    }
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
  hasResponse.value = false
}

// 复制代码
const copyCode = async (code: string) => {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('代码已复制')
  } catch {
    ElMessage.error('复制失败')
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
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  margin-bottom: var(--spacing-xl);
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
  gap: var(--spacing-md);
  justify-content: flex-end;
  margin-top: var(--spacing-lg);
}

.response-section {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.response-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-xl);
  background: var(--color-bg-sunken);
  border-bottom: 1px solid var(--color-border-muted);
}

.response-title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text-primary);
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

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-4xl);
  color: var(--color-text-tertiary);
}

.empty-state .empty-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-md);
  opacity: 0.5;
}

.empty-state p {
  margin: 0;
  font-size: var(--text-sm);
}
</style>
