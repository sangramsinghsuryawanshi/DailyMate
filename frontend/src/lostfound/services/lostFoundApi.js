import apiClient from '../../services/apiClient'

export const getLostFoundPosts = () => apiClient.get('/lost-found/posts').then((response) => response.data)

export const getMyLostFoundPosts = () => apiClient.get('/lost-found/my-posts').then((response) => response.data)

export const createLostFoundPost = (payload) => apiClient.post('/lost-found/posts', payload).then((response) => response.data)

export const updateLostFoundPost = (id, payload) => apiClient.patch(`/lost-found/posts/${id}`, payload).then((response) => response.data)

export const deleteLostFoundPost = (id) => apiClient.delete(`/lost-found/posts/${id}`)
