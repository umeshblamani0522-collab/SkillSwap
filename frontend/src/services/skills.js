import api from './api'

export const skillService = {
  categories: () => api.get('/skill-categories').then((r) => r.data),
  search: (q = '', page = 0, size = 100) =>
    api.get('/skills', { params: { q, page, size } }).then((r) => r.data),
  addUserSkill: (payload) => api.post('/users/me/skills', payload).then((r) => r.data),
  updateUserSkill: (id, payload) => api.put(`/users/me/skills/${id}`, payload).then((r) => r.data),
  deleteUserSkill: (id) => api.delete(`/users/me/skills/${id}`),
}
