const { app, BrowserWindow, ipcMain, dialog } = require('electron');
const path = require('path');
const fs = require('fs');

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 1200,
    minHeight: 700,
    title: 'RetroNism Mod Maker',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    }
  });

  mainWindow.loadFile('src/index.html');
  mainWindow.setMenuBarVisibility(false);
}

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  app.quit();
});

// IPC handlers for file operations
ipcMain.handle('save-file', async (event, { filePath, content }) => {
  try {
    const dir = path.dirname(filePath);
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(filePath, content, 'utf-8');
    return { success: true };
  } catch (err) {
    return { success: false, error: err.message };
  }
});

ipcMain.handle('save-dialog', async (event, { defaultPath, filters }) => {
  const result = await dialog.showSaveDialog(mainWindow, {
    defaultPath,
    filters: filters || [{ name: 'All Files', extensions: ['*'] }]
  });
  return result.canceled ? null : result.filePath;
});

ipcMain.handle('open-dialog', async (event, { filters }) => {
  const result = await dialog.showOpenDialog(mainWindow, {
    filters: filters || [{ name: 'JSON', extensions: ['json'] }],
    properties: ['openFile']
  });
  return result.canceled ? null : result.filePaths[0];
});

ipcMain.handle('read-file', async (event, filePath) => {
  try {
    return fs.readFileSync(filePath, 'utf-8');
  } catch (err) {
    return null;
  }
});

ipcMain.handle('get-project-root', () => {
  return path.resolve(__dirname, '..', '..');
});

ipcMain.handle('export-to-mod', async (event, { files }) => {
  const projectRoot = path.resolve(__dirname, '..', '..');
  const results = [];
  for (const file of files) {
    const fullPath = path.join(projectRoot, file.relativePath);
    try {
      fs.mkdirSync(path.dirname(fullPath), { recursive: true });
      fs.writeFileSync(fullPath, file.content, 'utf-8');
      results.push({ path: file.relativePath, success: true });
    } catch (err) {
      results.push({ path: file.relativePath, success: false, error: err.message });
    }
  }
  return results;
});

// ---------------------------------------------------------------------------
// MCP Live Sync — watch temp/mcp_state.json for real-time updates
// ---------------------------------------------------------------------------
const MCP_SYNC_FILE = path.resolve(__dirname, '..', '..', 'temp', 'mcp_state.json');
let lastSyncContent = '';

function startMcpSync() {
  const syncDir = path.dirname(MCP_SYNC_FILE);
  if (!fs.existsSync(syncDir)) {
    fs.mkdirSync(syncDir, { recursive: true });
  }

  // Read initial state if file exists
  if (fs.existsSync(MCP_SYNC_FILE)) {
    try {
      const content = fs.readFileSync(MCP_SYNC_FILE, 'utf-8');
      lastSyncContent = content;
      if (mainWindow && !mainWindow.isDestroyed()) {
        mainWindow.webContents.send('mcp-state-update', content);
      }
    } catch (_) {}
  }

  // Watch for changes
  try {
    fs.watch(syncDir, (eventType, filename) => {
      if (filename === 'mcp_state.json') {
        try {
          const content = fs.readFileSync(MCP_SYNC_FILE, 'utf-8');
          if (content !== lastSyncContent && content.length > 0) {
            lastSyncContent = content;
            if (mainWindow && !mainWindow.isDestroyed()) {
              mainWindow.webContents.send('mcp-state-update', content);
            }
          }
        } catch (_) {}
      }
    });
  } catch (err) {
    console.error('MCP sync watch failed:', err.message);
  }
}

app.whenReady().then(() => {
  setTimeout(startMcpSync, 500);
});
