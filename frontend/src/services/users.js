import api from './api'

export const userService = {
  me: () => api.get('/users/me').then((r) => r.data),
  updateMe: (payload) => api.put('/users/me', payload).then((r) => r.data),
  getById: (id) => api.get(`/users/${id}`).then((r) => r.data),
  search: (q, page = 0, size = 20) =>
    api.get('/users', { params: { q, page, size } }).then((r) => r.data),
}
