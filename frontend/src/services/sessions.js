import api from './api'

export const sessionService = {
  schedule: (payload) => api.post('/sessions', payload).then((r) => r.data),
  mine: () => api.get('/sessions').then((r) => r.data),
  getById: (id) => api.get(`/sessions/${id}`).then((r) => r.data),
  complete: (id) => api.post(`/sessions/${id}/complete`).then((r) => r.data),
  cancel: (id) => api.post(`/sessions/${id}/cancel`).then((r) => r.data),
  noShow: (id) => api.post(`/sessions/${id}/no-show`).then((r) => r.data),
  remove: (id) => api.delete(`/sessions/${id}`),
  videoToken: (id) => api.get(`/sessions/${id}/video-token`).then((r) => r.data),
}
