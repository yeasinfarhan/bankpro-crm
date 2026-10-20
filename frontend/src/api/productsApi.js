import { apiClient } from './client'

export async function fetchProducts(params = {}) {
  const { data } = await apiClient.get('/products', { params: clean(params) })
  return data
}

export async function fetchProduct(id) {
  const { data } = await apiClient.get(`/products/${id}`)
  return data
}

export async function createProduct(body) {
  const { data } = await apiClient.post('/products', body)
  return data
}

export async function updateProduct(id, body) {
  const { data } = await apiClient.put(`/products/${id}`, body)
  return data
}

function clean(params) {
  return Object.fromEntries(Object.entries(params).filter(([, v]) => v !== '' && v !== null && v !== undefined))
}
