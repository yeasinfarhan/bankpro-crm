import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { vi } from 'vitest'
import { AuthContext } from '../features/auth/AuthContext'
import { ToastContext } from '../components/feedback/ToastContext'

export const USERS = {
  admin: { id: 1, email: 'admin@bankpro.demo', fullName: 'Ayesha Rahman', role: 'ADMIN', jobTitle: 'System Administrator', branch: null, active: true },
  manager: { id: 2, email: 'manager.motijheel@bankpro.demo', fullName: 'Kamal Hossain', role: 'BRANCH_MANAGER', jobTitle: 'Branch Manager', branch: { id: 1, code: 'DHK-MTJ', name: 'Motijheel Branch', city: 'Dhaka' }, active: true },
  officer: { id: 3, email: 'nusrat.jahan@bankpro.demo', fullName: 'Nusrat Jahan', role: 'RELATIONSHIP_OFFICER', jobTitle: 'Relationship Officer', branch: { id: 1, code: 'DHK-MTJ', name: 'Motijheel Branch', city: 'Dhaka' }, active: true },
}

/** Renders `ui` inside a router at `route` with a fake auth context. */
export function renderWithAuth(ui, { route = '/', auth = {} } = {}) {
  const value = {
    status: auth.user ? 'authenticated' : 'anonymous',
    user: null,
    notice: null,
    login: vi.fn(),
    logout: vi.fn(),
    ...auth,
  }
  const toast = { notify: vi.fn() }
  const result = render(
    <ToastContext.Provider value={toast}>
      <AuthContext.Provider value={value}>
        <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
      </AuthContext.Provider>
    </ToastContext.Provider>,
  )
  return { ...result, auth: value, toast }
}
