import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: { port: 5173, strictPort: true, proxy: { '/oauth2': 'http://127.0.0.1:8080', '/login': 'http://127.0.0.1:8080', '/api': 'http://127.0.0.1:8080', '/actuator': 'http://127.0.0.1:8080' } },
});
