<template>
  <div class="layout">
    <aside
      class="sidebar"
      @mouseenter="expanded = true"
      @mouseleave="expanded = false"
    >
      <div class="sidebar-header">
        <div class="logo">
          <svg class="logo-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M8 9L12 5L16 9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M12 5V19" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M5 12H3C2.46957 12 1.96086 12.2107 1.58579 12.5858C1.21071 12.9609 1 13.4696 1 14V20C1 20.5304 1.21071 21.0391 1.58579 21.4142C1.96086 21.7893 2.46957 22 3 22H21C21.5304 22 22.0391 21.7893 22.4142 21.4142C22.7893 21.0391 23 20.5304 23 20V14C23 13.4696 22.7893 12.9609 22.4142 12.5858C22.0391 12.2107 21.5304 12 21 12H19" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span class="logo-text" v-show="expanded">CodeKit</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          :title="!expanded ? item.title : undefined"
        >
          <el-icon class="nav-icon">
            <component :is="item.icon" />
          </el-icon>
          <span class="nav-text" v-show="expanded">{{ item.title }}</span>
          <span v-if="item.status === 'preview' && expanded" class="preview-badge">预留</span>
        </router-link>
      </nav>

      <div class="sidebar-footer" v-show="expanded">
        <div class="status-indicator">
          <span class="status-dot"></span>
          <span class="status-text">本地环境</span>
        </div>
      </div>
    </aside>

    <main class="main">
      <div class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { Folder, Search, Timer, ChatDotRound, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const expanded = ref(false)

const menuItems = [
  { path: '/code-manager', title: '代码管理', icon: Folder, status: 'full' },
  { path: '/search-center', title: '检索中心', icon: Search, status: 'full' },
  { path: '/version-control', title: '版本管理', icon: Timer, status: 'limited' },
  { path: '/ai-assistant', title: 'AI 助手', icon: ChatDotRound, status: 'full' },
  { path: '/settings', title: '设置', icon: Setting, status: 'full' }
]

const isActive = (path: string) => {
  return route.path === path
}
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  background-color: var(--color-bg-base);
}

.sidebar {
  width: var(--sidebar-collapsed-width);
  background-color: var(--color-bg-elevated);
  border-right: 1px solid var(--color-border-muted);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.sidebar:hover {
  width: var(--sidebar-width);
}

.sidebar-header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  padding: 0 var(--spacing-lg);
  border-bottom: 1px solid var(--color-border-muted);
  white-space: nowrap;
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.logo-icon {
  width: 24px;
  height: 24px;
  color: var(--color-accent-primary);
  flex-shrink: 0;
}

.logo-text {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.sidebar:hover .logo-text {
  opacity: 1;
}

.sidebar-nav {
  flex: 1;
  padding: var(--spacing-md);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  overflow-y: auto;
  overflow-x: hidden;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  text-decoration: none;
  transition: all var(--transition-fast);
  position: relative;
  white-space: nowrap;
}

.sidebar:not(:hover) .nav-item {
  justify-content: center;
}

.nav-item:hover {
  background-color: var(--color-bg-muted);
  color: var(--color-text-primary);
}

.nav-item.active {
  background-color: var(--color-accent-subtle);
  color: var(--color-accent-primary);
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background-color: var(--color-accent-primary);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.nav-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.nav-text {
  flex: 1;
  font-size: var(--text-sm);
  font-weight: 500;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.sidebar:hover .nav-text {
  opacity: 1;
}

.preview-badge {
  flex-shrink: 0;
}

.sidebar-footer {
  padding: var(--spacing-lg);
  border-top: 1px solid var(--color-border-muted);
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background-color: var(--color-success);
  box-shadow: 0 0 0 2px var(--color-bg-elevated);
}

.status-text {
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.content {
  flex: 1;
  overflow: hidden;
}

@media (max-width: 768px) {
  .sidebar {
    width: var(--sidebar-width);
  }
  .sidebar:hover {
    width: var(--sidebar-width);
  }
  .logo-text,
  .nav-text {
    opacity: 1;
  }
}
</style>
