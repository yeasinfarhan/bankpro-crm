import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiClient, getErrorMessage, onUnauthorized, setAccessToken } from './client'

/** Replaces the HTTP layer so no real request is made. */
function respondWith(status, data = {}) {
  apiClient.defaults.adapter = vi.fn(async (config) => {
    const response = { status, data, headers: {}, config, statusText: String(status) }
    if (status >= 400) {
      const error = new Error(`HTTP ${status}`)
      error.response = response
      error.config = config
      throw error
    }
    return response
  })
  return apiClient.defaults.adapter
}

afterEach(() => {
  setAccessToken(null)
  onUnauthorized(null)
})

describe('apiClient', () => {
  it('attaches the bearer token to requests', async () => {
    const adapter = respondWith(200, { ok: true })
    setAccessToken('abc.def.ghi')
    await apiClient.get('/auth/me')
    expect(adapter.mock.calls[0][0].headers.Authorization).toBe('Bearer abc.def.ghi')
  })

  it('calls the unauthorized handler on 401 from a protected call, but not from login', async () => {
    const handler = vi.fn()
    onUnauthorized(handler)

    respondWith(401, { message: 'expired' })
    await expect(apiClient.get('/auth/me')).rejects.toBeTruthy()
    expect(handler).toHaveBeenCalledTimes(1)

    await expect(apiClient.post('/auth/login', {})).rejects.toBeTruthy()
    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('maps errors to safe messages', () => {
    expect(getErrorMessage({ response: { data: { message: 'Nope.' } } })).toBe('Nope.')
    expect(getErrorMessage({ request: {} })).toBe('Cannot reach the BankPro server.')
    expect(getErrorMessage({})).toBe('Something went wrong. Please try again.')
  })
})
