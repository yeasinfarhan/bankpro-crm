import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import CustomerDetailPage from './CustomerDetailPage'
import { renderWithAuth, USERS } from '../../test/renderWithAuth'
import { archiveCustomer, fetchCustomer } from '../../api/customersApi'
import { CUSTOMER } from './fixtures'

vi.mock('../../api/customersApi', () => ({ fetchCustomer: vi.fn(), archiveCustomer: vi.fn(), restoreCustomer: vi.fn() }))
vi.mock('../../api/leadsApi', () => ({ fetchLeads: vi.fn().mockResolvedValue({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 }) }))
vi.mock('../../api/followUpsApi', () => ({
  fetchFollowUps: vi.fn().mockResolvedValue({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 }),
  fetchTimeline: vi.fn().mockResolvedValue([{ kind: 'INTERACTION', at: '2026-10-10T04:00:00Z', title: 'Phone call', detail: 'Discussed FDR', actor: 'Nusrat Jahan', link: null }]),
  recordInteraction: vi.fn(),
}))

function renderDetail(user) {
  return renderWithAuth(
    <Routes>
      <Route path="/customers/:id" element={<CustomerDetailPage />} />
      <Route path="/customers" element={<p>Customer list</p>} />
    </Routes>,
    { route: '/customers/7', auth: { user } },
  )
}

describe('CustomerDetailPage', () => {
  it('shows profile, held products and only the actions the API allows', async () => {
    vi.mocked(fetchCustomer).mockResolvedValue(CUSTOMER)
    renderDetail(USERS.officer)
    expect(await screen.findByRole('heading', { name: 'Rafiqul Islam' })).toBeInTheDocument()
    expect(screen.getByText('BankPro Savings Account')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Edit' })).toHaveAttribute('href', '/customers/7/edit')
    expect(screen.queryByRole('button', { name: 'Archive' })).not.toBeInTheDocument()
    expect(await screen.findByText('Discussed FDR')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /new lead/i })).toHaveAttribute('href', '/leads/new?customerId=7')
  })

  it('lets a manager archive after confirming', async () => {
    vi.mocked(fetchCustomer).mockResolvedValue({ ...CUSTOMER, permissions: { canEdit: true, canArchive: true } })
    vi.mocked(archiveCustomer).mockResolvedValue()
    renderDetail(USERS.manager)

    await userEvent.click(await screen.findByRole('button', { name: 'Archive' }))
    expect(screen.getByRole('alertdialog')).toHaveTextContent('history are kept')
    await userEvent.click(screen.getByRole('button', { name: 'Archive customer' }))

    expect(archiveCustomer).toHaveBeenCalledWith('7')
    expect(await screen.findByText('Customer list')).toBeInTheDocument()
  })

  it('shows not-found errors from the API (e.g. another branch\'s customer)', async () => {
    vi.mocked(fetchCustomer).mockImplementation(() => Promise.reject({ response: { status: 404, data: { message: 'Customer not found.' } } }))
    renderDetail(USERS.officer)
    expect(await screen.findByRole('alert')).toHaveTextContent('Customer not found.')
  })
})
