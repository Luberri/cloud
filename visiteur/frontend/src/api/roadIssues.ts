export interface IssueImage {
  id: string // UUID en string
  storagePath: string
  downloadUrl: string
  createdAt: string
  thumbnailUrl?: string
  fileSizeBytes?: number
  mimeType?: string
}

export interface RoadIssuePoint {
  id: string
  title: string
  description: string
  latitude: number
  longitude: number
  surfaceM2: number
  budget: number
  statusId: number
  statusCode: string
  statusLabel: string
  companyId?: number
  companyName?: string
  reportedAt: string
}

const API_BASE_URL = 'http://localhost:8082/api'

export async function fetchRoadIssues(): Promise<RoadIssuePoint[]> {
  const response = await fetch(`${API_BASE_URL}/map/issues`)
  if (!response.ok) {
    throw new Error('Erreur lors du chargement des signalements')
  }
  return response.json()
}

export async function fetchIssueImages(issueId: string): Promise<IssueImage[]> {
  try {
    const response = await fetch(`${API_BASE_URL}/issues/${issueId}/images`)
    if (!response.ok) {
      throw new Error(`Erreur HTTP: ${response.status}`)
    }
    return response.json()
  } catch (error) {
    console.error('Erreur détaillée:', error)
    throw new Error('Erreur lors du chargement des photos')
  }
}
