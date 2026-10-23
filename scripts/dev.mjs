import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const windows = process.platform === 'win32';
const backend = windows
  ? spawn('cmd.exe', ['/d', '/s', '/c', 'mvnw.cmd spring-boot:run'], {cwd: path.join(root, 'backend'), stdio: 'inherit', windowsHide: true})
  : spawn('sh', ['./mvnw', 'spring-boot:run'], {cwd: path.join(root, 'backend'), stdio: 'inherit'});
const frontend = spawn(process.execPath, [path.join(root, 'node_modules/vite/bin/vite.js'), '--host', '127.0.0.1'], {cwd: path.join(root, 'frontend'), stdio: 'inherit', windowsHide: true});
console.log('\nLootRadar: http://localhost:5173 · API: http://localhost:8080\n');
let stopping = false;
function stop(code = 0) {
  if (stopping) return; stopping = true;
  for (const child of [backend, frontend]) {
    if (!child.pid) continue;
    if (windows) spawn('taskkill', ['/PID', String(child.pid), '/T', '/F'], {stdio: 'ignore', windowsHide: true});
    else child.kill('SIGTERM');
  }
  setTimeout(() => process.exit(code), 700);
}
for (const child of [backend, frontend]) {child.on('error', error => {console.error(error.message); stop(1);}); child.on('exit', code => {if (!stopping) stop(code ?? 1);});}
process.on('SIGINT', () => stop()); process.on('SIGTERM', () => stop());
