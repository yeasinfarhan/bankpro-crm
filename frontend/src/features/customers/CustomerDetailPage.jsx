import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { Archive, ArchiveRestore, Pencil } from 'lucide-react'
import PageHeader from '../../components/ui/PageHeader'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import StatusBadge from '../../components/ui/StatusBadge'
import ConfirmDialog from '../../components/ui/ConfirmDialog'
import LoadingState from '../../components/ui/LoadingState'
import ErrorState from '../../components/ui/ErrorState'
import CustomerActivity from './CustomerActivity'
import useApi from '../../hooks/useApi'
import { archiveCustomer, fetchCustomer, restoreCustomer } from '../../api/customersApi'
import { getErrorMessage } from '../../api/client'
import { useToast } from '../../components/feedback/ToastContext'
import { CUSTOMER_STATUSES, CUSTOMER_STATUS_TONE, EMPLOYMENT_TYPES, INCOME_RANGES, PRODUCT_CATEGORIES, SEGMENTS } from '../../utils/labels'
import { formatDate, formatDateTime, formatPhone } from '../../utils/format'

export default function CustomerDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { notify } = useToast()
  const { status, data: c, error, reload, setData } = useApi(() => fetchCustomer(id), [id])
  const [confirming, setConfirming] = useState(false)
  const [busy, setBusy] = useState(false)

  const archive = async () => {
    setBusy(true)
    try {
      await archiveCustomer(id)
      notify(`${c.fullName} archived`)
      navigate('/customers')
    } catch (e) {
      notify(getErrorMessage(e), 'error')
      setBusy(false)
      setConfirming(false)
    }
  }

  const restore = async () => {
    setBusy(true)
    try {
      setData(await restoreCustomer(id))
      notify(`${c.fullName} restored`)
    } catch (e) {
      notify(getErrorMessage(e), 'error')
    } finally {
      setBusy(false)
    }
  }

  if (status === 'loading') return <Card><LoadingState rows={8} label="Loading customer…" /></Card>
  if (status === 'error') return <Card><ErrorState message={error} onRetry={reload} /></Card>

  const details = [
    ['Phone', formatPhone(c.phone)],
    ['Email', c.email ?? '—'],
    ['City', c.city ?? '—'],
    ['Occupation', c.occupation ?? '—'],
    ['Employment type', EMPLOYMENT_TYPES[c.employmentType] ?? '—'],
    ['Income range', INCOME_RANGES[c.incomeRange] ?? '—'],
    ['Segment', SEGMENTS[c.segment]],
    ['Branch', c.branch.name],
    ['Relationship officer', c.assignedOfficer?.fullName ?? 'Unassigned'],
    ['Customer since', formatDate(c.createdAt)],
    ['Last interaction', formatDateTime(c.lastInteractionAt)],
  ]

  return (
    <>
      <PageHeader
        title={c.fullName}
        description={c.code}
        breadcrumbs={[{ label: 'Home', to: '/' }, { label: 'Customers', to: '/customers' }, { label: c.code }]}
        actions={
          <>
            <StatusBadge tone={CUSTOMER_STATUS_TONE[c.status]}>{CUSTOMER_STATUSES[c.status]}</StatusBadge>
            {c.permissions.canEdit && <Button as={Link} to={`/customers/${c.id}/edit`} variant="secondary" icon={Pencil}>Edit</Button>}
            {c.permissions.canArchive && c.status !== 'ARCHIVED' && (
              <Button variant="secondary" icon={Archive} onClick={() => setConfirming(true)}>Archive</Button>
            )}
            {c.permissions.canArchive && c.status === 'ARCHIVED' && (
              <Button icon={ArchiveRestore} loading={busy} onClick={restore}>Restore</Button>
            )}
          </>
        }
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <h2 className="border-b border-line px-6 py-4 text-sm font-semibold">Profile</h2>
          <dl className="grid gap-x-6 gap-y-4 p-6 sm:grid-cols-2">
            {details.map(([label, value]) => (
              <div key={label}>
                <dt className="text-xs text-ink-muted">{label}</dt>
                <dd className="mt-0.5 text-sm font-medium text-ink">{value}</dd>
              </div>
            ))}
          </dl>
        </Card>

        <Card>
          <h2 className="border-b border-line px-6 py-4 text-sm font-semibold">Banking products held</h2>
          {c.products.length === 0 ? (
            <p className="px-6 py-6 text-sm text-ink-muted">No products recorded yet.</p>
          ) : (
            <ul className="divide-y divide-line">
              {c.products.map((p) => (
                <li key={`${p.productId}-${p.since}`} className="px-6 py-3">
                  <p className="text-sm font-medium text-ink">{p.name}</p>
                  <p className="text-xs text-ink-muted">
                    {PRODUCT_CATEGORIES[p.category]} · since {formatDate(p.since)} {p.fromSale && '· from converted sale'}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <CustomerActivity customer={c} onChanged={() => fetchCustomer(id).then(setData, () => {})} />
      </div>

      <ConfirmDialog
        open={confirming}
        title={`Archive ${c.fullName}?`}
        message="The customer will be hidden from lists, but all leads, sales and history are kept. You can restore them later."
        confirmLabel="Archive customer"
        loading={busy}
        onConfirm={archive}
        onCancel={() => setConfirming(false)}
      />
    </>
  )
}
