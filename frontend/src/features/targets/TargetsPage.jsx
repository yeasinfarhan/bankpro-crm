import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { Pencil, Plus, Trash2, Trophy } from 'lucide-react'
import PageHeader from '../../components/ui/PageHeader'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import SelectFilter from '../../components/ui/SelectFilter'
import LoadingState from '../../components/ui/LoadingState'
import ErrorState from '../../components/ui/ErrorState'
import EmptyState from '../../components/ui/EmptyState'
import ConfirmDialog from '../../components/ui/ConfirmDialog'
import TargetProgress from './TargetProgress'
import TargetFormDialog from './TargetFormDialog'
import useApi from '../../hooks/useApi'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../../components/feedback/ToastContext'
import { deleteTarget, fetchTargetRanking, fetchTargets } from '../../api/metricsApi'
import { fetchBranches } from '../../api/branchesApi'
import { fetchAssignableStaff } from '../../api/usersApi'
import { getErrorMessage } from '../../api/client'
import { formatPercent, TARGET_CATEGORIES, targetOwner } from '../../utils/metricFormat'
import { bstToday, monthLabel } from '../../utils/period'
import { ROLES } from '../../utils/roles'

function monthBounds(ym) {
  const [y, m] = ym.split('-').map(Number)
  const last = new Date(Date.UTC(y, m, 0)).getUTCDate()
  return { from: `${ym}-01`, to: `${ym}-${String(last).padStart(2, '0')}` }
}

/**
 * Sales targets for a month (quarterly targets covering that month are included). Actuals are live.
 * Branch managers create, edit and delete their branch's targets; everyone else views (the API enforces this).
 */
