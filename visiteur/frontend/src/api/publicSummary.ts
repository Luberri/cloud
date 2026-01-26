export type RoadIssuesSummaryResponse = {
  totalSignalements: number
  totalSurfaceM2: number
  totalBudget: number
  progressPercent: number
}

const DEFAULT_BASE_URL = 'http://localhost:8080'

export async function fetchPublicSummary(baseUrl?: string): Promise<RoadIssuesSummaryResponse> {
  const apiBase = baseUrl ?? import.meta.env.VITE_API_BASE_URL ?? DEFAULT_BASE_URL
  const res = await fetch(`${apiBase}/public/summary`, {
    method: 'GET',
    headers: { 'Accept': 'application/json' }
  })

  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`HTTP ${res.status} ${res.statusText}${text ? ` - ${text}` : ''}`)
  }

  return res.json() as Promise<RoadIssuesSummaryResponse>
}
