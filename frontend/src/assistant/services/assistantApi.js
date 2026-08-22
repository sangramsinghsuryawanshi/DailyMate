import apiClient from '../../services/apiClient'

export const getAssistantTools = () =>
  apiClient.get('/assistant/tools').then((response) => response.data)

export const getAssistantConversations = () =>
  apiClient.get('/assistant/conversations').then((response) => response.data)

export const sendAssistantChat = (prompt, conversationId = null) =>
  apiClient.post('/assistant/chat', { prompt, conversationId }).then((response) => response.data)

export const confirmAssistantAction = (actionId, idempotencyKey) =>
  apiClient
    .post(`/assistant/actions/${actionId}/confirm`, { idempotencyKey })
    .then((response) => response.data)

export const cancelAssistantAction = (actionId) =>
  apiClient.post(`/assistant/actions/${actionId}/cancel`).then((response) => response.data)

export const deleteAssistantConversation = (id) =>
  apiClient.delete(`/assistant/conversations/${id}`).then((response) => response.data)
