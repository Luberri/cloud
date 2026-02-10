import { useState } from 'react'
import VisitorSummaryPage from './pages/VisitorSummaryPage'
import MapPage from './pages/MapPage'
import LoginPage from './pages/LoginPage'
import HomePage from './pages/HomePage'
import BlockedUsersPage from './pages/BlockedUsersPage'
import AllUsersPage from './pages/AllUsersPage'
import AddUserPage from './pages/AddUserPage'
import IssuesPage from './pages/IssuesPage'
import StatisticsPage from './pages/StatisticsPage'
import './styles.css'
import PrixForfaitairePage from './pages/PrixForfaitairePage'

type PageType = 'visitor' | 'map' | 'login' | 'home' | 'blocked-users' | 'all-users' | 'add-user' | 'issues' | 'summary' | 'statistics' | 'prix-forfaitaire'

export default function App() {
  // Check URL path for initial page
  const getInitialPage = (): PageType => {
    const path = window.location.pathname
    if (path === '/map' || path === '/carte') return 'map'
    if (path === '/login' || path === '/manager') return 'login'
    if (path === '/accueil' || path === '/home') return 'home'
    return 'visitor'
  }

  const [currentPage, setCurrentPage] = useState<PageType>(getInitialPage())
  const [isAuthenticated, setIsAuthenticated] = useState(false)

  const handleNavigate = (page: string) => {
    setCurrentPage(page as PageType)
  }

  const handleLoginSuccess = () => {
    setIsAuthenticated(true)
    setCurrentPage('home')
  }

  const handleLogout = () => {
    setIsAuthenticated(false)
    localStorage.removeItem('authToken')
    setCurrentPage('visitor')
  }

  const renderPage = () => {
    // Manager pages (require authentication)
    if (currentPage === 'login') {
      return <LoginPage onLoginSuccess={handleLoginSuccess} />
    }

    if (currentPage === 'home' && isAuthenticated) {
      return <HomePage onNavigate={handleNavigate} onLogout={handleLogout} />
    }

    if (currentPage === 'blocked-users' && isAuthenticated) {
      return <BlockedUsersPage onNavigate={handleNavigate} />
    }

    if (currentPage === 'all-users' && isAuthenticated) {
      return <AllUsersPage onNavigate={handleNavigate} />
    }

    if (currentPage === 'add-user' && isAuthenticated) {
      return <AddUserPage onNavigate={handleNavigate} />
    }

    if (currentPage === 'issues' && isAuthenticated) {
      return <IssuesPage onNavigate={handleNavigate} />
    }

    if (currentPage === 'statistics' && isAuthenticated) {
      return <StatisticsPage onNavigate={handleNavigate} />
    }

    // Redirect to login if trying to access protected pages without auth
    if (['home', 'blocked-users', 'all-users', 'add-user', 'issues', 'statistics'].includes(currentPage) && !isAuthenticated) {
      return <LoginPage onLoginSuccess={handleLoginSuccess} />
    }

    // Public pages
    if (currentPage === 'map') {
      return <MapPage />
    }

    if (currentPage === 'summary') {
      return <VisitorSummaryPage />
    }

    if (currentPage === 'prix-forfaitaire') {
      return <PrixForfaitairePage onNavigate={handleNavigate} />
    }

    // Default
    return <VisitorSummaryPage />
  }

  return (
    <div>
      <nav className="global-nav">
        <button
          className={`nav-btn ${currentPage === 'visitor' || currentPage === 'summary' ? 'active' : ''}`}
          onClick={() => setCurrentPage('visitor')}
        >
          Tableau de bord
        </button>
        <button
          className={`nav-btn ${currentPage === 'map' ? 'active' : ''}`}
          onClick={() => setCurrentPage('map')}
        >
          Carte
        </button>
        <button
          className={`nav-btn nav-btn-manager ${['home', 'blocked-users', 'all-users', 'add-user', 'issues', 'statistics'].includes(currentPage) ? 'active' : ''}`}
          onClick={() => setCurrentPage(isAuthenticated ? 'home' : 'login')}
        >
          Espace Manager
        </button>
      </nav>
      {renderPage()}
    </div>
  )
}
