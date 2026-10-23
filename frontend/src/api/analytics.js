import http from './http'

export const analyticsOverview = (days = 30) => http.get('/analytics/overview', { params: { days } })
