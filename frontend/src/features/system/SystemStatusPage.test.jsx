import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SystemStatusPage from './SystemStatusPage'
import { fetchSystemInfo } from '../../api/systemApi'

vi.mock('../../api/systemApi', () => ({ fetchSystemInfo: vi.fn() }))

describe('SystemStatusPage', () => {
  beforeEach(() => {
    vi.mocked(fetchSystemInfo).mockReset()
  })

  it('shows connected state with backend details', async () => {
    vi.mocked(fetchSystemInfo).mockResolvedValue({
      application: 'BankPro',
      version: '0.1.0',
      businessZone: 'Asia/Dhaka',
      businessLocalTime: '2026-10-19 08:00:00',
    })

    render(<SystemStatusPage />)

    expect(screen.getByText('Checking…')).toBeInTheDocument()
    expect(await screen.findByText('Connected')).toBeInTheDocument()
    expect(screen.getByText('BankPro v0.1.0')).toBeInTheDocument()
    expect(screen.getByText(/2026-10-19 08:00:00/)).toBeInTheDocument()
  })

  it('shows an error and recovers after retry', async () => {
    vi.mocked(fetchSystemInfo)
      .mockRejectedValueOnce({ request: {} })
      .mockResolvedValueOnce({
        application: 'BankPro',
        version: '0.1.0',
        businessZone: 'Asia/Dhaka',
        businessLocalTime: '2026-10-19 08:00:00',
      })

    render(<SystemStatusPage />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the BankPro server.')
    await userEvent.click(screen.getByRole('button', { name: /check again/i }))
    expect(await screen.findByText('Connected')).toBeInTheDocument()
    expect(fetchSystemInfo).toHaveBeenCalledTimes(2)
  })
})
