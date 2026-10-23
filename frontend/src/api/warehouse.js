import http from './http'

export const listInventory = (params) => http.get('/inventory', { params })
export const inventorySummary = () => http.get('/inventory/summary')

export const listLocations = () => http.get('/locations')
export const locationSummary = () => http.get('/locations/summary')
export const updateLocationStatus = (code, status) => http.patch(`/locations/${code}/status`, { status })

export const listInbound = (params) => http.get('/inbound/orders', { params })
export const inboundSummary = () => http.get('/inbound/summary')
export const createInbound = (payload) => http.post('/inbound/orders', payload)
export const receiveInbound = (id) => http.post(`/inbound/orders/${id}/receive`)
export const inspectInbound = (id) => http.post(`/inbound/orders/${id}/inspect`)
export const slotRecommendation = (id) => http.get(`/inbound/orders/${id}/slot-recommendation`)
export const putawayInbound = (id, locationCode) =>
  http.post(`/inbound/orders/${id}/putaway`, { locationCode })

export const listOutbound = (params) => http.get('/outbound/orders', { params })
export const outboundSummary = () => http.get('/outbound/summary')
export const outboundRoute = (waveNo) => http.get('/outbound/route', { params: { waveNo } })
export const createOutbound = (payload) => http.post('/outbound/orders', payload)
export const createWave = () => http.post('/outbound/waves')
export const pickOutbound = (id) => http.post(`/outbound/orders/${id}/pick`)
export const shipOutbound = (id) => http.post(`/outbound/orders/${id}/ship`)

export const listDevices = () => http.get('/devices')
export const deviceSummary = () => http.get('/devices/summary')
export const deviceTelemetry = (code, metric, hours = 24) =>
  http.get(`/devices/${code}/telemetry`, { params: { metric, hours } })
export const updateDevice = (code, payload) => http.put(`/devices/${code}`, payload)

export const listAuditLogs = (params) => http.get('/audit-logs', { params })
export const auditStats = () => http.get('/audit-logs/stats')

export const listStocktakes = (params) => http.get('/stocktakes', { params })
export const stocktakeSummary = () => http.get('/stocktakes/summary')
export const stocktakeDiffs = () => http.get('/stocktakes/diffs')
export const createStocktake = (payload) => http.post('/stocktakes', payload)
export const startStocktake = (id) => http.post(`/stocktakes/${id}/start`)
export const scanStocktake = (id, code) => http.post(`/stocktakes/${id}/scan`, { code })

export const listReturns = (params) => http.get('/returns', { params })
export const returnSummary = () => http.get('/returns/summary')
export const createReturn = (payload) => http.post('/returns', payload)
export const inspectReturn = (id, payload) => http.post(`/returns/${id}/inspect`, payload)

export const getSettings = () => http.get('/settings')
export const saveSettings = (values) => http.put('/settings', values)
