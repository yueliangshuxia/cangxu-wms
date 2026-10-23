import http from './http'

export const getOverview = () => http.get('/dashboard/overview')
