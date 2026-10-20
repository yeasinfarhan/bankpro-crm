import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ConvertDialog, LossDialog } from './LeadDialogs'
import { renderWithAuth, USERS } from '../../test/renderWithAuth'
import { changeLeadStatus, convertLead } from '../../api/leadsApi'
import { LEAD } from './fixtures'

vi.mock('../../api/leadsApi', () => ({ changeLeadStatus: vi.fn(), convertLead: vi.fn(), assignLead: vi.fn() }))
vi.mock('../../api/usersApi', () => ({ fetchAssignableStaff: vi.fn().mockResolvedValue([]) }))

describe('Lead dialogs', () => {
  beforeEach(() => {
    vi.mocked(convertLead).mockReset()
    vi.mocked(changeLeadStatus).mockReset()
  })

  it('convert: validates the amount, then reports the simulated sale', async () => {
    vi.mocked(convertLead).mockResolvedValue({ lead: { ...LEAD, status: 'CONVERTED' }, sale: { id: 1 }, alreadyConverted: false })
    const onDone = vi.fn()
    renderWithAuth(<ConvertDialog lead={LEAD} onClose={vi.fn()} onDone={onDone} />, { auth: { user: USERS.officer } })

    expect(screen.getByText(/No real account is opened/)).toBeInTheDocument()
    const amount = screen.getByLabelText(/Sale amount/)
    await userEvent.clear(amount)
    await userEvent.type(amount, '0')
    await userEvent.click(screen.getByRole('button', { name: 'Convert to sale' }))
    expect(screen.getByRole('alert')).toHaveTextContent('greater than zero')
    expect(convertLead).not.toHaveBeenCalled()

    await userEvent.clear(amount)
    await userEvent.type(amount, '250000')
    await userEvent.click(screen.getByRole('button', { name: 'Convert to sale' }))
    await waitFor(() => expect(convertLead).toHaveBeenCalledWith(12, 250000))
    expect(onDone).toHaveBeenCalledWith(expect.objectContaining({ status: 'CONVERTED' }), 'Lead converted to a simulated sale')
  })

  it('convert: loans explain that approval is needed and repeat conversions are reported, not duplicated', async () => {
    vi.mocked(convertLead).mockResolvedValue({ lead: LEAD, sale: { id: 1 }, alreadyConverted: true })
    const onDone = vi.fn()
    renderWithAuth(<ConvertDialog lead={{ ...LEAD, productCategory: 'LOAN' }} onClose={vi.fn()} onDone={onDone} />, { auth: { user: USERS.officer } })
    expect(screen.getByText(/pending a \(simulated\) manager approval/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Convert to sale' }))
    await waitFor(() => expect(onDone).toHaveBeenCalledWith(LEAD, 'This lead was already converted'))
  })

  it('lost: requires a reason before calling the API', async () => {
    vi.mocked(changeLeadStatus).mockResolvedValue({ ...LEAD, status: 'LOST' })
    const onDone = vi.fn()
    renderWithAuth(<LossDialog lead={LEAD} onClose={vi.fn()} onDone={onDone} />, { auth: { user: USERS.officer } })
    await userEvent.click(screen.getByRole('button', { name: 'Mark as lost' }))
    expect(screen.getByRole('alert')).toHaveTextContent('Choose why')
    expect(changeLeadStatus).not.toHaveBeenCalled()

    await userEvent.selectOptions(screen.getByLabelText(/Reason/), 'CHOSE_COMPETITOR')
    await userEvent.click(screen.getByRole('button', { name: 'Mark as lost' }))
    await waitFor(() => expect(changeLeadStatus).toHaveBeenCalledWith(12, { status: 'LOST', lossReason: 'CHOSE_COMPETITOR', lossNote: null }))
  })
})
