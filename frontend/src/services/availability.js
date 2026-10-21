import api from './api'

export const availabilityService = {
  list: async () => (await api.get('/users/me/availability')).data,
  add: async (payload) => (await api.post('/users/me/availability', payload)).data,
  remove: async (id) => api.delete(`/users/me/availability/${id}`),
}
