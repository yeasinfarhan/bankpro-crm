/**
 * Stat tile for a single headline number (data-viz rule: one number = a tile, not a chart).
 * `definition` states exactly how the number is computed so nobody has to guess.
 */
export default function KpiTile({ label, value, definition, icon: Icon, tone = 'default', loading }) {
  const toneClass = tone === 'warning' ? 'text-warning-600' : 'text-ink'
  return (
    <div className="rounded-xl border border-line bg-white p-3 shadow-card sm:p-4">
      <div className="flex items-center justify-between gap-2">
        <p className="text-[11px] font-medium uppercase tracking-wide text-ink-muted sm:text-xs">{label}</p>
        {Icon && <Icon className="hidden h-4 w-4 shrink-0 text-ink-muted sm:block" aria-hidden="true" />}
      </div>
      {loading ? (
        <div className="mt-3 h-7 w-24 animate-pulse rounded bg-gray-100" aria-hidden="true" />
      ) : (
        <p className={`mt-2 text-lg font-semibold tabular-nums sm:text-2xl ${toneClass}`}>{value}</p>
      )}
      <p className="mt-1 text-xs leading-snug text-ink-muted">{definition}</p>
    </div>
  )
}
