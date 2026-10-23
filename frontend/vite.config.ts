import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: { port: 5173, strictPort: true, proxy: { '/oauth2': 'http://127.0.0.1:8082', '/login': 'http://127.0.0.1:8082', '/api': 'http://127.0.0.1:8082', '/actuator': 'http://127.0.0.1:8082' } },
});
