import { expect, test } from '@playwright/test'
import { bdtToNumber, kpi, login, runId, signOut, uniquePhone, USERS, watchErrors } from './helpers'

/**
 * The whole customer-to-sale journey in the browser, across two roles:
 * officer creates a customer and a loan lead → works the stages → converts (pending approval)
 * → manager approves → the sale appears in the dashboard, the product-sales report and the customer export.
 */
test('customer → lead → conversion → manager approval → dashboard, report and export', async ({ page }) => {
  const errors = watchErrors(page)
  const id = runId()
  const name = `E2E Customer ${id}`
  const amount = 750000

  // Manager's "Completed sales" this month, before the new sale
  await login(page, USERS.managerMtj)
  await page.goto('/?period=thisMonth')
  const before = bdtToNumber(await kpi(page, 'Completed sales'))
  await signOut(page)

  // 1. Officer creates the customer
  await login(page, USERS.officerMtj)
  await page.goto('/customers/new')
  await page.getByLabel('Full name').fill(name)
  await page.getByLabel('Mobile number').fill(uniquePhone())
  await page.getByLabel('City').fill('Dhaka')
  await page.getByLabel('Segment').selectOption({ label: 'Mass' })
  await page.getByRole('button', { name: 'Create customer' }).click()
  await expect(page.getByRole('heading', { level: 1, name })).toBeVisible()

  // 2. A loan lead from the customer page (customer is pre-selected)
  await page.getByRole('link', { name: 'New lead' }).click()
  await expect(page.getByText(name)).toBeVisible()
  await page.getByLabel('Interested product').selectOption({ label: 'BankPro Personal Loan (Loan)' })
  await page.getByLabel('Lead source').selectOption({ label: 'Branch walk-in' })
  await page.getByLabel('Estimated value (BDT)').fill(String(amount))
  await page.getByRole('button', { name: /Create lead/ }).click()
  await expect(page.getByRole('heading', { level: 1, name: new RegExp(`Personal Loan — ${name}`) })).toBeVisible()
  const leadUrl = page.url()

  // 3. Work the pipeline with the stage menu: New → Contacted → Qualified → Proposal
  for (const stage of ['Contacted', 'Qualified', 'Proposal']) {
    await page.getByRole('button', { name: /^Change stage of LD-/ }).click()
    await page.getByRole('menuitem', { name: `Move to ${stage}` }).click()
    await expect(page.locator('main').getByText(stage, { exact: true }).first()).toBeVisible()
  }

  // 4. Convert: loans need a manager's (simulated) approval
  await page.getByRole('button', { name: 'Convert to sale' }).first().click()
  await page.getByLabel('Sale amount (BDT)').fill(String(amount))
  await page.getByRole('dialog').getByRole('button', { name: 'Convert to sale' }).click()
  await expect(page.getByText('Pending approval').first()).toBeVisible()
  await signOut(page)

  // 5. Manager approves from the Leads page queue
  await login(page, USERS.managerMtj)
  await page.goto('/leads')
  await page.getByRole('list', { name: 'Applications awaiting decision' })
    .getByRole('button', { name: `Approve ${name}'s BankPro Personal Loan application` }).click()
  await expect(page.getByText(/Approved \(simulated\)/)).toBeVisible()

  // 6. The lead shows a completed sale
  await page.goto(leadUrl)
  await expect(page.getByText('Completed', { exact: true }).first()).toBeVisible()

  // 7. Dashboard: completed sales rose by exactly the sale amount
  await page.goto('/?period=thisMonth')
  await expect.poll(async () => bdtToNumber(await kpi(page, 'Completed sales'))).toBe(before + amount)

  // 8. Product-sales report counts it, and the export downloads a CSV
  await page.goto('/reports?type=product-sales&period=thisMonth')
  await expect(page.getByRole('table', { name: 'Product-wise sales' })).toContainText('BankPro Personal Loan')
  const download = await Promise.all([page.waitForEvent('download'), page.getByRole('button', { name: 'Export CSV' }).click()]).then(([d]) => d)
  expect(download.suggestedFilename()).toMatch(/^bankpro-product-sales-.*\.csv$/)

  // 9. The customer is in the customer export
  await page.goto(`/customers?q=${encodeURIComponent(name)}`)
  await expect(page.getByRole('table', { name: /Customers/ })).toContainText(name)
  const exportFile = await Promise.all([page.waitForEvent('download'), page.getByRole('button', { name: 'Export CSV' }).click()]).then(([d]) => d)
  const csv = await (await exportFile.createReadStream()).toArray().then((chunks) => Buffer.concat(chunks).toString('utf8'))
  expect(csv).toContain(name)

  expect(errors).toEqual([])
})

test('follow-up: schedule for a customer, then complete it with an outcome', async ({ page }) => {
  const errors = watchErrors(page)
  await login(page, USERS.officerMtj)
  await page.goto('/customers')
  await page.getByRole('table', { name: /Customers/ }).getByRole('link').first().click()
  await expect(page).toHaveURL(/\/customers\/\d+$/)
  await expect(page.getByRole('heading', { level: 1 })).not.toHaveText('Customers')
  const customer = await page.getByRole('heading', { level: 1 }).innerText()

  await page.getByRole('button', { name: 'Schedule follow-up' }).click()
  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('Activity').selectOption({ label: 'Phone call' })
  // Tomorrow 10:00 Bangladesh time, so it is always in "Upcoming" whatever time the test runs
  const tomorrowBst = new Date(Date.now() + 6 * 3600_000 + 24 * 3600_000).toISOString().slice(0, 10)
  await dialog.getByLabel('Date and time (BST)').fill(`${tomorrowBst}T10:00`)
  await dialog.getByLabel('Notes').fill('E2E: confirm documents')
  await dialog.getByRole('button', { name: 'Schedule' }).click()
  await expect(dialog).toBeHidden()

  await page.goto('/follow-ups?view=UPCOMING')
  const item = page.locator('li').filter({ hasText: customer }).filter({ hasText: 'E2E: confirm documents' }).first()
  await item.getByRole('button', { name: 'Complete' }).click()
  await page.getByRole('dialog').getByLabel('Outcome').fill('E2E: documents confirmed by phone')
  await page.getByRole('dialog').getByRole('button', { name: 'Mark complete' }).click()
  await expect(page.getByText('Follow-up completed')).toBeVisible()

  await page.goto('/follow-ups?view=COMPLETED')
  await expect(page.getByText('E2E: documents confirmed by phone').first()).toBeVisible()
  expect(errors).toEqual([])
})
