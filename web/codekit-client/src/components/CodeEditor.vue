<template>
  <div ref="editorContainer" class="code-editor-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, shallowRef } from 'vue'
import * as monaco from 'monaco-editor'
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker'

(self as any).MonacoEnvironment = {
  getWorker(_: any, _label: string) {
    return new editorWorker()
  }
}

interface Props {
  modelValue?: string
  language?: string
  theme?: 'vs-dark' | 'vs-light' | 'hc-black'
  readOnly?: boolean
  fontSize?: number
  minimap?: boolean
  lineNumbers?: 'on' | 'off' | 'relative'
  wordWrap?: 'on' | 'off' | 'wordWrapColumn' | 'bounded'
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  language: 'plaintext',
  theme: 'vs-light',
  readOnly: false,
  fontSize: 13,
  minimap: false,
  lineNumbers: 'on',
  wordWrap: 'off'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'change', value: string): void
  (e: 'ready', editor: monaco.editor.IStandaloneCodeEditor): void
}>()

const editorContainer = ref<HTMLDivElement>()
const editor = shallowRef<monaco.editor.IStandaloneCodeEditor | null>(null)
let textModel: monaco.editor.ITextModel | null = null

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
  rust: 'rust',
  c: 'c',
  'c++': 'cpp',
  cpp: 'cpp',
  'c#': 'csharp',
  csharp: 'csharp',
  php: 'php',
  ruby: 'ruby',
  swift: 'swift',
  kotlin: 'kotlin',
  scala: 'scala',
  shell: 'shell',
  bash: 'shell',
  plaintext: 'plaintext'
}

const getMonacoLanguage = (lang: string): string => {
  return languageMap[lang.toLowerCase()] || 'plaintext'
}

const initEditor = () => {
  if (!editorContainer.value) return

  textModel = monaco.editor.createModel(
    props.modelValue || '',
    getMonacoLanguage(props.language || 'plaintext')
  )

  editor.value = monaco.editor.create(editorContainer.value, {
    value: textModel.getValue(),
    language: getMonacoLanguage(props.language || 'plaintext'),
    theme: props.theme,
    readOnly: props.readOnly,
    fontSize: props.fontSize,
    minimap: { enabled: props.minimap },
    lineNumbers: props.lineNumbers,
    wordWrap: props.wordWrap,
    automaticLayout: true,
    scrollBeyondLastLine: false,
    folding: true,
    foldingHighlight: true,
    showFoldingControls: 'mouseover',
    bracketPairColorization: { enabled: true },
    smoothScrolling: true,
    cursorBlinking: 'smooth',
    renderLineHighlight: 'line',
    tabSize: 2,
    insertSpaces: true,
    detectIndentation: true,
    trimAutoWhitespace: true,
    formatOnPaste: true,
    formatOnType: false,
    autoIndent: 'full',
    suggestOnTriggerCharacters: true,
    acceptSuggestionOnEnter: 'smart',
    quickSuggestions: {
      other: true,
      comments: false,
      strings: true
    },
    scrollbar: {
      verticalScrollbarSize: 10,
      horizontalScrollbarSize: 10
    }
  })

  editor.value.onDidChangeModelContent(() => {
    const value = editor.value?.getValue() || ''
    emit('update:modelValue', value)
    emit('change', value)
  })

  emit('ready', editor.value)
}

watch(() => props.modelValue, (newValue) => {
  if (textModel && newValue !== textModel.getValue()) {
    textModel.setValue(newValue || '')
  }
})

watch(() => props.language, (newLang) => {
  if (textModel && editor.value) {
    monaco.editor.setModelLanguage(textModel, getMonacoLanguage(newLang || 'plaintext'))
  }
})

watch(() => props.theme, (newTheme) => {
  monaco.editor.setTheme(newTheme)
})

watch(() => props.readOnly, (newReadOnly) => {
  editor.value?.updateOptions({ readOnly: newReadOnly })
})

watch(editorContainer, () => {
  editor.value?.layout()
})

onMounted(() => {
  initEditor()
})

onBeforeUnmount(() => {
  if (editor.value) {
    editor.value.dispose()
    editor.value = null
  }
  if (textModel) {
    textModel.dispose()
    textModel = null
  }
})

defineExpose({
  getEditor: () => editor.value,
  getValue: () => editor.value?.getValue() || '',
  setValue: (value: string) => editor.value?.setValue(value),
  focus: () => editor.value?.focus(),
  layout: () => editor.value?.layout()
})
</script>

<style scoped>
.code-editor-container {
  width: 100%;
  height: 100%;
  min-height: 200px;
  overflow: hidden;
}
</style>
