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

export interface IssueImage {
  id: string
  roadIssueId: string
  downloadUrl: string
  thumbnailUrl: string | null
  fileSizeBytes: number | null
  mimeType: string
  createdAt: string
}

const API_BASE = 'http://localhost:8082'

export async function fetchRoadIssues(): Promise<RoadIssuePoint[]> {
  const response = await fetch(`${API_BASE}/public/road-issues`)
  if (!response.ok) {
    throw new Error('Erreur lors du chargement des problèmes routiers')
  }
  return response.json()
}

export async function fetchIssueImages(issueId: string): Promise<IssueImage[]> {
  const response = await fetch(`${API_BASE}/public/road-issues/${issueId}/images`)
  if (!response.ok) {
    throw new Error('Erreur lors du chargement des photos')
  }
  return response.json()
}

export async function fetchIssueImageCount(issueId: string): Promise<number> {
  const response = await fetch(`${API_BASE}/public/road-issues/${issueId}/images/count`)
  if (!response.ok) {
    return 0
  }
  return response.json()
}
