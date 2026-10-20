import { useCallback, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { ChevronDown, LogOut, Settings } from 'lucide-react'
import { useAuth } from '../../features/auth/AuthContext'
import { initials, ROLE_LABELS } from '../../utils/roles'
import useDismiss from './useDismiss'

export default function UserMenu() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const ref = useRef(null)
  const close = useCallback(() => setOpen(false), [])
  useDismiss(ref, open, close)

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="flex items-center gap-2 rounded-lg p-1.5 hover:bg-gray-100"
        aria-label="Account menu"
        aria-expanded={open}
      >
        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-royal-600 text-xs font-semibold text-white">
          {initials(user.fullName)}
        </span>
        <span className="hidden text-left md:block">
          <span className="block text-sm font-medium leading-tight text-ink">{user.fullName}</span>
          <span className="block text-xs leading-tight text-ink-muted">{user.jobTitle ?? ROLE_LABELS[user.role]}</span>
        </span>
        <ChevronDown className="hidden h-4 w-4 text-ink-muted md:block" aria-hidden="true" />
      </button>
      {open && (
        <div className="absolute right-0 z-30 mt-2 w-64 rounded-xl border border-line bg-white py-1 shadow-lg">
          <div className="border-b border-line px-4 py-3">
            <p className="truncate text-sm font-medium">{user.email}</p>
            <p className="text-xs text-ink-muted">
              {ROLE_LABELS[user.role]}
              {user.branch ? ` · ${user.branch.name}` : ' · All branches'}
            </p>
          </div>
          <Link to="/settings" onClick={close} className="flex items-center gap-2 px-4 py-2 text-sm hover:bg-gray-50">
            <Settings className="h-4 w-4 text-ink-muted" aria-hidden="true" /> Settings
          </Link>
          <button
            type="button"
            onClick={() => {
              logout('You have been signed out.', true)
              navigate('/login', { replace: true })
            }}
            className="flex w-full items-center gap-2 px-4 py-2 text-left text-sm text-danger-600 hover:bg-danger-50"
          >
            <LogOut className="h-4 w-4" aria-hidden="true" /> Sign out
          </button>
        </div>
      )}
    </div>
  )
}
