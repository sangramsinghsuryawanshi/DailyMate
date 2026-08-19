import apiClient from '../../services/apiClient'

export const getEmergencyContacts = () => apiClient.get('/emergency-contacts/contacts').then((response) => response.data)

export const createEmergencyContact = (payload) => apiClient.post('/emergency-contacts/contacts', payload).then((response) => response.data)

export const updateEmergencyContact = (id, payload) => apiClient.patch(`/emergency-contacts/contacts/${id}`, payload).then((response) => response.data)

export const deleteEmergencyContact = (id) => apiClient.delete(`/emergency-contacts/contacts/${id}`)
