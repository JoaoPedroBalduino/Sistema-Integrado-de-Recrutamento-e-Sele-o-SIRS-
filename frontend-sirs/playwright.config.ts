import { defineConfig, devices } from '@playwright/test'
export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  timeout: 30000,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://127.0.0.1:3100',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'desktop',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 1000 } },
    },
    { name: 'mobile', use: { ...devices['Pixel 7'] } },
  ],
  webServer: [
    {
      command: 'node tests/fixtures/backend.mjs',
      url: 'http://127.0.0.1:4100/health',
      reuseExistingServer: false,
    },
    {
      command: 'node .output/server/index.mjs',
      url: 'http://127.0.0.1:3100/login',
      timeout: 60000,
      reuseExistingServer: false,
      env: {
        NITRO_HOST: '127.0.0.1',
        NITRO_PORT: '3100',
        NUXT_APP_ORIGIN: 'http://127.0.0.1:3100',
        NUXT_BACKEND_BASE_URL: 'http://127.0.0.1:4100/api/v1',
        NUXT_SESSION_SECRET: 'chave-ficticia-exclusiva-da-suite-de-navegador-nao-usar-em-producao',
      },
    },
  ],
})
