import { defineStore } from 'pinia'
import { loginApi, logoutApi, meApi } from '../api/auth'

function parseUser() {
  try {
    return JSON.parse(localStorage.getItem('wms_user') || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('wms_token') || '',
    user: parseUser()
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    displayName: (state) => state.user?.realName || state.user?.username || '未登录',
    role: (state) => state.user?.roleCode || ''
  },
  actions: {
    async login(username, password) {
      const res = await loginApi({ username, password })
      this.setSession(res.data)
      return res.data
    },
    setSession(data) {
      this.token = data.token
      this.user = data.user
      localStorage.setItem('wms_token', data.token)
      localStorage.setItem('wms_user', JSON.stringify(data.user))
    },
    async fetchMe() {
      const res = await meApi()
      this.user = res.data
      localStorage.setItem('wms_user', JSON.stringify(res.data))
    },
    async logout() {
      try {
        await logoutApi()
      } catch {
        // Token may already be expired; local cleanup is still required.
      }
      this.clear()
    },
    clear() {
      this.token = ''
      this.user = null
      localStorage.removeItem('wms_token')
      localStorage.removeItem('wms_user')
    }
  }
})
