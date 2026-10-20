import { apiClient } from './client'
import { cleanParams } from './customersApi'

export const fetchFollowUps = async (params) => (await apiClient.get('/follow-ups', { params: cleanParams(params) })).data
export const fetchFollowUpCounts = async () => (await apiClient.get('/follow-ups/counts')).data
export const createFollowUp = async (body) => (await apiClient.post('/follow-ups', body)).data
export const completeFollowUp = async (id, outcome, notes) =>
  (await apiClient.patch(`/follow-ups/${id}/complete`, { outcome, notes })).data
export const rescheduleFollowUp = async (id, scheduledAt) =>
  (await apiClient.patch(`/follow-ups/${id}/reschedule`, { scheduledAt })).data

export const fetchTimeline = async (customerId) => (await apiClient.get(`/customers/${customerId}/timeline`)).data
export const recordInteraction = async (customerId, body) =>
  (await apiClient.post(`/customers/${customerId}/interactions`, body)).data
