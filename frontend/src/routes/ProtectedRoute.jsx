import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../features/auth/AuthContext'

/** Only signed-in users get past this point; everyone else is sent to /login (and back afterwards). */
export default function ProtectedRoute() {
  const { status, signedOut } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return (
      <div role="status" className="flex min-h-screen items-center justify-center text-sm text-ink-muted">
        Restoring your session…
      </div>
    )
  }
  if (status !== 'authenticated') {
    // Remember the page only when the session ended on its own (expiry), not after a deliberate sign-out.
    const state = signedOut ? undefined : { from: location.pathname + location.search }
    return <Navigate to="/login" replace state={state} />
  }
  return <Outlet />
}
