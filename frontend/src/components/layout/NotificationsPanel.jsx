import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { Bell, BellOff } from 'lucide-react'
import useDismiss from './useDismiss'
import { fetchNotifications, markAllNotificationsRead, markNotificationRead } from '../../api/notificationsApi'
import { formatDateTime } from '../../utils/format'

const POLL_MS = 60_000

/** In-app notifications (lead assigned, overdue follow-ups, simulated approvals). Refreshes every minute. */
export default function NotificationsPanel() {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [inbox, setInbox] = useState({ unreadCount: 0, items: [] })
  const ref = useRef(null)
  const close = useCallback(() => setOpen(false), [])
  useDismiss(ref, open, close)

  const load = useCallback(() => fetchNotifications().then(setInbox, () => {}), [])

  useEffect(() => {
    load()
    const timer = setInterval(load, POLL_MS)
    return () => clearInterval(timer)
  }, [load])

  const openItem = async (n) => {
    close()
    if (!n.read) await markNotificationRead(n.id).catch(() => {})
    load()
    if (n.link) navigate(n.link)
  }

  const readAll = async () => {
    await markAllNotificationsRead().catch(() => {})
    load()
  }

  return (
    <div className="relative" ref={ref}>
      <button type="button" onClick={() => { setOpen((v) => !v); if (!open) load() }}
        className="relative rounded-lg p-2 text-ink-muted hover:bg-gray-100 hover:text-ink"
        aria-label={inbox.unreadCount ? `Notifications, ${inbox.unreadCount} unread` : 'Notifications'} aria-expanded={open}>
        <Bell className="h-5 w-5" />
        {inbox.unreadCount > 0 && (
          <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-danger-600 px-1 text-[10px] font-semibold text-white" aria-hidden="true">
            {inbox.unreadCount > 9 ? '9+' : inbox.unreadCount}
          </span>
        )}
      </button>
      {open && (
        <div className="absolute right-0 z-30 mt-2 w-80 max-w-[calc(100vw-2rem)] rounded-xl border border-line bg-white shadow-lg">
          <div className="flex items-center justify-between border-b border-line px-4 py-3">
            <span className="text-sm font-semibold">Notifications</span>
            {inbox.unreadCount > 0 && <button type="button" onClick={readAll} className="text-xs font-medium text-royal-600 hover:underline">Mark all read</button>}
          </div>
          {inbox.items.length === 0 ? (
            <div className="flex flex-col items-center px-6 py-8 text-center">
              <BellOff className="h-6 w-6 text-ink-muted" aria-hidden="true" />
              <p className="mt-2 text-sm text-ink-muted">You're all caught up.</p>
            </div>
          ) : (
            <ul className="max-h-96 divide-y divide-line overflow-y-auto">
              {inbox.items.map((n) => (
                <li key={n.id}>
                  <button type="button" onClick={() => openItem(n)} className={`flex w-full gap-2 px-4 py-3 text-left hover:bg-canvas ${n.read ? '' : 'bg-royal-50/60'}`}>
                    {!n.read && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-royal-600" aria-label="Unread" />}
                    <span className={n.read ? 'pl-4' : ''}>
                      <span className="block text-sm text-ink">{n.message}</span>
                      <span className="block text-xs text-ink-muted">{formatDateTime(n.createdAt)}</span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
