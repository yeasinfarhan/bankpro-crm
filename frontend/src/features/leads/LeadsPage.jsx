import { Link, useNavigate, useSearchParams } from 'react-router'
import { Columns3, Plus, Table2, Workflow } from 'lucide-react'
import PageHeader from '../../components/ui/PageHeader'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import SearchBar from '../../components/ui/SearchBar'
import SelectFilter from '../../components/ui/SelectFilter'
import StatusBadge from '../../components/ui/StatusBadge'
import Pagination from '../../components/ui/Pagination'
import LoadingState from '../../components/ui/LoadingState'
import ErrorState from '../../components/ui/ErrorState'
import EmptyState from '../../components/ui/EmptyState'
import DataTable from '../../components/tables/DataTable'
import LeadBoard from './LeadBoard'
import StageMenu from './StageMenu'
import LeadDialogHost from './LeadDialogHost'
import PendingApprovals from './PendingApprovals'
import useLeadActions from './useLeadActions'
import useApi from '../../hooks/useApi'
import useSearchText from '../../hooks/useSearchText'
import { fetchLeadBoard, fetchLeadSummary, fetchLeads } from '../../api/leadsApi'
import { fetchProducts } from '../../api/productsApi'
import { fetchAssignableStaff } from '../../api/usersApi'
import { LEAD_STATUSES, LEAD_STATUS_TONE, options, PRIORITIES, PRIORITY_TONE } from '../../utils/labels'
import { formatBdt, formatDate } from '../../utils/format'
import { useAuth } from '../auth/AuthContext'

/** Pipeline page: summary figures, Kanban board or table, filters in the URL. */
const DATE_INPUT = 'rounded-lg border border-line bg-white px-2.5 py-[7px] text-sm text-ink shadow-sm focus:border-royal-500 focus:outline-none focus:ring-2 focus:ring-royal-100'

