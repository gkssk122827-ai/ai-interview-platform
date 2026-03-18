import { useEffect } from 'react'
import { getStoredAuthSession } from '../api/client.js'
import useAuthStore from '../store/authStore.js'

function AuthBootstrap({ children }) {
  const authHydrated = useAuthStore((state) => state.authHydrated)
  const hydrateAuth = useAuthStore((state) => state.hydrateAuth)

  useEffect(() => {
    if (authHydrated) {
      return
    }

    hydrateAuth(getStoredAuthSession())
  }, [authHydrated, hydrateAuth])

  return children
}

export default AuthBootstrap
