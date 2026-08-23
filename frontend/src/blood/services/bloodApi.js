import apiClient from '../../services/apiClient'

// Blood Requests
export const getBloodRequests = (params = {}) => {
  const query = typeof params === 'object' ? params : {}
  return apiClient.get('/blood/requests', { params: query }).then((res) => res.data)
}

export const getMyBloodRequests = (params = {}) => {
  const query = typeof params === 'object' ? params : {}
  return apiClient.get('/blood/my-requests', { params: query }).then((res) => res.data)
}

export const createBloodRequest = (payload) =>
  apiClient.post('/blood/requests', payload).then((res) => res.data)

export const updateBloodRequest = (id, payload) =>
  apiClient.patch(`/blood/requests/${id}`, payload).then((res) => res.data)

export const deleteBloodRequest = (id) =>
  apiClient.delete(`/blood/requests/${id}`)

// Donation Centers
export const getDonationCenters = (params = {}) => {
  const query = typeof params === 'object' ? params : {}
  return apiClient.get('/blood/centers', { params: query }).then((res) => res.data)
}

export const createDonationCenter = (payload) =>
  apiClient.post('/blood/centers', payload).then((res) => res.data)

export const updateDonationCenter = (id, payload) =>
  apiClient.patch(`/blood/centers/${id}`, payload).then((res) => res.data)

export const deleteDonationCenter = (id) =>
  apiClient.delete(`/blood/centers/${id}`)
