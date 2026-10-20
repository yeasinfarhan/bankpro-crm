import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import LeadBoard from './LeadBoard'
import { renderWithAuth, USERS } from '../../test/renderWithAuth'
import { board, LEAD } from './fixtures'

describe('LeadBoard', () => {
  it('shows every pipeline column with counts and values', () => {
    renderWithAuth(<LeadBoard board={board()} onMove={vi.fn()} />, { auth: { user: USERS.officer } })
    expect(screen.getByRole('listitem', { name: 'Qualified: 1 leads' })).toBeInTheDocument()
    expect(screen.getByRole('listitem', { name: 'Lost: 0 leads' })).toBeInTheDocument()
    const qualified = screen.getByRole('listitem', { name: 'Qualified: 1 leads' })
    expect(within(qualified).getByRole('link', { name: 'Rafiqul Islam' })).toHaveAttribute('href', '/leads/12')
    expect(within(qualified).getAllByText('৳500,000').length).toBeGreaterThan(0)
  })

  it('offers only the moves the API allows, plus convert, via the keyboard-friendly menu', async () => {
    const onMove = vi.fn()
    renderWithAuth(<LeadBoard board={board()} onMove={onMove} />, { auth: { user: USERS.officer } })
    await userEvent.click(screen.getByRole('button', { name: 'Change stage of LD-000012' }))
    const items = screen.getAllByRole('menuitem').map((m) => m.textContent)
    expect(items).toEqual(['Move to Contacted', 'Move to Proposal', 'Convert to sale…', 'Mark as lost…'])
    await userEvent.click(screen.getByRole('menuitem', { name: 'Move to Proposal' }))
    expect(onMove).toHaveBeenCalledWith(LEAD, 'PROPOSAL')
  })

  it('hides the menu when the user cannot move the lead (e.g. read-only admin)', () => {
    const readOnly = { ...LEAD, allowedStatuses: [], permissions: { canEdit: false, canAssign: false, canConvert: false } }
    renderWithAuth(<LeadBoard board={board([readOnly])} onMove={vi.fn()} />, { auth: { user: USERS.admin } })
    expect(screen.queryByRole('button', { name: /Change stage/ })).not.toBeInTheDocument()
  })
})
