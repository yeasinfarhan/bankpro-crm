import { useState } from 'react'
import Card from '../ui/Card'
import LoadingState from '../ui/LoadingState'
import ErrorState from '../ui/ErrorState'
import ScrollRegion from '../ui/ScrollRegion'

/**
 * Frame for every chart: title, one-line definition, loading / error / empty states and a
 * "Show data" table so the numbers are available without the graphic (accessibility).
 * table = { columns: [{ key, label, format(v, row) }], rows }
 */
export default function ChartCard({ title, description, state, isEmpty, emptyText = 'No data for this period.', table, children, className = '' }) {
  const [showTable, setShowTable] = useState(false)
  return (
    <Card className={`flex flex-col ${className}`}>
      <div className="flex items-start justify-between gap-3 border-b border-line px-5 py-4">
        <div>
          <h2 className="text-sm font-semibold text-ink">{title}</h2>
          {description && <p className="mt-0.5 text-xs text-ink-muted">{description}</p>}
        </div>
        {table && state.status === 'ok' && !isEmpty && (
          <button type="button" onClick={() => setShowTable((v) => !v)} aria-pressed={showTable}
            className="shrink-0 rounded-md px-2 py-1 text-xs font-medium text-royal-600 hover:bg-royal-50">
            {showTable ? 'Show chart' : 'Show data'}
          </button>
        )}
      </div>
      <div className="flex-1 p-4">
        {state.status === 'loading' && <LoadingState rows={4} />}
        {state.status === 'error' && <ErrorState message={state.error} onRetry={state.reload} />}
        {state.status === 'ok' && isEmpty && <p className="py-10 text-center text-sm text-ink-muted">{emptyText}</p>}
        {state.status === 'ok' && !isEmpty && (showTable && table ? (
          <ScrollRegion label={`${title} data`}>
            <table className="min-w-full text-sm">
              <caption className="sr-only">{title}</caption>
              <thead><tr className="text-left text-xs text-ink-muted">{table.columns.map((c) => <th key={c.key} scope="col" className="px-2 py-1.5 font-medium">{c.label}</th>)}</tr></thead>
              <tbody className="divide-y divide-line">
                {table.rows.map((r, i) => (
                  <tr key={i}>{table.columns.map((c) => <td key={c.key} className="px-2 py-1.5 text-ink">{c.format ? c.format(r[c.key], r) : r[c.key]}</td>)}</tr>
                ))}
              </tbody>
            </table>
          </ScrollRegion>
        ) : children)}
      </div>
    </Card>
  )
}
