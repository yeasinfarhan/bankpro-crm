import { useState } from 'react'
import { Link } from 'react-router'
import { CalendarPlus, FileText, History, Plus } from 'lucide-react'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import StatusBadge from '../../components/ui/StatusBadge'
import LoadingState from '../../components/ui/LoadingState'
import ErrorState from '../../components/ui/ErrorState'
import Modal from '../../components/ui/Modal'
import FormField, { inputClass } from '../../components/ui/FormField'
import useApi from '../../hooks/useApi'
import { fetchLeads } from '../../api/leadsApi'
import { fetchFollowUps, fetchTimeline, recordInteraction } from '../../api/followUpsApi'
import { getErrorMessage } from '../../api/client'
import { useToast } from '../../components/feedback/ToastContext'
import { useAuth } from '../auth/AuthContext'
import FollowUpList from '../followups/FollowUpList'
import { CompleteDialog, RescheduleDialog, ScheduleDialog } from '../followups/FollowUpDialogs'
import { INTERACTION_TYPES, LEAD_STATUSES, LEAD_STATUS_TONE } from '../../utils/labels'
import { formatBdt, formatDateTime } from '../../utils/format'

const KIND_STYLE = {
  INTERACTION: 'bg-royal-600',
  FOLLOW_UP_SCHEDULED: 'bg-warning-600',
  LEAD_CREATED: 'bg-navy-700',
  LEAD_CLOSED: 'bg-gray-400',
  SALE: 'bg-success-600',
}

