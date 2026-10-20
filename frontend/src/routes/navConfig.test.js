import { describe, expect, it } from 'vitest'
import { navItemsFor, safeRedirectFor } from './navConfig'
import { USERS } from '../test/renderWithAuth'

const labels = (user) => navItemsFor(user).map((i) => i.label)

describe('navItemsFor', () => {
  it('shows team management to admins and managers', () => {
    expect(labels(USERS.admin)).toContain('Team Management')
    expect(labels(USERS.manager)).toContain('Team Management')
  })

  it('hides team management from relationship officers but keeps their working modules', () => {
    const items = labels(USERS.officer)
    expect(items).not.toContain('Team Management')
    expect(items).toEqual(expect.arrayContaining(['Dashboard', 'Customers', 'Leads & Opportunities', 'Follow-ups']))
  })
})

describe('safeRedirectFor', () => {
  it('returns to a remembered page the user may open', () => {
    expect(safeRedirectFor(USERS.manager, '/team')).toBe('/team')
    expect(safeRedirectFor(USERS.officer, '/customers?page=2')).toBe('/customers?page=2')
  })

  it('falls back to the dashboard for pages the user may not open', () => {
    expect(safeRedirectFor(USERS.officer, '/team')).toBe('/')
  })

  it('ignores missing or external redirect targets', () => {
    expect(safeRedirectFor(USERS.admin, undefined)).toBe('/')
    expect(safeRedirectFor(USERS.admin, '//evil.example')).toBe('/')
    expect(safeRedirectFor(USERS.admin, 'https://evil.example')).toBe('/')
  })
})
