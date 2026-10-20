import { createContext, useContext } from 'react'

/**
 * Shape: { status: 'loading' | 'authenticated' | 'anonymous', user, notice, login(), logout() }
 * Kept in its own file so tests can provide a fake value without the real provider.
 */
export const AuthContext = createContext(null)

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
