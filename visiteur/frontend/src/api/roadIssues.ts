export interface RoadIssuePoint {
  id: string
  title: string
  description: string
  latitude: number
  longitude: number
  surfaceM2: number
  budget: number
  statusCode: string
  statusLabel: string
  reportedAt: string | null
  companyName: string | null
}

const API_BASE = 'http://localhost:8082'

export async function fetchRoadIssues(): Promise<RoadIssuePoint[]> {
  const response = await fetch(`${API_BASE}/public/road-issues`)
  if (!response.ok) {
    throw new Error('Erreur lors du chargement des problèmes routiers')
  }
  return response.json()
}