export default function TargetsPage() {
  const { user } = useAuth()
  const { notify } = useToast()
  const [params, setParams] = useSearchParams()
  const month = params.get('month') ?? bstToday().slice(0, 7)
  const branchId = params.get('branchId') ?? ''
  const userId = params.get('userId') ?? ''
  const setParam = (k, v) => { const n = new URLSearchParams(params); if (v) n.set(k, v); else n.delete(k); setParams(n, { replace: true }) }

  const isAdmin = user.role === ROLES.ADMIN
  const isManager = user.role === ROLES.BRANCH_MANAGER
  const isOfficer = user.role === ROLES.RELATIONSHIP_OFFICER
  const { from, to } = monthBounds(month)

  const list = useApi(() => fetchTargets({ from, to, branchId: isAdmin ? branchId : undefined, userId }), [from, to, branchId, userId])
  const ranking = useApi(() => (isOfficer ? Promise.resolve([]) : fetchTargetRanking({ from, to, branchId: isAdmin ? branchId : undefined })), [from, to, branchId, isOfficer])
  const branches = useApi(() => (isAdmin ? fetchBranches() : Promise.resolve([])), [isAdmin])
  const staff = useApi(() => (isOfficer ? Promise.resolve([]) : fetchAssignableStaff(isAdmin ? branchId || undefined : undefined)), [isOfficer, branchId])

  const [dialog, setDialog] = useState(null) // { target? }
  const [toDelete, setToDelete] = useState(null)
  const [deleting, setDeleting] = useState(false)

  const reloadAll = () => { list.reload(); ranking.reload() }

  const onSaved = (saved, editing) => {
    setDialog(null)
    notify(editing ? 'Target updated' : `Target created for ${targetOwner(saved)}`, 'success')
    reloadAll()
  }

  const confirmDelete = async () => {
    setDeleting(true)
    try {
      await deleteTarget(toDelete.id)
      notify('Target deleted', 'success')
      setToDelete(null)
      reloadAll()
    } catch (e) {
      notify(getErrorMessage(e), 'error')
    } finally {
      setDeleting(false)
    }
  }

  // Group: branch-wide targets first, then one group per officer.
  const groups = []
  for (const t of list.data ?? []) {
    const key = t.user ? `u${t.user.id}` : `b${t.branch.id}`
    let g = groups.find((x) => x.key === key)
    if (!g) groups.push((g = { key, title: targetOwner(t), branchWide: !t.user, items: [] }))
    g.items.push(t)
  }
  groups.sort((a, b) => (a.branchWide === b.branchWide ? a.title.localeCompare(b.title) : a.branchWide ? -1 : 1))

  return (
    <>
      <PageHeader title="Sales targets" description={`${monthLabel(month)} · actuals are calculated live from completed sales, customers and leads`}
        actions={isManager && <Button icon={Plus} onClick={() => setDialog({})}>New target</Button>} />

      <div className="mb-5 flex flex-wrap items-end gap-3" role="group" aria-label="Target filters">
        <label className="flex flex-col gap-1 text-xs font-medium text-ink-muted">Month
          <input type="month" value={month} onChange={(e) => e.target.value && setParam('month', e.target.value)}
            className="rounded-lg border border-line bg-white px-2.5 py-[7px] text-sm text-ink shadow-sm focus:border-royal-500 focus:outline-none focus:ring-2 focus:ring-royal-100" />
        </label>
        {isAdmin && (
          <SelectFilter label="Branch" value={branchId} onChange={(v) => { const n = new URLSearchParams(params); n.delete('userId'); if (v) n.set('branchId', v); else n.delete('branchId'); setParams(n, { replace: true }) }}
            allLabel="All branches" options={(branches.data ?? []).map((b) => ({ value: String(b.id), label: b.name }))} />
        )}
        {!isOfficer && (
          <SelectFilter label="Officer" value={userId} onChange={(v) => setParam('userId', v)} allLabel="Everyone"
            options={(staff.data ?? []).map((u) => ({ value: String(u.id), label: u.fullName }))} />
        )}
      </div>

      <div className={`grid gap-5 ${isOfficer ? '' : 'lg:grid-cols-3'}`}>
        <div className="space-y-5 lg:col-span-2">
          {list.status === 'loading' && <Card><LoadingState rows={5} /></Card>}
          {list.status === 'error' && <Card><ErrorState message={list.error} onRetry={list.reload} /></Card>}
          {list.status === 'ok' && groups.length === 0 && (
            <Card><EmptyState title="No targets for this month" description={isManager ? 'Create a target for your branch or one of your officers.' : 'Targets are set by the branch manager.'} /></Card>
          )}
          {groups.map((g) => (
            <Card key={g.key} className="px-5 py-2">
              <h2 className="pt-2 text-sm font-semibold text-ink">{g.title}</h2>
              <div className="divide-y divide-line">
                {g.items.map((t) => (
                  <TargetProgress key={t.id} target={t} showOwner={false} actions={t.canEdit && (
                    <span className="ml-2 flex items-center gap-0.5">
                      <button type="button" onClick={() => setDialog({ target: t })} aria-label={`Edit ${TARGET_CATEGORIES[t.category]} target for ${g.title}`}
                        className="rounded p-1 text-ink-muted hover:bg-gray-100 hover:text-ink"><Pencil className="h-3.5 w-3.5" /></button>
                      <button type="button" onClick={() => setToDelete(t)} aria-label={`Delete ${TARGET_CATEGORIES[t.category]} target for ${g.title}`}
                        className="rounded p-1 text-ink-muted hover:bg-danger-50 hover:text-danger-600"><Trash2 className="h-3.5 w-3.5" /></button>
                    </span>
                  )} />
                ))}
              </div>
            </Card>
          ))}
        </div>

        {!isOfficer && (
          <Card className="h-fit">
            <div className="flex items-center gap-2 border-b border-line px-5 py-4">
              <Trophy className="h-4 w-4 text-ink-muted" aria-hidden="true" />
              <div>
                <h2 className="text-sm font-semibold text-ink">Officer ranking</h2>
                <p className="text-xs text-ink-muted">Average achievement across each officer’s targets.</p>
              </div>
            </div>
            {ranking.status === 'loading' && <LoadingState rows={3} />}
            {ranking.status === 'error' && <ErrorState message={ranking.error} onRetry={ranking.reload} />}
            {ranking.status === 'ok' && (ranking.data.length === 0
              ? <p className="px-5 py-8 text-center text-sm text-ink-muted">No officer targets this month.</p>
              : (
                <ol className="divide-y divide-line">
                  {ranking.data.map((r) => (
                    <li key={r.officer.id} className="flex items-center gap-3 px-5 py-3">
                      <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-canvas text-xs font-semibold text-ink">{r.rank}</span>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium text-ink">{r.officer.fullName}</p>
                        <p className="text-xs text-ink-muted">{r.targetsMet} of {r.targets} target{r.targets === 1 ? '' : 's'} met</p>
                      </div>
                      <span className="text-sm font-semibold tabular-nums text-ink">{formatPercent(r.averageAchievementPct)}</span>
                    </li>
                  ))}
                </ol>
              ))}
          </Card>
        )}
      </div>

      {dialog && (
        <TargetFormDialog open target={dialog.target} staff={(staff.data ?? []).filter((u) => u.role === ROLES.RELATIONSHIP_OFFICER)}
          defaultMonth={month} onClose={() => setDialog(null)} onSaved={onSaved} />
      )}
      <ConfirmDialog open={Boolean(toDelete)} title="Delete target?" confirmLabel="Delete" loading={deleting}
        message={toDelete ? `${TARGET_CATEGORIES[toDelete.category]} target for ${targetOwner(toDelete)} will be removed. Sales records are not affected.` : ''}
        onConfirm={confirmDelete} onCancel={() => setToDelete(null)} />
    </>
  )
}
