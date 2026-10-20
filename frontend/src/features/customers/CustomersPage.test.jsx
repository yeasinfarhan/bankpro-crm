import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CustomersPage from './CustomersPage'
import { renderWithAuth, USERS } from '../../test/renderWithAuth'
import { fetchCustomers } from '../../api/customersApi'
import { CUSTOMER, page } from './fixtures'

vi.mock('../../api/customersApi', () => ({ fetchCustomers: vi.fn(), downloadCustomersCsv: vi.fn() }))
vi.mock('../../api/usersApi', () => ({ fetchAssignableStaff: vi.fn().mockResolvedValue([{ id: 3, fullName: 'Nusrat Jahan' }]) }))

describe('CustomersPage', () => {
  beforeEach(() => {
    vi.mocked(fetchCustomers).mockReset()
  })

  it('loads customers from the API and shows them in the table', async () => {
    vi.mocked(fetchCustomers).mockResolvedValue(page([CUSTOMER]))
    renderWithAuth(<CustomersPage />, { route: '/customers', auth: { user: USERS.officer } })

    expect(screen.getByRole('status')).toHaveTextContent('Loading customers')
    expect(await screen.findByRole('link', { name: 'Rafiqul Islam' })).toBeInTheDocument()
    expect(screen.getByText('CUS-000007')).toBeInTheDocument()
    expect(screen.getByText('+880 1712-345678')).toBeInTheDocument()
    expect(fetchCustomers).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 20, sort: 'createdAt,desc' }))
  })

  it('sends search text (debounced) and filters to the server', async () => {
    vi.mocked(fetchCustomers).mockResolvedValue(page([CUSTOMER]))
    renderWithAuth(<CustomersPage />, { route: '/customers', auth: { user: USERS.manager } })
    await screen.findByRole('link', { name: 'Rafiqul Islam' })

    await userEvent.type(screen.getByLabelText('Search customers'), 'rafiq')
    await waitFor(() => expect(fetchCustomers).toHaveBeenLastCalledWith(expect.objectContaining({ q: 'rafiq' })))

    await userEvent.selectOptions(screen.getByLabelText('Segment'), 'SME')
    await waitFor(() => expect(fetchCustomers).toHaveBeenLastCalledWith(expect.objectContaining({ q: 'rafiq', segment: 'SME', page: 0 })))

    await userEvent.click(screen.getByRole('button', { name: /^Customer/ }))
    await waitFor(() => expect(fetchCustomers).toHaveBeenLastCalledWith(expect.objectContaining({ sort: 'fullName,asc' })))
  })

  it('shows an empty state and an error state with retry', async () => {
    vi.mocked(fetchCustomers).mockResolvedValueOnce(page([]))
    const { unmount } = renderWithAuth(<CustomersPage />, { route: '/customers', auth: { user: USERS.officer } })
    expect(await screen.findByText('No customers found')).toBeInTheDocument()
    unmount()

    vi.mocked(fetchCustomers).mockRejectedValueOnce({ request: {} }).mockResolvedValueOnce(page([CUSTOMER]))
    renderWithAuth(<CustomersPage />, { route: '/customers', auth: { user: USERS.officer } })
    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the BankPro server.')
    await userEvent.click(screen.getByRole('button', { name: /try again/i }))
    expect(await screen.findByRole('link', { name: 'Rafiqul Islam' })).toBeInTheDocument()
  })

  it('hides "New customer" and the archived filter from roles that cannot use them', async () => {
    vi.mocked(fetchCustomers).mockResolvedValue(page([CUSTOMER]))
    renderWithAuth(<CustomersPage />, { route: '/customers', auth: { user: USERS.admin } })
    await screen.findByRole('link', { name: 'Rafiqul Islam' })
    expect(screen.queryByRole('link', { name: /new customer/i })).not.toBeInTheDocument()
    expect(screen.getByLabelText('Include archived')).toBeInTheDocument()
  })
})
