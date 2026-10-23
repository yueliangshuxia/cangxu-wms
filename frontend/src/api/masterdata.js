import http from './http'

export const listProducts = (params) => http.get('/products', { params })
export const createProduct = (payload) => http.post('/products', payload)
export const updateProduct = (id, payload) => http.put(`/products/${id}`, payload)
export const deleteProduct = (id) => http.delete(`/products/${id}`)

export const listSuppliers = (params) => http.get('/suppliers', { params })
export const listCustomers = (params) => http.get('/customers', { params })
