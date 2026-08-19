import apiClient from '../../services/apiClient'

export const getAdminComplaints = () => apiClient.get('/admin/complaints').then((response) => response.data)

export const updateComplaintStatus = (id, status) => apiClient.patch(`/admin/complaints/${id}/status`, { status }).then((response) => response.data)
