import apiClient from '../../services/apiClient'

// Blood Requests
export const getBloodRequests = (params = {}) => {
  const queryParams = new URLSearchParams()
  if (params.bloodGroup) queryParams.append('bloodGroup', params.bloodGroup)
  if (params.status) queryParams.append('status', params.status)
  const queryString = queryParams.toString()
  return apiClient.get(`/blood/requests${queryString ? `?${queryString}` : ''}`).then((res) => res.data)
}

export const getMyBloodRequests = () =>
  apiClient.get('/blood/my-requests').then((res) => res.data)

export const createBloodRequest = (payload) =>
  apiClient.post('/blood/requests', payload).then((res) => res.data)

export const updateBloodRequest = (id, payload) =>
  apiClient.patch(`/blood/requests/${id}`, payload).then((res) => res.data)

export const deleteBloodRequest = (id) =>
  apiClient.delete(`/blood/requests/${id}`)

// Donation Centers
export const getDonationCenters = () =>
  apiClient.get('/blood/centers').then((res) => res.data)

export const createDonationCenter = (payload) =>
  apiClient.post('/blood/centers', payload).then((res) => res.data)

export const updateDonationCenter = (id, payload) =>
  apiClient.patch(`/blood/centers/${id}`, payload).then((res) => res.data)

export const deleteDonationCenter = (id) =>
  apiClient.delete(`/blood/centers/${id}`)
