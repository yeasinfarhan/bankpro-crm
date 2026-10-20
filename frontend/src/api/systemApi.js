import { apiClient } from './client'

/** GET /api/system/info — public endpoint used to check that the backend is reachable. */
export async function fetchSystemInfo() {
  const { data } = await apiClient.get('/system/info')
  return data
}
