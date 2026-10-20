import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import DataTable from './DataTable'

const columns = [
  { key: 'name', header: 'Name', sortKey: 'name' },
  { key: 'city', header: 'City' },
  { key: 'value', header: 'Value', sortKey: 'value', render: (r) => `৳${r.value}` },
]
const rows = [{ id: 1, name: 'Rafiqul Islam', city: 'Dhaka', value: 500 }]
const setPhone = (matches) => {
  window.matchMedia = vi.fn().mockImplementation(() => ({ matches, addEventListener: vi.fn(), removeEventListener: vi.fn() }))
}
afterEach(() => { delete window.matchMedia })

describe('DataTable', () => {
  it('renders a keyboard-scrollable table on wider screens', () => {
    setPhone(false)
    render(<DataTable columns={columns} rows={rows} caption="Customers" />)
    expect(screen.getByRole('region', { name: 'Customers' })).toHaveAttribute('tabindex', '0')
    expect(screen.getByRole('table', { name: 'Customers' })).toHaveTextContent('Rafiqul IslamDhaka৳500')
  })

  it('switches to labelled cards with a sort control on phones', async () => {
    setPhone(true)
    const onSortChange = vi.fn()
    render(<DataTable columns={columns} rows={rows} caption="Customers" sort="name,asc" onSortChange={onSortChange} />)
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    const card = within(screen.getByRole('list', { name: 'Customers' })).getByRole('listitem')
    expect(card).toHaveTextContent('Rafiqul IslamCityDhakaValue৳500')
    await userEvent.selectOptions(screen.getByLabelText('Sort by'), 'value,desc')
    expect(onSortChange).toHaveBeenCalledWith('value,desc')
  })
})
