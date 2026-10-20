import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { Download, Plus, Users } from 'lucide-react'
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
import useApi from '../../hooks/useApi'
import useSearchText from '../../hooks/useSearchText'
import { downloadCustomersCsv, fetchCustomers } from '../../api/customersApi'
import { fetchAssignableStaff } from '../../api/usersApi'
import { getErrorMessage } from '../../api/client'
import { useToast } from '../../components/feedback/ToastContext'
import { CUSTOMER_STATUSES, CUSTOMER_STATUS_TONE, options, SEGMENTS } from '../../utils/labels'
import { formatDate, formatPhone } from '../../utils/format'
import { useAuth } from '../auth/AuthContext'

const PAGE_SIZE = 20
const FILTER_KEYS = ['q', 'segment', 'status', 'officerId', 'includeArchived', 'sort', 'page']

/**
 * Customer list. Filters live in the URL (?segment=SME&page=2), so a refresh or a shared link
 * shows the same view. Searching, filtering, sorting and paging all happen on the server.
 */
export default function CustomersPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const { notify } = useToast()
  const [params, setParams] = useSearchParams()
  const [exporting, setExporting] = useState(false)
  const [q, setQ] = useSearchText(params, (text) => {
    const next = new URLSearchParams(params)
    if (text) next.set('q', text)
    else next.delete('q')
    next.delete('page')
    setParams(next, { replace: true })
  })

  const filters = Object.fromEntries(FILTER_KEYS.map((k) => [k, params.get(k) ?? '']))
  const page = Number(filters.page || 0)
  const canCreate = user.role === 'BRANCH_MANAGER' || user.role === 'RELATIONSHIP_OFFICER'
  const canSeeArchived = user.role !== 'RELATIONSHIP_OFFICER'

  const setFilter = (key, value) => {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value)
    else next.delete(key)
    if (key !== 'page') next.delete('page') // any filter change goes back to page 1
    setParams(next, { replace: true })
  }


  const apiParams = {
    q: filters.q,
    segment: filters.segment,
    status: filters.status,
    officerId: filters.officerId,
    includeArchived: filters.includeArchived === 'true',
    sort: filters.sort || 'createdAt,desc',
    page,
    size: PAGE_SIZE,
  }
  const key = JSON.stringify(apiParams)
  const list = useApi(() => fetchCustomers(apiParams), [key])
  const staff = useApi(() => fetchAssignableStaff(), [])

  const exportCsv = async () => {
    setExporting(true)
    try {
      await downloadCustomersCsv({ ...apiParams, page: undefined, size: undefined, sort: undefined })
      notify('Customer export downloaded')
    } catch (error) {
      notify(getErrorMessage(error, 'Export failed.'), 'error')
    } finally {
      setExporting(false)
    }
  }

  const columns = [
    {
      key: 'fullName',
      header: 'Customer',
      sortKey: 'fullName',
      render: (c) => (
        <div>
          <Link to={`/customers/${c.id}`} onClick={(e) => e.stopPropagation()} className="font-medium text-ink hover:text-royal-600">{c.fullName}</Link>
          <div className="text-xs text-ink-muted">{c.code}</div>
        </div>
      ),
    },
    {
      key: 'contact',
      header: 'Contact',
      render: (c) => (
        <div className="text-sm">
          <div>{formatPhone(c.phone)}</div>
          <div className="text-xs text-ink-muted">{c.email ?? '—'}</div>
        </div>
      ),
    },
    { key: 'occupation', header: 'Occupation', render: (c) => c.occupation ?? '—', className: 'hidden lg:table-cell' },
    { key: 'segment', header: 'Segment', sortKey: 'segment', render: (c) => SEGMENTS[c.segment] },
    { key: 'status', header: 'Status', sortKey: 'status', render: (c) => <StatusBadge tone={CUSTOMER_STATUS_TONE[c.status]}>{CUSTOMER_STATUSES[c.status]}</StatusBadge> },
    { key: 'officer', header: 'Relationship officer', render: (c) => c.assignedOfficer?.fullName ?? <span className="text-ink-muted">Unassigned</span>, className: 'hidden md:table-cell' },
    { key: 'createdAt', header: 'Added', sortKey: 'createdAt', render: (c) => formatDate(c.createdAt), className: 'hidden xl:table-cell whitespace-nowrap' },
  ]

  const staffOptions = (staff.data ?? []).map((s) => ({ value: String(s.id), label: s.fullName }))

  return (
    <>
      <PageHeader
        title="Customers"
        description={user.branch ? `${user.branch.name} customers` : 'Customers across all branches'}
        breadcrumbs={[{ label: 'Home', to: '/' }, { label: 'Customers' }]}
        actions={
          <>
            <Button variant="secondary" icon={Download} loading={exporting} onClick={exportCsv}>Export CSV</Button>
            {canCreate && <Button as={Link} to="/customers/new" icon={Plus}>New customer</Button>}
          </>
        }
      />

      <Card className="overflow-hidden">
        <div className="flex flex-col gap-3 border-b border-line p-4 lg:flex-row lg:items-end lg:justify-between">
          <SearchBar value={q} onChange={setQ} placeholder="Name, email, phone or CUS-code" label="Search customers" />
          <div className="grid grid-cols-2 gap-3 sm:flex sm:flex-wrap sm:items-end">
            <SelectFilter label="Segment" value={filters.segment} onChange={(v) => setFilter('segment', v)} options={options(SEGMENTS)} />
            <SelectFilter label="Status" value={filters.status} onChange={(v) => setFilter('status', v)}
              options={options(CUSTOMER_STATUSES).filter((o) => canSeeArchived || o.value !== 'ARCHIVED')} />
            <SelectFilter label="Officer" value={filters.officerId} onChange={(v) => setFilter('officerId', v)} options={staffOptions} />
            {canSeeArchived && (
              <label className="col-span-2 flex items-center gap-2 pb-2 text-sm text-ink">
                <input type="checkbox" className="h-4 w-4 rounded border-line" checked={filters.includeArchived === 'true'}
                  onChange={(e) => setFilter('includeArchived', e.target.checked ? 'true' : '')} />
                Include archived
              </label>
            )}
          </div>
        </div>

        {list.status === 'loading' && <LoadingState rows={6} label="Loading customers…" />}
        {list.status === 'error' && <ErrorState message={list.error} onRetry={list.reload} />}
        {list.status === 'ok' && list.data.content.length === 0 && (
          <EmptyState icon={Users} title="No customers found"
            description={filters.q || filters.segment || filters.status || filters.officerId ? 'Try clearing some filters.' : 'Customers you add will appear here.'}
            action={canCreate && <Button as={Link} to="/customers/new" icon={Plus}>New customer</Button>} />
        )}
        {list.status === 'ok' && list.data.content.length > 0 && (
          <>
            <DataTable
              caption="Customers"
              columns={columns}
              rows={list.data.content}
              sort={apiParams.sort}
              onSortChange={(s) => setFilter('sort', s)}
              onRowClick={(c) => navigate(`/customers/${c.id}`)}
            />
            <Pagination page={list.data.page} size={list.data.size} totalPages={list.data.totalPages}
              totalElements={list.data.totalElements} onPageChange={(p) => setFilter('page', p > 0 ? String(p) : '')} />
          </>
        )}
      </Card>
    </>
  )
}
