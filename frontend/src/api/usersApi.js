import { apiClient } from './client'

/** GET /api/users — team list, already scoped by the backend (ADMIN: all, manager: own branch). */
export async function fetchTeam() {
  const { data } = await apiClient.get('/users')
  return data
}

/** GET /api/users/assignable — active officers/managers who can own customers and leads (branch-scoped). */
export async function fetchAssignableStaff(branchId) {
  const { data } = await apiClient.get('/users/assignable', { params: branchId ? { branchId } : {} })
  return data
}
