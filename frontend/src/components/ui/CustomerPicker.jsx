import { useEffect, useState } from 'react'
import { Search, X } from 'lucide-react'
import useDebounce from '../../hooks/useDebounce'
import { fetchCustomers } from '../../api/customersApi'
import { inputClass } from './FormField'

/**
 * Search-as-you-type customer selector backed by the server-side customer search
 * (so it only ever offers customers the user is allowed to see).
 */
export default function CustomerPicker({ id, value, onChange, invalid }) {
  const [q, setQ] = useState('')
  const [results, setResults] = useState([])
  const [open, setOpen] = useState(false)
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

  return (
    <div className="relative">
      <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-ink-muted" aria-hidden="true" />
      <input
        id={id}
        type="search"
        role="combobox"
        aria-expanded={open && results.length > 0}
        aria-controls={`${id}-options`}
        aria-invalid={invalid ? 'true' : 'false'}
        autoComplete="off"
        placeholder="Search by name, phone or CUS-code"
        value={q}
        onChange={(e) => {
          setQ(e.target.value)
          setOpen(true)
          if (!e.target.value) setResults([])
        }}
        className={`${inputClass} pl-9`}
      />
      {open && term && results.length > 0 && (
        <ul id={`${id}-options`} role="listbox" className="absolute z-20 mt-1 max-h-60 w-full overflow-y-auto rounded-lg border border-line bg-white py-1 shadow-lg">
          {results.map((c) => (
            <li key={c.id} role="option" aria-selected="false">
              <button type="button" onClick={() => { onChange({ id: c.id, code: c.code, name: c.fullName }); setOpen(false); setQ('') }}
                className="flex w-full items-center justify-between px-3 py-2 text-left text-sm hover:bg-royal-50">
                <span className="font-medium text-ink">{c.fullName}</span>
                <span className="text-xs text-ink-muted">{c.code}</span>
              </button>
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
