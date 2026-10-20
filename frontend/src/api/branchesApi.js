import { apiClient } from './client'

export const fetchBranches = async () => (await apiClient.get('/branches')).data
