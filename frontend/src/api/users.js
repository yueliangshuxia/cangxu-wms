import http from './http'

export const listUsers = (params) => http.get('/users', { params })
export const listRoles = () => http.get('/users/roles')
export const createUser = (payload) => http.post('/users', payload)
export const updateUser = (id, payload) => http.put(`/users/${id}`, payload)
export const deleteUser = (id) => http.delete(`/users/${id}`)
export const resetUserPassword = (id, password) =>
  http.post(`/users/${id}/reset-password`, { password })
