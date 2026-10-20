import { Link } from 'react-router'
import { CalendarClock, CheckCircle2 } from 'lucide-react'
import StatusBadge from '../../components/ui/StatusBadge'
import Button from '../../components/ui/Button'
import { ACTIVITY_TYPES } from '../../utils/labels'
import { formatDateTime } from '../../utils/format'

/** List of follow-ups with Complete / Reschedule actions where the API allows them. */
export default function FollowUpList({ items, onComplete, onReschedule, showCustomer = true }) {
  return (
    <ul className="divide-y divide-line">
      {items.map((f) => (
        <li key={f.id} className="flex flex-col gap-3 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex gap-3">
            <span className={`mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full ${f.status === 'COMPLETED' ? 'bg-success-50 text-success-600' : f.overdue ? 'bg-danger-50 text-danger-600' : 'bg-royal-50 text-royal-600'}`}>
              {f.status === 'COMPLETED' ? <CheckCircle2 className="h-4 w-4" aria-hidden="true" /> : <CalendarClock className="h-4 w-4" aria-hidden="true" />}
            </span>
            <div className="text-sm">
              <p className="font-medium text-ink">
                {ACTIVITY_TYPES[f.activityType]}
                {showCustomer && <> with <Link to={`/customers/${f.customer.id}`} className="hover:text-royal-600">{f.customer.name}</Link></>}
              </p>
              <p className="text-xs text-ink-muted">
                {f.status === 'COMPLETED' ? `Completed ${formatDateTime(f.completedAt)}` : formatDateTime(f.scheduledAt)}
                {' · '}{f.assignedOfficer.fullName}
                {f.lead && <> · <Link to={`/leads/${f.lead.id}`} className="hover:text-royal-600">{f.lead.code}</Link></>}
              </p>
              {f.outcome && <p className="mt-1 text-sm text-ink">{f.outcome}</p>}
              {f.notes && <p className="mt-0.5 text-xs text-ink-muted">{f.notes}</p>}
            </div>
          </div>
          <div className="flex shrink-0 items-center gap-2 pl-11 sm:pl-0">
            {f.overdue && <StatusBadge tone="danger">Overdue</StatusBadge>}
            {f.canEdit && (
              <>
                <Button variant="secondary" onClick={() => onReschedule(f)}>Reschedule</Button>
                <Button onClick={() => onComplete(f)}>Complete</Button>
              </>
            )}
          </div>
        </li>
      ))}
    </ul>
  )
}
