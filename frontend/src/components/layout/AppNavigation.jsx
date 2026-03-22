import { useEffect, useRef, useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import authApi from '../../api/authApi.js'
import useAuthStore from '../../store/authStore.js'

const publicItems = [
  { to: '/auth/login', label: '로그인' },
  { to: '/auth/register', label: '회원가입' },
]

const privateItems = [
  { to: '/dashboard', label: '대시보드' },
  { to: '/profile-documents', label: '지원자료' },
  { to: '/job-posting', label: '채용공고' },
  { to: '/interview/setup', label: '모의면접' },
  { to: '/learning', label: '학습' },
  { to: '/books', label: '도서' },
  { to: '/cart', label: '장바구니' },
  { to: '/orders', label: '구매관리' },
]

const dropdownItems = [
  { to: '/my-page', label: '회원수정' },
  { to: '/profile-documents', label: '이력서 관리' },
  { to: '/orders', label: '구매관리' },
  { to: '/subscriptions', label: '구독관리' },
  { to: '/favorites', label: '찜목록' },
  { to: '/job-posting', label: '채용공고' },
]

function AppNavigation() {
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const clearUser = useAuthStore((state) => state.clearUser)
  const isAuthenticated = Boolean(user)
  const [isDropdownOpen, setIsDropdownOpen] = useState(false)
  const dropdownRef = useRef(null)

  useEffect(() => {
    function handleOutsideClick(event) {
      if (!dropdownRef.current?.contains(event.target)) {
        setIsDropdownOpen(false)
      }
    }

    function handleEscape(event) {
      if (event.key === 'Escape') {
        setIsDropdownOpen(false)
      }
    }

    document.addEventListener('mousedown', handleOutsideClick)
    document.addEventListener('keydown', handleEscape)
    return () => {
      document.removeEventListener('mousedown', handleOutsideClick)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [])

  async function handleLogout() {
    await authApi.logout()
    clearUser()
    setIsDropdownOpen(false)
    navigate('/auth/login', { replace: true })
  }

  const visibleItems = isAuthenticated ? privateItems : publicItems

  return (
    <nav className="app-nav" aria-label="주요 메뉴">
      <div className="app-nav__links">
        {visibleItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => (isActive ? 'app-nav__link app-nav__link--active' : 'app-nav__link')}
          >
            {item.label}
          </NavLink>
        ))}
        {user?.role === 'ADMIN' ? (
          <NavLink to="/admin" className={({ isActive }) => (isActive ? 'app-nav__link app-nav__link--active' : 'app-nav__link')}>
            관리자
          </NavLink>
        ) : null}
      </div>

      {isAuthenticated ? (
        <div ref={dropdownRef} className="app-nav__actions">
          <button
            type="button"
            className="button button--secondary app-nav__dropdown-trigger"
            onClick={() => setIsDropdownOpen((current) => !current)}
            aria-expanded={isDropdownOpen}
            aria-haspopup="menu"
          >
            {user?.name || user?.email}
          </button>
          {isDropdownOpen ? (
            <div className="app-nav__dropdown" role="menu">
              {dropdownItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  role="menuitem"
                  className="app-nav__dropdown-link"
                  onClick={() => setIsDropdownOpen(false)}
                >
                  {item.label}
                </NavLink>
              ))}
              <div className="app-nav__dropdown-section">
                <p className="app-nav__dropdown-label">찜목록</p>
                <NavLink to="/favorites?type=JOB_POSTING" role="menuitem" className="app-nav__dropdown-link" onClick={() => setIsDropdownOpen(false)}>
                  채용공고
                </NavLink>
                <NavLink to="/favorites?type=COMPANY" role="menuitem" className="app-nav__dropdown-link" onClick={() => setIsDropdownOpen(false)}>
                  관심기업
                </NavLink>
                <NavLink to="/favorites?type=BOOK" role="menuitem" className="app-nav__dropdown-link" onClick={() => setIsDropdownOpen(false)}>
                  도서관심
                </NavLink>
              </div>
              <button type="button" className="app-nav__dropdown-link app-nav__dropdown-link--button" onClick={handleLogout}>
                로그아웃
              </button>
            </div>
          ) : null}
        </div>
      ) : null}
    </nav>
  )
}

export default AppNavigation
