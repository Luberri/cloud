import VisitorSummaryPage from './pages/VisitorSummaryPage'
import MapPage from './pages/MapPage'

export default function App() {
  // Simple routing based on URL path
  const path = window.location.pathname

  if (path === '/map' || path === '/carte') {
    return <MapPage />
  }

  return <VisitorSummaryPage />
}
