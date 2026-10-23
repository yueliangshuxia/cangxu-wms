<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  createOutbound,
  createWave,
  listOutbound,
  outboundRoute,
  outboundSummary,
  pickOutbound,
  shipOutbound
} from '../api/warehouse'
import { exportOutbound } from '../api/exports'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import Icon from '../components/Icon.vue'
import { useEscape } from '../composables/useEscape'

const auth = useAuthStore()
const toast = useToastStore()

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const status = ref('')
const keyword = ref('')
const summary = ref({ total: 0, pending: 0, picking: 0, review: 0, shipped: 0 })
const loading = ref(false)
const error = ref('')
const modalOpen = ref(false)
useEscape(() => { modalOpen.value = false })
const saving = ref(false)
const route = ref({ waveNo: '', orders: 0, items: 0, totalQty: 0, stops: [] })
const zoneOrder = ['A', 'B', 'C', 'D', 'E']
const routePoints = computed(() => (route.value.stops || []).map((stop) => {
  const zi = Math.max(0, zoneOrder.indexOf(stop.zone))
  return {
    ...stop,
    x: 22 + zi * 64 + 10 + ((Math.max(1, stop.col) - 1) % 4) * 9,
    y: 52 + (Math.max(1, stop.row) - 1) * 42
  }
}))
const routePath = computed(() => {
  if (!routePoints.value.length) return ''
  return `M18 208 L18 190 ${routePoints.value.map((p) => `L${p.x} ${p.y}`).join(' ')} L342 190 L342 208`
})

const form = reactive({
  customerName: '天猫旗舰店',
  priority: 'NORMAL',
  itemCount: 5,
  totalQty: 60,
  promisedAt: '14:00'
})

const canOperate = computed(() => ['ADMIN', 'MANAGER', 'RECEIVER'].includes(auth.role))
const pages = computed(() => Math.max(1, Math.ceil(total.value / size)))
const statusLabel = { PENDING: '待分配', PICKING: '拣货中', REVIEW: '待复核', SHIPPED: '已发货' }
const statusTone = { PENDING: 'tag-neutral', PICKING: 'tag-accent', REVIEW: 'tag-warning', SHIPPED: 'tag-success' }
const tabs = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '待分配' },
  { value: 'PICKING', label: '拣货中' },
  { value: 'REVIEW', label: '待复核' },
  { value: 'SHIPPED', label: '已发货' }
]
async function loadRoute(waveNo) {
  try {
    const res = await outboundRoute(waveNo)
    route.value = res.data
  } catch {
    route.value = { waveNo: '', orders: 0, items: 0, totalQty: 0, stops: [] }
  }
}

