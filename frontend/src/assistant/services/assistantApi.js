import apiClient from '../../services/apiClient'

export const getAssistantConversations = () => apiClient.get('/assistant/conversations').then((response) => response.data)

export const createAssistantConversation = (payload) => apiClient.post('/assistant/conversations', payload).then((response) => response.data)

export const updateAssistantConversation = (id, payload) => apiClient.patch(`/assistant/conversations/${id}`, payload).then((response) => response.data)

export const deleteAssistantConversation = (id) => apiClient.delete(`/assistant/conversations/${id}`)
