import { contextBridge } from 'electron'

contextBridge.exposeInMainWorld('codekit', {
  getVersion: () => '1.0.0',
  platform: process.platform
})
