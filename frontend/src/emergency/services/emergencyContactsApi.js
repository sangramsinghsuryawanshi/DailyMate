import apiClient from '../../services/apiClient'

export const getEmergencyContacts = (params = {}) => {
  const queryParams = new URLSearchParams()
  if (params.category && params.category !== 'ALL') queryParams.append('category', params.category)
  const queryString = queryParams.toString()
  return apiClient.get(`/emergency-contacts/contacts${queryString ? `?${queryString}` : ''}`).then((res) => res.data)
}

export const getMyEmergencyContacts = (params = {}) => {
  const queryParams = new URLSearchParams()
  if (params.category && params.category !== 'ALL') queryParams.append('category', params.category)
  const queryString = queryParams.toString()
  return apiClient.get(`/emergency-contacts/my-contacts${queryString ? `?${queryString}` : ''}`).then((res) => res.data)
}

export const createEmergencyContact = (payload) =>
  apiClient.post('/emergency-contacts/contacts', payload).then((res) => res.data)

export const updateEmergencyContact = (id, payload) =>
  apiClient.patch(`/emergency-contacts/contacts/${id}`, payload).then((res) => res.data)

export const deleteEmergencyContact = (id) =>
  apiClient.delete(`/emergency-contacts/contacts/${id}`)
