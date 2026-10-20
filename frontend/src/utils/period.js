/**
 * Date-range presets in Bangladesh time. All values are "YYYY-MM-DD" calendar dates (inclusive),
 * which the API converts to UTC bounds itself.
 */
const pad = (n) => String(n).padStart(2, '0')

export function bstToday(now = Date.now()) {
  return new Date(now + 6 * 3600 * 1000).toISOString().slice(0, 10)
}

const ymd = (y, m, d) => `${y}-${pad(m)}-${pad(d)}`
const lastDay = (y, m) => new Date(Date.UTC(y, m, 0)).getUTCDate()

export const PRESETS = {
  thisMonth: 'This month',
  lastMonth: 'Last month',
  thisQuarter: 'This quarter',
  last6: 'Last 6 months',
  thisYear: 'This year',
  custom: 'Custom range',
}

export function presetRange(key, today = bstToday()) {
  const [y, m] = today.split('-').map(Number)
  switch (key) {
    case 'thisMonth':
      return { from: ymd(y, m, 1), to: today }
    case 'lastMonth': {
      const py = m === 1 ? y - 1 : y
      const pm = m === 1 ? 12 : m - 1
      return { from: ymd(py, pm, 1), to: ymd(py, pm, lastDay(py, pm)) }
    }
    case 'thisQuarter': {
      const qm = Math.floor((m - 1) / 3) * 3 + 1
      return { from: ymd(y, qm, 1), to: today }
    }
    case 'thisYear':
      return { from: ymd(y, 1, 1), to: today }
    case 'last6':
    default: {
      const d = new Date(Date.UTC(y, m - 1 - 5, 1))
      return { from: ymd(d.getUTCFullYear(), d.getUTCMonth() + 1, 1), to: today }
    }
  }
}

/** "2026-10" → "Oct 2026" */
export function monthLabel(ym) {
  const [y, m] = ym.split('-').map(Number)
  return new Intl.DateTimeFormat('en-GB', { month: 'short', year: 'numeric', timeZone: 'UTC' }).format(new Date(Date.UTC(y, m - 1, 1)))
}
