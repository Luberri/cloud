import { useEffect, useMemo, useState } from 'react'
import { fetchPublicSummary, type RoadIssuesSummaryResponse } from '../api/publicSummary'

function formatNumber(value: number) {
  return new Intl.NumberFormat('fr-FR').format(value)
}

function formatMoney(value: number) {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'MGA', maximumFractionDigits: 0 }).format(value)
}

export default function VisitorSummaryPage() {
  const [data, setData] = useState<RoadIssuesSummaryResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const apiBaseUrl = useMemo(() => {
    return (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080'
  }, [])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    fetchPublicSummary(apiBaseUrl)
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

    return () => {
      cancelled = true
    }
  }, [apiBaseUrl])

  return (
    <div className="page">
      <header className="header">
        <h1>Tableau de récapitulation (Visiteur)</h1>
        <p className="subtitle">Données publiques: nombre de signalements, surface totale, budget total, avancement.</p>
      </header>

      <section className="card">
        

        {loading && <div className="state">Chargement…</div>}
        {error && (
          <div className="state error">
            <div>Erreur: {error}</div>
            <div className="hint">Vérifie que le backend tourne sur {apiBaseUrl} et que CORS autorise http://localhost:5173.</div>
          </div>
        )}

        {data && !loading && !error && (
          <div className="grid">
            <div className="metric">
              <div className="metricTitle">Nb de points</div>
              <div className="metricValue">{formatNumber(data.totalSignalements)}</div>
            </div>
            <div className="metric">
              <div className="metricTitle">Surface totale</div>
              <div className="metricValue">{formatNumber(data.totalSurfaceM2)} m²</div>
            </div>
            <div className="metric">
              <div className="metricTitle">Budget total</div>
              <div className="metricValue">{formatMoney(data.totalBudget)}</div>
            </div>
            <div className="metric">
              <div className="metricTitle">Avancement</div>
              <div className="metricValue">{formatNumber(data.progressPercent)}%</div>
              <div className="progress">
                <div className="bar" style={{ width: `${Math.min(100, Math.max(0, data.progressPercent))}%` }} />
              </div>
            </div>
          </div>
        )}
      </section>

      <nav className="nav-links">
        <a href="/map">🗺️ Voir la carte des problèmes</a>
      </nav>
    </div>
  )
}
