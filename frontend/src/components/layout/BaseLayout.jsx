import { Outlet } from 'react-router-dom'
import AppNavigation from './AppNavigation.jsx'

function BaseLayout() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <div>
          <p className="app-header__eyebrow">AI Interview Platform</p>
          <h1 className="app-header__title">취업 준비를 위한 지원자료, 모의면접, 학습을 한 곳에서 관리하세요.</h1>
          <p className="app-header__description">지원자료와 채용공고를 바탕으로 면접을 준비하고, 학습과 도서 기능까지 하나의 흐름으로 이용할 수 있습니다.</p>
        </div>
        <AppNavigation />
      </header>
      <main className="app-main"><Outlet /></main>
    </div>
  )
}

export default BaseLayout
