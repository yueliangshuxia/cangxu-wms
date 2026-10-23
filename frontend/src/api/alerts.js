import http from './http'

export const listAlerts = () => http.get('/alerts')
