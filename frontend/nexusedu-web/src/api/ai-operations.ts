import request from '@/utils/http'
import type { ManagementPage, ManagementRow } from './management'
export function operationList(
  domain: 'config' | 'evaluation',
  resource: string,
  params: Record<string, unknown>
) {
  return request.get<ManagementPage>({ url: `/api/ai/${domain}/${resource}`, params })
}
export function draftVersion(data: Record<string, unknown>) {
  return request.post<ManagementRow>({ url: '/api/ai/config/versions', data })
}
export function reviewVersion(id: string, action: string, note = '') {
  return request.post<void>({ url: `/api/ai/config/versions/${id}/${action}`, data: { note } })
}
export function saveRollout(data: Record<string, unknown>) {
  return request.put<void>({ url: '/api/ai/config/rollouts', data })
}
export function saveResourcePolicy(data: Record<string, unknown>) {
  return request.put<void>({ url: '/api/ai/config/policies', data })
}
export function addEvaluationCase(data: Record<string, unknown>) {
  return request.post<ManagementRow>({ url: '/api/ai/evaluation/cases', data })
}
export function enableEvaluationCase(id: string, enabled: boolean) {
  return request.put<void>({ url: `/api/ai/evaluation/cases/${id}/enabled`, data: { enabled } })
}
export function runEvaluation(data: Record<string, unknown>) {
  return request.post<ManagementRow>({ url: '/api/ai/evaluation/batches', data })
}
export function evaluationSummary(id: string) {
  return request.get<Record<string, any>>({ url: `/api/ai/evaluation/batches/${id}/summary` })
}
export function saveEvaluationSchedule(data: Record<string, unknown>) {
  return request.put<void>({ url: '/api/ai/evaluation/schedules', data })
}
