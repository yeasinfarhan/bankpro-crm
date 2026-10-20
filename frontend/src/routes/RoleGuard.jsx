import { Navigate, Outlet } from 'react-router'
import { useAuth } from '../features/auth/AuthContext'
import { hasAnyRole } from '../utils/roles'

/**
 * Blocks a route for roles that may not use it. This is for user experience only —
 * the backend independently rejects the same requests with 403.
 */
export default function RoleGuard({ roles }) {
  const { user } = useAuth()
  return hasAnyRole(user, roles) ? <Outlet /> : <Navigate to="/forbidden" replace />
}
