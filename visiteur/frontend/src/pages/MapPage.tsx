import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, Marker, Tooltip } from 'react-leaflet'
import L from 'leaflet'
import { fetchRoadIssues, RoadIssuePoint } from '../api/roadIssues'
import 'leaflet/dist/leaflet.css'

// Fix for default marker icons in Leaflet with Vite
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

// Custom icons by status
const createIcon = (color: string) => new L.Icon({
  iconUrl: `https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-${color}.png`,
  shadowUrl: markerShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
})

const icons: Record<string, L.Icon> = {
  NEW: createIcon('red'),
  IN_PROGRESS: createIcon('orange'),
  DONE: createIcon('green'),
  default: new L.Icon({
    iconUrl: markerIcon,
    iconRetinaUrl: markerIcon2x,
    shadowUrl: markerShadow,
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41]
  })
}

// Centre d'Antananarivo
const ANTANANARIVO_CENTER: [number, number] = [-18.8792, 47.5079]
const DEFAULT_ZOOM = 13

export default function MapPage() {
  const [issues, setIssues] = useState<RoadIssuePoint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    fetchRoadIssues()
      .then(setIssues)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('fr-MG', {
      style: 'currency',
      currency: 'MGA',
      maximumFractionDigits: 0
    }).format(value)
  }

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return 'Non renseignée'
    const date = new Date(dateStr)
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric'
    })
  }

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Chargement de la carte...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="error-container">
        <p>❌ {error}</p>
        <button onClick={() => window.location.reload()}>Réessayer</button>
      </div>
    )
  }

  return (
    <div className="map-page">
      <header className="map-header">
        <h1>�️ Carte des Problèmes Routiers</h1>
        <span className="subtitle">Antananarivo • {issues.length} signalement(s)</span>
        <div className="legend">
          <span className="legend-item"><span className="dot red"></span> Nouveau</span>
          <span className="legend-item"><span className="dot orange"></span> En cours</span>
          <span className="legend-item"><span className="dot green"></span> Terminé</span>
        </div>
      </header>

      <div className="map-container">
        <MapContainer
          center={ANTANANARIVO_CENTER}
          zoom={DEFAULT_ZOOM}
          style={{ height: '100%', width: '100%' }}
        >
          {/* Serveur de tuiles OFFLINE local (tileserver-gl Docker) */}
          <TileLayer
            attribution='&copy; OpenStreetMap contributors (Offline)'
            url="http://localhost:8081/styles/osm-bright/{z}/{x}/{y}.png"
          />
          {issues.map(issue => (
            <Marker
              key={issue.id}
              position={[issue.latitude, issue.longitude]}
              icon={icons[issue.statusCode] || icons.default}
            >
              <Tooltip 
                direction="top" 
                offset={[0, -35]} 
                opacity={0.95}
                permanent={false}
              >
                <div className="tooltip-content">
                  <h3>{issue.title || 'Sans titre'}</h3>
                  <table className="tooltip-table">
                    <tbody>
                      <tr>
                        <td><strong>📅 Date:</strong></td>
                        <td>{formatDate(issue.reportedAt)}</td>
                      </tr>
                      <tr>
                        <td><strong>📊 Statut:</strong></td>
                        <td className={`status-${issue.statusCode?.toLowerCase()}`}>
                          {issue.statusLabel || 'Inconnu'}
                        </td>
                      </tr>
                      <tr>
                        <td><strong>📐 Surface:</strong></td>
                        <td>{issue.surfaceM2} m²</td>
                      </tr>
                      <tr>
                        <td><strong>💰 Budget:</strong></td>
                        <td>{formatCurrency(issue.budget)}</td>
                      </tr>
                      <tr>
                        <td><strong>🏢 Entreprise:</strong></td>
                        <td>{issue.companyName || 'Non assignée'}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </Tooltip>
            </Marker>
          ))}
        </MapContainer>
      </div>

      <nav className="nav-links">
        <a href="/">← Retour au tableau de bord</a>
      </nav>
    </div>
  )
}
