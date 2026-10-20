import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ReportsPage from './ReportsPage'
import { renderWithAuth, USERS } from '../../test/renderWithAuth'
import { downloadReportCsv, fetchReport } from '../../api/metricsApi'

vi.mock('../../api/metricsApi', () => ({ fetchReport: vi.fn(), downloadReportCsv: vi.fn() }))
vi.mock('../../api/usersApi', () => ({ fetchAssignableStaff: vi.fn().mockResolvedValue([{ id: 3, fullName: 'Nusrat Jahan' }]) }))
vi.mock('../../api/branchesApi', () => ({ fetchBranches: vi.fn().mockResolvedValue([]) }))

const REPORT = {
  type: 'PRODUCT_SALES', title: 'Product sales', description: 'Sales by product.', from: '2026-10-01', to: '2026-10-20',
  columns: [{ key: 'product', label: 'Product', format: 'text' }, { key: 'category', label: 'Category', format: 'category' }, { key: 'completedValue', label: 'Completed value', format: 'bdt' }],
  summary: [{ label: 'Completed value', value: 1500000, format: 'bdt' }, { label: 'Conversion rate', value: 62.5, format: 'percent' }],
  rows: [{ product: 'Fixed Deposit (fictional)', category: 'DEPOSIT', completedValue: 1500000 }],
  page: 0, size: 20, totalElements: 21, totalPages: 2,
  chart: { type: 'bar', xKey: 'product', series: [{ key: 'completedValue', label: 'Completed value', format: 'bdt' }] },
}

beforeEach(() => {
  vi.mocked(fetchReport).mockResolvedValue(REPORT)
})

describe('ReportsPage', () => {
  it('renders summary, formatted rows and pagination from the report definition', async () => {
    renderWithAuth(<ReportsPage />, { route: '/reports?type=product-sales&period=thisMonth', auth: { user: USERS.manager } })
    expect(await screen.findByRole('heading', { name: 'Product sales' })).toBeInTheDocument()
    expect(screen.getByText('62.5%')).toBeInTheDocument()
    expect(screen.getByRole('table', { name: 'Product sales' })).toHaveTextContent('Fixed Deposit (fictional)Deposit৳1,500,000')
    expect(fetchReport).toHaveBeenCalledWith('product-sales', expect.objectContaining({ page: 0, size: 20, officerId: '' }))

    await userEvent.click(screen.getByRole('button', { name: 'Next page' }))
    await waitFor(() => expect(fetchReport).toHaveBeenLastCalledWith('product-sales', expect.objectContaining({ page: 1 })))
  })

  it('switches report type and resets to the first page', async () => {
    renderWithAuth(<ReportsPage />, { route: '/reports?type=product-sales&page=1', auth: { user: USERS.manager } })
    await screen.findByRole('heading', { name: 'Product sales' })
    await userEvent.selectOptions(screen.getByLabelText('Report'), 'conversion')
    await waitFor(() => expect(fetchReport).toHaveBeenLastCalledWith('conversion', expect.objectContaining({ page: 0 })))
  })

  it('exports the CSV with the same filters; officers get no officer filter', async () => {
    vi.mocked(downloadReportCsv).mockResolvedValue()
    const { toast } = renderWithAuth(<ReportsPage />, { route: '/reports?type=product-sales&period=thisMonth', auth: { user: USERS.officer } })
    await screen.findByRole('heading', { name: 'Product sales' })
    expect(screen.queryByLabelText('Officer')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Export CSV' }))
    await waitFor(() => expect(downloadReportCsv).toHaveBeenCalledWith('product-sales', expect.objectContaining({ officerId: undefined, branchId: undefined })))
    expect(toast.notify).toHaveBeenCalledWith('CSV downloaded', 'success')
  })
})
