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
      phone: payload.user?.phone ?? payload.phone ?? '',
      role: payload.user?.role ?? payload.role,
      status: payload.user?.status ?? payload.status ?? 'ACTIVE',
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

  async getMe() {
    try {
      const response = await apiClient.get('/auth/me')
      return extractPayload(response)
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '내 정보를 불러오는 중 오류가 발생했습니다.'))
    }
  },

  async updateMe(payload) {
    try {
      const response = await apiClient.put('/auth/me', payload)
      const result = extractPayload(response)
      const session = getStoredAuthSession()
      if (session) {
        persistAuthSession({
          ...session,
          user: {
            ...session.user,
            name: result.name,
            email: result.email,
            phone: result.phone,
            role: result.role,
            status: result.status,
          },
        })
      }
      return result
    } catch (error) {
      throw new Error(extractApiErrorMessage(error, '회원 정보를 수정하는 중 오류가 발생했습니다.'))
    }
  },
}

export default authApi
