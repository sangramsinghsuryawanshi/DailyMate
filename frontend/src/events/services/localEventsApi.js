import apiClient from '../../services/apiClient'

export const getLocalEvents = (params = {}) => {
  const query = typeof params === 'object' ? params : {}
  return apiClient.get('/events/events', { params: query }).then((res) => res.data)
}

export const getMyLocalEvents = (params = {}) => {
  const query = typeof params === 'object' ? params : {}
  return apiClient.get('/events/my-events', { params: query }).then((res) => res.data)
}

export const createLocalEvent = (payload) =>
  apiClient.post('/events/events', payload).then((res) => res.data)

export const updateLocalEvent = (id, payload) =>
  apiClient.patch(`/events/events/${id}`, payload).then((res) => res.data)

export const deleteLocalEvent = (id) =>
  apiClient.delete(`/events/events/${id}`)
