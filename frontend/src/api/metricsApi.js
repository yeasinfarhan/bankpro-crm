import { apiClient } from './client'
import { cleanParams } from './customersApi'

const get = async (url, params) => (await apiClient.get(url, { params: cleanParams(params ?? {}) })).data

export const fetchDashboardSummary = (p) => get('/dashboard/summary', p)
export const fetchSalesTrend = (p) => get('/dashboard/sales-trend', p)
export const fetchCustomerAcquisition = (p) => get('/dashboard/customer-acquisition', p)
export const fetchLeadFunnel = (p) => get('/dashboard/lead-funnel', p)
export const fetchProductMix = (p) => get('/dashboard/product-mix', p)
export const fetchDashboardTargets = (p) => get('/dashboard/targets', p)
export const fetchOfficerPerformance = (p) => get('/dashboard/officer-performance', p)

export const fetchTargets = (p) => get('/targets', p)
export const fetchTargetRanking = (p) => get('/targets/ranking', p)
export const createTarget = async (body) => (await apiClient.post('/targets', body)).data
export const updateTarget = async (id, targetValue) => (await apiClient.put(`/targets/${id}`, { targetValue })).data
export const deleteTarget = async (id) => apiClient.delete(`/targets/${id}`)

export const fetchReport = (type, p) => get(`/reports/${type}`, p)

/** Downloads a report CSV with the JWT attached (same approach as the customer export). */
export async function downloadReportCsv(type, params) {
  const response = await apiClient.get(`/reports/${type}/export`, { params: cleanParams(params), responseType: 'blob' })
  const filename = /filename="([^"]+)"/.exec(response.headers?.['content-disposition'] ?? '')?.[1] ?? `bankpro-${type}.csv`
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
