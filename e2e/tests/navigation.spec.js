import { expect, test } from '@playwright/test'
import { login, USERS, watchErrors } from './helpers'

const PAGES = {
  officerMtj: ['Dashboard', 'Customers', 'Leads & Opportunities', 'Follow-ups', 'Banking Products', 'Sales Targets', 'Performance Reports', 'Settings'],
  managerMtj: ['Dashboard', 'Customers', 'Leads & Opportunities', 'Follow-ups', 'Banking Products', 'Sales Targets', 'Performance Reports', 'Team Management', 'Settings'],
  admin: ['Dashboard', 'Customers', 'Leads & Opportunities', 'Follow-ups', 'Banking Products', 'Sales Targets', 'Performance Reports', 'Team Management', 'Settings'],
}

for (const [who, links] of Object.entries(PAGES)) {
  test(`${who}: every sidebar page opens with a heading and no errors`, async ({ page }) => {
    const errors = watchErrors(page)
    await login(page, USERS[who])
    const sidebar = page.locator('aside').first()
    await expect(sidebar.getByRole('link')).toHaveText(links.map((l) => new RegExp(l)), { useInnerText: true })
    for (const link of links) {
      await sidebar.getByRole('link', { name: link }).click()
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
      await expect(page.locator('[role=alert]')).toHaveCount(0)
    }
    expect(errors).toEqual([])
  })
}

test('filters live in the URL: reload and back/forward keep them', async ({ page }) => {
  await login(page, USERS.managerMtj)
  await page.goto('/customers')
  await page.getByLabel('Segment').selectOption({ label: 'Affluent' })
  await expect(page).toHaveURL(/segment=AFFLUENT/)
  await page.reload()
  await expect(page.getByLabel('Segment')).toHaveValue('AFFLUENT')
  await page.goto('/leads?view=table')
  await page.goBack()
  await expect(page).toHaveURL(/segment=AFFLUENT/)
  await expect(page.getByLabel('Segment')).toHaveValue('AFFLUENT')
})

test('unknown pages show a not-found page inside the app', async ({ page }) => {
  await login(page, USERS.officerMtj)
  await page.goto('/this-page-does-not-exist')
  await expect(page.getByRole('heading', { level: 1, name: 'Page not found' })).toBeVisible()
  await page.getByRole('link', { name: 'Back to dashboard' }).click()
  await expect(page.getByRole('heading', { level: 1, name: /Welcome/ })).toBeVisible()
})
