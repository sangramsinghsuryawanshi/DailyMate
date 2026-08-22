import apiClient from '../../services/apiClient'

export const getLocalEvents = (params = {}) => {
  const queryParams = new URLSearchParams()
  if (params.category && params.category !== 'ALL') queryParams.append('category', params.category)
  if (params.status && params.status !== 'ALL') queryParams.append('status', params.status)
  const queryString = queryParams.toString()
  return apiClient.get(`/events/events${queryString ? `?${queryString}` : ''}`).then((res) => res.data)
}

export const getMyLocalEvents = () =>
  apiClient.get('/events/my-events').then((res) => res.data)

export const createLocalEvent = (payload) =>
  apiClient.post('/events/events', payload).then((res) => res.data)

export const updateLocalEvent = (id, payload) =>
  apiClient.patch(`/events/events/${id}`, payload).then((res) => res.data)

export const deleteLocalEvent = (id) =>
  apiClient.delete(`/events/events/${id}`)
