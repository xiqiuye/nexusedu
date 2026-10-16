import request from '@/utils/http'
import type { ManagementPage } from './management'
export interface ChatSource {
  documentNo: string
  chunkNo: string
  score?: number
  rank?: number
  excerpt?: string
}

export interface ChatReceipt {
  runId: string
  status: string
  conversationId?: string
}
export interface ChatSnapshot extends ChatReceipt {
  question: string
  answer?: string
  errorMessage?: string
  sources?: ChatSource[]
}
export function createConversation(title = '新对话') {
  return request.post<{ id: string; title: string }>({
    url: '/api/ai/conversations',
    data: { title }
  })
}
export function deleteConversation(id: string) {
  return request.del<void>({ url: `/api/ai/conversations/${id}` })
}
export function conversationRetention() {
  return request.get<{ retentionDays: number }>({ url: '/api/ai/conversations/retention' })
}
export function saveConversationRetention(retentionDays: number) {
  return request.put<void>({ url: '/api/ai/conversations/retention', data: { retentionDays } })
}
export function keepConversation(id: string, retainUntil?: string) {
  return request.put<void>({ url: `/api/ai/conversations/${id}/retention`, data: { retainUntil } })
}
export function startChat(question: string, idempotencyKey: string, conversationId?: string) {
  return request.post<ChatReceipt>({
    url: '/api/ai/chat/runs',
    data: { conversationId, question, idempotencyKey }
  })
}
export function cancelChat(runId: string) {
  return request.post<void>({ url: `/api/ai/runs/${runId}/cancel` })
}
export function chatSnapshot(runId: string) {
  return request.get<ChatSnapshot>({ url: `/api/ai/runs/${runId}/snapshot` })
}
export function conversationMessages(id: string, current = 1) {
  return request.get<ManagementPage>({
    url: `/api/ai/conversations/${id}/messages`,
    params: { current, size: 200 }
  })
}
