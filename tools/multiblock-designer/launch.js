const { spawn } = require('child_process');
const path = require('path');

const electronPath = path.join(__dirname, 'node_modules', '.bin', process.platform === 'win32' ? 'electron.cmd' : 'electron');
const child = spawn(electronPath, ['.'], { stdio: 'inherit', env: { ...process.env, ELECTRON_DISABLE_SECURITY_WARNINGS: 'true' } });
child.on('close', (code) => process.exit(code));
