import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, Marker, Tooltip } from 'react-leaflet'
import L from 'leaflet'
import { fetchRoadIssues, fetchIssueImages, RoadIssuePoint, IssueImage } from '../api/roadIssues'
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
  
  // Detail panel state
  const [selectedIssue, setSelectedIssue] = useState<RoadIssuePoint | null>(null)
  const [showPhotoGallery, setShowPhotoGallery] = useState(false)
  const [photos, setPhotos] = useState<IssueImage[]>([])
  const [loadingPhotos, setLoadingPhotos] = useState(false)
  const [selectedPhoto, setSelectedPhoto] = useState<IssueImage | null>(null)

  useEffect(() => {
    fetchRoadIssues()
      .then(setIssues)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  const handleMarkerClick = (issue: RoadIssuePoint) => {
    setSelectedIssue(issue)
    setShowPhotoGallery(false)
    setPhotos([])
    setSelectedPhoto(null)
  }

  const handleShowPhotos = async () => {
    if (!selectedIssue) return
    setLoadingPhotos(true)
    try {
      const images = await fetchIssueImages(selectedIssue.id)
      setPhotos(images)
      setShowPhotoGallery(true)
    } catch (err) {
      console.error('Erreur chargement photos:', err)
    } finally {
      setLoadingPhotos(false)
    }
  }

  const closePanel = () => {
    setSelectedIssue(null)
    setShowPhotoGallery(false)
    setPhotos([])
    setSelectedPhoto(null)
  }

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
        <div className="header-content">
          <h1>🗺️ Carte des Problèmes Routiers</h1>
          <span className="subtitle">Antananarivo - {issues.length} signalement(s)</span>
          <div className="legend">
            <span className="legend-item"><span className="dot red"></span> Nouveau</span>
            <span className="legend-item"><span className="dot orange"></span> En cours</span>
            <span className="legend-item"><span className="dot green"></span> Terminé</span>
          </div>
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
              eventHandlers={{
                click: () => handleMarkerClick(issue)
              }}
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
                  <p className="tooltip-hint">📷 Cliquez pour voir les photos</p>
                </div>
              </Tooltip>
            </Marker>
          ))}
        </MapContainer>

        {/* Detail Panel */}
        {selectedIssue && (
          <div className="issue-detail-panel">
            <div className="panel-header">
              <h2>{selectedIssue.title || 'Signalement routier'}</h2>
              <button className="close-btn" onClick={closePanel}>✕</button>
            </div>
            
            <div className="panel-body">
              {!showPhotoGallery ? (
                <>
                  <div className="detail-section">
                    <p className="description">{selectedIssue.description || 'Aucune description disponible.'}</p>
                  </div>
                  
                  <div className="detail-grid">
                    <div className="detail-item">
                      <span className="detail-label">📅 Date signalée</span>
                      <span className="detail-value">{formatDate(selectedIssue.reportedAt)}</span>
                    </div>
                    <div className="detail-item">
                      <span className="detail-label">📊 Statut</span>
                      <span className={`detail-value status-badge ${selectedIssue.statusCode?.toLowerCase()}`}>
                        {selectedIssue.statusLabel || 'Inconnu'}
                      </span>
                    </div>
                    <div className="detail-item">
                      <span className="detail-label">📐 Surface</span>
                      <span className="detail-value">{selectedIssue.surfaceM2} m²</span>
                    </div>
                    <div className="detail-item">
                      <span className="detail-label">💰 Budget estimé</span>
                      <span className="detail-value">{formatCurrency(selectedIssue.budget)}</span>
                    </div>
                    <div className="detail-item full-width">
                      <span className="detail-label">🏢 Entreprise assignée</span>
                      <span className="detail-value">{selectedIssue.companyName || 'Non assignée'}</span>
                    </div>
                    <div className="detail-item full-width">
                      <span className="detail-label">📍 Coordonnées</span>
                      <span className="detail-value coords">{selectedIssue.latitude.toFixed(6)}, {selectedIssue.longitude.toFixed(6)}</span>
                    </div>
                  </div>

                  <button 
                    className="view-photos-btn" 
                    onClick={handleShowPhotos}
                    disabled={loadingPhotos}
                  >
                    {loadingPhotos ? (
                      <>
                        <span className="spinner-small"></span>
                        Chargement...
                      </>
                    ) : (
                      <>📷 Voir les photos</>
                    )}
                  </button>
                </>
              ) : (
                <>
                  <button className="back-btn" onClick={() => setShowPhotoGallery(false)}>
                    ← Retour aux détails
                  </button>
                  
                  <h3 className="gallery-title">📷 Photos du signalement</h3>
                  
                  {photos.length === 0 ? (
                    <div className="no-photos">
                      <p>📭 Aucune photo disponible pour ce signalement.</p>
                    </div>
                  ) : (
                    <div className="photo-gallery">
                      {photos.map((photo) => (
                        <div 
                          key={photo.id} 
                          className="photo-item"
                          onClick={() => setSelectedPhoto(photo)}
                        >
                          <img 
                            src={photo.thumbnailUrl || photo.downloadUrl} 
                            alt="Photo du signalement"
                            loading="lazy"
                          />
                          <div className="photo-overlay">
                            <span>🔍 Agrandir</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        )}

        {/* Lightbox for full-size photo */}
        {selectedPhoto && (
          <div className="photo-lightbox" onClick={() => setSelectedPhoto(null)}>
            <div className="lightbox-content" onClick={(e) => e.stopPropagation()}>
              <button className="lightbox-close" onClick={() => setSelectedPhoto(null)}>✕</button>
              <img src={selectedPhoto.downloadUrl} alt="Photo en taille réelle" />
              <div className="lightbox-info">
                <span>📅 {formatDate(selectedPhoto.createdAt)}</span>
                {selectedPhoto.fileSizeBytes && (
                  <span>📦 {(selectedPhoto.fileSizeBytes / 1024).toFixed(0)} Ko</span>
                )}
              </div>
            </div>
          </div>
        )}
      </div>

      <nav className="nav-links">
        <a href="/">← Retour au tableau de bord</a>
      </nav>
    </div>
  )
}
