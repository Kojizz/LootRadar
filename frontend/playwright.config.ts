import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  webServer: {command: 'npm run dev', url: 'http://127.0.0.1:5173', reuseExistingServer: !process.env.CI, timeout: 30000},
  use: {baseURL: process.env.BASE_URL ?? 'http://127.0.0.1:5173', channel: process.platform === 'win32' ? 'chrome' : undefined, screenshot: 'only-on-failure'},
  projects: [{name: 'desktop', use: {viewport: {width: 1440, height: 900}}}, {name: 'mobile', use: {viewport: {width: 390, height: 844}}}],
});
