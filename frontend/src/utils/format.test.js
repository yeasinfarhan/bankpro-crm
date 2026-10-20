import { describe, expect, it } from 'vitest'
import { bstDateKey, bstInputToIso, formatBdt, formatDateTime, isoToBstInput } from './format'

describe('Bangladesh time helpers', () => {
  it('treats datetime-local input as BST (UTC+6) regardless of the browser time zone', () => {
    expect(bstInputToIso('2026-10-20T10:30')).toBe('2026-10-20T10:30:00+06:00')
    expect(isoToBstInput('2026-10-20T04:30:00Z')).toBe('2026-10-20T10:30')
    expect(bstInputToIso('')).toBeNull()
  })

  it('uses the BST calendar day (late-evening UTC is already the next day in Dhaka)', () => {
    expect(bstDateKey('2026-10-19T19:30:00Z')).toBe('2026-10-20')
    expect(bstDateKey('2026-10-19T17:59:00Z')).toBe('2026-10-19')
  })

  it('formats taka and BST date-times', () => {
    expect(formatBdt(1250000)).toBe('৳1,250,000')
    expect(formatBdt(null)).toBe('—')
    expect(formatDateTime('2026-10-20T04:30:00Z')).toMatch(/20 Oct 2026, 10:30/)
  })
})