async function download() {
  try {
    await exportOutbound({ status: status.value, keyword: keyword.value })
    toast.push('出库单已导出')
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

async function loadSummary() {
  try {
    const res = await outboundSummary()
    summary.value = res.data
  } catch {
    // ignore
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await listOutbound({ page: page.value, size, status: status.value, keyword: keyword.value })
    rows.value = res.data.records
    total.value = res.data.total
    const firstWave = rows.value.find((order) => order.waveNo)?.waveNo
    await loadRoute(firstWave)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

let timer
watch(keyword, () => {
  clearTimeout(timer)
  timer = setTimeout(() => { page.value = 1; load() }, 300)
})
watch(status, () => { page.value = 1; load() })

function goPage(target) {
  if (target < 1 || target > pages.value || target === page.value) return
  page.value = target
  load()
}

async function generateWave() {
  try {
    const res = await createWave()
    toast.push(`波次 ${res.data.waveNo} 已生成，${res.data.count} 个订单进入拣货`)
    await Promise.all([load(), loadSummary(), loadRoute(res.data.waveNo)])
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

async function pick(order) {
  try {
    await pickOutbound(order.id)
    toast.push(`${order.no} 拣货完成，进入复核`)
    await Promise.all([load(), loadSummary()])
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

async function ship(order) {
  try {
    await shipOutbound(order.id)
    toast.push(`${order.no} 已发运`)
    await Promise.all([load(), loadSummary()])
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

async function submitCreate() {
  saving.value = true
  try {
    await createOutbound({ ...form })
    toast.push('出库单已创建')
    modalOpen.value = false
    await Promise.all([load(), loadSummary()])
  } catch (e) {
    toast.push(e.message, 'err')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadSummary()
  load()
})
</script>

<template>
  <div class="page-head">
    <div>
      <div class="page-kicker"><span class="dot"></span>作业 · 出库</div>
      <h1>出库作业</h1>
      <p class="sub">订单分配、波次拣货、复核与发运，按承诺时间倒排。</p>
    </div>
    <div class="page-actions">
      <button class="btn" type="button" @click="download">
        <Icon name="download" :size="15" />导出
      </button>
      <button v-if="canOperate" class="btn" type="button" @click="modalOpen = true">
        <Icon name="send" :size="15" />新建出库单
      </button>
      <button v-if="canOperate" class="btn btn-primary" type="button" @click="generateWave">
        <Icon name="layers" :size="15" />生成波次
      </button>
    </div>
  </div>

  <div v-if="error" class="hint-box mb14" style="color: var(--danger)">
    <Icon name="triangle-alert" :size="15" /><span>出库数据加载失败：{{ error }}</span>
  </div>

  <section class="kpi-grid four" aria-label="出库概览">
    <div class="kpi">
      <div class="kpi-label"><Icon name="inbox" :size="14" />出库单</div>
      <div class="kpi-value"><strong>{{ summary.total }}</strong><span class="unit">单</span></div>
      <div class="kpi-foot"><span class="delta">数据库实时值</span><span class="muted" style="font-size: 9.5px">全部状态</span></div>
    </div>
    <div class="kpi hot">
      <div class="kpi-label"><Icon name="route" :size="14" />拣货中</div>
      <div class="kpi-value"><strong>{{ summary.picking }}</strong><span class="unit">单</span></div>
      <div class="kpi-foot"><span class="delta up">波次进行中</span><span class="muted" style="font-size: 9.5px">PICKING</span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="clipboard-check" :size="14" />待复核</div>
      <div class="kpi-value"><strong>{{ summary.review }}</strong><span class="unit">单</span></div>
      <div class="kpi-foot"><span class="delta">等待复核</span><span class="muted" style="font-size: 9.5px">REVIEW</span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="truck" :size="14" />已发货</div>
      <div class="kpi-value"><strong>{{ summary.shipped }}</strong><span class="unit">单</span></div>
      <div class="kpi-foot"><span class="delta up">已发运</span><span class="muted" style="font-size: 9.5px">SHIPPED</span></div>
    </div>
  </section>

  <div class="grid grid-12">
    <section class="card col-8">
      <div class="card-head">
        <div class="t"><h2 class="card-title">出库订单</h2><span class="card-sub">共 <strong>{{ total }}</strong> 条记录</span></div>
        <span class="tag tag-live tag-success"><span class="dot"></span>实时</span>
      </div>
      <div style="padding: 12px 18px 0">
        <div class="tabs" role="tablist" aria-label="出库状态筛选">
          <button v-for="tab in tabs" :key="tab.value" class="tab" :class="{ 'is-active': status === tab.value }" type="button" role="tab" @click="status = tab.value">{{ tab.label }}</button>
        </div>
        <div class="filter-bar">
          <div class="search-box">
            <Icon name="search" :size="15" />
            <input v-model="keyword" class="input" type="search" placeholder="搜索订单号或客户" aria-label="搜索出库订单" />
          </div>
          <button class="btn btn-sm" type="button" @click="load"><Icon name="refresh-cw" :size="13" />刷新</button>
          <span class="spacer"></span>
          <span class="filter-count">显示 <strong>{{ total }}</strong> 条</span>
        </div>
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr><th>订单号</th><th>客户</th><th style="text-align: right">数量</th><th>优先级</th><th>波次</th><th>状态</th><th>预计发运</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-for="order in rows" :key="order.id">
              <td><span class="code">{{ order.no }}</span></td>
              <td><div class="cell-main">{{ order.customerName }}</div><div class="cell-sub">{{ order.itemCount }} 个商品</div></td>
              <td class="num">{{ order.totalQty }}</td>
              <td>
                <span class="tag" :class="order.priority === 'URGENT' ? 'tag-accent' : 'tag-neutral'">
                  <span class="dot"></span>{{ order.priority === 'URGENT' ? '加急' : '普通' }}
                </span>
              </td>
              <td class="code">{{ order.waveNo || '—' }}</td>
              <td><span class="tag" :class="statusTone[order.status]"><span class="dot"></span>{{ statusLabel[order.status] || order.status }}</span></td>
              <td class="tight">{{ order.promisedAt || '—' }}</td>
              <td class="actions">
                <button v-if="order.status === 'PENDING'" class="btn btn-sm btn-primary" type="button" @click="pick(order)">拣货</button>
                <button v-else-if="order.status === 'PICKING'" class="btn btn-sm btn-primary" type="button" @click="pick(order)">拣货完成</button>
                <button v-else-if="order.status === 'REVIEW'" class="btn btn-sm btn-primary" type="button" @click="ship(order)">发运</button>
                <button v-else class="btn btn-sm btn-ghost" type="button" @click="toast.push('已打开出库单 ' + order.no)">查看</button>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="8"><div class="empty-state"><img src="/images/undraw/not-found.svg" alt="" /><p>{{ loading ? '正在加载…' : '没有匹配的出库单' }}</p></div></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination">
        <span class="pg">显示 {{ total ? (page - 1) * size + 1 : 0 }}–{{ Math.min(page * size, total) }} / 共 {{ total }} 条</span>
        <button class="pg-btn" type="button" :disabled="page === 1" @click="goPage(page - 1)">上一页</button>
        <button v-for="p in pages" :key="p" class="pg-btn" :class="{ 'is-active': p === page }" type="button" @click="goPage(p)">{{ p }}</button>
        <button class="pg-btn" type="button" :disabled="page === pages" @click="goPage(page + 1)">下一页</button>
      </div>
    </section>

    <div class="col-4 stack">
      <section class="card">
        <div class="card-head">
          <div class="t"><h2 class="card-title">波次拣货路线</h2><span class="card-sub">{{ route.waveNo || '暂无波次' }} · {{ route.stops.length }} 个库位 · {{ route.totalQty }} 件</span></div>
          <span class="tag tag-accent">{{ route.waveNo ? '实时' : '待生成' }}</span>
        </div>
        <div class="card-body">
          <svg class="route-svg" viewBox="0 0 360 236" role="img" aria-label="波次拣货路线示意图">
            <rect x="0" y="0" width="360" height="236" rx="8" fill="var(--surface-2)" />
            <g v-for="(zone, zi) in zoneOrder" :key="zone">
              <rect class="rack" :x="22 + zi * 64" y="36" width="50" height="130" rx="3" />
              <text class="lbl" :x="47 + zi * 64" y="30" text-anchor="middle">{{ zone }} 区</text>
            </g>
            <text class="lbl" x="16" y="222">起点</text>
            <text class="lbl" x="298" y="222">复核台</text>
            <path v-if="routePath" class="path" :d="routePath" />
            <circle v-for="p in routePoints" :key="p.code" class="stop" :cx="p.x" :cy="p.y" r="8" />
            <text v-for="p in routePoints" :key="'t-' + p.code" class="stop-t" :x="p.x" :y="p.y">{{ p.seq }}</text>
          </svg>
          <div class="stop-list mt12">
            <div v-for="s in route.stops" :key="s.code" class="stop-item">
              <span class="sn">{{ s.seq }}</span>
              <span class="sl"><strong>{{ s.code }}</strong><small>{{ s.productName }}</small></span>
              <span class="sq">× {{ s.qty }}</span>
            </div>
            <div v-if="!route.stops.length" class="chart-note">当前波次没有可用的拣货库位，生成波次或补充出库单明细后显示。</div>
          </div>
          <button class="btn btn-primary mt12" type="button" style="width: 100%" @click="generateWave">
            <Icon name="play" :size="15" />生成波次并开始拣货
          </button>
        </div>
      </section>

      <section class="card">
        <div class="card-body">
          <div class="side-note">
            <img class="illus illus-sm" src="/images/undraw/deliveries.svg" alt="配送插画" loading="lazy" />
            <div class="txt">
              <strong>发运交接</strong>
              <p>已发货 {{ summary.shipped }} 单，待复核 {{ summary.review }} 单。</p>
              <button class="card-link mt8" type="button" @click="toast.push('已打开发运交接单')">查看交接单<Icon name="arrow-right" :size="13" /></button>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>

  <p class="chart-note" style="margin-top: 18px">波次生成、拣货、发运与路线均来自后端；路线按库位编码分区、巷道排序。</p>

  <Teleport to="body">
    <template v-if="modalOpen">
      <div class="overlay is-open" @click="modalOpen = false"></div>
      <div class="modal is-open" role="dialog" aria-modal="true" aria-label="新建出库单">
        <div class="modal-head">
          <div><h2>新建出库单</h2><p>创建后进入待分配队列。</p></div>
          <button class="icon-btn" type="button" aria-label="关闭" @click="modalOpen = false"><Icon name="x" :size="18" /></button>
        </div>
        <form @submit.prevent="submitCreate">
          <div class="modal-body">
            <div class="form-grid">
              <div class="field">
                <label for="out-customer">客户</label>
                <input id="out-customer" v-model="form.customerName" class="input" required />
              </div>
              <div class="field">
                <label for="out-priority">优先级</label>
                <select id="out-priority" v-model="form.priority" class="select">
                  <option value="NORMAL">普通</option>
                  <option value="URGENT">加急</option>
                </select>
              </div>
              <div class="field">
                <label for="out-items">商品数</label>
                <input id="out-items" v-model.number="form.itemCount" class="input" type="number" min="1" required />
              </div>
              <div class="field">
                <label for="out-qty">总数量</label>
                <input id="out-qty" v-model.number="form.totalQty" class="input" type="number" min="1" required />
              </div>
              <div class="field">
                <label for="out-promise">承诺发运</label>
                <input id="out-promise" v-model="form.promisedAt" class="input" placeholder="14:00" />
              </div>
            </div>
          </div>
          <div class="modal-foot">
            <button class="btn" type="button" @click="modalOpen = false">取消</button>
            <button class="btn btn-primary" type="submit" :disabled="saving">
              <Icon name="check" :size="15" />{{ saving ? '创建中…' : '创建出库单' }}
            </button>
          </div>
        </form>
      </div>
    </template>
  </Teleport>
</template>
