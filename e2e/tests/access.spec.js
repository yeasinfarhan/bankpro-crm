import { expect, test } from '@playwright/test'
import { login, USERS } from './helpers'

const API = process.env.E2E_API_URL ?? 'http://localhost:8080'

/** Role restrictions and branch isolation as a user experiences them in the browser. */
test('officer: no Team or officer-performance areas, and no way into them by URL', async ({ page }) => {
  await login(page, USERS.officerMtj)
  const nav = page.getByRole('navigation', { name: /main/i }).or(page.locator('aside nav')).first()
  await expect(nav.getByRole('link', { name: 'Team Management' })).toHaveCount(0)
  await page.goto('/team')
  await expect(page.getByRole('heading', { level: 1, name: "You don't have access to this page" })).toBeVisible()
  await page.goto('/')
  await expect(page.getByText('Officer performance')).toHaveCount(0)
  await page.goto('/targets')
  await expect(page.getByRole('button', { name: 'New target' })).toHaveCount(0)
})

test("officer cannot open a colleague's lead by changing the ID in the URL", async ({ page, request }) => {
  // Find one of Tanvir's leads through the API as the branch manager
  const token = (await (await request.post(`${API}/api/auth/login`, { data: { email: USERS.managerMtj, password: 'BankPro@2026' } })).json()).accessToken
  const leads = await (await request.get(`${API}/api/leads?size=100`, { headers: { Authorization: `Bearer ${token}` } })).json()
  const colleagues = leads.content.find((l) => l.assignedOfficer.fullName === 'Tanvir Ahmed')

  await login(page, USERS.officerMtj)
  await page.goto(`/leads/${colleagues.id}`)
  await expect(page.getByText(/not found|could not be found|doesn't exist|does not exist/i).first()).toBeVisible()
  await expect(page.getByText(colleagues.customer.name)).toHaveCount(0)
})

test("another branch's customer is invisible, even with the exact URL", async ({ page, request }) => {
  const token = (await (await request.post(`${API}/api/auth/login`, { data: { email: USERS.managerMtj, password: 'BankPro@2026' } })).json()).accessToken
  const customers = await (await request.get(`${API}/api/customers?size=1`, { headers: { Authorization: `Bearer ${token}` } })).json()
  const motijheelCustomer = customers.content[0]

  await login(page, USERS.managerGul)
  await page.goto(`/customers/${motijheelCustomer.id}`)
  await expect(page.getByText(/not found|could not be found|doesn't exist|does not exist/i).first()).toBeVisible()
  await expect(page.getByText(motijheelCustomer.fullName)).toHaveCount(0)
  // Search does not find it either
  await page.getByRole('combobox', { name: 'Search customers and leads' }).fill(motijheelCustomer.fullName)
  await expect(page.getByText(/No customers or leads match/)).toBeVisible()
})

test('admin is read-only for customer work: no create, edit or stage controls', async ({ page }) => {
  await login(page, USERS.admin)
  await page.goto('/customers')
  await expect(page.getByRole('link', { name: 'New customer' })).toHaveCount(0)
  await page.goto('/customers/new')
  await expect(page.getByRole('heading', { level: 1, name: "You don't have access to this page" })).toBeVisible()
  await page.goto('/leads?view=table')
  await expect(page.getByRole('button', { name: /^Change stage of/ })).toHaveCount(0)
})

test('signed-out users are sent to sign-in, then back to where they were going', async ({ page }) => {
  await page.goto('/reports?type=conversion')
  await expect(page).toHaveURL(/\/login/)
  await page.getByLabel('Email').fill(USERS.managerMtj)
  await page.getByLabel('Password', { exact: true }).fill('BankPro@2026')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page).toHaveURL(/\/reports\?type=conversion/)
  await expect(page.getByRole('heading', { level: 1, name: 'Performance reports' })).toBeVisible()
})
