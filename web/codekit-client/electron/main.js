import { app, BrowserWindow, dialog } from 'electron'
import { spawn } from 'child_process'
import path from 'path'
import fs from 'fs'
import http from 'http'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const isDev = process.env.NODE_ENV === 'development' || !app.isPackaged

let mainWindow = null
let backendProcess = null
const BACKEND_PORT = 8080

function findJar() {
  const candidates = [
    path.join(process.resourcesPath || '', 'codekit.jar'),
    path.join(__dirname, '..', 'build', 'codekit.jar'),
    path.join(__dirname, '..', '..', 'target', 'codekit-0.0.1-SNAPSHOT.jar')
  ]
  for (const p of candidates) {
    if (fs.existsSync(p)) return p
  }
  return null
}

function getJavaPath() {
  return process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', 'java') : 'java'
}

function waitForBackend(retries = 30, delay = 1000) {
  return new Promise((resolve, reject) => {
    let attempts = 0
    const check = () => {
      attempts++
      const req = http.get(`http://localhost:${BACKEND_PORT}/api/ai/settings/providers`, (res) => {
        res.resume()
        if (res.statusCode < 500) {
          resolve()
        } else if (attempts < retries) {
          setTimeout(check, delay)
        } else {
          reject(new Error(`后端启动超时 (状态码: ${res.statusCode})`))
        }
      })
      req.on('error', () => {
        if (attempts < retries) {
          setTimeout(check, delay)
        } else {
          reject(new Error(`后端启动超时 (已尝试 ${retries} 次)`))
        }
      })
      req.end()
    }
    check()
  })
}

function startBackend() {
  const jarPath = findJar()
  if (!jarPath) {
    dialog.showErrorBox('启动失败', '找不到后端 JAR 文件\n\n请确保已构建后端: ./mvnw package -DskipTests')
    app.quit()
    return
  }

  const java = getJavaPath()
  console.log(`启动后端: ${java} -jar ${jarPath}`)

  const dataDir = path.join(app.getPath('userData'), 'data')

  backendProcess = spawn(java, [
    '-jar', jarPath,
    `--ai.settings.store-file=${path.join(dataDir, 'ai-settings.json')}`,
    `--ai.session.store-file=${path.join(dataDir, 'ai-sessions.json')}`,
    '--spring.profiles.active=electron'
  ], {
    cwd: app.getPath('userData'),
    env: {
      ...process.env,
      CODEKIT_AI_PROVIDER: process.env.CODEKIT_AI_PROVIDER || 'mock',
      CODEKIT_AI_API_KEY: process.env.CODEKIT_AI_API_KEY || '',
      CODEKIT_DB_URL: process.env.CODEKIT_DB_URL || 'jdbc:mysql://localhost:3306/codekit?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true',
      CODEKIT_DB_USERNAME: process.env.CODEKIT_DB_USERNAME || 'root',
      CODEKIT_DB_PASSWORD: process.env.CODEKIT_DB_PASSWORD || '12345678',
      CODEKIT_REDIS_HOST: process.env.CODEKIT_REDIS_HOST || 'localhost'
    }
  })

  backendProcess.stdout.on('data', (data) => console.log(`[后端] ${data.toString().trim()}`))
  backendProcess.stderr.on('data', (data) => console.error(`[后端] ${data.toString().trim()}`))
  backendProcess.on('close', (code) => console.log(`后端进程退出: ${code}`))
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 900,
    minHeight: 600,
    title: 'CodeKit',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true
    }
  })

  if (isDev) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL || 'http://localhost:5173')
    mainWindow.webContents.openDevTools()
  } else {
    mainWindow.loadFile(path.join(__dirname, '..', 'dist', 'index.html'))
  }

  mainWindow.on('closed', () => { mainWindow = null })
}

app.whenReady().then(async () => {
  if (!isDev) {
    startBackend()
    try {
      await waitForBackend()
    } catch (err) {
      dialog.showErrorBox('启动失败', err.message)
      app.quit()
      return
    }
  }
  createWindow()
})

app.on('before-quit', () => {
  if (backendProcess) {
    console.log('关闭后端...')
    backendProcess.kill('SIGTERM')
    setTimeout(() => { if (backendProcess) backendProcess.kill('SIGKILL') }, 5000)
  }
})

app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit() })
app.on('activate', () => { if (!mainWindow) createWindow() })
