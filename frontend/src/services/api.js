import axios from 'axios'

// Use relative API URLs so the same frontend works on localhost or another PC
// over the local network. Vite proxies /api to Spring Boot.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const publicAuthPaths = [
    '/auth/login',
    '/auth/register',
    '/auth/forgot-password',
    '/auth/reset-password',
    '/auth/verify-email',
    '/auth/resend-verification',
  ]

  const isPublicAuthRequest = publicAuthPaths.some((path) =>
    config.url?.endsWith(path)
  )

  if (!isPublicAuthRequest) {
    const token = localStorage.getItem('se_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('se_token')
      localStorage.removeItem('se_user')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default api
