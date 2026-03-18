import { Navigate, Outlet, useLocation } from 'react-router-dom'
import LoadingBlock from '../components/common/LoadingBlock.jsx'
import useAuthStore from '../store/authStore.js'

function GuardFallback() {
  return (
    <section className="workspace-page">
      <LoadingBlock label="인증 상태를 확인하는 중입니다." />
    </section>
  )
}

export function ProtectedRoute() {
  const location = useLocation()
  const authHydrated = useAuthStore((state) => state.authHydrated)
  const user = useAuthStore((state) => state.user)

  if (!authHydrated) return <GuardFallback />
  if (!user) return <Navigate to="/auth/login" replace state={{ from: location }} />
  return <Outlet />
}

export function PublicOnlyRoute() {
  const authHydrated = useAuthStore((state) => state.authHydrated)
  const user = useAuthStore((state) => state.user)

  if (!authHydrated) return <GuardFallback />
  if (user) return <Navigate to="/dashboard" replace />
  return <Outlet />
}

export function IndexRedirect() {
  const authHydrated = useAuthStore((state) => state.authHydrated)
  const user = useAuthStore((state) => state.user)

  if (!authHydrated) return <GuardFallback />
  return <Navigate to={user ? '/dashboard' : '/auth/login'} replace />
}

export function RoleRoute({ allowedRoles }) {
  const location = useLocation()
  const authHydrated = useAuthStore((state) => state.authHydrated)
  const user = useAuthStore((state) => state.user)

  if (!authHydrated) return <GuardFallback />
  if (!user) return <Navigate to="/auth/login" replace state={{ from: location }} />
  if (!allowedRoles.includes(user.role)) return <Navigate to="/dashboard" replace />
  return <Outlet />
}
