import apiClient from '../../services/apiClient'

export const getDonationCenters = () => apiClient.get('/blood/centers').then((response) => response.data)

export const createDonationCenter = (payload) => apiClient.post('/blood/centers', payload).then((response) => response.data)

export const updateDonationCenter = (id, payload) => apiClient.patch(`/blood/centers/${id}`, payload).then((response) => response.data)

export const deleteDonationCenter = (id) => apiClient.delete(`/blood/centers/${id}`)
