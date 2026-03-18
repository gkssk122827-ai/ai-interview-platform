import { NavLink, useNavigate } from 'react-router-dom'
import authApi from '../../api/authApi.js'
import { BUTTON_LABELS, NAV_TEXT } from '../../constants/messages.js'
import useAuthStore from '../../store/authStore.js'

const navigationItems = [
  { to: '/auth/login', label: NAV_TEXT.login, requiresAuth: false },
  { to: '/auth/register', label: NAV_TEXT.register, requiresAuth: false },
  { to: '/dashboard', label: NAV_TEXT.dashboard, requiresAuth: true },
  { to: '/profile-documents', label: NAV_TEXT.documents, requiresAuth: true },
  { to: '/job-posting', label: NAV_TEXT.jobPosting, requiresAuth: true },
  { to: '/interview/setup', label: NAV_TEXT.interview, requiresAuth: true },
  { to: '/learning', label: NAV_TEXT.learning, requiresAuth: true },
  { to: '/books', label: NAV_TEXT.books, requiresAuth: true },
  { to: '/cart', label: NAV_TEXT.cart, requiresAuth: true },
  { to: '/orders', label: NAV_TEXT.orders, requiresAuth: true },
  { to: '/admin', label: NAV_TEXT.admin, requiresAuth: true, roles: ['ADMIN'] },
]

function AppNavigation() {
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const clearUser = useAuthStore((state) => state.clearUser)
  const visibleItems = navigationItems.filter((item) => {
    if (item.requiresAuth !== Boolean(user)) return false
    if (!item.roles) return true
    return user ? item.roles.includes(user.role) : false
  })

  async function handleLogout() {
    await authApi.logout()
    clearUser()
    navigate('/auth/login', { replace: true })
  }

  return (
    <nav className="app-nav" aria-label={NAV_TEXT.aria}>
      {visibleItems.map((item) => (
        <NavLink key={item.to} to={item.to} className={({ isActive }) => (isActive ? 'app-nav__link app-nav__link--active' : 'app-nav__link')}>
          {item.label}
        </NavLink>
      ))}
      {user ? <button className="button button--secondary" type="button" onClick={handleLogout}>{BUTTON_LABELS.logout}</button> : null}
    </nav>
  )
}

export default AppNavigation