import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import MasterDataView from '../views/MasterDataView.vue'
import InventoryView from '../views/InventoryView.vue'
import LocationsView from '../views/LocationsView.vue'
import InboundView from '../views/InboundView.vue'
import OutboundView from '../views/OutboundView.vue'
import DevicesView from '../views/DevicesView.vue'
import AuditView from '../views/AuditView.vue'
import StocktakeView from '../views/StocktakeView.vue'
import ReturnsView from '../views/ReturnsView.vue'
import AnalyticsView from '../views/AnalyticsView.vue'
import SettingsView from '../views/SettingsView.vue'
import { pageMeta } from '../config/nav'


const routes = [
  {
    path: '/login',
    component: LoginView,
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', component: DashboardView, meta: { title: pageMeta['/dashboard'].title } },
      { path: 'masterdata', component: MasterDataView, meta: { title: pageMeta['/masterdata'].title } },
      { path: 'inventory', component: InventoryView, meta: { title: pageMeta['/inventory'].title } },
      { path: 'locations', component: LocationsView, meta: { title: pageMeta['/locations'].title } },
      { path: 'inbound', component: InboundView, meta: { title: pageMeta['/inbound'].title } },
      { path: 'outbound', component: OutboundView, meta: { title: pageMeta['/outbound'].title } },
      { path: 'devices', component: DevicesView, meta: { title: pageMeta['/devices'].title } },
      { path: 'audit', component: AuditView, meta: { title: pageMeta['/audit'].title } },
      { path: 'stocktake', component: StocktakeView, meta: { title: pageMeta['/stocktake'].title } },
      { path: 'returns', component: ReturnsView, meta: { title: pageMeta['/returns'].title } },
      { path: 'analytics', component: AnalyticsView, meta: { title: pageMeta['/analytics'].title } },
      { path: 'settings', component: SettingsView, meta: { title: pageMeta['/settings'].title } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  document.title = `${to.meta.title || '仓序 WMS'} · 仓序 WMS`
  const token = localStorage.getItem('wms_token')
  if (!to.meta.public && !token) {
    return { path: '/login' }
  }
  if (to.path === '/login' && token) {
    return { path: '/dashboard' }
  }
})

export default router
