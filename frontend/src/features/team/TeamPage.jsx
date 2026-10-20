import { useEffect, useState } from 'react'
import { Users } from 'lucide-react'
import PageHeader from '../../components/ui/PageHeader'
import Card from '../../components/ui/Card'
import DataTable from '../../components/tables/DataTable'
import StatusBadge from '../../components/ui/StatusBadge'
import LoadingState from '../../components/ui/LoadingState'
import ErrorState from '../../components/ui/ErrorState'
import EmptyState from '../../components/ui/EmptyState'
import { fetchTeam } from '../../api/usersApi'
import { getErrorMessage } from '../../api/client'
import { ROLE_LABELS } from '../../utils/roles'
import { formatDateTime } from '../../utils/format'
import { useAuth } from '../auth/AuthContext'

/** Team list. The backend decides who appears: all branches for ADMIN, own branch for managers. */
const COLUMNS = [
  { key: 'name', header: 'Name', render: (m) => (<><div className="font-medium text-ink">{m.fullName}</div><div className="text-xs text-ink-muted">{m.email}</div></>) },
  { key: 'role', header: 'Role', render: (m) => (<><div>{ROLE_LABELS[m.role]}</div>{m.jobTitle && m.jobTitle !== ROLE_LABELS[m.role] && <div className="text-xs text-ink-muted">{m.jobTitle}</div>}</>) },
  { key: 'branch', header: 'Branch', render: (m) => m.branch?.name ?? 'All branches' },
  { key: 'status', header: 'Status', render: (m) => <StatusBadge tone={m.active ? 'success' : 'neutral'}>{m.active ? 'Active' : 'Inactive'}</StatusBadge> },
  { key: 'lastLoginAt', header: 'Last sign-in (BST)', render: (m) => <span className="text-ink-muted">{formatDateTime(m.lastLoginAt)}</span> },
]

export default function TeamPage() {
  const { user } = useAuth()
  const [state, setState] = useState({ status: 'loading', rows: [], error: null })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let active = true
    fetchTeam().then(
      (rows) => active && setState({ status: 'ok', rows, error: null }),
      (error) => active && setState({ status: 'error', rows: [], error: getErrorMessage(error) }),
    )
    return () => {
      active = false
    }
  }, [attempt])

  const retry = () => {
    setState({ status: 'loading', rows: [], error: null })
    setAttempt((n) => n + 1)
  }

  return (
    <>
      <PageHeader
        title="Team Management"
        description={user.branch ? `Employees at ${user.branch.name}` : 'Employees across all branches'}
        breadcrumbs={[{ label: 'Home', to: '/' }, { label: 'Team Management' }]}
      />
      <Card className="overflow-hidden">
        {state.status === 'loading' && <LoadingState label="Loading team…" />}
        {state.status === 'error' && <ErrorState message={state.error} onRetry={retry} />}
        {state.status === 'ok' && state.rows.length === 0 && (
          <EmptyState icon={Users} title="No team members" description="No employees are assigned to this branch yet." />
        )}
        {state.status === 'ok' && state.rows.length > 0 && (
          <DataTable columns={COLUMNS} rows={state.rows} caption="Team members" />
        )}
      </Card>
    </>
  )
}
