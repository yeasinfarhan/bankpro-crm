import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react'
import ScrollRegion from '../ui/ScrollRegion'
import useMediaQuery, { PHONE } from '../../hooks/useMediaQuery'

/**
 * Generic table. columns: [{ key, header, render?(row), sortKey?, className?, hideOnCard? }]
 * sort: "field,asc|desc" (same format as the API). Clicking a sortable header toggles direction.
 * On phones the rows become stacked cards (first column = card title) with a "Sort by" control,
 * so nothing has to be scrolled sideways.
 */
export default function DataTable({ columns, rows, rowKey = 'id', sort, onSortChange, onRowClick, caption }) {
  const [sortField, sortDir] = (sort ?? '').split(',')

  const phone = useMediaQuery(PHONE)

  const toggle = (key) => {
    if (!onSortChange) return
    onSortChange(sortField === key && sortDir === 'asc' ? `${key},desc` : `${key},asc`)
  }

  if (phone) {
    return <CardList columns={columns} rows={rows} rowKey={rowKey} sort={sort} onSortChange={onSortChange} onRowClick={onRowClick} caption={caption} />
  }

  return (
    <ScrollRegion label={caption ?? 'Table'}>
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
    </ScrollRegion>
  )
}

function CardList({ columns, rows, rowKey, sort, onSortChange, onRowClick, caption }) {
  const [title, ...rest] = columns
  const sortable = columns.filter((c) => c.sortKey)
  return (
    <div>
      {onSortChange && sortable.length > 0 && (
        <div className="flex items-center gap-2 border-b border-line bg-canvas px-4 py-2 text-xs text-ink-muted">
          <label htmlFor="card-sort">Sort by</label>
          <select id="card-sort" value={sort ?? ''} onChange={(e) => onSortChange(e.target.value)}
            className="rounded-md border border-line bg-white px-2 py-1 text-sm text-ink">
            {!sort && <option value="">Default</option>}
            {sortable.flatMap((c) => [
              <option key={`${c.sortKey},asc`} value={`${c.sortKey},asc`}>{c.header} (ascending)</option>,
              <option key={`${c.sortKey},desc`} value={`${c.sortKey},desc`}>{c.header} (descending)</option>,
            ])}
          </select>
        </div>
      )}
      <ul aria-label={caption} className="divide-y divide-line">
        {rows.map((row) => (
          <li key={row[rowKey]} onClick={onRowClick ? () => onRowClick(row) : undefined}
            className={`px-4 py-3 text-sm ${onRowClick ? 'cursor-pointer active:bg-royal-50/40' : ''}`}>
            <div className="text-ink">{title.render ? title.render(row) : row[title.key]}</div>
            <dl className="mt-2 grid grid-cols-2 gap-x-4 gap-y-2">
              {rest.filter((c) => !c.hideOnCard).map((c) => (
                <div key={c.key} className={c.header ? '' : 'col-span-2'}>
                  {c.header && <dt className="text-xs text-ink-muted">{c.header}</dt>}
                  <dd className="text-ink">{c.render ? c.render(row) : (row[c.key] ?? '—')}</dd>
                </div>
              ))}
            </dl>
          </li>
        ))}
      </ul>
    </div>
  )
}
