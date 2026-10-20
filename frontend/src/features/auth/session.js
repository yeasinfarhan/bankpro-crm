/**
 * Session persistence (see DECISIONS.md D-05).
 * The token is kept in memory by the API client and mirrored to sessionStorage so a page refresh
 * keeps you signed in. sessionStorage is cleared when the tab closes and is never shared across tabs.
 */
const KEY = 'bankpro.session'

export function saveSession(token, expiresAt) {
  try {
    sessionStorage.setItem(KEY, JSON.stringify({ token, expiresAt }))
  } catch {
    // storage unavailable (private mode): the session simply won't survive a refresh
  }
}

export function loadSession(now = Date.now()) {
  try {
    const raw = sessionStorage.getItem(KEY)
    if (!raw) return null
    const session = JSON.parse(raw)
    if (!session?.token || !session?.expiresAt || new Date(session.expiresAt).getTime() <= now) {
      clearSession()
      return null
    }
    return session
  } catch {
    return null
  }
}

export function clearSession() {
  try {
    sessionStorage.removeItem(KEY)
  } catch {
    // ignore
  }
}
