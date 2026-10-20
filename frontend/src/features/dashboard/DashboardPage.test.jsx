import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DashboardPage from './DashboardPage'
import { renderWithAuth, USERS } from '../../test/renderWithAuth'
import * as metrics from '../../api/metricsApi'
import { fetchBranches } from '../../api/branchesApi'

vi.mock('../../api/metricsApi', () => ({
  fetchDashboardSummary: vi.fn(), fetchSalesTrend: vi.fn(), fetchCustomerAcquisition: vi.fn(), fetchLeadFunnel: vi.fn(),
  fetchProductMix: vi.fn(), fetchDashboardTargets: vi.fn(), fetchOfficerPerformance: vi.fn(),
}))
vi.mock('../../api/branchesApi', () => ({ fetchBranches: vi.fn() }))

const SUMMARY = {
  from: '2026-05-01', to: '2026-10-20', totalCustomers: 42, newCustomersThisMonth: 5, activeLeads: 17, leadsConverted: 9,
  totalSalesValue: 2350000, depositSalesValue: 1800000, targetAchievementPct: 64.5, targetsCounted: 8, overdueFollowUps: 3,
}
const TARGET = {
  id: 1, branch: { id: 1, name: 'Motijheel Branch' }, user: { id: 3, fullName: 'Nusrat Jahan' }, periodType: 'MONTHLY',
  periodStart: '2026-10-01', periodEnd: '2026-10-31', category: 'DEPOSIT_SALES', unit: 'BDT', targetValue: 800000,
  actual: 900000, achievementPct: 112.5, remaining: 0, canEdit: false,
}

beforeEach(() => {
  vi.mocked(metrics.fetchDashboardSummary).mockResolvedValue(SUMMARY)
  vi.mocked(metrics.fetchSalesTrend).mockResolvedValue([{ month: '2026-10', completedValue: 2350000, completedCount: 6, depositValue: 1800000 }])
  vi.mocked(metrics.fetchCustomerAcquisition).mockResolvedValue([{ month: '2026-10', newCustomers: 5, MASS: 3, EMERGING_AFFLUENT: 1, AFFLUENT: 1, SME: 0 }])
  vi.mocked(metrics.fetchLeadFunnel).mockResolvedValue([{ stage: 'NEW', leads: 4, estimatedValue: 100 }])
  vi.mocked(metrics.fetchProductMix).mockResolvedValue([])
  vi.mocked(metrics.fetchDashboardTargets).mockResolvedValue([TARGET])
  vi.mocked(metrics.fetchOfficerPerformance).mockResolvedValue([{ officerId: 3, officer: 'Nusrat Jahan', branch: 'Motijheel Branch', customersAdded: 4, leadsCreated: 6, conversions: 3, completedSalesValue: 900000, followUpsScheduled: 5, followUpsCompleted: 4, overdueNow: 1, targetAchievementPct: 80 }])
  vi.mocked(fetchBranches).mockResolvedValue([{ id: 1, name: 'Motijheel Branch' }, { id: 2, name: 'Gulshan Branch' }])
})

describe('DashboardPage', () => {
  it('shows the KPI tiles with definitions, targets and an accessible data view for an officer', async () => {
    renderWithAuth(<DashboardPage />, { route: '/?period=thisMonth', auth: { user: USERS.officer } })
    const kpis = await screen.findByRole('region', { name: 'Key figures' })
    expect(await within(kpis).findByText('৳2,350,000')).toBeInTheDocument()
    expect(within(kpis).getByText('64.5%')).toBeInTheDocument()
    expect(within(kpis).getByText(/Average of 8 targets/)).toBeInTheDocument()
    expect(metrics.fetchDashboardSummary).toHaveBeenCalledWith(expect.objectContaining({ from: expect.stringMatching(/-01$/), branchId: undefined }))

    // Officers see their own target progress and never the team table.
    expect(await screen.findByText('112.5%')).toBeInTheDocument()
    expect(screen.getByText('Met')).toBeInTheDocument()
    expect(metrics.fetchOfficerPerformance).not.toHaveBeenCalled()
    expect(screen.queryByText('Officer performance')).not.toBeInTheDocument()

    // Empty product mix says so instead of drawing an empty chart.
    expect(screen.getByText('No completed sales in this period.')).toBeInTheDocument()

    // "Show data" swaps a chart for a table with the exact numbers.
    const [first] = await screen.findAllByRole('button', { name: 'Show data' })
    await userEvent.click(first)
    expect(screen.getByRole('table', { name: 'Completed sales by month' })).toHaveTextContent('Oct 2026৳2,350,0006৳1,800,000')
  })

  it('lets an admin filter by branch and shows officer performance', async () => {
    renderWithAuth(<DashboardPage />, { route: '/', auth: { user: USERS.admin } })
    expect(await screen.findByRole('table', { name: 'Officer performance' })).toHaveTextContent('Nusrat Jahan')
    await screen.findByRole('option', { name: 'Gulshan Branch' })
    await userEvent.selectOptions(screen.getByLabelText('Branch'), '2')
    expect(metrics.fetchDashboardSummary).toHaveBeenLastCalledWith(expect.objectContaining({ branchId: '2' }))
    expect(await screen.findByText(/Gulshan Branch ·/)).toBeInTheDocument()
  })

  it('shows an error with retry when the summary fails', async () => {
    vi.mocked(metrics.fetchDashboardSummary).mockRejectedValueOnce({ response: { data: { message: 'Database unavailable' } } })
    renderWithAuth(<DashboardPage />, { route: '/', auth: { user: USERS.manager } })
    expect(await screen.findByText('Database unavailable')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByText('৳2,350,000')).toBeInTheDocument()
  })
})
