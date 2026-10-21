import axios from 'axios'

/**
 * One shared axios instance for the whole app.
 * - baseURL: '/api' in development (Vite proxies it), the full backend URL in production.
 * - The request interceptor attaches the JWT; the response interceptor reports 401s so the
 *   app can log out when a token expires or is revoked.
 */
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

/**
 * Free hosting stops the backend when nobody uses it; the first request then waits while it starts
 * (about a minute). Calls that can be the first of a visit (sign-in, session restore, the wake-up check)
 * use this longer timeout.
 */
export const WAKE_UP_TIMEOUT_MS = 90_000

export const apiClient = axios.create({
  baseURL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

let accessToken = null
let unauthorizedHandler = null

/** Called by AuthProvider after login / logout. The token lives in memory here. */
export function setAccessToken(token) {
  accessToken = token
}

/** AuthProvider registers a callback that logs the user out when the API answers 401. */
export function onUnauthorized(handler) {
  unauthorizedHandler = handler
}

apiClient.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLoginCall = error?.config?.url?.includes('/auth/login')
    if (error?.response?.status === 401 && !isLoginCall && unauthorizedHandler) {
      unauthorizedHandler()
    }
    return Promise.reject(error)
  },
)

/** Turns an axios error into a short, safe message for the UI. */
export function getErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  if (error?.response?.data?.message) return error.response.data.message
  if (error?.code === 'ECONNABORTED') return 'The server took too long to respond.'
  if (error?.request && !error?.response) return 'Cannot reach the BankPro server.'
  return fallback
}

/** Field-level validation errors from the backend as { fieldName: message }. */
export function getFieldErrors(error) {
  const list = error?.response?.data?.fieldErrors ?? []
  return Object.fromEntries(list.map((f) => [f.field, f.message]))
}
