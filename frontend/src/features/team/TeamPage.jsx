import { useEffect, useState } from 'react'
import { Users } from 'lucide-react'
import PageHeader from '../../components/ui/PageHeader'
import Card from '../../components/ui/Card'
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
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-line text-sm">
              <thead className="bg-canvas text-left text-xs font-semibold uppercase tracking-wide text-ink-muted">
                <tr>
                  <th scope="col" className="px-4 py-3">Name</th>
                  <th scope="col" className="px-4 py-3">Role</th>
                  <th scope="col" className="px-4 py-3">Branch</th>
                  <th scope="col" className="px-4 py-3">Status</th>
                  <th scope="col" className="px-4 py-3">Last sign-in (BST)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {state.rows.map((m) => (
                  <tr key={m.id} className="hover:bg-canvas/60">
                    <td className="px-4 py-3">
                      <div className="font-medium text-ink">{m.fullName}</div>
                      <div className="text-xs text-ink-muted">{m.email}</div>
                    </td>
                    <td className="px-4 py-3">
                      <div>{ROLE_LABELS[m.role]}</div>
                      {m.jobTitle && <div className="text-xs text-ink-muted">{m.jobTitle}</div>}
                    </td>
                    <td className="px-4 py-3">{m.branch?.name ?? 'All branches'}</td>
                    <td className="px-4 py-3">
                      <StatusBadge tone={m.active ? 'success' : 'neutral'}>{m.active ? 'Active' : 'Inactive'}</StatusBadge>
                    </td>
                    <td className="px-4 py-3 text-ink-muted">{formatDateTime(m.lastLoginAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  )
}
