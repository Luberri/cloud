import { useState } from 'react'
import VisitorSummaryPage from './pages/VisitorSummaryPage'
import MapPage from './pages/MapPage'
import LoginPage from './pages/LoginPage'
import HomePage from './pages/HomePage'
import BlockedUsersPage from './pages/BlockedUsersPage'
import AllUsersPage from './pages/AllUsersPage'
import AddUserPage from './pages/AddUserPage'
import IssuesPage from './pages/IssuesPage'
import './styles.css'

type PageType = 'visitor' | 'map' | 'login' | 'home' | 'blocked-users' | 'all-users' | 'add-user' | 'issues' | 'summary'

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

  // Redirect to login if trying to access protected pages without auth
  if (['home', 'blocked-users', 'all-users', 'add-user', 'issues'].includes(currentPage) && !isAuthenticated) {
    return <LoginPage onLoginSuccess={handleLoginSuccess} />
  }

  // Public pages
  if (currentPage === 'map') {
    return <MapPage />
  }

  if (currentPage === 'summary') {
    return <VisitorSummaryPage />
  }

  // Default: Visitor summary with navigation options
  return (
    <div>
      <nav style={{ 
        padding: '1rem', 
        backgroundColor: '#333', 
        display: 'flex', 
        gap: '1rem',
        justifyContent: 'center'
      }}>
        <button 
          onClick={() => setCurrentPage('visitor')}
          style={{ 
            padding: '0.5rem 1rem', 
            backgroundColor: currentPage === 'visitor' ? '#666' : '#444',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Résumé
        </button>
        <button 
          onClick={() => setCurrentPage('map')}
          style={{ 
            padding: '0.5rem 1rem', 
            backgroundColor: '#444',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Carte
        </button>
        <button 
          onClick={() => setCurrentPage('login')}
          style={{ 
            padding: '0.5rem 1rem', 
            backgroundColor: '#4f8cff',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Espace Manager
        </button>
      </nav>
      <VisitorSummaryPage />
    </div>
  )
}
