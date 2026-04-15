import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    status?: 'full' | 'limited' | 'preview'
  }
}

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/code-manager',
    children: [
      {
        path: 'code-manager',
        name: 'CodeManager',
        component: () => import('@/views/CodeManager.vue'),
        meta: { title: '代码管理', icon: 'Folder', status: 'full' }
      },
      {
        path: 'search-center',
        name: 'SearchCenter',
        component: () => import('@/views/SearchCenter.vue'),
        meta: { title: '检索中心', icon: 'Search', status: 'full' }
      },
      {
        path: 'version-control',
        name: 'VersionControl',
        component: () => import('@/views/VersionControl.vue'),
        meta: { title: '版本管理', icon: 'Timer', status: 'limited' }
      },
      {
        path: 'ai-assistant',
        name: 'AIAssistant',
        component: () => import('@/views/AIAssistant.vue'),
        meta: { title: 'AI 助手', icon: 'ChatDotRound', status: 'full' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '设置', icon: 'Setting', status: 'full' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
