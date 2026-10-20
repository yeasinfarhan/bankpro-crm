import { NavLink } from 'react-router'
import { ChevronsLeft, ChevronsRight, X } from 'lucide-react'
import Logo from '../ui/Logo'
import { navItemsFor } from '../../routes/navConfig'
import { useAuth } from '../../features/auth/AuthContext'

function NavItems({ collapsed, onNavigate }) {
  const { user } = useAuth()
  return (
    <ul className="space-y-1">
      {navItemsFor(user).map(({ path, label, icon: Icon, end }) => (
        <li key={path}>
          <NavLink
            to={path}
            end={end}
            onClick={onNavigate}
            title={collapsed ? label : undefined}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                isActive ? 'bg-white/10 text-white' : 'text-blue-100/80 hover:bg-white/5 hover:text-white'
              } ${collapsed ? 'justify-center' : ''}`
            }
          >
            <Icon className="h-5 w-5 shrink-0" aria-hidden="true" />
            <span className={collapsed ? 'sr-only' : ''}>{label}</span>
          </NavLink>
        </li>
      ))}
    </ul>
  )
}

/** Desktop: collapsible rail. Mobile: slide-in drawer controlled by the top bar's menu button. */
export default function Sidebar({ collapsed, onToggleCollapsed, mobileOpen, onCloseMobile }) {
  return (
    <>
      <aside
        className={`hidden shrink-0 flex-col bg-navy-900 transition-[width] duration-200 lg:flex ${
          collapsed ? 'w-[4.5rem]' : 'w-64'
        }`}
      >
        <div className={`flex h-16 items-center border-b border-white/10 ${collapsed ? 'justify-center' : 'px-5'}`}>
          {collapsed ? <span className="sr-only">BankPro</span> : <Logo inverted />}
        </div>
        <nav aria-label="Main navigation" className="flex-1 overflow-y-auto p-3">
          <NavItems collapsed={collapsed} />
        </nav>
        <button
          type="button"
          onClick={onToggleCollapsed}
          className="flex items-center gap-2 border-t border-white/10 px-5 py-3 text-xs text-blue-100/70 hover:text-white"
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? <ChevronsRight className="mx-auto h-4 w-4" /> : <ChevronsLeft className="h-4 w-4" />}
          {!collapsed && 'Collapse'}
        </button>
      </aside>

      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true" aria-label="Navigation">
          <div className="absolute inset-0 bg-navy-950/50" onClick={onCloseMobile} aria-hidden="true" />
          <aside className="absolute inset-y-0 left-0 flex w-72 flex-col bg-navy-900">
            <div className="flex h-16 items-center justify-between border-b border-white/10 px-5">
              <Logo inverted />
              <button type="button" onClick={onCloseMobile} className="text-blue-100 hover:text-white" aria-label="Close menu">
                <X className="h-5 w-5" />
              </button>
            </div>
            <nav aria-label="Main navigation" className="flex-1 overflow-y-auto p-3">
              <NavItems collapsed={false} onNavigate={onCloseMobile} />
            </nav>
          </aside>
        </div>
      )}
    </>
  )
}
