import { onBeforeUnmount, onMounted } from 'vue'

export function useEscape(handler) {
  const onKeydown = (event) => {
    if (event.key === 'Escape') handler()
  }
  onMounted(() => window.addEventListener('keydown', onKeydown))
  onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
}
