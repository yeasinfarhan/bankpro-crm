import { useMemo, useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import Card from '../../components/ui/Card'
import LoadingState from '../../components/ui/LoadingState'
import ErrorState from '../../components/ui/ErrorState'
import useApi from '../../hooks/useApi'
import { fetchFollowUps } from '../../api/followUpsApi'
import { bstDateKey, formatTime } from '../../utils/format'
import { ACTIVITY_TYPES } from '../../utils/labels'
import FollowUpList from './FollowUpList'

const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
const pad = (n) => String(n).padStart(2, '0')

/** Month calendar in Bangladesh time. Clicking a day shows that day's activities below the grid. */
export default function FollowUpCalendar({ onComplete, onReschedule, refreshKey }) {
  // Today's date is read once when the calendar opens (keeps rendering pure).
  const [todayKey] = useState(() => bstDateKey(new Date().toISOString()))
  const [month, setMonth] = useState(() => todayKey.slice(0, 7)) // "YYYY-MM"
  const [selected, setSelected] = useState(todayKey)

  const [year, mon] = month.split('-').map(Number)
  const daysInMonth = new Date(Date.UTC(year, mon, 0)).getUTCDate()
  const firstWeekday = new Date(Date.UTC(year, mon - 1, 1)).getUTCDay()
  const from = `${month}-01`
  const to = `${month}-${pad(daysInMonth)}`

  const { status, data, error, reload } = useApi(() => fetchFollowUps({ view: 'ALL', from, to, size: 200 }), [from, to, refreshKey])

  const byDay = useMemo(() => {
    const map = {}
    for (const f of data?.content ?? []) (map[bstDateKey(f.scheduledAt)] ??= []).push(f)
    return map
  }, [data])

  const shift = (delta) => {
    const d = new Date(Date.UTC(year, mon - 1 + delta, 1))
    setMonth(`${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}`)
  }
  const title = new Intl.DateTimeFormat('en-GB', { month: 'long', year: 'numeric', timeZone: 'UTC' }).format(new Date(Date.UTC(year, mon - 1, 1)))
  const cells = [...Array(firstWeekday).fill(null), ...Array.from({ length: daysInMonth }, (_, i) => `${month}-${pad(i + 1)}`)]
  const selectedItems = byDay[selected] ?? []

  return (
    <Card>
      <div className="flex items-center justify-between border-b border-line px-4 py-3">
        <button type="button" onClick={() => shift(-1)} aria-label="Previous month" className="rounded-lg p-1.5 hover:bg-gray-100"><ChevronLeft className="h-4 w-4" /></button>
        <h2 className="text-sm font-semibold text-ink" aria-live="polite">{title}</h2>
        <button type="button" onClick={() => shift(1)} aria-label="Next month" className="rounded-lg p-1.5 hover:bg-gray-100"><ChevronRight className="h-4 w-4" /></button>
      </div>
      {status === 'loading' && <LoadingState />}
      {status === 'error' && <ErrorState message={error} onRetry={reload} />}
      {status === 'ok' && (
        <>
          <div className="grid grid-cols-7 border-b border-line text-center text-xs font-medium text-ink-muted">
            {WEEKDAYS.map((d) => <div key={d} className="py-2">{d}</div>)}
          </div>
          <div className="grid grid-cols-7">
            {cells.map((day, i) => {
              if (!day) return <div key={`blank-${i}`} className="min-h-16 border-b border-r border-line bg-canvas/50 sm:min-h-24" />
              const items = byDay[day] ?? []
              const overdue = items.some((f) => f.overdue)
              return (
                <button key={day} type="button" onClick={() => setSelected(day)} aria-pressed={selected === day}
                  aria-label={`${day}: ${items.length} activities`}
                  className={`min-h-16 border-b border-r border-line p-1.5 text-left align-top hover:bg-royal-50/50 sm:min-h-24 ${selected === day ? 'bg-royal-50' : ''}`}>
                  <span className={`inline-flex h-6 w-6 items-center justify-center rounded-full text-xs ${day === todayKey ? 'bg-navy-900 font-semibold text-white' : 'text-ink'}`}>
                    {Number(day.slice(8))}
                  </span>
                  <ul className="mt-1 hidden space-y-0.5 sm:block">
                    {items.slice(0, 2).map((f) => (
                      <li key={f.id} className={`truncate rounded px-1 text-[11px] ${f.status === 'COMPLETED' ? 'bg-success-50 text-success-600' : f.overdue ? 'bg-danger-50 text-danger-600' : 'bg-royal-100 text-royal-700'}`}>
                        {formatTime(f.scheduledAt)} {ACTIVITY_TYPES[f.activityType]}
                      </li>
                    ))}
                    {items.length > 2 && <li className="px-1 text-[11px] text-ink-muted">+{items.length - 2} more</li>}
                  </ul>
                  {items.length > 0 && (
                    <span className={`mt-1 block h-1.5 w-1.5 rounded-full sm:hidden ${overdue ? 'bg-danger-600' : 'bg-royal-600'}`} aria-hidden="true" />
                  )}
                </button>
              )
            })}
          </div>
          <div className="border-t border-line">
            <h3 className="px-4 pt-3 text-sm font-semibold text-ink">
              {new Intl.DateTimeFormat('en-GB', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' }).format(new Date(`${selected}T00:00:00Z`))}
            </h3>
            {selectedItems.length === 0
              ? <p className="px-4 py-4 text-sm text-ink-muted">No activities on this day.</p>
              : <FollowUpList items={selectedItems} onComplete={onComplete} onReschedule={onReschedule} />}
          </div>
        </>
      )}
    </Card>
  )
}
