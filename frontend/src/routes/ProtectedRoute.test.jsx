import { screen } from '@testing-library/react'
import { Route, Routes, useLocation } from 'react-router'
import { describe, expect, it } from 'vitest'
import ProtectedRoute from './ProtectedRoute'
import RoleGuard from './RoleGuard'
import { renderWithAuth, USERS } from '../test/renderWithAuth'

function renderRoutes(route, auth) {
  return renderWithAuth(
    <Routes>
      <Route path="/login" element={<p>Login screen</p>} />
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<p>Dashboard home</p>} />
        <Route element={<RoleGuard roles={['ADMIN', 'BRANCH_MANAGER']} />}>
          <Route path="/team" element={<p>Team page</p>} />
        </Route>
        <Route path="/forbidden" element={<p>Forbidden screen</p>} />
      </Route>
    </Routes>,
    { route, auth },
  )
}

describe('ProtectedRoute and RoleGuard', () => {
  it('redirects anonymous visitors to the login page', () => {
    renderRoutes('/team', {})
    expect(screen.getByText('Login screen')).toBeInTheDocument()
  })

  it('remembers the page after an expired session but not after a deliberate sign-out', () => {
    const LoginProbe = () => {
      const location = useLocation()
      return <p>Login from: {location.state?.from ?? 'none'}</p>
    }
    const routes = (
      <Routes>
        <Route path="/login" element={<LoginProbe />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/customers" element={<p>Customers</p>} />
        </Route>
      </Routes>
    )
    const expired = renderWithAuth(routes, { route: '/customers?page=2', auth: { signedOut: false } })
    expect(screen.getByText('Login from: /customers?page=2')).toBeInTheDocument()
    expired.unmount()

    renderWithAuth(routes, { route: '/customers', auth: { signedOut: true } })
    expect(screen.getByText('Login from: none')).toBeInTheDocument()
  })

  it('shows a loading message while the session is being restored', () => {
    renderRoutes('/', { status: 'loading' })
    expect(screen.getByRole('status')).toHaveTextContent('Restoring your session')
  })

  it('lets a signed-in user through', () => {
    renderRoutes('/', { user: USERS.officer })
    expect(screen.getByText('Dashboard home')).toBeInTheDocument()
  })

  it('allows managers into team management', () => {
    renderRoutes('/team', { user: USERS.manager })
    expect(screen.getByText('Team page')).toBeInTheDocument()
  })

  it('sends relationship officers to the forbidden page for team management', () => {
    renderRoutes('/team', { user: USERS.officer })
    expect(screen.getByText('Forbidden screen')).toBeInTheDocument()
  })
})
