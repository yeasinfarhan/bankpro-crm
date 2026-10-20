import { useEffect, useState } from 'react'
import { Search, X } from 'lucide-react'
import useDebounce from '../../hooks/useDebounce'
import { fetchCustomers } from '../../api/customersApi'
import { inputClass } from './FormField'

/**
 * Search-as-you-type customer selector backed by the server-side customer search
 * (so it only ever offers customers the user is allowed to see). ARIA combobox: arrow keys + Enter pick a customer.
 */
export default function CustomerPicker({ id, value, onChange, invalid }) {
  const [q, setQ] = useState('')
  const [results, setResults] = useState([])
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(-1)
  const term = useDebounce(q, 250)

  useEffect(() => {
    if (!term || value) return undefined
    let active = true
    fetchCustomers({ q: term, size: 8, sort: 'fullName,asc' }).then(
      (page) => active && setResults(page.content),
      () => active && setResults([]),
    )
    return () => {
      active = false
    }
  }, [term, value])

  if (value) {
    return (
      <div className="flex items-center justify-between rounded-lg border border-line bg-canvas px-3 py-2 text-sm">
        <span>
          <span className="font-medium text-ink">{value.name ?? value.fullName}</span>{' '}
          <span className="text-ink-muted">{value.code}</span>
        </span>
        <button type="button" onClick={() => onChange(null)} aria-label="Change customer" className="text-ink-muted hover:text-ink">
          <X className="h-4 w-4" />
        </button>
      </div>
    )
  }

  const showList = open && Boolean(term) && results.length > 0
  const pick = (c) => {
    onChange({ id: c.id, code: c.code, name: c.fullName })
    setOpen(false)
    setQ('')
    setActive(-1)
  }

  return (
    <div className="relative">
      <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-ink-muted" aria-hidden="true" />
      <input
        id={id}
        type="search"
        role="combobox"
        aria-expanded={showList}
        aria-controls={`${id}-options`}
        aria-invalid={invalid ? 'true' : 'false'}
        autoComplete="off"
        placeholder="Search by name, phone or CUS-code"
        value={q}
        aria-activedescendant={showList && active >= 0 ? `${id}-opt-${results[active].id}` : undefined}
        onChange={(e) => {
          setQ(e.target.value)
          setOpen(true)
          setActive(-1)
          if (!e.target.value) setResults([])
        }}
        onKeyDown={(e) => {
          if (!showList) return
          if (e.key === 'ArrowDown') { e.preventDefault(); setActive((i) => Math.min(i + 1, results.length - 1)) }
          else if (e.key === 'ArrowUp') { e.preventDefault(); setActive((i) => Math.max(i - 1, 0)) }
          else if (e.key === 'Enter' && active >= 0) { e.preventDefault(); pick(results[active]) }
          else if (e.key === 'Escape') { e.preventDefault(); setOpen(false) }
        }}
        className={`${inputClass} pl-9`}
      />
      {showList && (
        <ul id={`${id}-options`} role="listbox" aria-label="Matching customers" className="absolute z-20 mt-1 max-h-60 w-full overflow-y-auto rounded-lg border border-line bg-white py-1 shadow-lg">
          {results.map((c, i) => (
            <li key={c.id} id={`${id}-opt-${c.id}`} role="option" aria-selected={active === i}
              onMouseDown={(e) => e.preventDefault()} onClick={() => pick(c)} onMouseEnter={() => setActive(i)}
              className={`flex cursor-pointer items-center justify-between px-3 py-2 text-sm ${active === i ? 'bg-royal-50' : ''}`}>
              <span className="font-medium text-ink">{c.fullName}</span>
              <span className="text-xs text-ink-muted">{c.code}</span>
            </li>
          ))}
        </ul>
      )}
      {open && term && results.length === 0 && (
        <p className="absolute z-20 mt-1 w-full rounded-lg border border-line bg-white px-3 py-2 text-sm text-ink-muted shadow-lg">No matching customers.</p>
      )}
    </div>
  )
}
