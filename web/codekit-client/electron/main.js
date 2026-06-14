import { app, BrowserWindow, dialog } from 'electron'
import { spawn } from 'child_process'
import path from 'path'
import fs from 'fs'
import http from 'http'
import os from 'os'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const isDev = process.env.NODE_ENV === 'development' || !app.isPackaged

let mainWindow = null
let backendProcess = null
const BACKEND_PORT = 18080

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
  if (process.env.JAVA_HOME) return path.join(process.env.JAVA_HOME, 'bin', 'java')
  const candidates = [
    '/opt/homebrew/opt/openjdk@21/bin/java',
    '/opt/homebrew/opt/openjdk/bin/java',
    '/opt/homebrew/bin/java',
    '/usr/local/bin/java',
    '/usr/bin/java'
  ]
  for (const p of candidates) {
    if (fs.existsSync(p)) return p
  }
  return 'java'
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
  const homeCodekitDir = path.join(os.homedir(), '.codekit', 'data')
  fs.mkdirSync(dataDir, { recursive: true })
  fs.mkdirSync(homeCodekitDir, { recursive: true })

  const logFile = path.join(dataDir, 'backend.log')
  const logStream = fs.createWriteStream(logFile, { flags: 'a' })

  backendProcess = spawn(java, [
    '-jar', jarPath,
    `--ai.settings.store-file=${path.join(dataDir, 'ai-settings.json')}`,
    `--ai.session.store-file=${path.join(dataDir, 'ai-sessions.json')}`,
    `--codekit.fs.workspace-root=${os.homedir()}`,
    '--spring.profiles.active=electron'
  ], {
    cwd: dataDir,
    env: { ...process.env }
  })

  backendProcess.stdout.on('data', (data) => { const s = data.toString(); console.log(s); logStream.write(s) })
  backendProcess.stderr.on('data', (data) => { const s = data.toString(); console.error(s); logStream.write(s) })
  backendProcess.on('close', (code) => { console.log(`后端退出: ${code}`); logStream.end() })
  backendProcess.on('error', (err) => {
    console.error(`后端启动失败: ${err.message}`)
    logStream.write(`启动失败: ${err.message}\n`)
    logStream.end()
    dialog.showErrorBox('启动失败', `无法启动后端\n\n${err.message}\n\n请确认已安装 JDK 21:\nbrew install openjdk@21`)
    app.quit()
  })
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
