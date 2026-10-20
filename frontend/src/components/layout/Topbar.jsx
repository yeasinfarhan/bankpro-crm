import { useState } from 'react'
import { Menu, Search, X } from 'lucide-react'
import GlobalSearch from './GlobalSearch'
import Logo from '../ui/Logo'
import NotificationsPanel from './NotificationsPanel'
import UserMenu from './UserMenu'
import { useAuth } from '../../features/auth/AuthContext'

export default function Topbar({ onOpenMobileNav }) {
  const { user } = useAuth()
  const [mobileSearch, setMobileSearch] = useState(false)
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

      <span className="sm:hidden"><Logo /></span>
      <div className="hidden max-w-md flex-1 sm:block">
        <GlobalSearch />
      </div>

      <div className="ml-auto flex items-center gap-1 sm:gap-3">
        <span className="hidden rounded-md bg-royal-50 px-2 py-1 text-xs font-medium text-royal-700 md:inline">
          {user.branch ? user.branch.name : 'All branches'}
        </span>
        <button type="button" onClick={() => setMobileSearch(true)} aria-label="Search customers and leads"
          className="rounded-lg p-2 text-ink-muted hover:bg-gray-100 sm:hidden">
          <Search className="h-5 w-5" />
        </button>
        <NotificationsPanel />
        <UserMenu />
      </div>
      {mobileSearch && (
        <div className="absolute inset-x-0 top-0 z-40 flex h-16 items-center gap-2 border-b border-line bg-white px-3 sm:hidden">
          <GlobalSearch autoFocus onNavigate={() => setMobileSearch(false)} />
          <button type="button" onClick={() => setMobileSearch(false)} aria-label="Close search" className="rounded-lg p-2 text-ink-muted hover:bg-gray-100">
            <X className="h-5 w-5" />
          </button>
        </div>
      )}
    </header>
  )
}
