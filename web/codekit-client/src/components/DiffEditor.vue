<template>
  <div ref="diffContainer" class="diff-editor-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as monaco from 'monaco-editor'

interface Props {
  originalContent?: string
  modifiedContent?: string
  originalLanguage?: string
  modifiedLanguage?: string
  theme?: 'vs-dark' | 'vs-light' | 'hc-black'
  readOnly?: boolean
  fontSize?: number
  renderSideBySide?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  originalContent: '',
  modifiedContent: '',
  originalLanguage: 'plaintext',
  modifiedLanguage: 'plaintext',
  theme: 'vs-light',
  readOnly: true,
  fontSize: 13,
  renderSideBySide: true
})

const emit = defineEmits<{
  (e: 'ready', diffEditor: monaco.editor.IStandaloneDiffEditor): void
}>()

const diffContainer = ref<HTMLDivElement>()
let diffEditor: monaco.editor.IStandaloneDiffEditor | null = null
let originalModel: monaco.editor.ITextModel | null = null
let modifiedModel: monaco.editor.ITextModel | null = null

const languageMap: Record<string, string> = {
  java: 'java',
  python: 'python',
  javascript: 'javascript',
  typescript: 'typescript',
  js: 'javascript',
  ts: 'typescript',
  html: 'html',
  css: 'css',
  json: 'json',
  xml: 'xml',
  sql: 'sql',
  md: 'markdown',
  markdown: 'markdown',
  go: 'go',
  rust: 'rust'
}

const getMonacoLanguage = (lang: string): string => {
  return languageMap[lang.toLowerCase()] || 'plaintext'
}

const initDiffEditor = () => {
  if (!diffContainer.value) return

  originalModel = monaco.editor.createModel(
    props.originalContent || '',
    getMonacoLanguage(props.originalLanguage || 'plaintext')
  )

  modifiedModel = monaco.editor.createModel(
    props.modifiedContent || '',
    getMonacoLanguage(props.modifiedLanguage || 'plaintext')
  )

  diffEditor = monaco.editor.createDiffEditor(diffContainer.value, {
    theme: props.theme,
    readOnly: props.readOnly,
    fontSize: props.fontSize,
    renderSideBySide: props.renderSideBySide,
    automaticLayout: true,
    scrollBeyondLastLine: false,
    folding: true,
    foldingHighlight: true,
    showFoldingControls: 'mouseover',
    bracketPairColorization: { enabled: true },
    smoothScrolling: true,
    cursorBlinking: 'smooth',
    minimap: { enabled: false },
    lineNumbers: 'on',
    renderLineHighlight: 'line',
    ignoreTrimWhitespace: false,
    renderOverviewRuler: true,
    diffWordWrap: 'off',
    diffAlgorithm: 'advanced'
  })

  diffEditor.setModel({
    original: originalModel,
    modified: modifiedModel
  })

  emit('ready', diffEditor)
}

const getDiff = (): { added: number; removed: number; modifications: number } => {
  if (!diffEditor) return { added: 0, removed: 0, modifications: 0 }

  const diff = diffEditor.getLineChanges()
  if (!diff || diff.length === 0) return { added: 0, removed: 0, modifications: 0 }

  let added = 0
  let removed = 0
  let modifications = 0

  diff.forEach((change) => {
    const originalLines = change.originalEndLineNumber === 0 ? 0 : change.originalEndLineNumber - change.originalStartLineNumber + 1
    const modifiedLines = change.modifiedEndLineNumber === 0 ? 0 : change.modifiedEndLineNumber - change.modifiedStartLineNumber + 1

    if (originalLines === 0 && modifiedLines > 0) {
      added += modifiedLines
    } else if (originalLines > 0 && modifiedLines === 0) {
      removed += originalLines
    } else {
      modifications += 1
    }
  })

  return { added, removed, modifications }
}

watch(() => props.originalContent, (newValue) => {
  if (originalModel) {
    originalModel.setValue(newValue || '')
  }
})

watch(() => props.modifiedContent, (newValue) => {
  if (modifiedModel) {
    modifiedModel.setValue(newValue || '')
  }
})

watch(() => props.theme, (newTheme) => {
  monaco.editor.setTheme(newTheme)
})

watch(() => props.renderSideBySide, (newValue) => {
  if (diffEditor) {
    diffEditor.updateOptions({ renderSideBySide: newValue })
  }
})

onMounted(() => {
  initDiffEditor()
})

onBeforeUnmount(() => {
  if (diffEditor) {
    diffEditor.dispose()
    diffEditor = null
  }
  if (originalModel) {
    originalModel.dispose()
    originalModel = null
  }
  if (modifiedModel) {
    modifiedModel.dispose()
    modifiedModel = null
  }
})

defineExpose({
  getDiffEditor: () => diffEditor,
  getDiff
})
</script>

<style scoped>
.diff-editor-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>
