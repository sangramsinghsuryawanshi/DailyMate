import apiClient from '../../services/apiClient'

export const getCommunityComplaints = () => apiClient.get('/community-complaints/complaints').then((response) => response.data)

export const createCommunityComplaint = (payload) => apiClient.post('/community-complaints/complaints', payload).then((response) => response.data)

export const updateCommunityComplaint = (id, payload) => apiClient.patch(`/community-complaints/complaints/${id}`, payload).then((response) => response.data)

export const deleteCommunityComplaint = (id) => apiClient.delete(`/community-complaints/complaints/${id}`)
