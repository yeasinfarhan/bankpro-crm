import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { CalendarPlus, Pencil, UserCog } from 'lucide-react'
import PageHeader from '../../components/ui/PageHeader'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import StatusBadge from '../../components/ui/StatusBadge'
import LoadingState from '../../components/ui/LoadingState'
import ErrorState from '../../components/ui/ErrorState'
import useApi from '../../hooks/useApi'
import { decideSale, fetchLead } from '../../api/leadsApi'
import { fetchFollowUps } from '../../api/followUpsApi'
import { getErrorMessage } from '../../api/client'
import { useToast } from '../../components/feedback/ToastContext'
import StageMenu from './StageMenu'
import LeadDialogHost from './LeadDialogHost'
import useLeadActions from './useLeadActions'
import FollowUpList from '../followups/FollowUpList'
import { CompleteDialog, RescheduleDialog, ScheduleDialog } from '../followups/FollowUpDialogs'
import { LEAD_SOURCES, LEAD_STATUSES, LEAD_STATUS_TONE, LOSS_REASONS, PIPELINE, PRIORITIES, PRIORITY_TONE, PRODUCT_CATEGORIES, SALE_STATUSES, SALE_STATUS_TONE } from '../../utils/labels'
import { formatBdt, formatDate, formatDateTime } from '../../utils/format'

/** Visual pipeline position: completed steps are filled; LOST is shown separately. */
function StageTrack({ status }) {
  const steps = PIPELINE.filter((s) => s !== 'LOST')
  const current = steps.indexOf(status)
  return (
    <ol className="flex flex-wrap items-center gap-2 text-xs" aria-label="Pipeline progress">
      {steps.map((s, i) => (
        <li key={s} className="flex items-center gap-2">
          <span className={`rounded-full px-2.5 py-1 font-medium ${status === 'LOST' ? 'bg-gray-100 text-ink-muted' : i <= current ? 'bg-navy-900 text-white' : 'bg-gray-100 text-ink-muted'}`}
            aria-current={i === current ? 'step' : undefined}>
            {LEAD_STATUSES[s]}
          </span>
          {i < steps.length - 1 && <span className="h-px w-4 bg-line" aria-hidden="true" />}
        </li>
      ))}
      {status === 'LOST' && <li><StatusBadge tone="neutral">Lost</StatusBadge></li>}
    </ol>
  )
}

