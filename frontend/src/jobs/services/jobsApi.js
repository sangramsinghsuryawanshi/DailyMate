import apiClient from '../../services/apiClient'

export const getJobs = (params = {}) => {
  const query = typeof params === 'object' ? params : {}
  return apiClient.get('/jobs/posts', { params: query }).then((response) => response.data)
}

export const getMyJobs = (params = {}) => {
  const query = typeof params === 'object' ? params : {}
  return apiClient.get('/jobs/my-posts', { params: query }).then((response) => response.data)
}

export const createJob = (payload) => apiClient.post('/jobs/posts', payload).then((response) => response.data)

export const updateJob = (id, payload) => apiClient.patch(`/jobs/posts/${id}`, payload).then((response) => response.data)

export const deleteJob = (id) => apiClient.delete(`/jobs/posts/${id}`)
