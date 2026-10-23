import { defineStore } from 'pinia'

export const useThemeStore = defineStore('theme', {
  state: () => ({
    theme: document.documentElement.dataset.theme || 'light'
  }),
  actions: {
    apply(theme) {
      this.theme = theme
      document.documentElement.dataset.theme = theme
      localStorage.setItem('wms-theme', theme)
      const meta = document.querySelector('meta[name="theme-color"]')
      if (meta) {
        meta.setAttribute('content', theme === 'dark' ? '#101410' : '#E8EAE3')
      }
    },
    toggle() {
      this.apply(this.theme === 'dark' ? 'light' : 'dark')
    }
  }
})
