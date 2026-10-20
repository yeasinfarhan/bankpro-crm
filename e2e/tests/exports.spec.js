import { expect, test } from '@playwright/test'
import { login, runId, uniquePhone, USERS } from './helpers'

const readDownload = async (download) => Buffer.concat(await (await download.createReadStream()).toArray()).toString('utf8')

test('CSV export neutralises spreadsheet formulas typed into customer fields', async ({ page }) => {
  const formula = `=HYPERLINK("http://example.invalid","E2E ${runId()}")`
  await login(page, USERS.officerMtj)
  await page.goto('/customers/new')
  await page.getByLabel('Full name').fill(formula)
  await page.getByLabel('Mobile number').fill(uniquePhone())
  await page.getByLabel('Occupation').fill('+SUM(1,2)')
  await page.getByLabel('Segment').selectOption({ label: 'Mass' })
  await page.getByRole('button', { name: 'Create customer' }).click()
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(formula)

  await page.goto('/customers?q=HYPERLINK')
  const download = await Promise.all([page.waitForEvent('download'), page.getByRole('button', { name: 'Export CSV' }).click()]).then(([d]) => d)
  const csv = await readDownload(download)
  expect(download.suggestedFilename()).toMatch(/\.csv$/)
  // Each dangerous cell starts with a single quote, so Excel/Sheets treat it as text, not a formula
  expect(csv).toContain(`"'=HYPERLINK(""http://example.invalid"",`)
  expect(csv).toContain("'+SUM(1,2)")
  expect(csv).not.toMatch(/(^|,)"?=HYPERLINK/m)
})

test('every report exports a CSV with the same columns as the table', async ({ page }) => {
  await login(page, USERS.managerMtj)
  for (const [type, title] of [['lead-pipeline', 'Lead pipeline'], ['conversion', 'Conversion'], ['target-achievement', 'Target achievement'], ['follow-up-completion', 'Follow-up completion']]) {
    await page.goto(`/reports?type=${type}&period=last6`)
    const table = page.getByRole('table', { name: title })
    await expect(table).toBeVisible()
    const headers = (await table.locator('thead th').allInnerTexts()).map((h) => h.trim().toLowerCase())
    const download = await Promise.all([page.waitForEvent('download'), page.getByRole('button', { name: 'Export CSV' }).click()]).then(([d]) => d)
    const firstLine = (await readDownload(download)).replace(/^﻿/, '').split(/\r?\n/)[0].toLowerCase()
    expect(firstLine.split(',')).toEqual(headers)
  }
})