function LogInteractionDialog({ customer, onClose, onDone }) {
  const [type, setType] = useState('NOTE')
  const [summary, setSummary] = useState('')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const submit = async () => {
    if (!summary.trim()) return setError('Write a short summary.')
    setBusy(true)
    try {
      await recordInteraction(customer.id, { interactionType: type, summary: summary.trim() })
      onDone()
    } catch (e) {
      setError(getErrorMessage(e))
      setBusy(false)
    }
  }
  return (
    <Modal open title="Log interaction" onClose={onClose}
      footer={<><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={busy} onClick={submit}>Save</Button></>}>
      <div className="space-y-4">
        {error && <p role="alert" className="rounded-lg bg-danger-50 p-2.5 text-sm text-danger-600">{error}</p>}
        <FormField label="Type" id="int-type">
          <select id="int-type" value={type} onChange={(e) => setType(e.target.value)} className={inputClass}>
            {Object.entries(INTERACTION_TYPES).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </FormField>
        <FormField label="What happened?" id="int-summary" required>
          <textarea id="int-summary" rows={3} maxLength={2000} value={summary} onChange={(e) => setSummary(e.target.value)} className={inputClass} />
        </FormField>
      </div>
    </Modal>
  )
}

/** Leads, follow-ups and the chronological history shown on the customer page. */
export default function CustomerActivity({ customer, onChanged }) {
  const { user } = useAuth()
  const { notify } = useToast()
  const canWork = user.role !== 'ADMIN' && customer.status !== 'ARCHIVED'
  const ref = { id: customer.id, code: customer.code, name: customer.fullName }
  const leads = useApi(() => fetchLeads({ customerId: customer.id, size: 50, sort: 'createdAt,desc' }), [customer.id])
  const followUps = useApi(() => fetchFollowUps({ view: 'ALL', customerId: customer.id, size: 50 }), [customer.id])
  const timeline = useApi(() => fetchTimeline(customer.id), [customer.id])
  const [dialog, setDialog] = useState(null)

  const refresh = (message) => {
    setDialog(null)
    if (message) notify(message)
    followUps.reload()
    timeline.reload()
    onChanged?.() // e.g. refresh "Last interaction" on the profile card
  }
  const upcoming = (followUps.data?.content ?? []).filter((f) => f.status === 'SCHEDULED')

  return (
    <>
      <Card className="lg:col-span-2">
        <div className="flex items-center justify-between border-b border-line px-6 py-4">
          <h2 className="text-sm font-semibold">Leads</h2>
          {canWork && <Button as={Link} to={`/leads/new?customerId=${customer.id}`} variant="secondary" icon={Plus}>New lead</Button>}
        </div>
        {leads.status === 'loading' && <LoadingState rows={2} />}
        {leads.status === 'error' && <ErrorState message={leads.error} onRetry={leads.reload} />}
        {leads.status === 'ok' && leads.data.content.length === 0 && (
          <p className="px-6 py-4 text-sm text-ink-muted">
            {user.role === 'RELATIONSHIP_OFFICER' ? 'You have no leads for this customer.' : 'No leads for this customer yet.'}
          </p>
        )}
        {leads.status === 'ok' && leads.data.content.length > 0 && (
          <ul className="divide-y divide-line">
            {leads.data.content.map((l) => (
              <li key={l.id} className="flex items-center justify-between gap-3 px-6 py-3 text-sm">
                <div>
                  <Link to={`/leads/${l.id}`} className="font-medium text-ink hover:text-royal-600">{l.product.name}</Link>
                  <p className="text-xs text-ink-muted">{l.code} · {formatBdt(l.estimatedValue)} · {l.assignedOfficer?.fullName}</p>
                </div>
                <StatusBadge tone={LEAD_STATUS_TONE[l.status]}>{LEAD_STATUSES[l.status]}</StatusBadge>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card>
        <div className="flex items-center justify-between border-b border-line px-6 py-4">
          <h2 className="text-sm font-semibold">Upcoming follow-ups</h2>
          {canWork && <Button variant="secondary" icon={CalendarPlus} onClick={() => setDialog({ type: 'schedule' })} aria-label="Schedule follow-up">Schedule</Button>}
        </div>
        {followUps.status === 'loading' && <LoadingState rows={2} />}
        {followUps.status === 'ok' && upcoming.length === 0 && <p className="px-6 py-4 text-sm text-ink-muted">Nothing scheduled.</p>}
        {followUps.status === 'ok' && upcoming.length > 0 && (
          <FollowUpList items={upcoming} showCustomer={false}
            onComplete={(f) => setDialog({ type: 'complete', followUp: f })}
            onReschedule={(f) => setDialog({ type: 'reschedule', followUp: f })} />
        )}
      </Card>

      <Card className="lg:col-span-3">
        <div className="flex items-center justify-between border-b border-line px-6 py-4">
          <h2 className="flex items-center gap-2 text-sm font-semibold"><History className="h-4 w-4 text-ink-muted" aria-hidden="true" /> Activity history</h2>
          {canWork && <Button variant="secondary" icon={FileText} onClick={() => setDialog({ type: 'log' })}>Log interaction</Button>}
        </div>
        {timeline.status === 'loading' && <LoadingState rows={3} />}
        {timeline.status === 'error' && <ErrorState message={timeline.error} onRetry={timeline.reload} />}
        {timeline.status === 'ok' && timeline.data.length === 0 && <p className="px-6 py-4 text-sm text-ink-muted">No activity recorded yet.</p>}
        {timeline.status === 'ok' && timeline.data.length > 0 && (
          <ol className="relative mx-6 my-4 border-l border-line">
            {timeline.data.map((item, i) => (
              <li key={`${item.kind}-${item.at}-${i}`} className="mb-4 ml-4">
                <span className={`absolute -left-1.5 mt-1.5 h-3 w-3 rounded-full ring-4 ring-white ${KIND_STYLE[item.kind] ?? 'bg-gray-400'}`} aria-hidden="true" />
                <p className="text-sm font-medium text-ink">
                  {item.link ? <Link to={item.link} className="hover:text-royal-600">{item.title}</Link> : item.title}
                </p>
                {item.detail && <p className="text-sm text-ink-muted">{item.detail}</p>}
                <p className="text-xs text-ink-muted">{formatDateTime(item.at)}{item.actor && ` · ${item.actor}`}</p>
              </li>
            ))}
          </ol>
        )}
      </Card>

      {dialog?.type === 'schedule' && <ScheduleDialog customer={ref} onClose={() => setDialog(null)} onDone={(_, m) => refresh(m)} />}
      {dialog?.type === 'complete' && <CompleteDialog followUp={dialog.followUp} onClose={() => setDialog(null)} onDone={(_, m) => refresh(m)} />}
      {dialog?.type === 'reschedule' && <RescheduleDialog followUp={dialog.followUp} onClose={() => setDialog(null)} onDone={(_, m) => refresh(m)} />}
      {dialog?.type === 'log' && <LogInteractionDialog customer={customer} onClose={() => setDialog(null)} onDone={() => refresh('Interaction logged')} />}
    </>
  )
}
