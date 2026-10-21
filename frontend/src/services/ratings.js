import api from './api'

export const ratingService = {
  rate: (payload) => api.post('/ratings', payload).then((r) => r.data),
  forUser: (userId, page = 0, size = 20) =>
    api.get(`/ratings/user/${userId}`, { params: { page, size } }).then((r) => r.data),
  average: (userId) => api.get(`/ratings/user/${userId}/average`).then((r) => r.data),
}
