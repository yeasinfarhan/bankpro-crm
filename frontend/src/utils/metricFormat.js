import { formatBdt } from './format'
import { LEAD_STATUSES, PRODUCT_CATEGORIES } from './labels'
import { monthLabel } from './period'

export const TARGET_CATEGORIES = {
  NEW_CUSTOMERS: 'New customers',
  DEPOSIT_SALES: 'Deposit sales',
  CREDIT_CARD_SALES: 'Credit card sales',
  LOAN_LEADS: 'Loan leads',
  CONVERTED_LEADS: 'Converted leads',
}
export const PERIOD_TYPES = { MONTHLY: 'Monthly', QUARTERLY: 'Quarterly' }

const int = new Intl.NumberFormat('en-US')

export function formatPercent(v) {
  if (v === null || v === undefined) return '—'
  return `${Number(v).toFixed(1)}%`
}

/** Formats a value by the API's column format (text | number | bdt | percent | month | stage | category | unitValue). */
export function formatMetric(value, format, row = {}) {
  if (value === null || value === undefined || value === '') return '—'
  switch (format) {
    case 'number': return int.format(Number(value))
    case 'bdt': return formatBdt(value)
    case 'percent': return formatPercent(value)
    case 'month': return monthLabel(String(value))
    case 'stage': return LEAD_STATUSES[value] ?? value
    case 'category': return TARGET_CATEGORIES[value] ?? PRODUCT_CATEGORIES[value] ?? value
    case 'unitValue': return row.unit === 'BDT' ? formatBdt(value) : int.format(Number(value))
    default: return String(value)
  }
}

/** "Nusrat Jahan" for an officer target, "Motijheel (branch)" for a branch-wide one. */
export function targetOwner(t) {
  return t.user ? t.user.fullName : `${t.branch.name} (branch)`
}
