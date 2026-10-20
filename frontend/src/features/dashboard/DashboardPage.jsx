import { AlarmClock, Banknote, CheckCircle2, Landmark, Target, TrendingUp, UserPlus, Users } from 'lucide-react'
import { Link } from 'react-router'
import PageHeader from '../../components/ui/PageHeader'
import Card from '../../components/ui/Card'
import DateRangeFilter from '../../components/ui/DateRangeFilter'
import SelectFilter from '../../components/ui/SelectFilter'
import ChartCard from '../../components/charts/ChartCard'
import BarChartView from '../../components/charts/BarChartView'
import LineChartView from '../../components/charts/LineChartView'
import { compactBdt, NEUTRAL } from '../../components/charts/chartTheme'
import DataTable from '../../components/tables/DataTable'
import LoadingState from '../../components/ui/LoadingState'
import ErrorState from '../../components/ui/ErrorState'
import KpiTile from './KpiTile'
import useMetricFilters from './useMetricFilters'
import TargetProgress from '../targets/TargetProgress'
import useApi from '../../hooks/useApi'
import { useAuth } from '../auth/AuthContext'
import { fetchBranches } from '../../api/branchesApi'
import {
  fetchCustomerAcquisition, fetchDashboardSummary, fetchDashboardTargets, fetchLeadFunnel,
  fetchOfficerPerformance, fetchProductMix, fetchSalesTrend,
} from '../../api/metricsApi'
import { formatBdt, formatDate } from '../../utils/format'
import { formatMetric, formatPercent } from '../../utils/metricFormat'
import { LEAD_STATUSES } from '../../utils/labels'
import { monthLabel } from '../../utils/period'
import { ROLE_LABELS, ROLES } from '../../utils/roles'

const num = (v) => formatMetric(v, 'number')

/**
 * Sales dashboard. Every number is computed by the API from database records for the chosen period
 * and the caller's data scope (officer: own records, manager: own branch, admin: all or one branch).
 */
