import { extractPayload } from './apiUtils.js'
import apiClient, {
  clearAuthSession,
  extractApiErrorMessage,
  getStoredAuthSession,
  persistAuthSession,
} from './client.js'
import { stubLogin, stubSignup } from './stubDatabase.js'

const useStubApi = import.meta.env.VITE_USE_API_STUB === 'true'

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

const authApi = {
  async login(payload) {
    try {
      if (useStubApi) {
        const result = await stubLogin(payload)
        persistAuthSession(result)
        return result
      }

      const response = await apiClient.post('/auth/login', payload)
      const result = normalizeAuthPayload(extractPayload(response))
      persistAuthSession(result)
      return result
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '로그인에 실패했습니다.'))
    }
  },

  async signup(payload) {
    const requestPayload = {
      name: payload.name,
      email: payload.email,
      phone: payload.phone,
      password: payload.password,
    }

    try {
      if (useStubApi) {
        const result = await stubSignup(requestPayload)
        persistAuthSession(result)
        return result
      }

      const response = await apiClient.post('/auth/signup', requestPayload)
      const result = normalizeAuthPayload(extractPayload(response))
      persistAuthSession(result)
      return result
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '회원가입에 실패했습니다.'))
    }
  },

  async logout() {
    const session = getStoredAuthSession()

    if (!useStubApi && session?.refreshToken) {
      try {
        await apiClient.post('/auth/logout', {
          refreshToken: session.refreshToken,
        })
      } catch {
      }
    }

    clearAuthSession()
  },
}

export default authApi
