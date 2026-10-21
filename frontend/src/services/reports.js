import api from './api'

export const reportService = {
  create: (payload) => api.post('/reports', payload).then((r) => r.data),
  byStatus: (status, page = 0, size = 20) =>
    api.get('/reports', { params: { status, page, size } }).then((r) => r.data),
  resolve: (id, payload) => api.post(`/reports/${id}/resolve`, payload).then((r) => r.data),
}
