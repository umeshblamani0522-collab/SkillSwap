import api from './api'

export const matchService = {
  myMatches: (limit = 20) => api.get('/matches', { params: { limit } }).then((r) => r.data),
  matchWith: (candidateId) => api.get(`/matches/${candidateId}`).then((r) => r.data),
}
