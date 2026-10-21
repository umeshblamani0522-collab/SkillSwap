import api from './api'

export const chatService = {
  conversations: async () => (await api.get('/chat/conversations')).data,
  send: (payload) => api.post('/chat/messages', payload).then((r) => r.data),
  deleteMessage: (messageId) => api.delete(`/chat/messages/${messageId}`),
  conversation: (otherUserId, page = 0, size = 50) =>
    api.get(`/chat/conversations/${otherUserId}`, { params: { page, size } }).then((r) => r.data),
  markRead: (otherUserId) => api.post(`/chat/conversations/${otherUserId}/read`),
  unreadCount: () => api.get('/chat/unread-count').then((r) => r.data),
  getStatus: (otherUserId) => api.get(`/chat/conversations/${otherUserId}/status`).then((r) => r.data),
  accept: (otherUserId) => api.post(`/chat/conversations/${otherUserId}/accept`),
  reject: (otherUserId) => api.post(`/chat/conversations/${otherUserId}/reject`),
  block: (otherUserId) => api.post(`/chat/conversations/${otherUserId}/block`),
  unblock: (otherUserId) => api.post(`/chat/conversations/${otherUserId}/unblock`),
}