export default function LeadDetailPage() {
  const { id } = useParams()
  const { notify } = useToast()
  const lead = useApi(() => fetchLead(id), [id])
  const followUps = useApi(() => fetchFollowUps({ view: 'ALL', leadId: id, size: 50 }), [id])
  const actions = useLeadActions(() => {
    lead.reload()
    followUps.reload()
  })
  const [fuDialog, setFuDialog] = useState(null)
  const [deciding, setDeciding] = useState(false)

  if (lead.status === 'loading') return <Card><LoadingState rows={8} label="Loading lead…" /></Card>
  if (lead.status === 'error') return <Card><ErrorState message={lead.error} onRetry={lead.reload} /></Card>
  const l = lead.data

  const decide = async (decision) => {
    setDeciding(true)
    try {
      await decideSale(l.sale.id, decision, null)
      notify(decision === 'APPROVE' ? 'Approved (simulated)' : 'Declined (simulated)')
      lead.reload()
    } catch (e) {
      notify(getErrorMessage(e), 'error')
    } finally {
      setDeciding(false)
    }
  }

  const fuDone = (_, message) => {
    setFuDialog(null)
    notify(message)
    followUps.reload()
  }

  const details = [
    ['Customer', <Link key="c" to={`/customers/${l.customer.id}`} className="text-royal-600 hover:underline">{l.customer.name} ({l.customer.code})</Link>],
    ['Product', `${l.product.name} · ${PRODUCT_CATEGORIES[l.productCategory]}`],
    ['Estimated value', formatBdt(l.estimatedValue)],
    ['Source', LEAD_SOURCES[l.source]],
    ['Priority', <StatusBadge key="p" tone={PRIORITY_TONE[l.priority]}>{PRIORITIES[l.priority]}</StatusBadge>],
    ['Relationship officer', l.assignedOfficer?.fullName],
    ['Expected close', formatDate(l.expectedCloseDate)],
    ['Last contacted', formatDateTime(l.lastContactedAt)],
    ['Created', formatDateTime(l.createdAt)],
    ['Stage changed', formatDateTime(l.statusChangedAt)],
  ]

  return (
    <>
      <PageHeader
        title={`${l.product.name} — ${l.customer.name}`}
        description={l.code}
        breadcrumbs={[{ label: 'Home', to: '/' }, { label: 'Leads', to: '/leads' }, { label: l.code }]}
        actions={
          <>
            <StatusBadge tone={LEAD_STATUS_TONE[l.status]}>{LEAD_STATUSES[l.status]}</StatusBadge>
            {l.permissions.canAssign && <Button variant="secondary" icon={UserCog} onClick={() => actions.setDialog({ type: 'assign', lead: l })}>Assign</Button>}
            {l.permissions.canEdit && <Button as={Link} to={`/leads/${l.id}/edit`} variant="secondary" icon={Pencil}>Edit</Button>}
            {l.permissions.canConvert && <Button onClick={() => actions.move(l, 'CONVERTED')}>Convert to sale</Button>}
            <StageMenu lead={l} onMove={actions.move} disabled={actions.busyId === l.id} />
          </>
        }
      />

      <Card className="mb-6 p-4"><StageTrack status={l.status} /></Card>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <h2 className="border-b border-line px-6 py-4 text-sm font-semibold">Opportunity</h2>
          <dl className="grid gap-x-6 gap-y-4 p-6 sm:grid-cols-2">
            {details.map(([label, value]) => (
              <div key={label}><dt className="text-xs text-ink-muted">{label}</dt><dd className="mt-0.5 text-sm font-medium text-ink">{value}</dd></div>
            ))}
            {l.notes && <div className="sm:col-span-2"><dt className="text-xs text-ink-muted">Notes</dt><dd className="mt-0.5 whitespace-pre-line text-sm text-ink">{l.notes}</dd></div>}
            {l.status === 'LOST' && (
              <div className="rounded-lg bg-gray-50 p-3 sm:col-span-2">
                <dt className="text-xs text-ink-muted">Loss reason</dt>
                <dd className="mt-0.5 text-sm font-medium text-ink">{LOSS_REASONS[l.lossReason]}{l.lossNote && ` — ${l.lossNote}`}</dd>
              </div>
            )}
          </dl>
        </Card>

        <Card>
          <h2 className="border-b border-line px-6 py-4 text-sm font-semibold">Simulated sale</h2>
          {l.sale ? (
            <div className="space-y-3 p-6 text-sm">
              <StatusBadge tone={SALE_STATUS_TONE[l.sale.status]}>{SALE_STATUSES[l.sale.status]}</StatusBadge>
              <p className="text-2xl font-semibold text-ink">{formatBdt(l.sale.amount)}</p>
              <p className="text-ink-muted">Converted {formatDateTime(l.sale.convertedAt)}</p>
              {l.sale.decidedAt && <p className="text-ink-muted">Decided {formatDateTime(l.sale.decidedAt)}</p>}
              {l.sale.status === 'PENDING_APPROVAL' && (
                <p className="rounded-lg bg-warning-50 p-2.5 text-xs text-warning-600">Awaiting a branch manager's simulated decision. Not counted as a completed sale yet.</p>
              )}
              {l.sale.canDecide && (
                <div className="flex gap-2">
                  <Button variant="secondary" loading={deciding} onClick={() => decide('DECLINE')}>Decline</Button>
                  <Button loading={deciding} onClick={() => decide('APPROVE')}>Approve</Button>
                </div>
              )}
            </div>
          ) : (
            <p className="p-6 text-sm text-ink-muted">
              {l.permissions.canConvert ? 'This lead is ready to convert.' : 'Leads can be converted once they are Qualified or at Proposal stage.'}
            </p>
          )}
        </Card>

        <Card className="lg:col-span-3">
          <div className="flex items-center justify-between border-b border-line px-6 py-4">
            <h2 className="text-sm font-semibold">Follow-ups for this lead</h2>
            {l.permissions.canEdit && <Button variant="secondary" icon={CalendarPlus} onClick={() => setFuDialog({ type: 'schedule' })}>Schedule</Button>}
          </div>
          {followUps.status === 'loading' && <LoadingState rows={2} />}
          {followUps.status === 'ok' && followUps.data.content.length === 0 && <p className="px-6 py-4 text-sm text-ink-muted">No follow-ups yet.</p>}
          {followUps.status === 'ok' && followUps.data.content.length > 0 && (
            <FollowUpList items={followUps.data.content} showCustomer={false}
              onComplete={(f) => setFuDialog({ type: 'complete', followUp: f })}
              onReschedule={(f) => setFuDialog({ type: 'reschedule', followUp: f })} />
          )}
        </Card>
      </div>

      <LeadDialogHost dialog={actions.dialog} onClose={actions.closeDialog} onDone={actions.done} />
      {fuDialog?.type === 'schedule' && <ScheduleDialog customer={l.customer} lead={l} onClose={() => setFuDialog(null)} onDone={fuDone} />}
      {fuDialog?.type === 'complete' && <CompleteDialog followUp={fuDialog.followUp} onClose={() => setFuDialog(null)} onDone={fuDone} />}
      {fuDialog?.type === 'reschedule' && <RescheduleDialog followUp={fuDialog.followUp} onClose={() => setFuDialog(null)} onDone={fuDone} />}
    </>
  )
}
