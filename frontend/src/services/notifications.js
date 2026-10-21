import api from './api'

export const notificationService = {
  list: (page = 0, size = 20) => api.get('/notifications', { params: { page, size } }).then((r) => r.data),
  unreadCount: () => api.get('/notifications/unread-count').then((r) => r.data),
  markRead: (id) => api.post(`/notifications/${id}/read`),
  remove: (id) => api.delete(`/notifications/${id}`),
}
