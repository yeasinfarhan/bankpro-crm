// @ts-check
import { defineConfig, devices } from '@playwright/test'

/**
 * Runs against an already running app (backend on :8080, frontend on :5173 by default).
 * Tests create a few clearly-labelled "E2E ..." demo records; they never delete seed data.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1, // tests share one demo database, so they run one after another
  timeout: 60_000,
  expect: { timeout: 10_000 },
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    timezoneId: 'Asia/Dhaka',
    ...devices['Desktop Chrome'],
    launchOptions: process.env.PW_CHROMIUM_PATH ? { executablePath: process.env.PW_CHROMIUM_PATH } : {},
  },
})
