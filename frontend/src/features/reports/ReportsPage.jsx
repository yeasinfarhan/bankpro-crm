import { useState } from 'react'
import { Download } from 'lucide-react'
import PageHeader from '../../components/ui/PageHeader'
import Card from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import SelectFilter from '../../components/ui/SelectFilter'
import DateRangeFilter from '../../components/ui/DateRangeFilter'
import LoadingState from '../../components/ui/LoadingState'
import ErrorState from '../../components/ui/ErrorState'
import Pagination from '../../components/ui/Pagination'
import ChartCard from '../../components/charts/ChartCard'
import BarChartView from '../../components/charts/BarChartView'
import { compactBdt, NEUTRAL } from '../../components/charts/chartTheme'
import useMetricFilters from '../dashboard/useMetricFilters'
import useApi from '../../hooks/useApi'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../../components/feedback/ToastContext'
import { downloadReportCsv, fetchReport } from '../../api/metricsApi'
import { fetchBranches } from '../../api/branchesApi'
import { fetchAssignableStaff } from '../../api/usersApi'
import { getErrorMessage } from '../../api/client'
import { formatMetric, TARGET_CATEGORIES } from '../../utils/metricFormat'
import { formatDate } from '../../utils/format'
import { ROLES } from '../../utils/roles'

const REPORT_TYPES = {
  'customer-acquisition': 'Customer acquisition',
  'lead-pipeline': 'Lead pipeline',
  conversion: 'Lead conversion',
  'product-sales': 'Product sales',
  'officer-performance': 'Officer performance',
  'target-achievement': 'Target achievement',
  'follow-up-completion': 'Follow-up completion',
}
const PAGE_SIZE = 20
const axisFor = (format) => (format === 'bdt' ? compactBdt : format === 'percent' ? (v) => `${v}%` : undefined)

/** One page renders every report: the API describes its columns, summary figures and chart. */
export default function ReportsPage() {
  const { user } = useAuth()
  const { notify } = useToast()
  const { range, branchId, setRange, params, update } = useMetricFilters('thisMonth')
  const type = REPORT_TYPES[params.get('type')] ? params.get('type') : 'customer-acquisition'
  const officerId = params.get('officerId') ?? ''
  const page = Number(params.get('page') ?? 0)
  const isAdmin = user.role === ROLES.ADMIN
  const isOfficer = user.role === ROLES.RELATIONSHIP_OFFICER
  const [exporting, setExporting] = useState(false)

  const query = { from: range.from, to: range.to, branchId: isAdmin ? branchId : undefined, officerId: isOfficer ? undefined : officerId }
  const report = useApi(() => fetchReport(type, { ...query, page, size: PAGE_SIZE }), [type, query.from, query.to, query.branchId, query.officerId, page])
  const branches = useApi(() => (isAdmin ? fetchBranches() : Promise.resolve([])), [isAdmin])
  const staff = useApi(() => (isOfficer ? Promise.resolve([]) : fetchAssignableStaff(isAdmin ? branchId || undefined : undefined)), [isOfficer, branchId])

  const exportCsv = async () => {
    setExporting(true)
    try {
      await downloadReportCsv(type, query)
      notify('CSV downloaded', 'success')
    } catch (e) {
      notify(getErrorMessage(e), 'error')
    } finally {
      setExporting(false)
    }
  }

  const r = report.data
  return (
    <>
      <PageHeader title="Performance reports" description="Computed from records in your data scope. All figures are fictional demo data."
        actions={<Button variant="secondary" icon={Download} loading={exporting} disabled={report.status !== 'ok' || !r?.totalElements} onClick={exportCsv}>Export CSV</Button>} />

      <div className="mb-5 flex flex-wrap items-end gap-3" role="group" aria-label="Report filters">
        <label className="flex flex-col gap-1 text-xs font-medium text-ink-muted">Report
          <select value={type} onChange={(e) => update({ type: e.target.value, page: null })}
            className="rounded-lg border border-line bg-white px-2.5 py-2 text-sm text-ink shadow-sm focus:border-royal-500 focus:outline-none focus:ring-2 focus:ring-royal-100">
            {Object.entries(REPORT_TYPES).map(([k, l]) => <option key={k} value={k}>{l}</option>)}
          </select>
        </label>
        <DateRangeFilter value={range} onChange={(v) => setRange(v, { page: null })} />
        {isAdmin && (
          <SelectFilter label="Branch" value={branchId} onChange={(v) => update({ branchId: v, officerId: null, page: null })} allLabel="All branches"
            options={(branches.data ?? []).map((b) => ({ value: String(b.id), label: b.name }))} />
        )}
        {!isOfficer && (
          <SelectFilter label="Officer" value={officerId} onChange={(v) => update({ officerId: v, page: null })} allLabel="Everyone"
            options={(staff.data ?? []).map((u) => ({ value: String(u.id), label: u.fullName }))} />
        )}
      </div>

      {report.status === 'loading' && <Card><LoadingState rows={6} label="Loading report…" /></Card>}
      {report.status === 'error' && <Card><ErrorState message={report.error} onRetry={report.reload} /></Card>}
      {report.status === 'ok' && <ReportBody report={r} onPage={(p) => update({ page: p || null })} />}
    </>
  )
}

