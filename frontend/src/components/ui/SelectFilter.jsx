/** Compact labelled <select> used in filter bars. Empty value = "All". */
export default function SelectFilter({ label, value, onChange, options, allLabel = 'All' }) {
  const id = `filter-${label.toLowerCase().replace(/\s+/g, '-')}`
  return (
    <label htmlFor={id} className="flex flex-col gap-1 text-xs font-medium text-ink-muted">
      {label}
      <select
        id={id}
        value={value ?? ''}
        onChange={(e) => onChange(e.target.value || '')}
        className="rounded-lg border border-line bg-white px-2.5 py-2 text-sm text-ink shadow-sm focus:border-royal-500 focus:outline-none focus:ring-2 focus:ring-royal-100"
      >
        <option value="">{allLabel}</option>
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    </label>
  )
}
