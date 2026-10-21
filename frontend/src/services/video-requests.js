import api from './api'

export const videoRequestService = {
  create: (partnerId) => api.post(`/video-requests/partner/${partnerId}`).then((r) => r.data),
  createForSession: (sessionId) => api.post(`/video-requests/session/${sessionId}`).then((r) => r.data),
  incoming: (page = 0, size = 20) => api.get('/video-requests/incoming', { params: { page, size } }).then((r) => r.data),
  mine: (page = 0, size = 20) => api.get('/video-requests', { params: { page, size } }).then((r) => r.data),
  accept: (id) => api.post(`/video-requests/${id}/accept`).then((r) => r.data),
  reject: (id) => api.post(`/video-requests/${id}/reject`).then((r) => r.data),
  cancel: (id) => api.post(`/video-requests/${id}/cancel`).then((r) => r.data),
}
