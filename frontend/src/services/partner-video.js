import api from './api'

export const partnerVideoService = {
  createRoom: (partnerId) => api.post(`/partner-video/rooms/partner/${partnerId}`).then((r) => r.data),
  authorizeRoom: (roomId) => api.get(`/partner-video/rooms/${roomId}`).then((r) => r.data),
  prepareSessionRoom: (sessionId) => api.get(`/partner-video/sessions/${sessionId}/room`).then((r) => r.data),
  readyRooms: () => api.get('/partner-video/ready').then((r) => r.data),
  endRoom: (roomId) => api.post(`/partner-video/rooms/${roomId}/end`).then((r) => r.data),
}
