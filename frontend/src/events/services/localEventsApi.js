import apiClient from '../../services/apiClient'

export const getLocalEvents = () => apiClient.get('/events/events').then((response) => response.data)

export const createLocalEvent = (payload) => apiClient.post('/events/events', payload).then((response) => response.data)

export const updateLocalEvent = (id, payload) => apiClient.patch(`/events/events/${id}`, payload).then((response) => response.data)

export const deleteLocalEvent = (id) => apiClient.delete(`/events/events/${id}`)
