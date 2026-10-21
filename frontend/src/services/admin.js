import api from './api'

export const adminService = {
  dashboard: () => api.get('/admin/dashboard').then((r) => r.data),
  users: (page = 0, size = 20) => api.get('/admin/users', { params: { page, size } }).then((r) => r.data),
  suspend: (id, reason) => api.post(`/admin/users/${id}/suspend`, { reason }).then((r) => r.data),
  unsuspend: (id, reason) => api.post(`/admin/users/${id}/unsuspend`, { reason }).then((r) => r.data),
  createSkill: (payload) => api.post('/admin/skills', payload).then((r) => r.data),
  createCategory: (payload) => api.post('/admin/skill-categories', payload).then((r) => r.data),
  deactivateSkill: (id) => api.post(`/admin/skills/${id}/deactivate`),
  activateSkill: (id) => api.post(`/admin/skills/${id}/activate`),
  adjustCredits: (userId, amount, reason) =>
    api.post(`/admin/credits/${userId}/adjust`, { amount, reason }),

  deleteUser: (id) =>
    api.delete(`/admin/users/${id}`),

  permanentBlockAndDelete: (id) =>
    api.post(`/admin/users/${id}/permanent-block-delete`),
}
