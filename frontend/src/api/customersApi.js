import { apiClient } from './client'

/** Drops empty filters so the URL stays clean and the backend sees "no filter". */
export function cleanParams(params) {
  return Object.fromEntries(Object.entries(params).filter(([, v]) => v !== '' && v !== null && v !== undefined && v !== false))
}

/** GET /api/customers → { content, page, size, totalElements, totalPages } */
export async function fetchCustomers(params) {
  const { data } = await apiClient.get('/customers', { params: cleanParams(params) })
  return data
}

export async function fetchCustomer(id) {
  const { data } = await apiClient.get(`/customers/${id}`)
  return data
}

export async function createCustomer(body) {
  const { data } = await apiClient.post('/customers', body)
  return data
}

export async function updateCustomer(id, body) {
  const { data } = await apiClient.put(`/customers/${id}`, body)
  return data
}

export async function archiveCustomer(id) {
  await apiClient.delete(`/customers/${id}`)
}

export async function restoreCustomer(id) {
  const { data } = await apiClient.post(`/customers/${id}/restore`)
  return data
}

/** Downloads the CSV through axios (so the JWT header is sent) and saves it via a temporary link. */
export async function downloadCustomersCsv(params) {
  const response = await apiClient.get('/customers/export', { params: cleanParams(params), responseType: 'blob' })
  const disposition = response.headers?.['content-disposition'] ?? ''
  const filename = /filename="([^"]+)"/.exec(disposition)?.[1] ?? 'bankpro-customers.csv'
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
