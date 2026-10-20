import { expect, test } from '@playwright/test'
import { login, uniquePhone, USERS } from './helpers'

test('login: required fields, then one generic message for a wrong password', async ({ page }) => {
  await page.goto('/login')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByText(/email is required|enter your email/i)).toBeVisible()
  await page.getByLabel('Email').fill(USERS.officerMtj)
  await page.getByLabel('Password', { exact: true }).fill('wrong-password')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('alert')).toContainText(/invalid email or password/i)
})

test('customer form: required fields, phone format, and duplicate phone', async ({ page }) => {
  await login(page, USERS.officerMtj)
  await page.goto('/customers/new')
  await page.getByRole('button', { name: 'Create customer' }).click()
  await expect(page.getByText('Full name is required')).toBeVisible()
  await expect(page.getByText(/phone number is required|mobile number is required/i)).toBeVisible()

  await page.getByLabel('Full name').fill('E2E Validation')
  await page.getByLabel('Mobile number').fill('12345')
  await page.getByLabel('Segment').selectOption({ label: 'Mass' })
  await page.getByRole('button', { name: 'Create customer' }).click()
  await expect(page.getByText(/valid Bangladesh mobile/i)).toBeVisible()

  // A number that belongs to an existing customer is rejected by the server (normalised duplicate check)
  await page.goto('/customers')
  const contact = await page.getByRole('table', { name: /Customers/ }).locator('tbody tr').first().locator('td').nth(1).innerText()
  const existing = contact.match(/\+880 \d{4}-\d{6}/)[0] // shown as "+880 1712-345678"
  await page.goto('/customers/new')
  await page.getByLabel('Full name').fill('E2E Duplicate')
  await page.getByLabel('Mobile number').fill(existing.replace(/[\s-]/g, ''))
  await page.getByLabel('Segment').selectOption({ label: 'Mass' })
  await page.getByRole('button', { name: 'Create customer' }).click()
  await expect(page.getByText(/already (exists|registered|used)/i).first()).toBeVisible()
})

test('lead form: customer and product are required, value cannot be negative', async ({ page }) => {
  await login(page, USERS.officerMtj)
  await page.goto('/leads/new')
  await page.getByRole('button', { name: 'Create lead' }).click()
  await expect(page.getByText(/choose a customer/i)).toBeVisible()
  await expect(page.getByText(/choose a product/i)).toBeVisible()
  await page.getByLabel('Estimated value (BDT)').fill('-5')
  await page.getByRole('button', { name: 'Create lead' }).click()
  await expect(page.getByText('Cannot be negative')).toBeVisible()
})

test('follow-up: a time in the past is refused', async ({ page }) => {
  await login(page, USERS.officerMtj)
  await page.goto('/follow-ups')
  await page.getByRole('button', { name: 'Schedule follow-up' }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByRole('combobox').first().fill('Ra')
  await dialog.getByRole('listbox', { name: 'Matching customers' }).getByRole('option').first().click()
  await dialog.getByLabel('Date and time (BST)').fill('2020-01-01T10:00')
  await dialog.getByRole('button', { name: 'Schedule' }).click()
  await expect(dialog.getByText(/future|in the past/i).first()).toBeVisible()
})

test('target form: count targets must be whole numbers; duplicates are refused', async ({ page }) => {
  await login(page, USERS.managerMtj)
  await page.goto('/targets')
  await page.getByRole('button', { name: 'New target' }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('Category').selectOption({ label: 'Converted leads' })
  await dialog.getByLabel(/Target \(count\)/).fill('2.5')
  await dialog.getByRole('button', { name: 'Create target' }).click()
  await expect(dialog.getByText('Use a whole number')).toBeVisible()
  // Whole-branch converted-leads target for this quarter already exists in the demo data
  await dialog.getByLabel('Period type').selectOption('QUARTERLY')
  await dialog.getByLabel(/Target \(count\)/).fill('3')
  await dialog.getByRole('button', { name: 'Create target' }).click()
  await expect(dialog.getByText(/already exists/i)).toBeVisible()
})

test('phone numbers typed as +880… are stored and shown in one standard format', async ({ page }) => {
  await login(page, USERS.officerMtj)
  await page.goto('/customers/new')
  const local = uniquePhone()
  await page.getByLabel('Full name').fill(`E2E Phone ${local.slice(-4)}`)
  await page.getByLabel('Mobile number').fill(`+88${local}`)
  await page.getByLabel('Segment').selectOption({ label: 'Mass' })
  await page.getByRole('button', { name: 'Create customer' }).click()
  await expect(page.getByText(`+880 ${local.slice(1, 5)}-${local.slice(5)}`)).toBeVisible()
})