function chartLabel(row, xKey) {
  if (xKey === 'owner') {
    const owner = String(row.owner).replace(/^Branch: (.*?)( Branch)?$/, '$1 (branch)')
    return `${owner} · ${TARGET_CATEGORIES[row.category] ?? row.category}`
  }
  return row[xKey]
}

function ReportBody({ report: r, onPage }) {
  const xCol = r.columns.find((c) => c.key === r.chart?.xKey)
  const chartRows = r.chart ? r.rows.map((row) => ({ ...row, __x: chartLabel(row, r.chart.xKey) })) : []
  const hasValues = chartRows.some((row) => r.chart.series.some((s) => Number(row[s.key]) > 0))
  const seriesFormat = r.chart?.series[0]?.format
  const horizontal = r.chart && !['month'].includes(xCol?.format)

  return (
    <div className="space-y-5">
      <Card className="p-5">
        <h2 className="text-base font-semibold text-ink">{r.title}</h2>
        <p className="mt-1 text-sm text-ink-muted">{r.description}</p>
        <p className="mt-1 text-xs text-ink-muted">{formatDate(r.from)} – {formatDate(r.to)}</p>
        {r.summary.length > 0 && (
          <dl className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {r.summary.map((m) => (
              <div key={m.label} className="rounded-lg bg-canvas px-4 py-3">
                <dt className="text-xs font-medium text-ink-muted">{m.label}</dt>
                <dd className="mt-1 text-lg font-semibold tabular-nums text-ink">{formatMetric(m.value, m.format)}</dd>
              </div>
            ))}
          </dl>
        )}
      </Card>

      {r.chart && r.rows.length > 0 && (
        <ChartCard title={r.chart.series.map((s) => s.label).join(' and ')} description={r.totalPages > 1 ? 'Rows on this page.' : undefined}
          state={{ status: 'ok' }} isEmpty={!hasValues}>
          <BarChartView data={chartRows} xKey="__x" layout={horizontal ? 'vertical' : 'horizontal'}
            labelFormat={(v) => (xCol && xCol.format !== 'text' ? formatMetric(v, xCol.format) : v)}
            valueFormat={(v) => formatMetric(v, seriesFormat)} axisFormat={axisFor(seriesFormat)}
            series={r.chart.series.map((s) => ({ key: s.key, label: s.label, format: (v) => formatMetric(v, s.format), color: s.key === 'lost' ? NEUTRAL : undefined }))}
            reference={r.type === 'TARGET_ACHIEVEMENT' ? { value: 100, label: 'Target' } : undefined}
            colorFor={r.type === 'LEAD_PIPELINE' ? (row) => (row.stage === 'LOST' ? NEUTRAL : null) : undefined}
            categoryWidth={r.chart.xKey === 'owner' ? 220 : 150}
            height={horizontal ? Math.max(200, chartRows.length * (r.chart.series.length > 1 ? 48 : 40)) : 260} />
        </ChartCard>
      )}

      <Card>
        {r.rows.length === 0 ? (
          <p className="py-12 text-center text-sm text-ink-muted">No records for this period and filter.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-line text-sm">
              <caption className="sr-only">{r.title}</caption>
              <thead className="bg-canvas text-left text-xs font-semibold uppercase tracking-wide text-ink-muted">
                <tr>{r.columns.map((c) => (
                  <th key={c.key} scope="col" className={`whitespace-nowrap px-4 py-3 ${c.format === 'text' || c.format === 'category' || c.format === 'stage' || c.format === 'month' ? '' : 'text-right'}`}>{c.label}</th>
                ))}</tr>
              </thead>
              <tbody className="divide-y divide-line bg-white">
                {r.rows.map((row, i) => (
                  <tr key={i} className="hover:bg-canvas">
                    {r.columns.map((c) => (
                      <td key={c.key} className={`whitespace-nowrap px-4 py-2.5 text-ink ${c.format === 'text' || c.format === 'category' || c.format === 'stage' || c.format === 'month' ? '' : 'text-right tabular-nums'}`}>
                        {formatMetric(row[c.key], c.format, row)}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pagination page={r.page} totalPages={r.totalPages} totalElements={r.totalElements} size={r.size} onPageChange={onPage} />
      </Card>
    </div>
  )
}
