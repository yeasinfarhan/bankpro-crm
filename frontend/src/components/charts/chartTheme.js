/**
 * Chart styling in one place (data-viz rules: one measure per chart, thin marks with 4px rounded ends,
 * solid hairline grid, muted axis text, values in ink colours — never in the series colour).
 * Series colours come from the validated categorical order; slot 1 is used for single-series charts.
 */
export const SERIES = ['#2a78d6', '#eb6834', '#1baf7a', '#eda100']
export const NEUTRAL = '#a3aab5' // "lost" / reference marks — not a status colour
export const GRID = '#e8ecf1'
export const AXIS_TEXT = '#5b6474'
export const INK = '#111827'

export const axisProps = {
  tick: { fill: AXIS_TEXT, fontSize: 12 },
  tickLine: false,
  axisLine: { stroke: GRID },
}

export const barRadius = { vertical: [4, 4, 0, 0], horizontal: [0, 4, 4, 0] }

/** Compact BDT for axes: ৳1.2M, ৳850K. */
export function compactBdt(v) {
  const n = Number(v)
  if (Math.abs(n) >= 1e6) return `৳${(n / 1e6).toFixed(n >= 1e7 ? 0 : 1)}M`
  if (Math.abs(n) >= 1e3) return `৳${Math.round(n / 1e3)}K`
  return `৳${n}`
}
