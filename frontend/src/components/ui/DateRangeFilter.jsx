import { PRESETS, presetRange } from '../../utils/period'

/**
 * Preset ranges (This month, Last 6 months, ...) plus a custom from/to.
 * value = { preset, from, to }; onChange receives the same shape.
 */
export default function DateRangeFilter({ value, onChange }) {
  const pick = (preset) => onChange(preset === 'custom' ? { ...value, preset } : { preset, ...presetRange(preset) })
  return (
    <div className="flex flex-wrap items-end gap-3">
      <label className="flex flex-col gap-1 text-xs font-medium text-ink-muted">
        Period
        <select value={value.preset} onChange={(e) => pick(e.target.value)}
          className="rounded-lg border border-line bg-white px-2.5 py-2 text-sm text-ink shadow-sm focus:border-royal-500 focus:outline-none focus:ring-2 focus:ring-royal-100">
          {Object.entries(PRESETS).map(([k, l]) => <option key={k} value={k}>{l}</option>)}
        </select>
      </label>
      {value.preset === 'custom' && (
        <>
          <label className="flex flex-col gap-1 text-xs font-medium text-ink-muted">From
            <input type="date" value={value.from} max={value.to} onChange={(e) => e.target.value && onChange({ ...value, from: e.target.value })}
              className="rounded-lg border border-line bg-white px-2.5 py-1.5 text-sm text-ink" />
          </label>
          <label className="flex flex-col gap-1 text-xs font-medium text-ink-muted">To
            <input type="date" value={value.to} min={value.from} onChange={(e) => e.target.value && onChange({ ...value, to: e.target.value })}
              className="rounded-lg border border-line bg-white px-2.5 py-1.5 text-sm text-ink" />
          </label>
        </>
      )}
    </div>
  )
}
