import api from './api'

export const creditService = {
  wallet: () => api.get('/credits/wallet').then((r) => r.data),
  transactions: (page = 0, size = 20) =>
    api.get('/credits/transactions', { params: { page, size } }).then((r) => r.data),
}
