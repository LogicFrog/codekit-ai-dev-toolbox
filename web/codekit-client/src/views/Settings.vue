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
          <el-radio-group v-model="settings.theme" size="default">
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
          <el-select v-model="settings.editorTheme" style="width: 200px">
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
          <el-input-number v-model="settings.fontSize" :min="12" :max="24" />
        </div>
      </div>

      <div class="settings-section">
        <h2 class="section-title">代码管理</h2>
        
        <div class="setting-item">
          <div class="setting-info">
            <span class="setting-label">默认分页大小</span>
            <span class="setting-desc">代码列表每页显示数量</span>
          </div>
          <el-select v-model="settings.pageSize" style="width: 200px">
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
          <el-switch v-model="settings.autoSave" />
        </div>
      </div>

      <div class="settings-section">
        <h2 class="section-title">AI 设置</h2>
        
        <div class="setting-item disabled">
          <div class="setting-info">
            <span class="setting-label">
              AI 模型
              <span class="preview-badge">预留</span>
            </span>
            <span class="setting-desc">选择 AI 服务的模型</span>
          </div>
          <el-select v-model="settings.aiModel" disabled style="width: 200px">
            <el-option value="gpt-4" label="GPT-4" />
            <el-option value="gpt-3.5" label="GPT-3.5" />
            <el-option value="claude" label="Claude" />
          </el-select>
        </div>

        <div class="setting-item disabled">
          <div class="setting-info">
            <span class="setting-label">
              API Key
              <span class="preview-badge">预留</span>
            </span>
            <span class="setting-desc">配置 AI 服务的 API 密钥</span>
          </div>
          <el-input 
            v-model="settings.apiKey" 
            type="password" 
            placeholder="未配置" 
            disabled 
            style="width: 300px"
          />
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
            <span class="about-value">Vue 3 + TypeScript + Element Plus</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'

const settings = reactive({
  theme: 'light',
  editorTheme: 'vs-light',
  fontSize: 14,
  pageSize: 20,
  autoSave: false,
  aiModel: 'gpt-4',
  apiKey: ''
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

.setting-item.disabled {
  opacity: 0.6;
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
