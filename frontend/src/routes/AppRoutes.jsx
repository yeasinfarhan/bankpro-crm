import { lazy } from 'react'
import { Route, Routes } from 'react-router'
import AppShell from '../components/layout/AppShell'
import ProtectedRoute from './ProtectedRoute'
import RoleGuard from './RoleGuard'
import { rolesForPath } from './navConfig'
import LoginPage from '../features/auth/LoginPage'
import SystemStatusPage from '../features/system/SystemStatusPage'
import ForbiddenPage from '../pages/ForbiddenPage'
import NotFoundPage from '../pages/NotFoundPage'
import { ROLES } from '../utils/roles'

// Signed-in pages are code-split: each one downloads on first visit (see AppShell's Suspense).
const CustomersPage = lazy(() => import('../features/customers/CustomersPage'))
const CustomerDetailPage = lazy(() => import('../features/customers/CustomerDetailPage'))
const CustomerFormPage = lazy(() => import('../features/customers/CustomerFormPage'))
const ProductsPage = lazy(() => import('../features/products/ProductsPage'))
const ProductFormPage = lazy(() => import('../features/products/ProductFormPage'))
const LeadsPage = lazy(() => import('../features/leads/LeadsPage'))
const LeadDetailPage = lazy(() => import('../features/leads/LeadDetailPage'))
const LeadFormPage = lazy(() => import('../features/leads/LeadFormPage'))
const FollowUpsPage = lazy(() => import('../features/followups/FollowUpsPage'))
const TeamPage = lazy(() => import('../features/team/TeamPage'))
const SettingsPage = lazy(() => import('../features/settings/SettingsPage'))
const TargetsPage = lazy(() => import('../features/targets/TargetsPage'))
const DashboardPage = lazy(() => import('../features/dashboard/DashboardPage'))
const ReportsPage = lazy(() => import('../features/reports/ReportsPage'))

export default function AppRoutes() {
  return (
    <Routes>
      {/* Public */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/status" element={<SystemStatusPage />} />

      {/* Signed-in area */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route index element={<DashboardPage />} />

          <Route path="/customers" element={<CustomersPage />} />
          <Route path="/customers/:id" element={<CustomerDetailPage />} />
          <Route element={<RoleGuard roles={[ROLES.BRANCH_MANAGER, ROLES.RELATIONSHIP_OFFICER]} />}>
            <Route path="/customers/new" element={<CustomerFormPage />} />
            <Route path="/customers/:id/edit" element={<CustomerFormPage />} />
          </Route>

          <Route path="/products" element={<ProductsPage />} />
          <Route element={<RoleGuard roles={[ROLES.ADMIN]} />}>
            <Route path="/products/new" element={<ProductFormPage />} />
            <Route path="/products/:id/edit" element={<ProductFormPage />} />
          </Route>

          <Route path="/leads" element={<LeadsPage />} />
          <Route element={<RoleGuard roles={[ROLES.BRANCH_MANAGER, ROLES.RELATIONSHIP_OFFICER]} />}>
            <Route path="/leads/new" element={<LeadFormPage />} />
            <Route path="/leads/:id/edit" element={<LeadFormPage />} />
          </Route>
          <Route path="/leads/:id" element={<LeadDetailPage />} />
          <Route path="/follow-ups" element={<FollowUpsPage />} />

          <Route path="/targets" element={<TargetsPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route element={<RoleGuard roles={rolesForPath('/team')} />}>
            <Route path="/team" element={<TeamPage />} />
          </Route>
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/forbidden" element={<ForbiddenPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
