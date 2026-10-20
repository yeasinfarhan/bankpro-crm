import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import LeadsPage from './LeadsPage'
import { renderWithAuth, USERS } from '../../test/renderWithAuth'
import { changeLeadStatus, fetchLeadBoard, fetchLeads, fetchLeadSummary } from '../../api/leadsApi'
import { board, LEAD } from './fixtures'

vi.mock('../../api/leadsApi', () => ({
  fetchLeadBoard: vi.fn(),
  fetchLeads: vi.fn(),
  fetchLeadSummary: vi.fn(),
  changeLeadStatus: vi.fn(),
  fetchSales: vi.fn().mockResolvedValue([]),
  decideSale: vi.fn(),
}))
vi.mock('../../api/productsApi', () => ({ fetchProducts: vi.fn().mockResolvedValue([]) }))
vi.mock('../../api/usersApi', () => ({ fetchAssignableStaff: vi.fn().mockResolvedValue([]) }))

const SUMMARY = { countByStatus: {}, valueByStatus: {}, openCount: 4, openPipelineValue: 1250000, convertedCount: 3, lostCount: 1, conversionRate: 75.0 }

describe('LeadsPage', () => {
  it('shows pipeline figures and the board by default, and moves a lead through the API', async () => {
    vi.mocked(fetchLeadSummary).mockResolvedValue(SUMMARY)
    vi.mocked(fetchLeadBoard).mockResolvedValue(board())
    vi.mocked(changeLeadStatus).mockResolvedValue({ ...LEAD, status: 'PROPOSAL' })
    renderWithAuth(<LeadsPage />, { route: '/leads', auth: { user: USERS.officer } })

    expect(await screen.findByText('৳1,250,000')).toBeInTheDocument()
    expect(screen.getByText('75%')).toBeInTheDocument()
    expect(await screen.findByRole('listitem', { name: 'Qualified: 1 leads' })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Change stage of LD-000012' }))
    await userEvent.click(screen.getByRole('menuitem', { name: 'Move to Proposal' }))
    await waitFor(() => expect(changeLeadStatus).toHaveBeenCalledWith(12, { status: 'PROPOSAL' }))
    await waitFor(() => expect(fetchLeadBoard).toHaveBeenCalledTimes(2)) // refreshed after the move
  })

  it('switches to the paginated table view', async () => {
    vi.mocked(fetchLeadSummary).mockResolvedValue({ ...SUMMARY, conversionRate: null })
    vi.mocked(fetchLeadBoard).mockResolvedValue(board())
    vi.mocked(fetchLeads).mockResolvedValue({ content: [LEAD], page: 0, size: 20, totalElements: 1, totalPages: 1 })
    renderWithAuth(<LeadsPage />, { route: '/leads', auth: { user: USERS.officer } })

    await userEvent.click(await screen.findByRole('button', { name: /Table/ }))
    expect(await screen.findByRole('table')).toBeInTheDocument()
    expect(fetchLeads).toHaveBeenCalledWith(expect.objectContaining({ sort: 'createdAt,desc', page: 0, size: 20 }))
    expect(screen.getAllByText('—').length).toBeGreaterThan(0) // conversion rate without closed leads
  })
})
