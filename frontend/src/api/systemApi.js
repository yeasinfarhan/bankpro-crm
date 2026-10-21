import { apiClient, WAKE_UP_TIMEOUT_MS } from './client'

/** GET /api/system/info — public endpoint used to check that the backend is reachable. */
export async function fetchSystemInfo({ timeout } = {}) {
  const { data } = await apiClient.get('/system/info', timeout ? { timeout } : undefined)
  return data
}

/** Same check, but patient enough for a sleeping free-tier server to start. */
export const wakeServer = () => fetchSystemInfo({ timeout: WAKE_UP_TIMEOUT_MS })
