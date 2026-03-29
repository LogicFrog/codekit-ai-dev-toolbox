<template>
  <el-drawer
    v-model="visible"
    title="AI 助手"
    size="480px"
    :close-on-click-modal="false"
    @closed="handleClose"
  >
    <div class="ai-drawer">
      <div class="ai-placeholder" v-if="!hasRealAI">
        <div class="placeholder-icon">
          <el-icon class="ai-icon"><ChatDotRound /></el-icon>
        </div>
        <h3 class="placeholder-title">AI 助手功能开发中</h3>
        <p class="placeholder-desc">该功能正在积极开发中，敬请期待</p>
        
        <div class="feature-list">
          <div class="feature-item">
            <el-icon class="feature-icon"><Check /></el-icon>
            <span>代码解释与优化建议</span>
          </div>
          <div class="feature-item">
            <el-icon class="feature-icon"><Check /></el-icon>
            <span>智能代码补全</span>
          </div>
          <div class="feature-item">
            <el-icon class="feature-icon"><Check /></el-icon>
            <span>Bug 检测与修复建议</span>
          </div>
          <div class="feature-item">
            <el-icon class="feature-icon"><Check /></el-icon>
            <span>代码重构建议</span>
          </div>
        </div>

        <div class="preview-notice">
          <el-icon><Warning /></el-icon>
          <span>以上功能预计在下一版本推出</span>
        </div>
      </div>

      <div class="ai-chat" v-else>
        <div class="chat-messages" ref="messagesRef">
          <div
            v-for="(message, index) in messages"
            :key="index"
            class="message-item"
            :class="{ user: message.role === 'user', assistant: message.role === 'assistant' }"
          >
            <div class="message-avatar">
              <el-icon v-if="message.role === 'user'"><User /></el-icon>
              <el-icon v-else><ChatDotRound /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-text">{{ message.content }}</div>
              <div class="message-time" v-if="message.timestamp">
                {{ formatTime(message.timestamp) }}
              </div>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="3"
            placeholder="输入您的问题..."
            resize="none"
            @keydown.enter.ctrl="handleSend"
          />
          <el-button type="primary" @click="handleSend" :loading="sending" :disabled="!inputText.trim()">
            <el-icon><Promotion /></el-icon>
            发送
          </el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { ChatDotRound, Check, Warning, User, Promotion } from '@element-plus/icons-vue'

interface Message {
  role: 'user' | 'assistant'
  content: string
  timestamp?: Date
}

interface Props {
  modelValue: boolean
  hasRealAI?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  hasRealAI: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'send', message: string): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const messages = ref<Message[]>([])
const inputText = ref('')
const sending = ref(false)
const messagesRef = ref<HTMLElement>()

const handleSend = async () => {
  if (!inputText.value.trim() || sending.value) return
  
  const userMessage = inputText.value.trim()
  messages.value.push({
    role: 'user',
    content: userMessage,
    timestamp: new Date()
  })
  
  inputText.value = ''
  sending.value = true
  
  emit('send', userMessage)
  
  try {
    await nextTick()
    scrollToBottom()
  } finally {
    sending.value = false
  }
}

const scrollToBottom = () => {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const formatTime = (date: Date): string => {
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const handleClose = () => {
  messages.value = []
  inputText.value = ''
}

watch(messages, () => {
  nextTick(() => scrollToBottom())
}, { deep: true })
</script>

<style scoped>
.ai-drawer {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ai-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-2xl);
  text-align: center;
}

.placeholder-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, var(--color-accent-primary), var(--color-accent-hover));
  border-radius: var(--radius-xl);
  margin-bottom: var(--spacing-xl);
}

.ai-icon {
  font-size: 40px;
  color: white;
}

.placeholder-title {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-sm);
}

.placeholder-desc {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  margin: 0 0 var(--spacing-2xl);
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  width: 100%;
  max-width: 280px;
  margin-bottom: var(--spacing-2xl);
}

.feature-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.feature-icon {
  color: var(--color-success);
}

.preview-notice {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--color-warning-muted);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--color-warning);
}

.ai-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.message-item {
  display: flex;
  gap: var(--spacing-sm);
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: var(--color-bg-muted);
  border-radius: 50%;
  flex-shrink: 0;
}

.message-item.assistant .message-avatar {
  background: var(--color-accent-subtle);
  color: var(--color-accent-primary);
}

.message-item.user .message-avatar {
  background: var(--color-primary-subtle);
  color: var(--color-primary);
}

.message-content {
  max-width: 80%;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.message-text {
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  line-height: 1.5;
}

.message-item.assistant .message-text {
  background: var(--color-bg-sunken);
  color: var(--color-text-primary);
}

.message-item.user .message-text {
  background: var(--color-accent-primary);
  color: white;
}

.message-time {
  font-size: 10px;
  color: var(--color-text-muted);
}

.chat-input {
  padding: var(--spacing-lg);
  border-top: 1px solid var(--color-border-muted);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.chat-input .el-button {
  align-self: flex-end;
}
</style>