export default function DashboardPage() {
  const { user } = useAuth()
  const { range, branchId, setRange, setBranchId } = useMetricFilters('last6')
  const isAdmin = user.role === ROLES.ADMIN
  const isManagerView = user.role !== ROLES.RELATIONSHIP_OFFICER
  const q = { from: range.from, to: range.to, branchId: isAdmin ? branchId : undefined }
  const deps = [q.from, q.to, q.branchId]

  const branches = useApi(() => (isAdmin ? fetchBranches() : Promise.resolve([])), [isAdmin])
  const summary = useApi(() => fetchDashboardSummary(q), deps)
  const trend = useApi(() => fetchSalesTrend(q), deps)
  const acquisition = useApi(() => fetchCustomerAcquisition(q), deps)
  const funnel = useApi(() => fetchLeadFunnel(q), deps)
  const mix = useApi(() => fetchProductMix(q), deps)
  const targets = useApi(() => fetchDashboardTargets(q), deps)
  const officers = useApi(() => (isManagerView ? fetchOfficerPerformance(q) : Promise.resolve([])), [...deps, isManagerView])

  const s = summary.data
  const scopeText = user.role === ROLES.RELATIONSHIP_OFFICER ? 'Your customers, leads and sales'
    : isAdmin ? (branchId ? branches.data?.find((b) => String(b.id) === branchId)?.name ?? 'Selected branch' : 'All branches')
      : user.branch?.name ?? ''
  const periodText = `${formatDate(range.from)} – ${formatDate(range.to)}`

  const mixRows = (mix.data ?? []).filter((r) => Number(r.completedValue) > 0)
    .sort((a, b) => b.completedValue - a.completedValue).slice(0, 8)
  const targetRows = [...(targets.data ?? [])].sort((a, b) => b.periodStart.localeCompare(a.periodStart)).slice(0, 6)

  return (
    <>
      <PageHeader
        title={`Welcome, ${user.fullName.split(' ')[0]}`}
        description={`${ROLE_LABELS[user.role]} · ${scopeText} · ${periodText}`}
      />

      <div className="mb-5 flex flex-wrap items-end gap-3" role="group" aria-label="Dashboard filters">
        <DateRangeFilter value={range} onChange={setRange} />
        {isAdmin && (
          <SelectFilter label="Branch" value={branchId} onChange={setBranchId} allLabel="All branches"
            options={(branches.data ?? []).map((b) => ({ value: String(b.id), label: b.name }))} />
        )}
      </div>

      {summary.status === 'error' ? (
        <Card className="mb-5"><ErrorState message={summary.error} onRetry={summary.reload} /></Card>
      ) : (
        <section aria-label="Key figures" className="mb-5 grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
          <KpiTile loading={!s} icon={Users} label="Total customers" value={s && num(s.totalCustomers)}
            definition="Non-archived customers in your scope right now." />
          <KpiTile loading={!s} icon={UserPlus} label="New this month" value={s && num(s.newCustomersThisMonth)}
            definition="Customers created in the current calendar month (Bangladesh time)." />
          <KpiTile loading={!s} icon={TrendingUp} label="Active leads" value={s && num(s.activeLeads)}
            definition="Leads currently in New, Contacted, Qualified or Proposal." />
          <KpiTile loading={!s} icon={CheckCircle2} label="Leads converted" value={s && num(s.leadsConverted)}
            definition="Leads converted in the period (declined applications excluded)." />
          <KpiTile loading={!s} icon={Banknote} label="Completed sales" value={s && formatBdt(s.totalSalesValue)}
            definition="Value of completed simulated sales in the period. Loans and cards count once approved." />
          <KpiTile loading={!s} icon={Landmark} label="Deposit sales" value={s && formatBdt(s.depositSalesValue)}
            definition="Deposit-product part of completed sales (sales volume, not balances)." />
          <KpiTile loading={!s} icon={Target} label="Target achievement" value={s && formatPercent(s.targetAchievementPct)}
            definition={s?.targetsCounted ? `Average of ${s.targetsCounted} target${s.targetsCounted === 1 ? '' : 's'} overlapping the period.` : 'No targets set for this period.'} />
          <KpiTile loading={!s} icon={AlarmClock} label="Overdue follow-ups" value={s && num(s.overdueFollowUps)}
            tone={s?.overdueFollowUps > 0 ? 'warning' : 'default'} definition="Scheduled activities whose due time has passed." />
        </section>
      )}

      <div className="grid gap-5 lg:grid-cols-2">
        <ChartCard title="Completed sales by month" description="Value of completed simulated sales (BDT), by completion month."
          state={trend} isEmpty={!trend.data?.some((r) => Number(r.completedValue) > 0)}
          table={{ columns: [
            { key: 'month', label: 'Month', format: monthLabel },
            { key: 'completedValue', label: 'Completed value', format: formatBdt },
            { key: 'completedCount', label: 'Sales', format: num },
            { key: 'depositValue', label: 'Deposit value', format: formatBdt }], rows: trend.data ?? [] }}>
          <BarChartView data={trend.data ?? []} xKey="month" labelFormat={monthLabel} valueFormat={formatBdt} axisFormat={compactBdt}
            series={[{ key: 'completedValue', label: 'Completed value' }]} />
        </ChartCard>

        <ChartCard title="New customers by month" description="Customers created each month."
          state={acquisition} isEmpty={!acquisition.data?.some((r) => r.newCustomers > 0)}
          table={{ columns: [
            { key: 'month', label: 'Month', format: monthLabel },
            { key: 'newCustomers', label: 'New customers', format: num },
            { key: 'MASS', label: 'Mass', format: num },
            { key: 'EMERGING_AFFLUENT', label: 'Emerging affluent', format: num },
            { key: 'AFFLUENT', label: 'Affluent', format: num },
            { key: 'SME', label: 'SME', format: num }], rows: acquisition.data ?? [] }}>
          <LineChartView data={acquisition.data ?? []} xKey="month" labelFormat={monthLabel} valueFormat={num}
            series={[{ key: 'newCustomers', label: 'New customers' }]} />
        </ChartCard>

        <ChartCard title="Lead pipeline" description="Leads created in the period, by their current stage."
          state={funnel} isEmpty={!funnel.data?.some((r) => r.leads > 0)}
          table={{ columns: [
            { key: 'stage', label: 'Stage', format: (v) => LEAD_STATUSES[v] },
            { key: 'leads', label: 'Leads', format: num },
            { key: 'estimatedValue', label: 'Estimated value', format: formatBdt }], rows: funnel.data ?? [] }}>
          <BarChartView data={funnel.data ?? []} xKey="stage" layout="vertical" labelFormat={(v) => LEAD_STATUSES[v] ?? v}
            valueFormat={num} series={[{ key: 'leads', label: 'Leads' }]} colorFor={(r) => (r.stage === 'LOST' ? NEUTRAL : null)} />
        </ChartCard>

        <ChartCard title="Product mix" description="Completed sales value by product (top 8)."
          state={mix} isEmpty={mixRows.length === 0} emptyText="No completed sales in this period."
          table={{ columns: [
            { key: 'product', label: 'Product' },
            { key: 'completedCount', label: 'Completed', format: num },
            { key: 'completedValue', label: 'Value', format: formatBdt },
            { key: 'pendingCount', label: 'Pending', format: num },
            { key: 'declinedCount', label: 'Declined', format: num }], rows: mix.data ?? [] }}>
          <BarChartView data={mixRows} xKey="product" layout="vertical" valueFormat={formatBdt} axisFormat={compactBdt}
            series={[{ key: 'completedValue', label: 'Completed value' }]} height={Math.max(180, mixRows.length * 36)} />
        </ChartCard>

        <ChartCard title="Targets" description="Actual vs target for targets overlapping the period."
          state={targets} isEmpty={targetRows.length === 0} emptyText="No targets set for this period." className="lg:col-span-2">
          <div className="divide-y divide-line">
            {targetRows.map((t) => <TargetProgress key={t.id} target={t} showOwner={user.role !== ROLES.RELATIONSHIP_OFFICER} />)}
          </div>
          <Link to="/targets" className="mt-2 inline-block text-sm font-medium text-royal-600 hover:underline">View all targets</Link>
        </ChartCard>
      </div>

      {isManagerView && <OfficerPerformance state={officers} />}
    </>
  )
}

