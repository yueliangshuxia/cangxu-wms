<script setup>
import { useToastStore } from '../stores/toast'
import Icon from './Icon.vue'

const toast = useToastStore()
</script>

<template>
  <div class="toast-stack" role="status" aria-live="polite">
    <div v-for="item in toast.items" :key="item.id" class="toast" :class="item.type">
      <Icon :name="item.type === 'err' ? 'circle-x' : item.type === 'warn' ? 'triangle-alert' : 'circle-check'" :size="16" />
      <span>{{ item.message }}</span>
    </div>
  </div>
</template>
