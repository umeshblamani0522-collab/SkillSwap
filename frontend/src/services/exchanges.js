import api from './api'

export const exchangeService = {
  create: (payload) => api.post('/exchange-requests', payload).then((r) => r.data),
  mine: (page = 0, size = 20) => api.get('/exchange-requests', { params: { page, size } }).then((r) => r.data),
  getById: (id) => api.get(`/exchange-requests/${id}`).then((r) => r.data),
  accept: (id) => api.post(`/exchange-requests/${id}/accept`).then((r) => r.data),
  reject: (id) => api.post(`/exchange-requests/${id}/reject`).then((r) => r.data),
  cancel: (id) => api.post(`/exchange-requests/${id}/cancel`).then((r) => r.data),
}
