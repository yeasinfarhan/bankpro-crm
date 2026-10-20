const BUSINESS_TIME_ZONE = 'Asia/Dhaka'

/** Formats an ISO timestamp in Bangladesh Standard Time, e.g. "19 Oct 2026, 9:31 am" (explicit 12-hour clock). */
export function formatDateTime(iso) {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('en-GB', {
    timeZone: BUSINESS_TIME_ZONE,
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  }).format(new Date(iso))
}

/** Date only, in Bangladesh time, e.g. "19 Oct 2026". */
export function formatDate(iso) {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('en-GB', {
    timeZone: BUSINESS_TIME_ZONE,
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date(iso))
}

/** Bangladeshi Taka with grouping, e.g. "৳1,250,000". */
export function formatBdt(amount) {
  if (amount === null || amount === undefined) return '—'
  return '৳' + new Intl.NumberFormat('en-US', { maximumFractionDigits: 2 }).format(Number(amount))
}

/** "+8801712345678" → "+880 1712-345678" for readability. */
export function formatPhone(phone) {
  const m = /^\+880(\d{4})(\d{6})$/.exec(phone ?? '')
  return m ? `+880 ${m[1]}-${m[2]}` : (phone ?? '—')
}

/** Time only in BST, e.g. "10:30 am". */
export function formatTime(iso) {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('en-GB', { timeZone: BUSINESS_TIME_ZONE, hour: 'numeric', minute: '2-digit', hour12: true }).format(new Date(iso))
}

/** Calendar date (YYYY-MM-DD) of an instant in Bangladesh time. */
export function bstDateKey(iso) {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: BUSINESS_TIME_ZONE, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date(iso))
  const get = (t) => parts.find((p) => p.type === t).value
  return `${get('year')}-${get('month')}-${get('day')}`
}

/**
 * <input type="datetime-local"> gives "2026-10-20T10:30" with no zone. Business times are always BST (UTC+6,
 * no daylight saving), so we attach the offset explicitly instead of trusting the browser's own time zone.
 */
export function bstInputToIso(value) {
  return value ? `${value}:00+06:00` : null
}

/** The reverse: an instant → "YYYY-MM-DDTHH:mm" in BST for a datetime-local input. */
export function isoToBstInput(iso) {
  if (!iso) return ''
  const shifted = new Date(new Date(iso).getTime() + 6 * 60 * 60 * 1000)
  return shifted.toISOString().slice(0, 16)
}
