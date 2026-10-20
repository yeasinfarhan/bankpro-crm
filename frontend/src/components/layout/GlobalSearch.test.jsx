import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import GlobalSearch from './GlobalSearch'
import { renderWithAuth, USERS } from '../../test/renderWithAuth'
import { fetchCustomers } from '../../api/customersApi'
import { fetchLeads } from '../../api/leadsApi'
import { CUSTOMER, page } from '../../features/customers/fixtures'
import { LEAD } from '../../features/leads/fixtures'

vi.mock('../../api/customersApi', () => ({ fetchCustomers: vi.fn() }))
vi.mock('../../api/leadsApi', () => ({ fetchLeads: vi.fn() }))

function Where() {
  const loc = useLocation()
  return <p data-testid="where">{loc.pathname + loc.search}</p>
}
const renderSearch = () => renderWithAuth(
  <><GlobalSearch /><Routes><Route path="*" element={<Where />} /></Routes></>,
  { route: '/', auth: { user: USERS.officer } },
)

beforeEach(() => {
  vi.mocked(fetchCustomers).mockResolvedValue(page([CUSTOMER]))
  vi.mocked(fetchLeads).mockResolvedValue(page([LEAD]))
})

describe('GlobalSearch', () => {
  it('waits for 2 characters, then shows grouped customer and lead results', async () => {
    renderSearch()
    const box = screen.getByRole('combobox', { name: 'Search customers and leads' })
    await userEvent.type(box, 'r')
    expect(fetchCustomers).not.toHaveBeenCalled()
    await userEvent.type(box, 'a')
    expect(await screen.findByRole('option', { name: /^Rafiqul Islam, CUS-000007/ })).toBeInTheDocument()
    expect(screen.getByRole('group', { name: 'Leads' })).toHaveTextContent('Rafiqul Islam — BankPro Fixed Deposit (FDR)LD-000012 · Qualified')
    expect(fetchCustomers).toHaveBeenCalledWith({ q: 'ra', size: 5 })
    expect(fetchLeads).toHaveBeenCalledWith({ q: 'ra', size: 5 })
    expect(box).toHaveAttribute('aria-expanded', 'true')
  })

  it('opens a result with the keyboard', async () => {
    renderSearch()
    const box = screen.getByRole('combobox')
    await userEvent.type(box, 'rafiq')
    await screen.findByRole('option', { name: /^Rafiqul Islam, CUS/ })
    await userEvent.keyboard('{ArrowDown}{ArrowDown}')
    expect(screen.getAllByRole('option')[1]).toHaveAttribute('aria-selected', 'true')
    await userEvent.keyboard('{Enter}')
    expect(screen.getByTestId('where')).toHaveTextContent('/leads/12')
    expect(box).toHaveValue('')
  })

  it('Enter without a selection goes to the full customer search; "/" focuses the box', async () => {
    renderSearch()
    await userEvent.keyboard('/')
    expect(screen.getByRole('combobox')).toHaveFocus()
    await userEvent.keyboard('dhaka bank{Enter}')
    expect(screen.getByTestId('where')).toHaveTextContent('/customers?q=dhaka%20bank')
  })

  it('says when nothing matches and reports errors', async () => {
    vi.mocked(fetchCustomers).mockResolvedValue(page([]))
    vi.mocked(fetchLeads).mockResolvedValue(page([]))
    renderSearch()
    await userEvent.type(screen.getByRole('combobox'), 'zzz')
    expect(await screen.findByText('No customers or leads match “zzz”.')).toBeInTheDocument()
    vi.mocked(fetchLeads).mockRejectedValue({ response: { data: { message: 'Server unavailable' } } })
    await userEvent.type(screen.getByRole('combobox'), 'q')
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Server unavailable'))
  })
})
