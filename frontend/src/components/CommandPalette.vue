<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { flatNav } from '../config/nav'
import Icon from './Icon.vue'

const props = defineProps({ open: Boolean })
const emit = defineEmits(['close'])
const router = useRouter()
const query = ref('')
const active = ref(0)
const inputRef = ref(null)

const items = computed(() => {
  const q = query.value.trim().toLowerCase()
  return flatNav.filter((item) => !q || item.label.toLowerCase().includes(q))
})

watch(
  () => props.open,
  async (open) => {
    if (open) {
      query.value = ''
      active.value = 0
      await nextTick()
      inputRef.value?.focus()
    }
  }
)

watch(items, () => {
  active.value = 0
})

function choose(item) {
  if (!item) return
  router.push(item.path)
  emit('close')
}

function onKey(event) {
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    active.value = (active.value + 1) % Math.max(1, items.value.length)
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    active.value = (active.value - 1 + Math.max(1, items.value.length)) % Math.max(1, items.value.length)
  } else if (event.key === 'Enter') {
    choose(items.value[active.value])
  } else if (event.key === 'Escape') {
    emit('close')
  }
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      style="position: fixed; inset: 0; z-index: 120; background: rgba(10, 14, 10, 0.42)"
      @click.self="emit('close')"
    >
      <div
        class="cmdk is-open"
        style="position: relative; left: auto; top: auto; transform: none; margin: 14vh auto 0"
      >
        <div class="cmdk-input">
          <Icon name="search" />
          <input
            ref="inputRef"
            v-model="query"
            type="text"
            placeholder="搜索页面…"
            aria-label="搜索页面"
            @keydown="onKey"
          />
        </div>
        <div class="cmdk-list">
          <div v-if="!items.length" class="cmdk-empty">没有匹配的结果</div>
          <div
            v-for="(item, index) in items"
            :key="item.path"
            class="cmdk-item"
            :class="{ 'is-active': index === active }"
            @mouseenter="active = index"
            @click="choose(item)"
          >
            <Icon :name="item.icon" :size="15" />
            <span>{{ item.label }}</span>
            <span class="k">页面</span>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
