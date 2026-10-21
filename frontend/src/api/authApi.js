import { apiClient, WAKE_UP_TIMEOUT_MS } from './client'

/** POST /api/auth/login → { accessToken, tokenType, expiresIn, expiresAt, user } */
export async function login(email, password) {
  const { data } = await apiClient.post('/auth/login', { email, password }, { timeout: WAKE_UP_TIMEOUT_MS })
  return data
}

/** GET /api/auth/me → current user's profile */
export async function fetchCurrentUser() {
  const { data } = await apiClient.get('/auth/me', { timeout: WAKE_UP_TIMEOUT_MS })
  return data
}
