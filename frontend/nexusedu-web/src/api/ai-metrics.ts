import request from '@/utils/http'

export type MetricValue = string | number | null
export type MetricRow = Record<string, MetricValue>
export interface MetricPage {
  records: MetricRow[]
  current: number
  size: number
  total: number | string
}
export interface AiMetricsOverview {
  startDate: string
  endDate: string
  generatedAt: string
  model: MetricRow
  latency: MetricRow
  costs: MetricRow[]
  models: MetricRow[]
  daily: MetricRow[]
  rag: MetricRow
  tools: MetricRow[]
  toolSteps: MetricRow
  queue: MetricRow
  queueHistory: MetricRow
  queueThroughput: MetricRow
  instanceWriteFailures: number | string
}
export interface ModelPriceInput {
  provider: string
  modelName: string
  currency: string
  inputPrice: string
  cachedInputPrice?: string
  outputPrice: string
}
export function getAiMetrics(startDate: string, endDate: string) {
  return request.get<AiMetricsOverview>({
    url: '/api/ai/metrics',
    params: { startDate, endDate }
  })
}
export function getModelCalls(params: Record<string, string | number>) {
  return request.get<MetricPage>({ url: '/api/ai/metrics/calls', params })
}
export function getModelPrices(current = 1, size = 20) {
  return request.get<MetricPage>({ url: '/api/ai/metrics/prices', params: { current, size } })
}
export function publishModelPrice(data: ModelPriceInput) {
  return request.post<MetricRow>({ url: '/api/ai/metrics/prices', data })
}
