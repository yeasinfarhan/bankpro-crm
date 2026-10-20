import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TargetsPage from './TargetsPage'
import { renderWithAuth, USERS } from '../../test/renderWithAuth'
import { createTarget, deleteTarget, fetchTargetRanking, fetchTargets } from '../../api/metricsApi'
import { fetchAssignableStaff } from '../../api/usersApi'

vi.mock('../../api/metricsApi', () => ({
  fetchTargets: vi.fn(), fetchTargetRanking: vi.fn(), createTarget: vi.fn(), updateTarget: vi.fn(), deleteTarget: vi.fn(),
}))
vi.mock('../../api/usersApi', () => ({ fetchAssignableStaff: vi.fn() }))
vi.mock('../../api/branchesApi', () => ({ fetchBranches: vi.fn().mockResolvedValue([]) }))

const BRANCH = { id: 1, name: 'Motijheel Branch' }
const target = (over) => ({
  id: 1, branch: BRANCH, user: { id: 3, fullName: 'Nusrat Jahan' }, periodType: 'MONTHLY', periodStart: '2026-10-01',
  periodEnd: '2026-10-31', category: 'NEW_CUSTOMERS', unit: 'COUNT', targetValue: 4, actual: 3, achievementPct: 75, remaining: 1, canEdit: true, ...over,
})

beforeEach(() => {
  vi.mocked(fetchTargets).mockResolvedValue([target(), target({ id: 2, user: null, category: 'DEPOSIT_SALES', unit: 'BDT', periodType: 'QUARTERLY', targetValue: 5000000, actual: 1250000, achievementPct: 25, remaining: 3750000 })])
  vi.mocked(fetchTargetRanking).mockResolvedValue([{ rank: 1, officer: { id: 3, fullName: 'Nusrat Jahan' }, targets: 1, averageAchievementPct: 75, targetsMet: 0 }])
  vi.mocked(fetchAssignableStaff).mockResolvedValue([{ id: 3, fullName: 'Nusrat Jahan', role: 'RELATIONSHIP_OFFICER' }, { id: 2, fullName: 'Kamal Hossain', role: 'BRANCH_MANAGER' }])
})

describe('TargetsPage', () => {
  it('groups branch and officer targets for the chosen month with live progress and a ranking', async () => {
    renderWithAuth(<TargetsPage />, { route: '/targets?month=2026-10', auth: { user: USERS.manager } })
    expect(await screen.findByRole('heading', { name: 'Motijheel Branch (branch)' })).toBeInTheDocument()
    expect(screen.getByText(/৳1,250,000 of ৳5,000,000 · ৳3,750,000 to go/)).toBeInTheDocument()
    expect(screen.getByText(/3 of 4 · 1 to go/)).toBeInTheDocument()
    expect(fetchTargets).toHaveBeenCalledWith({ from: '2026-10-01', to: '2026-10-31', branchId: undefined, userId: '' })
    expect(await screen.findByText('0 of 1 target met')).toBeInTheDocument()
  })

  it('lets a branch manager create a whole-number count target and validates it first', async () => {
    vi.mocked(createTarget).mockResolvedValue(target({ id: 9, category: 'CONVERTED_LEADS' }))
    const { toast } = renderWithAuth(<TargetsPage />, { route: '/targets?month=2026-10', auth: { user: USERS.manager } })
    await userEvent.click(await screen.findByRole('button', { name: 'New target' }))
    const ownerSelect = screen.getByLabelText('Target for')
    expect(within(ownerSelect).getByRole('option', { name: 'Nusrat Jahan' })).toBeInTheDocument()
    expect(within(ownerSelect).queryByRole('option', { name: 'Kamal Hossain' })).not.toBeInTheDocument()
    await userEvent.selectOptions(screen.getByLabelText('Target for'), '3')
    await userEvent.selectOptions(screen.getByLabelText(/Category/), 'CONVERTED_LEADS')
    await userEvent.type(screen.getByLabelText(/Target \(count\)/), '2.5')
    await userEvent.click(screen.getByRole('button', { name: 'Create target' }))
    expect(await screen.findByText('Use a whole number')).toBeInTheDocument()
    expect(createTarget).not.toHaveBeenCalled()

    await userEvent.clear(screen.getByLabelText(/Target \(count\)/))
    await userEvent.type(screen.getByLabelText(/Target \(count\)/), '3')
    await userEvent.click(screen.getByRole('button', { name: 'Create target' }))
    await waitFor(() => expect(createTarget).toHaveBeenCalledWith({ userId: 3, periodType: 'MONTHLY', periodStart: '2026-10-01', category: 'CONVERTED_LEADS', targetValue: 3 }))
    expect(toast.notify).toHaveBeenCalledWith('Target created for Nusrat Jahan', 'success')
  })

  it('confirms before deleting a target', async () => {
    vi.mocked(deleteTarget).mockResolvedValue({})
    renderWithAuth(<TargetsPage />, { route: '/targets?month=2026-10', auth: { user: USERS.manager } })
    await userEvent.click(await screen.findByRole('button', { name: 'Delete New customers target for Nusrat Jahan' }))
    await userEvent.click(screen.getByRole('button', { name: 'Delete' }))
    await waitFor(() => expect(deleteTarget).toHaveBeenCalledWith(1))
  })

  it('is read-only for officers: no create button, no edit controls, no ranking', async () => {
    vi.mocked(fetchTargets).mockResolvedValue([target({ canEdit: false })])
    renderWithAuth(<TargetsPage />, { route: '/targets?month=2026-10', auth: { user: USERS.officer } })
    expect(await screen.findByText(/3 of 4/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'New target' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Delete/ })).not.toBeInTheDocument()
    expect(fetchTargetRanking).not.toHaveBeenCalled()
  })
})
