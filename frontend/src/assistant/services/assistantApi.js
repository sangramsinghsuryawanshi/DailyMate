import apiClient from '../../services/apiClient'

export const getAssistantConversations = async () => {
  try {
    const response = await apiClient.get('/assistant/conversations')
    return response.data
  } catch (err) {
    // Backend may be down in local dev — return empty list so UI remains usable
    return []
  }
}

export const createAssistantConversation = (payload) => apiClient.post('/assistant/conversations', payload).then((response) => response.data)

export const updateAssistantConversation = (id, payload) => apiClient.patch(`/assistant/conversations/${id}`, payload).then((response) => response.data)

export const deleteAssistantConversation = (id) => apiClient.delete(`/assistant/conversations/${id}`)
