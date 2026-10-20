import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import NotificationsPanel from './NotificationsPanel'
import { renderWithAuth, USERS } from '../../test/renderWithAuth'
import { fetchNotifications, markNotificationRead } from '../../api/notificationsApi'

vi.mock('../../api/notificationsApi', () => ({
  fetchNotifications: vi.fn(),
  markNotificationRead: vi.fn().mockResolvedValue(),
  markAllNotificationsRead: vi.fn().mockResolvedValue(),
}))

describe('NotificationsPanel', () => {
  it('shows the unread count and opens the linked page, marking it read', async () => {
    vi.mocked(fetchNotifications).mockResolvedValue({
      unreadCount: 1,
      items: [{ id: 9, type: 'LEAD_ASSIGNED', message: 'New lead assigned: FDR for Rafiqul Islam', link: '/leads/12', read: false, createdAt: '2026-10-19T04:00:00Z' }],
    })
    renderWithAuth(
      <Routes>
        <Route path="/" element={<NotificationsPanel />} />
        <Route path="/leads/:id" element={<p>Lead page</p>} />
      </Routes>,
      { auth: { user: USERS.officer } },
    )
    const bell = await screen.findByRole('button', { name: 'Notifications, 1 unread' })
    await userEvent.click(bell)
    await userEvent.click(await screen.findByText('New lead assigned: FDR for Rafiqul Islam'))
    await waitFor(() => expect(markNotificationRead).toHaveBeenCalledWith(9))
    expect(await screen.findByText('Lead page')).toBeInTheDocument()
  })
})
