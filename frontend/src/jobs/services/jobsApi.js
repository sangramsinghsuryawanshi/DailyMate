import apiClient from '../../services/apiClient'

export const getJobs = () => apiClient.get('/jobs/posts').then((response) => response.data)

export const createJob = (payload) => apiClient.post('/jobs/posts', payload).then((response) => response.data)

export const updateJob = (id, payload) => apiClient.patch(`/jobs/posts/${id}`, payload).then((response) => response.data)

export const deleteJob = (id) => apiClient.delete(`/jobs/posts/${id}`)
