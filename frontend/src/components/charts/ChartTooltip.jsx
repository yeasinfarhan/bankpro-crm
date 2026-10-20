/** Hover tooltip: label in ink, each value with a small colour key (identity is never colour-only — the name is shown). */
export default function ChartTooltip({ active, payload, label, labelFormatter, valueFormatter }) {
  if (!active || !payload?.length) return null
  return (
    <div className="rounded-lg border border-line bg-white px-3 py-2 text-xs shadow-lg">
      <p className="mb-1 font-semibold text-ink">{labelFormatter ? labelFormatter(label, payload) : label}</p>
      {payload.map((p) => (
        <p key={p.dataKey} className="flex items-center gap-2 text-ink">
          <span className="h-2 w-2 rounded-sm" style={{ background: p.color ?? p.fill }} aria-hidden="true" />
          <span className="text-ink-muted">{p.name}:</span>
          <span className="font-medium">{valueFormatter ? valueFormatter(p.value, p) : p.value}</span>
        </p>
      ))}
    </div>
  )
}
