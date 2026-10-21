import { expect } from '@playwright/test'

export const PASSWORD = 'BankPro@2026'
export const USERS = {
  admin: 'admin@bankpro.demo',
  managerMtj: 'manager.motijheel@bankpro.demo',
  officerMtj: 'nusrat.jahan@bankpro.demo',
  officer2Mtj: 'tanvir.ahmed@bankpro.demo',
  managerGul: 'manager.gulshan@bankpro.demo',
  officerGul: 'imran.hossain@bankpro.demo',
}

/** Signs in through the real login form and waits for the dashboard. */
export async function login(page, email) {
  await page.goto('/login')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('heading', { level: 1, name: /Welcome/ })).toBeVisible({ timeout: 120_000 })
}

export async function signOut(page) {
  await page.getByRole('button', { name: 'Account menu' }).click()
  await page.getByRole('button', { name: 'Sign out' }).click()
  await expect(page).toHaveURL(/\/login/)
}

/** A unique, valid Bangladesh mobile number so repeated runs never collide with earlier records. */
export function uniquePhone() {
  const n = String(Date.now() % 100_000_000).padStart(8, '0')
  return `017${n}`
}

export const runId = () => Date.now().toString(36).slice(-5).toUpperCase()

/** Reads a KPI tile value from the dashboard, e.g. kpi(page, 'Completed sales') → "৳660,000". */
export async function kpi(page, label) {
  const tile = page.getByRole('region', { name: 'Key figures' }).locator('div.rounded-xl').filter({ has: page.getByText(label, { exact: true }) })
  const value = tile.locator('p.tabular-nums')
  await expect(value).toBeVisible()
  return value.innerText()
}

export const bdtToNumber = (text) => Number(text.replace(/[^\d.]/g, ''))

/** Collects uncaught errors and console errors (except expected 4xx responses) during a test. */
export function watchErrors(page) {
  const errors = []
  page.on('pageerror', (e) => errors.push(e.message))
  page.on('console', (m) => {
    if (m.type() === 'error' && !/status of 4\d\d/.test(m.text())) errors.push(m.text())
  })
  return errors
}
