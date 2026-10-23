import { defineStore } from 'pinia'

let seq = 0

export const useToastStore = defineStore('toast', {
  state: () => ({ items: [] }),
  actions: {
    push(message, type = 'ok') {
      const id = ++seq
      this.items.push({ id, message, type })
      setTimeout(() => this.remove(id), 3400)
    },
    remove(id) {
      this.items = this.items.filter((item) => item.id !== id)
    },
    clear() {
      this.items = []
    }
  }
})
