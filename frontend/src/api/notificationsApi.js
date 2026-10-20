import { apiClient } from './client'

export const fetchNotifications = async () => (await apiClient.get('/notifications', { params: { limit: 15 } })).data
export const markNotificationRead = async (id) => apiClient.patch(`/notifications/${id}/read`)
export const markAllNotificationsRead = async () => apiClient.post('/notifications/read-all')
