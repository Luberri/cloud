import { useEffect, useMemo, useState } from 'react'
import { fetchStatistics, type StatisticsResponse } from '../api/statistics'
import './StatisticsPage.css'

function formatNumber(value: number) {
  return new Intl.NumberFormat('fr-FR').format(value)
}

function formatDays(value: number | null) {
  if (value === null || value === undefined) return 'N/A'
  return `${formatNumber(value)} jour${value > 1 ? 's' : ''}`
}

function formatMoney(value: number) {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'MGA', maximumFractionDigits: 0 }).format(value)
}

interface StatisticsPageProps {
  onNavigate: (page: string) => void
}

export default function StatisticsPage({ onNavigate }: StatisticsPageProps) {
  const [data, setData] = useState<StatisticsResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const apiBaseUrl = useMemo(() => {
    return (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8082'
  }, [])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    fetchStatistics(apiBaseUrl)
      .then((json) => {
        if (cancelled) return
        setData(json)
      })
      .catch((e: unknown) => {
        if (cancelled) return
        setError(e instanceof Error ? e.message : 'Erreur inconnue')
      })
      .finally(() => {
        if (cancelled) return
        setLoading(false)
      })

    return () => { cancelled = true }
  }, [apiBaseUrl])

  return (
    <div className="statistics-page">
      <button className="btn back-btn" onClick={() => onNavigate('home')}>
        ← Retour à l'accueil
      </button>

      <h1>Statistiques des travaux</h1>
      <p className="subtitle">Avancement et délais de traitement des signalements routiers</p>

      {loading && <div className="state">Chargement…</div>}
      {error && <div className="state error">Erreur : {error}</div>}

      {data && !loading && !error && (
        <>
          {/* Tableau Avancement */}
          <section className="stats-section">
            <h2>Avancement des signalements</h2>
            <table className="stats-table">
              <thead>
                <tr>
                  <th>Statut</th>
                  <th>Avancement</th>
                  <th>Nombre</th>
                  <th>Pourcentage</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td><span className="badge badge-new">Nouveau</span></td>
                  <td>0%</td>
                  <td>{data.countNew}</td>
                  <td>{data.totalSignalements > 0 ? formatNumber(Math.round(data.countNew * 100 / data.totalSignalements)) : 0}%</td>
                </tr>
                <tr>
                  <td><span className="badge badge-progress">En cours</span></td>
                  <td>50%</td>
                  <td>{data.countInProgress}</td>
                  <td>{data.totalSignalements > 0 ? formatNumber(Math.round(data.countInProgress * 100 / data.totalSignalements)) : 0}%</td>
                </tr>
                <tr>
                  <td><span className="badge badge-done">Terminé</span></td>
                  <td>100%</td>
                  <td>{data.countDone}</td>
                  <td>{data.totalSignalements > 0 ? formatNumber(Math.round(data.countDone * 100 / data.totalSignalements)) : 0}%</td>
                </tr>
                <tr className="total-row">
                  <td><strong>Total</strong></td>
                  <td><strong>{formatNumber(data.avgProgressPercent)}%</strong></td>
                  <td><strong>{data.totalSignalements}</strong></td>
                  <td><strong>100%</strong></td>
                </tr>
              </tbody>
            </table>

            <div className="progress-bar-container">
              <div className="progress-label">Avancement global : {formatNumber(data.avgProgressPercent)}%</div>
              <div className="progress-bar">
                <div className="progress-fill" style={{ width: `${Math.min(100, Math.max(0, data.avgProgressPercent))}%` }} />
              </div>
            </div>
          </section>

          {/* Tableau Délais */}
          <section className="stats-section">
            <h2>Délais de traitement</h2>
            <table className="stats-table">
              <thead>
                <tr>
                  <th>Indicateur</th>
                  <th>Étape</th>
                  <th>Délai moyen</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>Prise en charge</td>
                  <td>Nouveau → En cours</td>
                  <td>{formatDays(data.avgStartDelayDays)}</td>
                </tr>
                <tr>
                  <td>Traitement</td>
                  <td>En cours → Terminé</td>
                  <td>{formatDays(data.avgTreatmentDays)}</td>
                </tr>
                <tr className="total-row">
                  <td><strong>Total</strong></td>
                  <td><strong>Nouveau → Terminé</strong></td>
                  <td><strong>{formatDays(data.avgCompletionDays)}</strong></td>
                </tr>
              </tbody>
            </table>
          </section>

          {/* Résumé financier */}
          <section className="stats-section">
            <h2>Résumé</h2>
            <div className="metrics-grid">
              <div className="metric-card">
                <div className="metric-label">Surface totale</div>
                <div className="metric-value">{formatNumber(data.totalSurfaceM2)} m²</div>
              </div>
              <div className="metric-card">
                <div className="metric-label">Budget total</div>
                <div className="metric-value">{formatMoney(data.totalBudget)}</div>
              </div>
              <div className="metric-card">
                <div className="metric-label">Signalements</div>
                <div className="metric-value">{data.totalSignalements}</div>
              </div>
            </div>
          </section>
        </>
      )}
    </div>
  )
}
