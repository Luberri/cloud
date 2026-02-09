export type StatisticsResponse = {
  totalSignalements: number
  countNew: number
  countInProgress: number
  countDone: number
  avgProgressPercent: number
  totalSurfaceM2: number
  totalBudget: number
  avgCompletionDays: number | null
  avgStartDelayDays: number | null
  avgTreatmentDays: number | null
}

const DEFAULT_BASE_URL = 'http://localhost:8082'

export async function fetchStatistics(baseUrl?: string): Promise<StatisticsResponse> {
  const apiBase = baseUrl ?? import.meta.env.VITE_API_BASE_URL ?? DEFAULT_BASE_URL
  const res = await fetch(`${apiBase}/public/statistics`, {
    method: 'GET',
    headers: { 'Accept': 'application/json' }
  })

  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`HTTP ${res.status} ${res.statusText}${text ? ` - ${text}` : ''}`)
  }

  return res.json() as Promise<StatisticsResponse>
}