export default function LeadsPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const view = params.get('view') === 'table' ? 'table' : 'board'
  const isManager = user.role === 'BRANCH_MANAGER'
  const isOfficer = user.role === 'RELATIONSHIP_OFFICER'

  const get = (k) => params.get(k) ?? ''
  const setParam = (key, value) => {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value)
    else next.delete(key)
    if (key !== 'page') next.delete('page')
    setParams(next, { replace: true })
  }

  const [q, setQ] = useSearchText(params, (text) => setParam('q', text))

  const filters = { q: get('q'), productId: get('productId'), officerId: get('officerId'), from: get('from'), to: get('to') }
  const tableParams = { ...filters, status: get('status'), sort: get('sort') || 'createdAt,desc', page: Number(get('page') || 0), size: 20 }
  const key = JSON.stringify(view === 'table' ? tableParams : filters)

  const summary = useApi(() => fetchLeadSummary(filters), [JSON.stringify(filters)])
  const data = useApi(() => (view === 'table' ? fetchLeads(tableParams) : fetchLeadBoard(filters)), [view, key])
  const products = useApi(() => fetchProducts(), [])
  const staff = useApi(() => (isOfficer ? Promise.resolve([]) : fetchAssignableStaff()), [isOfficer])

  const refresh = () => {
    data.reload()
    summary.reload()
  }
  const actions = useLeadActions(refresh)

  const s = summary.data
  const stats = [
    { label: 'Open leads', value: s ? s.openCount : '—' },
    { label: 'Pipeline value (estimated)', value: s ? formatBdt(s.openPipelineValue) : '—', hint: 'Sum of estimated values of open leads — not sales' },
    { label: 'Converted', value: s ? s.convertedCount : '—' },
    { label: 'Conversion rate', value: s ? (s.conversionRate === null ? '—' : `${s.conversionRate}%`) : '—', hint: 'Converted ÷ (converted + lost)' },
  ]

  const columns = [
    { key: 'customer', header: 'Lead', render: (l) => (
      <div><Link to={`/leads/${l.id}`} onClick={(e) => e.stopPropagation()} className="font-medium text-ink hover:text-royal-600">{l.customer.name}</Link>
        <div className="text-xs text-ink-muted">{l.code}</div></div>) },
    { key: 'product', header: 'Product', render: (l) => l.product.name, className: 'hidden md:table-cell' },
    { key: 'status', header: 'Stage', sortKey: 'status', render: (l) => <StatusBadge tone={LEAD_STATUS_TONE[l.status]}>{LEAD_STATUSES[l.status]}</StatusBadge> },
    { key: 'priority', header: 'Priority', sortKey: 'priority', render: (l) => <StatusBadge tone={PRIORITY_TONE[l.priority]}>{PRIORITIES[l.priority]}</StatusBadge>, className: 'hidden lg:table-cell' },
    { key: 'estimatedValue', header: 'Est. value', sortKey: 'estimatedValue', render: (l) => formatBdt(l.estimatedValue), className: 'whitespace-nowrap' },
    { key: 'officer', header: 'Officer', render: (l) => l.assignedOfficer?.fullName, className: 'hidden lg:table-cell' },
    { key: 'expectedCloseDate', header: 'Expected close', sortKey: 'expectedCloseDate', render: (l) => formatDate(l.expectedCloseDate), className: 'hidden xl:table-cell whitespace-nowrap' },
    { key: 'actions', header: <span className="sr-only">Actions</span>, render: (l) => <StageMenu lead={l} onMove={actions.move} compact disabled={actions.busyId === l.id} /> },
  ]

  return (
    <>
      <PageHeader
        title="Leads & Opportunities"
        description={isOfficer ? 'Your sales pipeline' : user.branch ? `${user.branch.name} pipeline` : 'Pipeline across all branches'}
        breadcrumbs={[{ label: 'Home', to: '/' }, { label: 'Leads & Opportunities' }]}
        actions={
          <>
            <div className="inline-flex rounded-lg border border-line bg-white p-0.5" role="group" aria-label="View">
              <button type="button" onClick={() => setParam('view', '')} aria-pressed={view === 'board'}
                className={`inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm ${view === 'board' ? 'bg-navy-900 text-white' : 'text-ink-muted hover:text-ink'}`}>
                <Columns3 className="h-4 w-4" aria-hidden="true" /> Board
              </button>
              <button type="button" onClick={() => setParam('view', 'table')} aria-pressed={view === 'table'}
                className={`inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm ${view === 'table' ? 'bg-navy-900 text-white' : 'text-ink-muted hover:text-ink'}`}>
                <Table2 className="h-4 w-4" aria-hidden="true" /> Table
              </button>
            </div>
            {user.role !== 'ADMIN' && <Button as={Link} to="/leads/new" icon={Plus}>New lead</Button>}
          </>
        }
      />

      <dl className="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
        {stats.map((st) => (
          <Card key={st.label} className="p-4">
            <dt className="text-xs text-ink-muted" title={st.hint}>{st.label}</dt>
            <dd className="mt-1 text-xl font-semibold text-ink">{st.value}</dd>
          </Card>
        ))}
      </dl>

      {isManager && <PendingApprovals onDecided={refresh} />}

      <Card className="mb-4 flex flex-col gap-3 p-4 xl:flex-row xl:items-end xl:justify-between">
        <SearchBar value={q} onChange={setQ} placeholder="Customer, product or LD-code" label="Search leads" />
        <div className="grid grid-cols-2 gap-3 sm:flex sm:flex-wrap sm:items-end">
          {view === 'table' && <SelectFilter label="Stage" value={get('status')} onChange={(v) => setParam('status', v)} options={options(LEAD_STATUSES)} />}
          <SelectFilter label="Product" value={get('productId')} onChange={(v) => setParam('productId', v)}
            options={(products.data ?? []).map((p) => ({ value: String(p.id), label: p.name }))} />
          {!isOfficer && <SelectFilter label="Officer" value={get('officerId')} onChange={(v) => setParam('officerId', v)}
            options={(staff.data ?? []).map((s) => ({ value: String(s.id), label: s.fullName }))} />}
          <label className="flex flex-col gap-1 text-xs font-medium text-ink-muted">Created from
            <input type="date" value={get('from')} max={get('to') || undefined} onChange={(e) => setParam('from', e.target.value)} className={DATE_INPUT} />
          </label>
          <label className="flex flex-col gap-1 text-xs font-medium text-ink-muted">Created to
            <input type="date" value={get('to')} min={get('from') || undefined} onChange={(e) => setParam('to', e.target.value)} className={DATE_INPUT} />
          </label>
        </div>
      </Card>

      {data.status === 'loading' && <Card><LoadingState rows={6} label="Loading leads…" /></Card>}
      {data.status === 'error' && <Card><ErrorState message={data.error} onRetry={data.reload} /></Card>}
      {data.status === 'ok' && view === 'board' && <LeadBoard board={data.data} onMove={actions.move} busyId={actions.busyId} />}
      {data.status === 'ok' && view === 'table' && (
        <Card className="overflow-hidden">
          {data.data.content.length === 0 ? (
            <EmptyState icon={Workflow} title="No leads found" description="Try clearing filters or create a new lead." />
          ) : (
            <>
              <DataTable caption="Leads" columns={columns} rows={data.data.content} sort={tableParams.sort}
                onSortChange={(v) => setParam('sort', v)} onRowClick={(l) => navigate(`/leads/${l.id}`)} />
              <Pagination page={data.data.page} size={data.data.size} totalPages={data.data.totalPages}
                totalElements={data.data.totalElements} onPageChange={(p) => setParam('page', p > 0 ? String(p) : '')} />
            </>
          )}
        </Card>
      )}

      <LeadDialogHost dialog={actions.dialog} onClose={actions.closeDialog} onDone={(lead, msg) => { actions.done(lead, msg) }} />
    </>
  )
}
