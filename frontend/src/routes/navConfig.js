import {
  BarChart3,
  CalendarClock,
  Landmark,
  LayoutDashboard,
  Settings,
  Target,
  UserCog,
  Users,
  Workflow,
} from 'lucide-react'
import { hasAnyRole, ROLES } from '../utils/roles'

const { ADMIN, BRANCH_MANAGER } = ROLES

/**
 * Single source of truth for navigation. `roles` (if set) limits who sees the item AND who may open
 * the route (enforced by RoleGuard). The backend enforces the same rules independently.
 */
export const NAV_ITEMS = [
  { path: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { path: '/customers', label: 'Customers', icon: Users },
  { path: '/leads', label: 'Leads & Opportunities', icon: Workflow },
  { path: '/follow-ups', label: 'Follow-ups', icon: CalendarClock },
  { path: '/products', label: 'Banking Products', icon: Landmark },
  { path: '/targets', label: 'Sales Targets', icon: Target },
  { path: '/reports', label: 'Performance Reports', icon: BarChart3 },
  { path: '/team', label: 'Team Management', icon: UserCog, roles: [ADMIN, BRANCH_MANAGER] },
  { path: '/settings', label: 'Settings', icon: Settings },
]

export function navItemsFor(user) {
  return NAV_ITEMS.filter((item) => hasAnyRole(user, item.roles))
}

export function rolesForPath(path) {
  return NAV_ITEMS.find((item) => item.path === path)?.roles
}

/**
 * Where to go after login: the remembered page if the signed-in user may open it, otherwise the dashboard.
 * Only internal paths are accepted, so a crafted link cannot redirect to another site.
 */
export function safeRedirectFor(user, from) {
  if (typeof from !== 'string' || !from.startsWith('/') || from.startsWith('//')) return '/'
  const section = '/' + (from.split(/[?#]/)[0].split('/')[1] ?? '')
  return hasAnyRole(user, rolesForPath(section)) ? from : '/'
}
