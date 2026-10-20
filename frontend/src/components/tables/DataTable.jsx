import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react'

/**
 * Generic table. columns: [{ key, header, render?(row), sortKey?, className? }]
 * sort: "field,asc|desc" (same format as the API). Clicking a sortable header toggles direction.
 */
export default function DataTable({ columns, rows, rowKey = 'id', sort, onSortChange, onRowClick, caption }) {
  const [sortField, sortDir] = (sort ?? '').split(',')

  const toggle = (key) => {
    if (!onSortChange) return
    onSortChange(sortField === key && sortDir === 'asc' ? `${key},desc` : `${key},asc`)
  }

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-line text-sm">
        {caption && <caption className="sr-only">{caption}</caption>}
        <thead className="bg-canvas text-left text-xs font-semibold uppercase tracking-wide text-ink-muted">
          <tr>
            {columns.map((col) => {
              const active = col.sortKey && sortField === col.sortKey
              const ariaSort = active ? (sortDir === 'desc' ? 'descending' : 'ascending') : undefined
              return (
                <th key={col.key} scope="col" aria-sort={ariaSort} className={`whitespace-nowrap px-4 py-3 ${col.className ?? ''}`}>
                  {col.sortKey ? (
                    <button type="button" onClick={() => toggle(col.sortKey)} className="inline-flex items-center gap-1 uppercase hover:text-ink">
                      {col.header}
                      {active ? (
                        sortDir === 'desc' ? <ArrowDown className="h-3.5 w-3.5" aria-hidden="true" /> : <ArrowUp className="h-3.5 w-3.5" aria-hidden="true" />
                      ) : (
                        <ArrowUpDown className="h-3.5 w-3.5 opacity-40" aria-hidden="true" />
                      )}
                    </button>
                  ) : (
                    col.header
                  )}
                </th>
              )
            })}
          </tr>
        </thead>
        <tbody className="divide-y divide-line bg-white">
          {rows.map((row) => (
            <tr
              key={row[rowKey]}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              className={onRowClick ? 'cursor-pointer hover:bg-royal-50/40' : ''}
            >
              {columns.map((col) => (
                <td key={col.key} className={`px-4 py-3 align-top ${col.className ?? ''}`}>
                  {col.render ? col.render(row) : (row[col.key] ?? '—')}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
