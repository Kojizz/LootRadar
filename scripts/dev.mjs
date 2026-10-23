import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import net from 'node:net';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const windows = process.platform === 'win32';
async function portInUse(port) {
  return new Promise(resolve => {
    const socket = net.connect({host: '127.0.0.1', port});
    socket.setTimeout(1500);
    const finish = value => {socket.destroy(); resolve(value);};
    socket.once('connect', () => finish(true));
    socket.once('error', () => finish(false));
    socket.once('timeout', () => finish(true));
  });
}
if (await portInUse(5173)) {
  console.error('Port 5173 is already in use. If LootRadar is running, open http://127.0.0.1:5173/. Otherwise stop that frontend before retrying.');
  process.exit(1);
}
let reuseBackend = false;
if (await portInUse(8082)) {
  try {
    const response = await fetch('http://127.0.0.1:8082/api/catalog', {signal: AbortSignal.timeout(5000)});
    const catalog = await response.json();
    reuseBackend = response.ok && Array.isArray(catalog.games) && typeof catalog.status?.syncing === 'boolean'
      && ['1', '25', '7'].every(id => catalog.stores?.some(store => store.id === id));
  } catch {}
  if (!reuseBackend) {
    console.error('Port 8082 is occupied, but a ready LootRadar API was not found. Wait for the IntelliJ backend to finish starting, or stop the other service and retry.');
    process.exit(1);
  }
  console.log('Using the LootRadar backend already running on port 8082. It will stay running when this command stops.');
}
const backend = reuseBackend ? null : windows
  ? spawn('cmd.exe', ['/d', '/s', '/c', 'mvnw.cmd spring-boot:run'], {cwd: path.join(root, 'backend'), stdio: 'inherit', windowsHide: true})
  : spawn('sh', ['./mvnw', 'spring-boot:run'], {cwd: path.join(root, 'backend'), stdio: 'inherit'});
const frontend = spawn(process.execPath, [path.join(root, 'node_modules/vite/bin/vite.js'), '--host', '127.0.0.1'], {cwd: path.join(root, 'frontend'), stdio: 'inherit', windowsHide: true});
const children = [backend, frontend].filter(Boolean);
console.log('\nLootRadar: http://127.0.0.1:5173 · API: http://127.0.0.1:8082\n');
let stopping = false;
function stop(code = 0) {
  if (stopping) return; stopping = true;
  for (const child of children) {
    if (!child.pid) continue;
    if (windows) spawn('taskkill', ['/PID', String(child.pid), '/T', '/F'], {stdio: 'ignore', windowsHide: true});
    else child.kill('SIGTERM');
  }
  setTimeout(() => process.exit(code), 700);
}
for (const child of children) {child.on('error', error => {console.error(error.message); stop(1);}); child.on('exit', code => {if (!stopping) {console.error(`${child === frontend ? 'Frontend' : 'Backend'} exited (${code ?? 'signal'}). Stopping processes started by this launcher.`); stop(code ?? 1);}});}
process.on('SIGINT', () => stop()); process.on('SIGTERM', () => stop());
