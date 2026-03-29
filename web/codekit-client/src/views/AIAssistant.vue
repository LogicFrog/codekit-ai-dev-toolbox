<template>
  <div class="ai-assistant">
    <div class="coming-soon">
      <div class="icon-wrapper">
        <el-icon class="ai-icon"><ChatDotRound /></el-icon>
      </div>
      
      <h1 class="title">AI 助手</h1>
      <p class="subtitle">智能代码辅助功能正在开发中</p>
      
      <div class="features">
        <div class="feature-card">
          <div class="feature-icon">
            <el-icon><Document /></el-icon>
          </div>
          <h3>代码解释</h3>
          <p>智能分析代码逻辑，提供清晰的代码解释和文档</p>
        </div>
        
        <div class="feature-card">
          <div class="feature-icon">
            <el-icon><MagicStick /></el-icon>
          </div>
          <h3>代码优化</h3>
          <p>自动检测代码问题，提供性能优化和重构建议</p>
        </div>
        
        <div class="feature-card">
          <div class="feature-icon">
            <el-icon><WarningFilled /></el-icon>
          </div>
          <h3>Bug 检测</h3>
          <p>智能识别潜在 Bug，给出修复方案和最佳实践</p>
        </div>
        
        <div class="feature-card">
          <div class="feature-icon">
            <el-icon><ChatLineSquare /></el-icon>
          </div>
          <h3>智能问答</h3>
          <p>自然语言交互，快速获取代码相关的答案和建议</p>
        </div>
      </div>
      
      <div class="timeline">
        <h3>开发进度</h3>
        <div class="timeline-items">
          <div class="timeline-item completed">
            <span class="timeline-dot"></span>
            <span class="timeline-text">基础架构设计</span>
          </div>
          <div class="timeline-item completed">
            <span class="timeline-dot"></span>
            <span class="timeline-text">LLM 接口集成</span>
          </div>
          <div class="timeline-item active">
            <span class="timeline-dot"></span>
            <span class="timeline-text">提示词优化</span>
          </div>
          <div class="timeline-item">
            <span class="timeline-dot"></span>
            <span class="timeline-text">功能测试</span>
          </div>
          <div class="timeline-item">
            <span class="timeline-dot"></span>
            <span class="timeline-text">正式发布</span>
          </div>
        </div>
      </div>
      
      <div class="notice">
        <el-icon><InfoFilled /></el-icon>
        <span>该功能预计在下一版本中推出，敬请期待</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ChatDotRound, Document, MagicStick, ChatLineSquare, InfoFilled, WarningFilled } from '@element-plus/icons-vue'
</script>

<style scoped>
.ai-assistant {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-4xl);
}

.coming-soon {
  max-width: 800px;
  text-align: center;
}

.icon-wrapper {
  display: inline-flex;
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

.title {
  font-size: var(--text-3xl);
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-sm);
}

.subtitle {
  font-size: var(--text-lg);
  color: var(--color-text-tertiary);
  margin: 0 0 var(--spacing-3xl);
}

.features {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-3xl);
}

.feature-card {
  padding: var(--spacing-xl);
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border-muted);
  border-radius: var(--radius-lg);
  text-align: left;
  transition: all var(--transition-fast);
}

.feature-card:hover {
  border-color: var(--color-accent-primary);
  box-shadow: var(--shadow-sm);
}

.feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: var(--color-accent-subtle);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-md);
}

.feature-icon .el-icon {
  font-size: 20px;
  color: var(--color-accent-primary);
}

.feature-card h3 {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-sm);
}

.feature-card p {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  margin: 0;
  line-height: 1.5;
}

.timeline {
  margin-bottom: var(--spacing-2xl);
}

.timeline h3 {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-lg);
}

.timeline-items {
  display: flex;
  justify-content: center;
  gap: var(--spacing-xl);
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  position: relative;
}

.timeline-item:not(:last-child)::after {
  content: '';
  position: absolute;
  right: calc(-1 * var(--spacing-xl) + 4px);
  width: calc(var(--spacing-xl) - 8px);
  height: 2px;
  background: var(--color-border-default);
}

.timeline-item.completed:not(:last-child)::after {
  background: var(--color-success);
}

.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--color-bg-muted);
  border: 2px solid var(--color-border-default);
  flex-shrink: 0;
}

.timeline-item.completed .timeline-dot {
  background: var(--color-success);
  border-color: var(--color-success);
}

.timeline-item.active .timeline-dot {
  background: var(--color-accent-primary);
  border-color: var(--color-accent-primary);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { 
    box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.4);
  }
  50% { 
    box-shadow: 0 0 0 6px rgba(37, 99, 235, 0);
  }
}

.timeline-text {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  white-space: nowrap;
}

.timeline-item.completed .timeline-text {
  color: var(--color-text-secondary);
}

.timeline-item.active .timeline-text {
  color: var(--color-accent-primary);
  font-weight: 500;
}

.notice {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-xl);
  background: var(--color-info-muted);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--color-info);
}

.notice .el-icon {
  font-size: 18px;
}
</style>
