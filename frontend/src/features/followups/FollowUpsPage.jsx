import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { CalendarCheck, Plus } from 'lucide-react'
import PageHeader from '../../components/ui/PageHeader'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import LoadingState from '../../components/ui/LoadingState'
import ErrorState from '../../components/ui/ErrorState'
import EmptyState from '../../components/ui/EmptyState'
import Pagination from '../../components/ui/Pagination'
import useApi from '../../hooks/useApi'
import { fetchFollowUpCounts, fetchFollowUps } from '../../api/followUpsApi'
import { useToast } from '../../components/feedback/ToastContext'
import { useAuth } from '../auth/AuthContext'
import FollowUpList from './FollowUpList'
import FollowUpCalendar from './FollowUpCalendar'
import { CompleteDialog, RescheduleDialog, ScheduleDialog } from './FollowUpDialogs'

const TABS = [
  { key: 'TODAY', label: 'Today', count: 'today', empty: 'Nothing scheduled for today.' },
  { key: 'UPCOMING', label: 'Upcoming', count: 'upcoming', empty: 'No upcoming follow-ups.' },
  { key: 'OVERDUE', label: 'Overdue', count: 'overdue', empty: 'No overdue follow-ups — well done.' },
  { key: 'COMPLETED', label: 'Completed', empty: 'No completed activities yet.' },
  { key: 'CALENDAR', label: 'Calendar' },
]

/** Follow-up work queue. "Today" and "overdue" are evaluated in Bangladesh time by the backend. */
export default function FollowUpsPage() {
  const { user } = useAuth()
  const { notify } = useToast()
  const [params, setParams] = useSearchParams()
  const tab = TABS.find((t) => t.key === params.get('view'))?.key ?? 'TODAY'
  const [page, setPage] = useState(0)
  const [dialog, setDialog] = useState(null)
  const [refreshKey, setRefreshKey] = useState(0)

  const counts = useApi(() => fetchFollowUpCounts(), [refreshKey])
  const list = useApi(() => (tab === 'CALENDAR' ? Promise.resolve(null) : fetchFollowUps({ view: tab, page, size: 25 })), [tab, page, refreshKey])

  const selectTab = (key) => {
    setPage(0)
    setParams(key === 'TODAY' ? {} : { view: key }, { replace: true })
  }
  const done = (_, message) => {
    setDialog(null)
    notify(message)
    setRefreshKey((n) => n + 1)
  }
  const tabMeta = TABS.find((t) => t.key === tab)

  return (
    <>
      <PageHeader
        title="Follow-ups"
        description="Times are shown in Bangladesh Standard Time (UTC+6)."
        breadcrumbs={[{ label: 'Home', to: '/' }, { label: 'Follow-ups' }]}
        actions={user.role !== 'ADMIN' && <Button icon={Plus} onClick={() => setDialog({ type: 'schedule' })}>Schedule follow-up</Button>}
      />

      <div role="tablist" aria-label="Follow-up views" className="mb-4 flex gap-1 overflow-x-auto border-b border-line">
        {TABS.map((t) => {
          const n = t.count && counts.data ? counts.data[t.count] : null
          return (
            <button key={t.key} type="button" role="tab" aria-selected={tab === t.key} onClick={() => selectTab(t.key)}
              className={`-mb-px flex items-center gap-2 whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium ${tab === t.key ? 'border-royal-600 text-royal-700' : 'border-transparent text-ink-muted hover:text-ink'}`}>
              {t.label}
              {n !== null && n > 0 && (
                <span className={`rounded-full px-1.5 text-xs ${t.key === 'OVERDUE' ? 'bg-danger-50 text-danger-600' : 'bg-gray-100 text-ink'}`}>{n}</span>
              )}
            </button>
          )
        })}
      </div>

      {tab === 'CALENDAR' ? (
        <FollowUpCalendar refreshKey={refreshKey}
          onComplete={(f) => setDialog({ type: 'complete', followUp: f })}
          onReschedule={(f) => setDialog({ type: 'reschedule', followUp: f })} />
      ) : (
        <Card role="tabpanel">
          {list.status === 'loading' && <LoadingState rows={5} label="Loading follow-ups…" />}
          {list.status === 'error' && <ErrorState message={list.error} onRetry={list.reload} />}
          {list.status === 'ok' && list.data.content.length === 0 && <EmptyState icon={CalendarCheck} title={tabMeta.empty} />}
          {list.status === 'ok' && list.data.content.length > 0 && (
            <>
              <FollowUpList items={list.data.content}
                onComplete={(f) => setDialog({ type: 'complete', followUp: f })}
                onReschedule={(f) => setDialog({ type: 'reschedule', followUp: f })} />
              <Pagination page={list.data.page} size={list.data.size} totalPages={list.data.totalPages} totalElements={list.data.totalElements} onPageChange={setPage} />
            </>
          )}
        </Card>
      )}

      {dialog?.type === 'schedule' && <ScheduleDialog onClose={() => setDialog(null)} onDone={done} />}
      {dialog?.type === 'complete' && <CompleteDialog followUp={dialog.followUp} onClose={() => setDialog(null)} onDone={done} />}
      {dialog?.type === 'reschedule' && <RescheduleDialog followUp={dialog.followUp} onClose={() => setDialog(null)} onDone={done} />}
    </>
  )
}
