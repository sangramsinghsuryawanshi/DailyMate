import apiClient from '../../services/apiClient'

// 1. Statistics
export const getAdminStats = () => apiClient.get('/admin/stats').then((res) => res.data)

// 2. Complaints Moderation
export const getAdminComplaints = () => apiClient.get('/admin/complaints').then((res) => res.data)
export const updateComplaintStatus = (id, status) =>
  apiClient.patch(`/admin/complaints/${id}/status`, { status }).then((res) => res.data)

// 3. Lost & Found Moderation
export const getAdminLostFound = () => apiClient.get('/admin/lost-found').then((res) => res.data)
export const deleteAdminLostFound = (id) => apiClient.delete(`/admin/lost-found/${id}`).then((res) => res.data)

// 4. Jobs Moderation
export const getAdminJobs = () => apiClient.get('/admin/jobs').then((res) => res.data)
export const updateAdminJobStatus = (id, status) =>
  apiClient.patch(`/admin/jobs/${id}/status`, { status }).then((res) => res.data)
export const deleteAdminJob = (id) => apiClient.delete(`/admin/jobs/${id}`).then((res) => res.data)

// 5. Blood Requests Moderation
export const getAdminBloodRequests = () => apiClient.get('/admin/blood-requests').then((res) => res.data)
export const updateAdminBloodRequestStatus = (id, status) =>
  apiClient.patch(`/admin/blood-requests/${id}/status`, { status }).then((res) => res.data)
export const deleteAdminBloodRequest = (id) => apiClient.delete(`/admin/blood-requests/${id}`).then((res) => res.data)

// 6. Events Moderation
export const getAdminEvents = () => apiClient.get('/admin/events').then((res) => res.data)
export const updateAdminEventStatus = (id, status) =>
  apiClient.patch(`/admin/events/${id}/status`, { status }).then((res) => res.data)
export const deleteAdminEvent = (id) => apiClient.delete(`/admin/events/${id}`).then((res) => res.data)

// 7. Users Management
export const getAdminUsers = () => apiClient.get('/admin/users').then((res) => res.data)
export const updateAdminUserStatus = (id, status) =>
  apiClient.patch(`/admin/users/${id}/status`, { status }).then((res) => res.data)
