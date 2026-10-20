import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router'
import LoadingState from '../components/ui/LoadingState'
import AppShell from '../components/layout/AppShell'
import ProtectedRoute from './ProtectedRoute'
import RoleGuard from './RoleGuard'
import { rolesForPath } from './navConfig'
import LoginPage from '../features/auth/LoginPage'
import SystemStatusPage from '../features/system/SystemStatusPage'
import CustomersPage from '../features/customers/CustomersPage'
import CustomerDetailPage from '../features/customers/CustomerDetailPage'
import CustomerFormPage from '../features/customers/CustomerFormPage'
import ProductsPage from '../features/products/ProductsPage'
import ProductFormPage from '../features/products/ProductFormPage'
import LeadsPage from '../features/leads/LeadsPage'
import LeadDetailPage from '../features/leads/LeadDetailPage'
import LeadFormPage from '../features/leads/LeadFormPage'
import FollowUpsPage from '../features/followups/FollowUpsPage'
import TeamPage from '../features/team/TeamPage'
import SettingsPage from '../features/settings/SettingsPage'
import TargetsPage from '../features/targets/TargetsPage'
import ForbiddenPage from '../pages/ForbiddenPage'
import NotFoundPage from '../pages/NotFoundPage'
import { ROLES } from '../utils/roles'

// Chart-heavy pages load the charting library on demand, so the sign-in page stays small.
const DashboardPage = lazy(() => import('../features/dashboard/DashboardPage'))
const ReportsPage = lazy(() => import('../features/reports/ReportsPage'))
const withSuspense = (el) => <Suspense fallback={<LoadingState rows={6} />}>{el}</Suspense>

export default function AppRoutes() {
  return (
    <Routes>
      {/* Public */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/status" element={<SystemStatusPage />} />

      {/* Signed-in area */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route index element={withSuspense(<DashboardPage />)} />

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
          <Route path="/reports" element={withSuspense(<ReportsPage />)} />
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
