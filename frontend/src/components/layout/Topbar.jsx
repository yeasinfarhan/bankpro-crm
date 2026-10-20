import { Menu, Search } from 'lucide-react'
import NotificationsPanel from './NotificationsPanel'
import UserMenu from './UserMenu'
import { useAuth } from '../../features/auth/AuthContext'

export default function Topbar({ onOpenMobileNav }) {
  const { user } = useAuth()
  return (
    <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-line bg-white/95 px-4 backdrop-blur sm:px-6">
      <button
        type="button"
        onClick={onOpenMobileNav}
        className="rounded-lg p-2 text-ink-muted hover:bg-gray-100 lg:hidden"
        aria-label="Open menu"
      >
        <Menu className="h-5 w-5" />
      </button>

      {/* Global search across customers and leads is planned for Phase 6 (polish). */}
      <div className="relative hidden max-w-md flex-1 sm:block">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" aria-hidden="true" />
        <input
          type="search"
          disabled
          placeholder="Global search (coming soon — use the Customers search for now)"
          aria-label="Global search"
          className="w-full rounded-lg border border-line bg-canvas py-2 pl-9 pr-3 text-sm placeholder:text-ink-muted disabled:cursor-not-allowed"
        />
      </div>

      <div className="ml-auto flex items-center gap-1 sm:gap-3">
        <span className="hidden rounded-md bg-royal-50 px-2 py-1 text-xs font-medium text-royal-700 md:inline">
          {user.branch ? user.branch.name : 'All branches'}
        </span>
        <NotificationsPanel />
        <UserMenu />
      </div>
    </header>
  )
}
