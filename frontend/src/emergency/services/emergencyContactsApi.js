import apiClient from '../../services/apiClient'

export const getEmergencyContacts = (params = {}) => {
  const query = typeof params === 'object' ? params : {}
  return apiClient.get('/emergency-contacts/contacts', { params: query }).then((res) => res.data)
}

export const getMyEmergencyContacts = (params = {}) => {
  const query = typeof params === 'object' ? params : {}
  return apiClient.get('/emergency-contacts/my-contacts', { params: query }).then((res) => res.data)
}

export const createEmergencyContact = (payload) =>
  apiClient.post('/emergency-contacts/contacts', payload).then((res) => res.data)

export const updateEmergencyContact = (id, payload) =>
  apiClient.patch(`/emergency-contacts/contacts/${id}`, payload).then((res) => res.data)

export const deleteEmergencyContact = (id) =>
  apiClient.delete(`/emergency-contacts/contacts/${id}`)
