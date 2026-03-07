const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('api', {
  saveFile: (filePath, content) => ipcRenderer.invoke('save-file', { filePath, content }),
  saveDialog: (defaultPath, filters) => ipcRenderer.invoke('save-dialog', { defaultPath, filters }),
  openDialog: (filters) => ipcRenderer.invoke('open-dialog', { filters }),
  readFile: (filePath) => ipcRenderer.invoke('read-file', filePath),
  getProjectRoot: () => ipcRenderer.invoke('get-project-root'),
  exportToMod: (files) => ipcRenderer.invoke('export-to-mod', { files }),
  onMcpStateUpdate: (callback) => ipcRenderer.on('mcp-state-update', (_event, content) => callback(content)),
});
