<script setup>
const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 18 }
})

const dimension = (value) => (typeof value === 'number' ? `${value}px` : value)
</script>

<template>
  <svg
    class="icon"
    :style="{ width: dimension(props.size), height: dimension(props.size) }"
    aria-hidden="true"
  >
    <use :href="`#i-${props.name}`" />
  </svg>
</template>
