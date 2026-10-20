import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import CustomerPicker from './CustomerPicker'
import { fetchCustomers } from '../../api/customersApi'

vi.mock('../../api/customersApi', () => ({ fetchCustomers: vi.fn() }))

const people = [
  { id: 7, code: 'CUS-000007', fullName: 'Rafiqul Islam' },
  { id: 9, code: 'CUS-000009', fullName: 'Habibur Rahman' },
]

describe('CustomerPicker', () => {
  it('searches the server and picks a customer with the keyboard', async () => {
    vi.mocked(fetchCustomers).mockResolvedValue({ content: people })
    const onChange = vi.fn()
    render(<CustomerPicker id="customer" value={null} onChange={onChange} />)
    const box = screen.getByRole('combobox')
    await userEvent.type(box, 'ra')
    expect(await screen.findByRole('option', { name: /Habibur Rahman/ })).toBeInTheDocument()
    expect(fetchCustomers).toHaveBeenCalledWith({ q: 'ra', size: 8, sort: 'fullName,asc' })
    await userEvent.keyboard('{ArrowDown}{ArrowDown}')
    expect(screen.getByRole('option', { name: /Habibur Rahman/ })).toHaveAttribute('aria-selected', 'true')
    await userEvent.keyboard('{Enter}')
    expect(onChange).toHaveBeenCalledWith({ id: 9, code: 'CUS-000009', name: 'Habibur Rahman' })
  })

  it('picks with the mouse and shows the chosen customer with a change button', async () => {
    vi.mocked(fetchCustomers).mockResolvedValue({ content: people })
    const onChange = vi.fn()
    const { rerender } = render(<CustomerPicker id="customer" value={null} onChange={onChange} />)
    await userEvent.type(screen.getByRole('combobox'), 'raf')
    await userEvent.click(await screen.findByRole('option', { name: /Rafiqul Islam/ }))
    expect(onChange).toHaveBeenCalledWith({ id: 7, code: 'CUS-000007', name: 'Rafiqul Islam' })
    rerender(<CustomerPicker id="customer" value={{ id: 7, code: 'CUS-000007', name: 'Rafiqul Islam' }} onChange={onChange} />)
    expect(screen.getByText('Rafiqul Islam')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Change customer' }))
    expect(onChange).toHaveBeenLastCalledWith(null)
  })
})
