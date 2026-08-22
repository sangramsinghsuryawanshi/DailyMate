import apiClient from '../../services/apiClient'

export const getJobs = (params = {}) => {
  const queryParams = new URLSearchParams()
  if (params.search) queryParams.append('search', params.search)
  if (params.category && params.category !== 'ALL') queryParams.append('category', params.category)
  if (params.type && params.type !== 'ALL') queryParams.append('type', params.type)
  if (params.status && params.status !== 'ALL') queryParams.append('status', params.status)
  const queryString = queryParams.toString()
  return apiClient.get(`/jobs/posts${queryString ? `?${queryString}` : ''}`).then((response) => response.data)
}

export const getMyJobs = () => apiClient.get('/jobs/my-posts').then((response) => response.data)

export const createJob = (payload) => apiClient.post('/jobs/posts', payload).then((response) => response.data)

export const updateJob = (id, payload) => apiClient.patch(`/jobs/posts/${id}`, payload).then((response) => response.data)

export const deleteJob = (id) => apiClient.delete(`/jobs/posts/${id}`)