function OfficerPerformance({ state }) {
  const columns = [
    { key: 'officer', header: 'Officer', render: (r) => <span className="font-medium text-ink">{r.officer}</span> },
    { key: 'branch', header: 'Branch' },
    { key: 'customersAdded', header: 'Customers added', render: (r) => num(r.customersAdded), className: 'text-right' },
    { key: 'leadsCreated', header: 'Leads', render: (r) => num(r.leadsCreated), className: 'text-right' },
    { key: 'conversions', header: 'Conversions', render: (r) => num(r.conversions), className: 'text-right' },
    { key: 'completedSalesValue', header: 'Completed sales', render: (r) => formatBdt(r.completedSalesValue), className: 'text-right' },
    { key: 'followUpsCompleted', header: 'Follow-ups done', render: (r) => `${num(r.followUpsCompleted)} / ${num(r.followUpsScheduled)}`, className: 'text-right' },
    { key: 'overdueNow', header: 'Overdue', render: (r) => num(r.overdueNow), className: 'text-right' },
    { key: 'targetAchievementPct', header: 'Target avg', render: (r) => formatPercent(r.targetAchievementPct), className: 'text-right' },
  ]
  return (
    <Card className="mt-5">
      <div className="border-b border-line px-5 py-4">
        <h2 className="text-sm font-semibold text-ink">Officer performance</h2>
        <p className="mt-0.5 text-xs text-ink-muted">Activity in the period per relationship officer. “Follow-ups done” = completed / due in the period.</p>
      </div>
      {state.status === 'loading' && <LoadingState rows={4} />}
      {state.status === 'error' && <ErrorState message={state.error} onRetry={state.reload} />}
      {state.status === 'ok' && (state.data.length === 0
        ? <p className="py-10 text-center text-sm text-ink-muted">No officers in this scope.</p>
        : <DataTable columns={columns} rows={state.data} rowKey="officerId" caption="Officer performance" />)}
    </Card>
  )
}
