import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

const http = axios.create({
  baseURL,
  timeout: 12000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('wms_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 200) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('wms_token')
      localStorage.removeItem('wms_user')
      if (!location.pathname.startsWith('/login')) {
        location.href = '/login'
      }
    }
    const message = error.response?.data?.message || error.message || '网络异常'
    return Promise.reject(new Error(message))
  }
)

/**
 * Downloads a CSV endpoint with the JWT header. Axios is bypassed because the
 * shared interceptor unwraps JSON envelopes and would break binary responses.
 */
export async function downloadCsv(path, filename) {
  const token = localStorage.getItem('wms_token')
  const response = await fetch(`${baseURL}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (response.status === 401) {
    localStorage.removeItem('wms_token')
    localStorage.removeItem('wms_user')
    location.href = '/login'
    throw new Error('登录已过期')
  }
  if (!response.ok) {
    let message = '导出失败'
    try {
      const body = await response.json()
      message = body.message || message
    } catch {
      // ignore non-JSON error bodies
    }
    throw new Error(message)
  }
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

export default http
