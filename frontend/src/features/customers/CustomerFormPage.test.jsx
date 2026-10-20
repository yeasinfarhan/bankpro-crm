import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CustomerFormPage from './CustomerFormPage'
import { renderWithAuth, USERS } from '../../test/renderWithAuth'
import { createCustomer } from '../../api/customersApi'
import { CUSTOMER } from './fixtures'

vi.mock('../../api/customersApi', () => ({ createCustomer: vi.fn(), updateCustomer: vi.fn(), fetchCustomer: vi.fn() }))
vi.mock('../../api/productsApi', () => ({
  fetchProducts: vi.fn().mockResolvedValue([{ id: 1, name: 'BankPro Savings Account', category: 'ACCOUNT' }]),
}))
vi.mock('../../api/usersApi', () => ({ fetchAssignableStaff: vi.fn().mockResolvedValue([]) }))

function renderForm() {
  return renderWithAuth(
    <Routes>
      <Route path="/customers/new" element={<CustomerFormPage />} />
      <Route path="/customers/:id" element={<p>Customer detail page</p>} />
    </Routes>,
    { route: '/customers/new', auth: { user: USERS.officer } },
  )
}

describe('CustomerFormPage', () => {
  beforeEach(() => {
    vi.mocked(createCustomer).mockReset()
  })

  it('validates required fields and phone/email format before calling the API', async () => {
    renderForm()
    await userEvent.click(await screen.findByRole('button', { name: 'Create customer' }))
    expect(await screen.findByText('Full name is required')).toBeInTheDocument()
    expect(screen.getByText('Phone number is required')).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText(/Mobile number/), '12345')
    await userEvent.type(screen.getByLabelText(/Email/), 'not-an-email')
    await userEvent.click(screen.getByRole('button', { name: 'Create customer' }))
    expect(await screen.findByText('Enter a valid Bangladesh mobile number')).toBeInTheDocument()
    expect(screen.getByText('Enter a valid email address')).toBeInTheDocument()
    expect(createCustomer).not.toHaveBeenCalled()
  })

  it('submits a valid customer with selected products and opens the detail page', async () => {
    vi.mocked(createCustomer).mockResolvedValue(CUSTOMER)
    const { toast } = renderForm()

    await userEvent.type(await screen.findByLabelText(/Full name/), 'Rafiqul Islam')
    await userEvent.type(screen.getByLabelText(/Mobile number/), '01712-345678')
    await userEvent.selectOptions(screen.getByLabelText('Segment *'), 'EMERGING_AFFLUENT')
    await userEvent.click(screen.getByLabelText(/BankPro Savings Account/))
    await userEvent.click(screen.getByRole('button', { name: 'Create customer' }))

    await waitFor(() => expect(createCustomer).toHaveBeenCalledTimes(1))
    expect(vi.mocked(createCustomer).mock.calls[0][0]).toMatchObject({
      fullName: 'Rafiqul Islam', phone: '01712-345678', segment: 'EMERGING_AFFLUENT', email: null, productIds: [1],
    })
    expect(await screen.findByText('Customer detail page')).toBeInTheDocument()
    expect(toast.notify).toHaveBeenCalledWith('Customer CUS-000007 created')
  })

  it('shows field errors returned by the server (e.g. duplicate phone)', async () => {
    vi.mocked(createCustomer).mockImplementation(() => Promise.reject(Object.assign(new Error('HTTP 409'), {
      response: { status: 409, data: { message: 'A customer with this phone number already exists.',
        fieldErrors: [{ field: 'phone', message: 'A customer with this phone number already exists.' }] } },
    })))
    renderForm()
    await userEvent.type(await screen.findByLabelText(/Full name/), 'Duplicate Person')
    await userEvent.type(screen.getByLabelText(/Mobile number/), '01712345678')
    await userEvent.click(screen.getByRole('button', { name: 'Create customer' }))

    try { await screen.findByRole('alert') } catch (e) { console.log('DBG no alert', e.message.slice(0,300)) }
    console.log('DBG calls', vi.mocked(createCustomer).mock.calls.length)
    expect(await screen.findByRole('alert')).toHaveTextContent('already exists')
    expect(screen.getByLabelText(/Mobile number/)).toHaveAttribute('aria-invalid', 'true')
  })
})
