import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { AuthContext } from './AuthContext'
import { clearSession, loadSession, saveSession } from './session'
import { fetchCurrentUser, login as loginRequest } from '../../api/authApi'
import { onUnauthorized, setAccessToken } from '../../api/client'

const MAX_TIMER_MS = 2_147_483_647 // setTimeout limit

/**
 * Holds the signed-in user for the whole app.
 * - On start-up it restores a saved session and confirms it with GET /api/auth/me.
 * - It logs out automatically when the token expires or any API call returns 401.
 */
export default function AuthProvider({ children }) {
  const [state, setState] = useState(() => ({
    status: loadSession() ? 'loading' : 'anonymous',
    user: null,
    notice: null,
    signedOut: false,
  }))
  const expiryTimer = useRef(null)

  /**
   * @param notice    message for the login page
   * @param signedOut true for a deliberate "Sign out": the current page is then NOT remembered for the
   *                  next sign-in (an expired session does remember it, so the user can continue)
   */
  const logout = useCallback((notice = null, signedOut = false) => {
    clearTimeout(expiryTimer.current)
    clearSession()
    setAccessToken(null)
    setState({ status: 'anonymous', user: null, notice, signedOut })
  }, [])

  const scheduleExpiry = useCallback(
    (expiresAt) => {
      clearTimeout(expiryTimer.current)
      const ms = Math.min(new Date(expiresAt).getTime() - Date.now(), MAX_TIMER_MS)
      expiryTimer.current = setTimeout(() => logout('Your session has expired. Please sign in again.'), Math.max(ms, 0))
    },
    [logout],
  )

  // Restore a saved session once on start-up.
  useEffect(() => {
    const saved = loadSession()
    if (!saved) return undefined
    let active = true
    setAccessToken(saved.token)
    fetchCurrentUser().then(
      (user) => {
        if (!active) return
        scheduleExpiry(saved.expiresAt)
        setState({ status: 'authenticated', user, notice: null, signedOut: false })
      },
      (error) => active && logout(error?.response?.status === 401
        ? 'Your session has expired. Please sign in again.'
        : 'Could not reach the server to restore your session. Please sign in again.'),
    )
    return () => {
      active = false
    }
  }, [logout, scheduleExpiry])

  // Any 401 from the API (expired/revoked token) signs the user out.
  useEffect(() => {
    onUnauthorized(() => logout('Your session has expired. Please sign in again.'))
    return () => onUnauthorized(null)
  }, [logout])

  useEffect(() => () => clearTimeout(expiryTimer.current), [])

  const login = useCallback(
    async (email, password) => {
      const result = await loginRequest(email, password)
      setAccessToken(result.accessToken)
      saveSession(result.accessToken, result.expiresAt)
      scheduleExpiry(result.expiresAt)
      setState({ status: 'authenticated', user: result.user, notice: null, signedOut: false })
      return result.user
    },
    [scheduleExpiry],
  )

  const value = useMemo(() => ({ ...state, login, logout }), [state, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
