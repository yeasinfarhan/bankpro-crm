import { describe, expect, it } from 'vitest'
import { formatMetric, formatPercent } from './metricFormat'
import { monthLabel, presetRange } from './period'

describe('metric formatting', () => {
  it('formats values by the API column format', () => {
    expect(formatMetric(1250000, 'bdt')).toBe('৳1,250,000')
    expect(formatMetric(1234, 'number')).toBe('1,234')
    expect(formatMetric(66.666, 'percent')).toBe('66.7%')
    expect(formatMetric('2026-10', 'month')).toBe('Oct 2026')
    expect(formatMetric('QUALIFIED', 'stage')).toBe('Qualified')
    expect(formatMetric('DEPOSIT_SALES', 'category')).toBe('Deposit sales')
    expect(formatMetric(800000, 'unitValue', { unit: 'BDT' })).toBe('৳800,000')
    expect(formatMetric(4, 'unitValue', { unit: 'COUNT' })).toBe('4')
    expect(formatMetric(null, 'bdt')).toBe('—')
    expect(formatPercent(null)).toBe('—')
  })

  it('builds Bangladesh-calendar presets including year boundaries', () => {
    expect(presetRange('thisMonth', '2026-10-20')).toEqual({ from: '2026-10-01', to: '2026-10-20' })
    expect(presetRange('lastMonth', '2026-01-15')).toEqual({ from: '2025-12-01', to: '2025-12-31' })
    expect(presetRange('thisQuarter', '2026-08-09')).toEqual({ from: '2026-07-01', to: '2026-08-09' })
    expect(presetRange('last6', '2026-02-10')).toEqual({ from: '2025-09-01', to: '2026-02-10' })
    expect(monthLabel('2025-12')).toBe('Dec 2025')
  })
})
