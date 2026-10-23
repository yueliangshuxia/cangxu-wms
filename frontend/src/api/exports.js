import { downloadCsv } from './http'

function stamp() {
  const now = new Date()
  return `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
}

function query(params = {}) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, value)
    }
  })
  const text = search.toString()
  return text ? `?${text}` : ''
}

export const exportInventory = (params) => downloadCsv(`/exports/inventory${query(params)}`, `inventory-${stamp()}.csv`)
export const exportLocations = () => downloadCsv('/exports/locations', `locations-${stamp()}.csv`)
export const exportInbound = (params) => downloadCsv(`/exports/inbound${query(params)}`, `inbound-${stamp()}.csv`)
export const exportOutbound = (params) => downloadCsv(`/exports/outbound${query(params)}`, `outbound-${stamp()}.csv`)
export const exportStocktakes = (params) => downloadCsv(`/exports/stocktakes${query(params)}`, `stocktakes-${stamp()}.csv`)
export const exportReturns = (params) => downloadCsv(`/exports/returns${query(params)}`, `returns-${stamp()}.csv`)
export const exportAuditLogs = (params) => downloadCsv(`/exports/audit-logs${query(params)}`, `audit-logs-${stamp()}.csv`)
export const exportMasterdata = () => downloadCsv('/exports/masterdata', `masterdata-${stamp()}.csv`)
export const exportAnalytics = (days) => downloadCsv(`/exports/analytics${query({ days })}`, `analytics-${stamp()}.csv`)
