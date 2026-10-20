import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import FollowUpsPage from './FollowUpsPage'
import { renderWithAuth, USERS } from '../../test/renderWithAuth'
import { completeFollowUp, fetchFollowUpCounts, fetchFollowUps } from '../../api/followUpsApi'

vi.mock('../../api/followUpsApi', () => ({
  fetchFollowUps: vi.fn(),
  fetchFollowUpCounts: vi.fn(),
  completeFollowUp: vi.fn(),
  rescheduleFollowUp: vi.fn(),
  createFollowUp: vi.fn(),
}))
vi.mock('../../api/leadsApi', () => ({ fetchLeads: vi.fn().mockResolvedValue({ content: [] }) }))

const OVERDUE = {
  id: 5, customer: { id: 7, code: 'CUS-000007', name: 'Rafiqul Islam' }, lead: { id: 12, code: 'LD-000012' },
  assignedOfficer: { id: 3, fullName: 'Nusrat Jahan' }, activityType: 'PHONE_CALL', scheduledAt: '2026-10-18T05:00:00Z',
  status: 'SCHEDULED', overdue: true, notes: 'Confirm FDR tenure', outcome: null, completedAt: null, canEdit: true,
}
const page = (content) => ({ content, page: 0, size: 25, totalElements: content.length, totalPages: 1 })

describe('FollowUpsPage', () => {
  it('shows tab counts and completes an overdue follow-up with an outcome', async () => {
    vi.mocked(fetchFollowUpCounts).mockResolvedValue({ today: 2, upcoming: 3, overdue: 1 })
    vi.mocked(fetchFollowUps).mockResolvedValue(page([OVERDUE]))
    vi.mocked(completeFollowUp).mockResolvedValue({ ...OVERDUE, status: 'COMPLETED' })
    const { toast } = renderWithAuth(<FollowUpsPage />, { route: '/follow-ups?view=OVERDUE', auth: { user: USERS.officer } })

    expect(await screen.findByRole('tab', { name: /Overdue\s*1/ })).toHaveAttribute('aria-selected', 'true')
    expect(fetchFollowUps).toHaveBeenCalledWith(expect.objectContaining({ view: 'OVERDUE' }))
    expect(await within(screen.getByRole('tabpanel')).findByText('Overdue')).toBeInTheDocument()
    expect(screen.getByText(/18 Oct 2026, 11:00 am/)).toBeInTheDocument() // 05:00 UTC shown as 11:00 BST

    await userEvent.click(screen.getByRole('button', { name: 'Complete' }))
    await userEvent.click(screen.getByRole('button', { name: 'Mark complete' }))
    expect(screen.getByRole('alert')).toHaveTextContent('Describe the outcome')
    await userEvent.type(screen.getByLabelText(/Outcome/), 'Customer will visit on Sunday')
    await userEvent.click(screen.getByRole('button', { name: 'Mark complete' }))

    await waitFor(() => expect(completeFollowUp).toHaveBeenCalledWith(5, 'Customer will visit on Sunday', null))
    expect(toast.notify).toHaveBeenCalledWith('Follow-up completed')
  })

  it('shows a friendly empty state', async () => {
    vi.mocked(fetchFollowUpCounts).mockResolvedValue({ today: 0, upcoming: 0, overdue: 0 })
    vi.mocked(fetchFollowUps).mockResolvedValue(page([]))
    renderWithAuth(<FollowUpsPage />, { route: '/follow-ups', auth: { user: USERS.officer } })
    expect(await screen.findByText('Nothing scheduled for today.')).toBeInTheDocument()
  })
})
