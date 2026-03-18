import axios from 'axios'
import { ERROR_MESSAGES } from '../constants/messages.js'
import useAuthStore from '../store/authStore.js'

const AUTH_STORAGE_KEY = 'aimentor.auth'
const AUTH_NOTICE_KEY = 'aimentor.auth.notice'
const API_BASE_URL = '/api/v1'

function readStoredAuth() {
  if (typeof window === 'undefined') return null
  const storedValue = window.localStorage.getItem(AUTH_STORAGE_KEY)
  if (!storedValue) return null

  try {
    return JSON.parse(storedValue)
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}

function extractPayload(response) {
  return response?.data?.data ?? response?.data ?? null
}

function isAuthEndpoint(url) {
  return typeof url === 'string' && url.includes('/auth/')
}

function normalizeAuthPayload(payload) {
  return {
    user: {
      id: payload.user?.id ?? payload.userId,
      name: payload.user?.name ?? payload.name ?? payload.email,
      email: payload.user?.email ?? payload.email,
      role: payload.user?.role ?? payload.role,
    },
    accessToken: payload.accessToken,
    accessTokenExpiresAt: payload.accessTokenExpiresAt ?? null,
    refreshToken: payload.refreshToken,
    refreshTokenExpiresAt: payload.refreshTokenExpiresAt ?? null,
  }
}

function syncAuthState(session) {
  useAuthStore.getState().hydrateAuth(session)
}

function sanitizeMessage(message) {
  if (!message) return ''

  const trimmed = String(message).trim()
  if (!trimmed) return ''

  const lowered = trimmed.toLowerCase()
  if (
    lowered.includes('sql') ||
    lowered.includes('hibernate') ||
    lowered.includes('stack trace') ||
    lowered.includes('exception') ||
    lowered.includes('constraint') ||
    lowered.includes('jdbc') ||
    lowered.includes('could not execute statement')
  ) {
    return ''
  }

  return trimmed
}

function mapStatusMessage(status, fallbackMessage) {
  if (status === 400) return fallbackMessage || ERROR_MESSAGES.badRequest
  if (status === 401) return ERROR_MESSAGES.sessionExpired
  if (status === 403) return ERROR_MESSAGES.forbidden
  if (status === 404) return ERROR_MESSAGES.notFound
  if (status >= 500) return fallbackMessage || ERROR_MESSAGES.server
  return fallbackMessage || ERROR_MESSAGES.generic
}

export function extractApiErrorMessage(error, fallbackMessage) {
  if (error?.code === 'ECONNABORTED') {
    return ERROR_MESSAGES.network
  }

  const status = error?.response?.status
  const apiMessage = sanitizeMessage(
    error?.response?.data?.error?.message ??
      error?.response?.data?.detail?.message ??
      error?.response?.data?.message,
  )

  if (!status) {
    return apiMessage || fallbackMessage || ERROR_MESSAGES.network
  }

  if (status === 401) {
    return ERROR_MESSAGES.sessionExpired
  }

  if (status === 403 || status === 404 || status >= 500) {
    return mapStatusMessage(status, fallbackMessage)
  }

  return apiMessage || mapStatusMessage(status, fallbackMessage)
}

export function setAuthNotice(message) {
  if (typeof window === 'undefined') return
  window.sessionStorage.setItem(AUTH_NOTICE_KEY, message)
}

export function consumeAuthNotice() {
  if (typeof window === 'undefined') return ''
  const message = window.sessionStorage.getItem(AUTH_NOTICE_KEY) ?? ''
  window.sessionStorage.removeItem(AUTH_NOTICE_KEY)
  return message
}

function handleSessionExpired() {
  clearAuthSession()
  useAuthStore.getState().clearUser()
  setAuthNotice(ERROR_MESSAGES.sessionExpired)

  if (typeof window !== 'undefined' && !['/login', '/auth/login'].includes(window.location.pathname)) {
    window.location.replace('/auth/login')
  }
}

export function getStoredAuthSession() {
  return readStoredAuth()
}

export function persistAuthSession(session) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session))
  syncAuthState(session)
}

export function clearAuthSession() {
  if (typeof window === 'undefined') return
  window.localStorage.removeItem(AUTH_STORAGE_KEY)
}

const apiClient = axios.create({ baseURL: API_BASE_URL, timeout: 10000, headers: { 'Content-Type': 'application/json' } })
const authClient = axios.create({ baseURL: API_BASE_URL, timeout: 10000, headers: { 'Content-Type': 'application/json' } })

let refreshRequestPromise = null

async function refreshAccessToken() {
  const session = readStoredAuth()
  if (!session?.refreshToken) throw new Error(ERROR_MESSAGES.missingRefreshToken)
  const response = await authClient.post('/auth/refresh', { refreshToken: session.refreshToken })
  const nextSession = normalizeAuthPayload(extractPayload(response))
  persistAuthSession(nextSession)
  return nextSession
}

apiClient.interceptors.request.use((config) => {
  const session = readStoredAuth()
  config.headers = config.headers ?? {}

  if (isAuthEndpoint(config.url)) {
    delete config.headers.Authorization
    return config
  }

  if (session?.accessToken) {
    config.headers.Authorization = 'Bearer ' + session.accessToken
  } else {
    delete config.headers.Authorization
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    const status = error.response?.status
    const isAuthRequest = originalRequest?.url?.includes('/auth/')

    if (status !== 401 || !originalRequest || originalRequest._retry || isAuthRequest) {
      return Promise.reject(error)
    }

    try {
      originalRequest._retry = true
      if (!refreshRequestPromise) {
        refreshRequestPromise = refreshAccessToken().finally(() => {
          refreshRequestPromise = null
        })
      }

      const refreshedSession = await refreshRequestPromise
      originalRequest.headers = originalRequest.headers ?? {}
      originalRequest.headers.Authorization = 'Bearer ' + refreshedSession.accessToken
      return apiClient(originalRequest)
    } catch (refreshError) {
      handleSessionExpired()
      return Promise.reject(refreshError)
    }
  },
)

export default apiClient
