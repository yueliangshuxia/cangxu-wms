<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { navGroups, pageMeta } from '../config/nav'
import { listAlerts } from '../api/alerts'
import { useAuthStore } from '../stores/auth'
import { useThemeStore } from '../stores/theme'
import { useToastStore } from '../stores/toast'
import Icon from '../components/Icon.vue'
import CommandPalette from '../components/CommandPalette.vue'
import ToastHost from '../components/ToastHost.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()
const toast = useToastStore()

const railOpen = ref(false)
const paletteOpen = ref(false)
const notifOpen = ref(false)
const userOpen = ref(false)
const hasUnread = ref(false)
const notifications = ref([])

async function loadAlerts() {
  try {
    const res = await listAlerts()
    notifications.value = res.data
    hasUnread.value = notifications.value.length > 0
  } catch {
    notifications.value = []
    hasUnread.value = false
  }
}

function openNotification(item) {
  notifOpen.value = false
  if (item.route) {
    router.push(item.route)
  }
}

const meta = computed(() => pageMeta[route.path] || { title: '工作台', desc: '' })
const initial = computed(() => (auth.displayName || '仓').slice(0, 1))
const roleLabel = computed(() => {
  const map = { ADMIN: '系统管理员', MANAGER: '仓储主管', RECEIVER: '收货员', INSPECTOR: '质检员', DEVICE_OPS: '设备运维' }
  return map[auth.role] || '成员'
})

function isActive(path) {
  return route.path === path || route.path.startsWith(path + '/')
}

function toggleNotif() {
  notifOpen.value = !notifOpen.value
  userOpen.value = false
}

function toggleUser() {
  userOpen.value = !userOpen.value
  notifOpen.value = false
}

function readAll() {
  hasUnread.value = false
  toast.push('通知已全部标记为已读')
}

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}

function onKeydown(event) {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    paletteOpen.value = !paletteOpen.value
  }
  if (event.key === 'Escape') {
    notifOpen.value = false
    userOpen.value = false
    railOpen.value = false
  }
}

function closePopovers(event) {
  if (!event.target.closest('.pop-wrap')) {
    notifOpen.value = false
    userOpen.value = false
  }
}

watch(
  () => route.path,
  () => {
    railOpen.value = false
    notifOpen.value = false
    userOpen.value = false
    toast.clear()
  }
)

onMounted(async () => {
  document.addEventListener('keydown', onKeydown)
  document.addEventListener('click', closePopovers)
  loadAlerts()
  if (auth.token && !auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      auth.clear()
      router.push('/login')
    }
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  document.removeEventListener('click', closePopovers)
})
</script>

