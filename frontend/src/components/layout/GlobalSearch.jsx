import { useEffect, useId, useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { Loader2, Search, Target, User } from 'lucide-react'
import { fetchCustomers } from '../../api/customersApi'
import { fetchLeads } from '../../api/leadsApi'
import { getErrorMessage } from '../../api/client'
import useDebounce from '../../hooks/useDebounce'
import useDismiss from './useDismiss'
import { LEAD_STATUSES } from '../../utils/labels'

const LIMIT = 5

/**
 * Top-bar search across customers and leads (ARIA combobox). Uses the same scoped list endpoints as the
 * Customers and Leads pages, so it can only ever return records the signed-in user is allowed to see.
 * Shortcut: "/" or Ctrl+K focuses it.
 */
export default function GlobalSearch({ autoFocus = false, onNavigate }) {
  const navigate = useNavigate()
  const listId = useId()
  const boxRef = useRef(null)
  const inputRef = useRef(null)
  const [q, setQ] = useState('')
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(-1)
  const [result, setResult] = useState({ term: '', status: 'idle', customers: [], leads: [], error: null })
  const term = useDebounce(q.trim(), 250)

  useDismiss(boxRef, open, () => setOpen(false))

  useEffect(() => {
    if (term.length < 2) return undefined
    let current = true
    Promise.all([fetchCustomers({ q: term, size: LIMIT }), fetchLeads({ q: term, size: LIMIT })])
      .then(([c, l]) => current && setResult({ term, status: 'ok', customers: c.content, leads: l.content, error: null }))
      .catch((e) => current && setResult({ term, status: 'error', customers: [], leads: [], error: getErrorMessage(e) }))
    return () => { current = false }
  }, [term])

  useEffect(() => {
    const onKey = (e) => {
      const typing = /^(INPUT|TEXTAREA|SELECT)$/.test(e.target.tagName) || e.target.isContentEditable
      if ((e.key === '/' && !typing) || (e.key.toLowerCase() === 'k' && (e.ctrlKey || e.metaKey))) {
        e.preventDefault()
        inputRef.current?.focus()
        setOpen(true)
      }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [])

  const ready = term.length >= 2 && result.term === term
  const items = ready && result.status === 'ok' ? [
    ...result.customers.map((c) => ({ id: `c${c.id}`, to: `/customers/${c.id}`, kind: 'customer', title: c.fullName, meta: `${c.code} · ${c.branch?.name ?? ''}` })),
    ...result.leads.map((l) => ({ id: `l${l.id}`, to: `/leads/${l.id}`, kind: 'lead', title: `${l.customer.name} — ${l.product.name}`, meta: `${l.code} · ${LEAD_STATUSES[l.status]}` })),
  ] : []
  const go = (to) => {
    setOpen(false)
    setQ('')
    setActive(-1)
    navigate(to)
    onNavigate?.()
  }
  const seeAll = (path) => `${path}${path.includes('?') ? '&' : '?'}q=${encodeURIComponent(q.trim())}`

  const onKeyDown = (e) => {
    if (e.key === 'ArrowDown') { e.preventDefault(); setOpen(true); setActive((i) => Math.min(i + 1, items.length - 1)) }
    else if (e.key === 'ArrowUp') { e.preventDefault(); setActive((i) => Math.max(i - 1, -1)) }
    else if (e.key === 'Enter' && q.trim().length >= 2) { e.preventDefault(); go(active >= 0 && items[active] ? items[active].to : seeAll('/customers')) }
    else if (e.key === 'Escape') { setOpen(false); setActive(-1) }
  }

  const showPanel = open && q.trim().length >= 2
  const loading = !ready
  const group = (kind, label, Icon) => {
    const list = items.map((it, i) => ({ ...it, i })).filter((it) => it.kind === kind)
    if (!list.length) return null
    return (
      <div role="group" aria-label={label}>
        <p className="px-3 pb-1 pt-2 text-[11px] font-semibold uppercase tracking-wide text-ink-muted" aria-hidden="true">{label}</p>
        {list.map((it) => (
          <div key={it.id} id={`${listId}-${it.id}`} role="option" aria-selected={active === it.i} aria-label={`${it.title}, ${it.meta}`}
            onMouseDown={(e) => e.preventDefault()} onClick={() => go(it.to)} onMouseEnter={() => setActive(it.i)}
            className={`flex cursor-pointer items-start gap-2.5 px-3 py-2 ${active === it.i ? 'bg-royal-50' : ''}`}>
            <Icon className="mt-0.5 h-4 w-4 shrink-0 text-ink-muted" aria-hidden="true" />
            <span className="min-w-0">
              <span className="block truncate text-sm font-medium text-ink">{it.title}</span>
              <span className="block truncate text-xs text-ink-muted">{it.meta}</span>
            </span>
          </div>
        ))}
      </div>
    )
  }

  return (
    <div ref={boxRef} className="relative w-full">
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" aria-hidden="true" />
      <input ref={inputRef} type="search" value={q} autoFocus={autoFocus} autoComplete="off"
        onChange={(e) => { setQ(e.target.value); setOpen(true); setActive(-1) }}
        onFocus={() => setOpen(true)} onKeyDown={onKeyDown}
        placeholder="Search customers and leads…" aria-label="Search customers and leads"
        role="combobox" aria-expanded={showPanel} aria-controls={listId} aria-autocomplete="list"
        aria-activedescendant={active >= 0 && items[active] ? `${listId}-${items[active].id}` : undefined}
        className="w-full rounded-lg border border-line bg-canvas py-2 pl-9 pr-9 text-sm placeholder:text-ink-muted focus:border-royal-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-royal-100" />
      {!q && <kbd className="pointer-events-none absolute right-2.5 top-1/2 hidden -translate-y-1/2 rounded border border-line bg-white px-1.5 text-[11px] text-ink-muted sm:block" aria-hidden="true">/</kbd>}

      {showPanel && (
        <div className="absolute left-0 right-0 top-full z-30 mt-1 overflow-hidden rounded-xl border border-line bg-white shadow-lg">
          <div id={listId} role="listbox" aria-label="Search results" className="max-h-96 overflow-y-auto py-1">
            {loading && <p className="flex items-center gap-2 px-3 py-3 text-sm text-ink-muted" role="status"><Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />Searching…</p>}
            {!loading && result.status === 'error' && <p className="px-3 py-3 text-sm text-danger-600" role="alert">{result.error}</p>}
            {!loading && result.status === 'ok' && items.length === 0 && <p className="px-3 py-3 text-sm text-ink-muted" role="status">No customers or leads match “{term}”.</p>}
            {!loading && group('customer', 'Customers', User)}
            {!loading && group('lead', 'Leads', Target)}
          </div>
          <div className="flex flex-wrap gap-x-4 gap-y-1 border-t border-line bg-canvas px-3 py-2 text-xs">
            <button type="button" onClick={() => go(seeAll('/customers'))} className="font-medium text-royal-600 hover:underline">All matching customers</button>
            <button type="button" onClick={() => go(seeAll('/leads?view=table'))} className="font-medium text-royal-600 hover:underline">All matching leads</button>
          </div>
        </div>
      )}
    </div>
  )
}
