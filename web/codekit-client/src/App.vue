<template>
  <router-view />
</template>

<script setup lang="ts">
import { watch, onMounted } from 'vue'
import { useSettingsStore } from '@/stores/settings'

const store = useSettingsStore()

function applyTheme(val: string) {
  if (val === 'dark') {
    document.documentElement.setAttribute('data-theme', 'dark')
    localStorage.setItem('codekit-theme', 'dark')
  } else {
    document.documentElement.removeAttribute('data-theme')
    localStorage.setItem('codekit-theme', 'light')
  }
}

watch(() => store.settings.theme, (val) => {
  applyTheme(val)
}, { immediate: true })

onMounted(() => {
  const saved = localStorage.getItem('codekit-theme')
  if (saved === 'dark' || saved === 'light') {
    store.settings.theme = saved
  }
})
</script>

<style>
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
}

#app {
  height: 100%;
}
</style>
