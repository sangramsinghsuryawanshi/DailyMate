import apiClient from '../../services/apiClient'

export const getLostFoundPosts = (params = {}) => {
  const query = typeof params === 'object' ? params : {}
  return apiClient.get('/lost-found/posts', { params: query }).then((response) => response.data)
}

export const getMyLostFoundPosts = (params = {}) => {
  const query = typeof params === 'object' ? params : {}
  return apiClient.get('/lost-found/my-posts', { params: query }).then((response) => response.data)
}

export const createLostFoundPost = (payload) => apiClient.post('/lost-found/posts', payload).then((response) => response.data)

export const updateLostFoundPost = (id, payload) => apiClient.patch(`/lost-found/posts/${id}`, payload).then((response) => response.data)

export const deleteLostFoundPost = (id) => apiClient.delete(`/lost-found/posts/${id}`)
