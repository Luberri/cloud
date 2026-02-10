import { useEffect, useMemo, useState } from 'react'
import { getJson } from '../api/client'
import './StatisticsPage.css'

interface RoadIssue {
  id: string;
  title: string;
  statusId: number;
  niveau: number;
  surfaceM2: number;
  budget: number;
  companyId: number | null;
  reportedAt: string;
  updatedAt: string;
}

interface Company {
  id: number;
  name: string;
}

interface StatisticsPageProps {
  onNavigate: (page: string) => void
}

function formatNumber(value: number) {
  return new Intl.NumberFormat('fr-FR').format(value)
}

function formatMoney(value: number) {
  return new Intl.NumberFormat('fr-FR', { 
    style: 'currency', 
    currency: 'MGA', 
    maximumFractionDigits: 0 
  }).format(value)
}

function calculateDaysBetween(start: string, end: string): number {
  const startDate = new Date(start)
  const endDate = new Date(end)
  const diffTime = Math.abs(endDate.getTime() - startDate.getTime())
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
}

export default function StatisticsPage({ onNavigate }: StatisticsPageProps) {
  const [issues, setIssues] = useState<RoadIssue[]>([])
  const [companies, setCompanies] = useState<Company[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Filtres
  const [selectedCompany, setSelectedCompany] = useState<number | null>(null)
  const [selectedPeriod, setSelectedPeriod] = useState<string>('all')
  const [selectedNiveau, setSelectedNiveau] = useState<string>('all')

  useEffect(() => {
    Promise.all([
      getJson<RoadIssue[]>('/api/issues'),
      getJson<Company[]>('/api/companies').catch(() => [])
    ])
      .then(([issuesData, companiesData]) => {
        setIssues(issuesData)
        setCompanies(companiesData)
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  // Filtrer les données
  const filteredIssues = useMemo(() => {
    let filtered = [...issues]

    if (selectedCompany !== null) {
      filtered = filtered.filter(i => i.companyId === selectedCompany)
    }

    if (selectedPeriod !== 'all') {
      const now = new Date()
      const days = parseInt(selectedPeriod)
      const cutoff = new Date(now.getTime() - days * 24 * 60 * 60 * 1000)
      filtered = filtered.filter(i => new Date(i.reportedAt) >= cutoff)
    }

    if (selectedNiveau !== 'all') {
      if (selectedNiveau === 'faible') {
        filtered = filtered.filter(i => i.niveau >= 1 && i.niveau <= 3)
      } else if (selectedNiveau === 'moyen') {
        filtered = filtered.filter(i => i.niveau >= 4 && i.niveau <= 6)
      } else if (selectedNiveau === 'critique') {
        filtered = filtered.filter(i => i.niveau >= 7 && i.niveau <= 10)
      }
    }

    return filtered
  }, [issues, selectedCompany, selectedPeriod, selectedNiveau])

  // Calculs statistiques
  const stats = useMemo(() => {
    const total = filteredIssues.length
    const nouveau = filteredIssues.filter(i => i.statusId === 1).length
    const enCours = filteredIssues.filter(i => i.statusId === 2).length
    const termine = filteredIssues.filter(i => i.statusId === 3).length

    const totalSurface = filteredIssues.reduce((sum, i) => sum + (i.surfaceM2 || 0), 0)
    const totalBudget = filteredIssues.reduce((sum, i) => sum + (i.budget || 0), 0)

    // Par niveau
    const niveauFaible = filteredIssues.filter(i => i.niveau >= 1 && i.niveau <= 3).length
    const niveauMoyen = filteredIssues.filter(i => i.niveau >= 4 && i.niveau <= 6).length
    const niveauCritique = filteredIssues.filter(i => i.niveau >= 7 && i.niveau <= 10).length

    const budgetFaible = filteredIssues.filter(i => i.niveau >= 1 && i.niveau <= 3).reduce((s, i) => s + (i.budget || 0), 0)
    const budgetMoyen = filteredIssues.filter(i => i.niveau >= 4 && i.niveau <= 6).reduce((s, i) => s + (i.budget || 0), 0)
    const budgetCritique = filteredIssues.filter(i => i.niveau >= 7 && i.niveau <= 10).reduce((s, i) => s + (i.budget || 0), 0)

    const progress = total > 0 ? Math.round((termine / total) * 100) : 0

    // Par entreprise
    const byCompany = companies.map(company => {
      const companyIssues = filteredIssues.filter(i => i.companyId === company.id)
      return {
        id: company.id,
        name: company.name,
        count: companyIssues.length,
        budget: companyIssues.reduce((s, i) => s + (i.budget || 0), 0)
      }
    }).filter(c => c.count > 0)

    // Calcul des délais de traitement
    const terminedIssues = filteredIssues.filter(i => i.statusId === 3)
    const delaysByStatus = terminedIssues.map(issue => ({
      id: issue.id,
      title: issue.title,
      niveau: issue.niveau,
      delay: calculateDaysBetween(issue.reportedAt, issue.updatedAt),
      reportedAt: issue.reportedAt,
      updatedAt: issue.updatedAt
    }))

    const avgDelay = delaysByStatus.length > 0 
      ? Math.round(delaysByStatus.reduce((sum, d) => sum + d.delay, 0) / delaysByStatus.length)
      : 0

    const delayByNiveau = [
      {
        niveau: 'Faible (1-3)',
        delays: terminedIssues.filter(i => i.niveau >= 1 && i.niveau <= 3).map(i => calculateDaysBetween(i.reportedAt, i.updatedAt)),
        count: terminedIssues.filter(i => i.niveau >= 1 && i.niveau <= 3).length
      },
      {
        niveau: 'Moyen (4-6)',
        delays: terminedIssues.filter(i => i.niveau >= 4 && i.niveau <= 6).map(i => calculateDaysBetween(i.reportedAt, i.updatedAt)),
        count: terminedIssues.filter(i => i.niveau >= 4 && i.niveau <= 6).length
      },
      {
        niveau: 'Critique (7-10)',
        delays: terminedIssues.filter(i => i.niveau >= 7 && i.niveau <= 10).map(i => calculateDaysBetween(i.reportedAt, i.updatedAt)),
        count: terminedIssues.filter(i => i.niveau >= 7 && i.niveau <= 10).length
      }
    ].map(item => ({
      ...item,
      avgDelay: item.delays.length > 0 
        ? Math.round(item.delays.reduce((s, d) => s + d, 0) / item.delays.length)
        : 0
    }))

    return {
      total,
      nouveau,
      enCours,
      termine,
      totalSurface,
      totalBudget,
      niveauFaible,
      niveauMoyen,
      niveauCritique,
      budgetFaible,
      budgetMoyen,
      budgetCritique,
      progress,
      byCompany,
      avgDelay,
      delayByNiveau,
      recentCompleted: delaysByStatus.slice(-5).reverse()
    }
  }, [filteredIssues, companies])

  if (loading) return <div className="statistics-page"><div className="state">Chargement...</div></div>
  if (error) return <div className="statistics-page"><div className="state error">Erreur : {error}</div></div>

  return (
    <div className="statistics-page">
      <button className="btn-back" onClick={() => onNavigate('home')}>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
        Retour
      </button>

      <header className="stats-header">
        <h1>Tableau de Bord</h1>
        <p>Analyse des signalements routiers</p>
      </header>

      {/* FILTRES */}
      <section className="filters-section">
        <div className="filter-group">
          <label>Entreprise</label>
          <select value={selectedCompany ?? ''} onChange={e => setSelectedCompany(e.target.value ? Number(e.target.value) : null)}>
            <option value="">Toutes</option>
            {companies.map(c => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </div>

        <div className="filter-group">
          <label>Période</label>
          <select value={selectedPeriod} onChange={e => setSelectedPeriod(e.target.value)}>
            <option value="all">Toutes</option>
            <option value="7">7 jours</option>
            <option value="30">30 jours</option>
            <option value="90">90 jours</option>
          </select>
        </div>

        <div className="filter-group">
          <label>Niveau</label>
          <select value={selectedNiveau} onChange={e => setSelectedNiveau(e.target.value)}>
            <option value="all">Tous</option>
            <option value="faible">Faible</option>
            <option value="moyen">Moyen</option>
            <option value="critique">Critique</option>
          </select>
        </div>

        <button className="btn-reset" onClick={() => {
          setSelectedCompany(null)
          setSelectedPeriod('all')
          setSelectedNiveau('all')
        }}>
          Réinitialiser
        </button>
      </section>

      {/* CARDS PRINCIPALES */}
      <section className="cards-grid">
        <div className="stat-card">
          <div className="card-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
            </svg>
          </div>
          <div className="card-content">
            <div className="card-label">Total</div>
            <div className="card-value">{formatNumber(stats.total)}</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="card-icon success">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <div className="card-content">
            <div className="card-label">Terminés</div>
            <div className="card-value">{formatNumber(stats.termine)}</div>
            <div className="card-sub">{stats.total > 0 ? Math.round((stats.termine / stats.total) * 100) : 0}%</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="card-icon warning">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>
            </svg>
          </div>
          <div className="card-content">
            <div className="card-label">En Cours</div>
            <div className="card-value">{formatNumber(stats.enCours)}</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="card-icon danger">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
            </svg>
          </div>
          <div className="card-content">
            <div className="card-label">Nouveaux</div>
            <div className="card-value">{formatNumber(stats.nouveau)}</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="card-icon info">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
            </svg>
          </div>
          <div className="card-content">
            <div className="card-label">Surface Totale</div>
            <div className="card-value-small">{formatNumber(Math.round(stats.totalSurface))} m²</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="card-icon money">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <div className="card-content">
            <div className="card-label">Budget Total</div>
            <div className="card-value-small">{formatMoney(stats.totalBudget)}</div>
          </div>
        </div>
      </section>

      {/* DÉLAI DE TRAITEMENT */}
      <section className="chart-section">
        <h2>Délai de Traitement Moyen</h2>
        <div className="delay-summary">
          <div className="delay-card">
            <div className="delay-value">{stats.avgDelay}</div>
            <div className="delay-label">jours en moyenne</div>
          </div>
        </div>

        <div className="delay-table">
          <table>
            <thead>
              <tr>
                <th>Niveau de Gravité</th>
                <th>Nb Terminés</th>
                <th>Délai Moyen</th>
              </tr>
            </thead>
            <tbody>
              {stats.delayByNiveau.map((item, idx) => (
                <tr key={idx}>
                  <td>{item.niveau}</td>
                  <td>{item.count}</td>
                  <td>
                    {item.count > 0 ? (
                      <span className="delay-badge">{item.avgDelay} jours</span>
                    ) : (
                      <span className="no-data">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {stats.recentCompleted.length > 0 && (
          <>
            <h3>Travaux Récemment Terminés</h3>
            <div className="recent-table">
              <table>
                <thead>
                  <tr>
                    <th>Titre</th>
                    <th>Niveau</th>
                    <th>Date Signalement</th>
                    <th>Date Fin</th>
                    <th>Délai</th>
                  </tr>
                </thead>
                <tbody>
                  {stats.recentCompleted.map((item) => (
                    <tr key={item.id}>
                      <td>{item.title}</td>
                      <td>
                        <span className={`niveau-badge niveau-${
                          item.niveau <= 3 ? 'low' : item.niveau <= 6 ? 'medium' : 'high'
                        }`}>
                          {item.niveau}
                        </span>
                      </td>
                      <td>{new Date(item.reportedAt).toLocaleDateString('fr-FR')}</td>
                      <td>{new Date(item.updatedAt).toLocaleDateString('fr-FR')}</td>
                      <td><span className="delay-badge">{item.delay} j</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </section>

      {/* GRAPHIQUE NIVEAU */}
      <section className="chart-section">
        <h2>Répartition par Niveau</h2>
        <div className="bar-chart">
          <div className="bar-item">
            <div className="bar-label">Faible</div>
            <div className="bar-wrapper">
              <div 
                className="bar-fill bar-green" 
                style={{ width: stats.total > 0 ? `${(stats.niveauFaible / stats.total) * 100}%` : '0%' }}
              >
                <span className="bar-value">{stats.niveauFaible}</span>
              </div>
            </div>
          </div>
          <div className="bar-item">
            <div className="bar-label">Moyen</div>
            <div className="bar-wrapper">
              <div 
                className="bar-fill bar-orange" 
                style={{ width: stats.total > 0 ? `${(stats.niveauMoyen / stats.total) * 100}%` : '0%' }}
              >
                <span className="bar-value">{stats.niveauMoyen}</span>
              </div>
            </div>
          </div>
          <div className="bar-item">
            <div className="bar-label">Critique</div>
            <div className="bar-wrapper">
              <div 
                className="bar-fill bar-red" 
                style={{ width: stats.total > 0 ? `${(stats.niveauCritique / stats.total) * 100}%` : '0%' }}
              >
                <span className="bar-value">{stats.niveauCritique}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* BUDGET PAR NIVEAU */}
      <section className="chart-section">
        <h2>Budget par Niveau</h2>
        <div className="budget-bars">
          <div className="budget-item">
            <div className="budget-header">
              <span className="budget-label">Faible</span>
              <span className="budget-amount">{formatMoney(stats.budgetFaible)}</span>
            </div>
            <div className="budget-bar">
              <div 
                className="budget-fill bg-green" 
                style={{ width: stats.totalBudget > 0 ? `${(stats.budgetFaible / stats.totalBudget) * 100}%` : '0%' }}
              ></div>
            </div>
          </div>
          <div className="budget-item">
            <div className="budget-header">
              <span className="budget-label">Moyen</span>
              <span className="budget-amount">{formatMoney(stats.budgetMoyen)}</span>
            </div>
            <div className="budget-bar">
              <div 
                className="budget-fill bg-orange" 
                style={{ width: stats.totalBudget > 0 ? `${(stats.budgetMoyen / stats.totalBudget) * 100}%` : '0%' }}
              ></div>
            </div>
          </div>
          <div className="budget-item">
            <div className="budget-header">
              <span className="budget-label">Critique</span>
              <span className="budget-amount">{formatMoney(stats.budgetCritique)}</span>
            </div>
            <div className="budget-bar">
              <div 
                className="budget-fill bg-red" 
                style={{ width: stats.totalBudget > 0 ? `${(stats.budgetCritique / stats.totalBudget) * 100}%` : '0%' }}
              ></div>
            </div>
          </div>
        </div>
      </section>

      {/* PAR ENTREPRISE */}
      {stats.byCompany.length > 0 && (
        <section className="chart-section">
          <h2>Par Entreprise</h2>
          <div className="company-stats">
            {stats.byCompany.map(company => (
              <div key={company.id} className="company-item">
                <div className="company-header">
                  <span className="company-name">{company.name}</span>
                  <span className="company-count">{company.count}</span>
                </div>
                <div className="company-budget">{formatMoney(company.budget)}</div>
                <div className="company-bar">
                  <div 
                    className="company-fill" 
                    style={{ width: stats.total > 0 ? `${(company.count / stats.total) * 100}%` : '0%' }}
                  ></div>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}