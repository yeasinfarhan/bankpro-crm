import { useState } from 'react'
import { Link } from 'react-router'
import { ClipboardCheck } from 'lucide-react'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import useApi from '../../hooks/useApi'
import { decideSale, fetchSales } from '../../api/leadsApi'
import { getErrorMessage } from '../../api/client'
import { useToast } from '../../components/feedback/ToastContext'
import { formatBdt, formatDate } from '../../utils/format'

/** Managers: loan/card applications waiting for a SIMULATED approval decision. */
export default function PendingApprovals({ onDecided }) {
  const { notify } = useToast()
  const { status, data, reload } = useApi(() => fetchSales('PENDING_APPROVAL'), [])
  const [busy, setBusy] = useState(null)

  if (status !== 'ok' || data.length === 0) return null

  const decide = async (sale, decision) => {
    setBusy(sale.id)
    try {
      await decideSale(sale.id, decision, null)
      notify(decision === 'APPROVE' ? 'Approved (simulated) — counted as a completed sale' : 'Declined (simulated)')
      reload()
      onDecided?.()
    } catch (e) {
      notify(getErrorMessage(e), 'error')
    } finally {
      setBusy(null)
    }
  }

  return (
    <Card className="mb-4 border-warning-600/30">
      <h2 className="flex items-center gap-2 border-b border-line px-4 py-3 text-sm font-semibold text-ink">
        <ClipboardCheck className="h-4 w-4 text-warning-600" aria-hidden="true" />
        Applications awaiting your simulated decision ({data.length})
      </h2>
      <ul className="divide-y divide-line">
        {data.map((s) => (
          <li key={s.id} className="flex flex-col gap-2 px-4 py-3 text-sm sm:flex-row sm:items-center sm:justify-between">
            <div>
              <Link to={`/leads/${s.leadId}`} className="font-medium text-ink hover:text-royal-600">{s.customer.name}</Link>
              <span className="text-ink-muted"> · {s.product.name} · {formatBdt(s.amount)} · {s.officer.fullName} · {formatDate(s.convertedAt)}</span>
            </div>
            {s.canDecide && (
              <div className="flex gap-2">
                <Button variant="secondary" loading={busy === s.id} onClick={() => decide(s, 'DECLINE')}>Decline</Button>
                <Button loading={busy === s.id} onClick={() => decide(s, 'APPROVE')}>Approve</Button>
              </div>
            )}
          </li>
        ))}
      </ul>
      <p className="border-t border-line px-4 py-2 text-xs text-ink-muted">Demonstration only — BankPro does not assess credit or eligibility.</p>
    </Card>
  )
}
