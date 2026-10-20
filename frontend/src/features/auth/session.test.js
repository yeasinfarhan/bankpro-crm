import { beforeEach, describe, expect, it } from 'vitest'
import { clearSession, loadSession, saveSession } from './session'

describe('session storage', () => {
  beforeEach(() => sessionStorage.clear())

  it('restores a session that has not expired', () => {
    saveSession('tok', '2026-10-19T04:00:00Z')
    expect(loadSession(Date.parse('2026-10-19T03:00:00Z'))).toEqual({ token: 'tok', expiresAt: '2026-10-19T04:00:00Z' })
  })

  it('discards an expired session', () => {
    saveSession('tok', '2026-10-19T04:00:00Z')
    expect(loadSession(Date.parse('2026-10-19T04:00:01Z'))).toBeNull()
    expect(sessionStorage.getItem('bankpro.session')).toBeNull()
  })

  it('clears the session on logout', () => {
    saveSession('tok', '2099-01-01T00:00:00Z')
    clearSession()
    expect(loadSession()).toBeNull()
  })
})
