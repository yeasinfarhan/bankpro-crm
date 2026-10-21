import { expect, test } from '@playwright/test'
import { kpi, login, USERS, watchErrors } from './helpers'

/**
 * Read-only checks for a deployed copy (creates no data):  npm run smoke
 * with E2E_BASE_URL=https://<frontend> and E2E_API_URL=https://<backend>.
 */
const API = process.env.E2E_API_URL ?? 'http://localhost:8080'

test.describe('@smoke deployed app', () => {
  test.setTimeout(150_000) // a sleeping free-tier backend can take about a minute to start

  test('backend is up and only exposes what it should', async ({ request }) => {
    const health = await request.get(`${API}/actuator/health`, { timeout: 120_000 })
    expect(health.status()).toBe(200)
    expect(await health.json()).toEqual({ status: 'UP' })
    expect((await request.get(`${API}/swagger-ui.html`)).status()).toBe(404)
    expect((await request.get(`${API}/api/customers`)).status()).toBe(401)
  })

  test('CORS allows this frontend and refuses other sites', async ({ request, baseURL }) => {
    const origin = new URL(baseURL).origin
    const allowed = await request.fetch(`${API}/api/auth/login`, { method: 'OPTIONS', headers: { Origin: origin, 'Access-Control-Request-Method': 'POST' } })
    expect(allowed.headers()['access-control-allow-origin']).toBe(origin)
    const other = await request.fetch(`${API}/api/auth/login`, { method: 'OPTIONS', headers: { Origin: 'https://not-bankpro.example', 'Access-Control-Request-Method': 'POST' } })
    expect(other.status()).toBe(403)
  })

  test('frontend sends security headers and deep links load the app', async ({ page, request, baseURL }) => {
    const res = await request.get(`${baseURL}/customers`)
    expect(res.status()).toBe(200)
    if (baseURL.startsWith('https://')) {
      expect(res.headers()['content-security-policy']).toContain("frame-ancestors 'none'")
      expect(res.headers()['x-content-type-options']).toBe('nosniff')
    }
    await page.goto('/customers')
    await expect(page).toHaveURL(/\/login/)
  })

  for (const [who, kpiLabel] of [['officerMtj', 'Total customers'], ['managerMtj', 'Completed sales'], ['admin', 'Active leads']]) {
    test(`${who} signs in and sees live dashboard figures`, async ({ page }) => {
      const errors = watchErrors(page)
      await login(page, USERS[who])
      expect(await kpi(page, kpiLabel)).toMatch(/\d/)
      await page.getByRole('link', { name: 'Customers' }).first().click()
      await expect(page.getByRole('heading', { level: 1, name: 'Customers' })).toBeVisible()
      await page.getByRole('link', { name: 'Performance Reports' }).click()
      await expect(page.getByRole('heading', { level: 1, name: 'Performance reports' })).toBeVisible()
      expect(errors).toEqual([])
    })
  }
})