<template>
  <div class="app">
    <aside class="rail" :class="{ 'is-open': railOpen }" aria-label="主导航">
      <div class="rail-top">
        <RouterLink class="brand" to="/dashboard" aria-label="仓序 WMS 首页">
          <span class="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M4 7h16M4 12h16M4 17h10" />
              <circle cx="18" cy="17" r="2" fill="currentColor" stroke="none" />
            </svg>
          </span>
          <span class="brand-text"><strong>仓序</strong><small>CANGXU WMS</small></span>
        </RouterLink>
        <button class="wh-switch" type="button" @click="toast.push('仓库切换为「华东一号仓」')">
          <span class="wh-dot"></span>
          <span class="wh-meta"><strong>华东一号仓</strong><small>苏州 · 华东运营中心</small></span>
          <Icon name="chevron-down" :size="15" />
        </button>
      </div>

      <nav class="rail-nav">
        <div v-for="group in navGroups" :key="group.group" class="nav-group">
          <p class="nav-label">{{ group.group }}</p>
          <RouterLink
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ 'is-active': isActive(item.path) }"
            @click="railOpen = false"
          >
            <Icon :name="item.icon" :size="17" />
            <span>{{ item.label }}</span>
            <span v-if="item.badge" class="nav-badge">{{ item.badge }}</span>
          </RouterLink>
        </div>
      </nav>

      <div class="rail-bottom">
        <div class="rail-user">
          <span class="avatar">{{ initial }}</span>
          <span class="meta"><strong>{{ auth.displayName }}</strong><small>{{ roleLabel }}</small></span>
          <button class="out" type="button" aria-label="退出登录" title="退出登录" @click="handleLogout">
            <Icon name="log-out" :size="15" />
          </button>
        </div>
      </div>
    </aside>

    <div class="shell">
      <header class="topbar">
        <button class="icon-btn rail-toggle" type="button" aria-label="打开导航" @click="railOpen = !railOpen">
          <Icon name="menu" />
        </button>
        <div class="crumbs">
          <span>华东一号仓</span>
          <Icon name="chevron-right" :size="13" />
          <strong>{{ meta.title }}</strong>
        </div>
        <div class="topbar-right">
          <button class="search-trigger" type="button" aria-label="搜索" @click="paletteOpen = true">
            <Icon name="search" :size="15" />
            <span>搜索页面、商品、库位</span>
            <kbd>Ctrl K</kbd>
          </button>
          <span class="sys-live"><i></i>系统在线</span>
          <button
            class="icon-btn"
            type="button"
            :aria-label="theme.theme === 'dark' ? '切换到浅色模式' : '切换到深色模式'"
            @click="theme.toggle()"
          >
            <Icon :name="theme.theme === 'dark' ? 'sun' : 'moon'" />
          </button>

          <div class="pop-wrap">
            <button class="icon-btn" type="button" aria-label="通知" style="position: relative" @click.stop="toggleNotif">
              <Icon name="bell" />
              <span
                v-if="hasUnread"
                style="position: absolute; top: 7px; right: 8px; width: 6px; height: 6px; border-radius: 50%; background: var(--accent)"
              ></span>
            </button>
            <div class="popover" :class="{ 'is-open': notifOpen }">
              <div class="pop-head"><strong>通知</strong><button type="button" @click="readAll">全部已读</button></div>
              <div v-for="n in notifications" :key="n.id" class="notif-item" style="cursor: pointer" @click="openNotification(n)">
                <span class="nicon" :class="n.level === 'danger' ? 'danger' : n.level === 'info' ? 'info' : ''">
                  <Icon :name="n.icon" :size="15" />
                </span>
                <span class="ntext">{{ n.title }}<small>{{ n.meta }}</small></span>
              </div>
              <div v-if="!notifications.length" class="notif-item">
                <span class="ntext">暂无待处理预警<small>库存、设备和退货状态正常</small></span>
              </div>
            </div>
          </div>

          <div class="pop-wrap">
            <button class="user-btn" type="button" aria-label="账户菜单" @click.stop="toggleUser">
              <span class="avatar">{{ initial }}</span>
              <span class="user-meta"><strong>{{ auth.displayName }}</strong><small>{{ roleLabel }}</small></span>
              <Icon name="chevron-down" :size="14" />
            </button>
            <div class="popover user-pop" :class="{ 'is-open': userOpen }">
              <RouterLink to="/settings" @click="userOpen = false"><Icon name="user" :size="15" />账户与权限</RouterLink>
              <RouterLink to="/settings" @click="userOpen = false"><Icon name="bell" :size="15" />通知设置</RouterLink>
              <div class="sep"></div>
              <button type="button" @click="theme.toggle()">
                <Icon :name="theme.theme === 'dark' ? 'sun' : 'moon'" :size="15" />切换主题
              </button>
              <button type="button" @click="handleLogout"><Icon name="log-out" :size="15" />退出登录</button>
            </div>
          </div>
        </div>
      </header>

      <main class="main"><RouterView /></main>
    </div>

    <div class="scrim" :class="{ 'is-open': railOpen }" @click="railOpen = false"></div>
    <CommandPalette :open="paletteOpen" @close="paletteOpen = false" />
    <ToastHost />
  </div>
</template>
