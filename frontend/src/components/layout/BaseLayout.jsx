import { Outlet } from 'react-router-dom'
import AppNavigation from './AppNavigation.jsx'

function BaseLayout() {
  return (
    <div className="app-shell page-shell">
      <div className="app-layout">
        <aside className="app-sidebar">
          <div className="app-sidebar__brand">
            <p className="app-header__eyebrow">AI Interview Platform</p>
            <h1 className="app-sidebar__title">AIMENTOR</h1>
          </div>
          <AppNavigation />
        </aside>

        <div className="app-content">
          <header className="app-header page-header">
            <div>
              <p className="app-header__eyebrow">AI Interview Platform</p>
              <h1 className="app-header__title">취업 준비를 위한 지원자료, 모의면접<br />학습을 한 곳에서 관리하세요.</h1>
              <p className="app-header__description">지원자료, 채용공고, 구독, 구매, 관리자 기능까지 한 흐름으로 연결해 취업 준비를 관리하세요.</p>
            </div>
          </header>
          <main className="app-main page-shell__content"><Outlet /></main>
        </div>
      </div>
    </div>
  )
}

export default BaseLayout
