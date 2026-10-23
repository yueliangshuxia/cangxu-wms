<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  createInbound,
  inboundSummary,
  inspectInbound,
  listInbound,
  putawayInbound,
  receiveInbound,
  slotRecommendation
} from '../api/warehouse'
import { exportInbound } from '../api/exports'
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
const summary = ref({ total: 0, pending: 0, ready: 0, done: 0 })
const loading = ref(false)
const error = ref('')
const modalOpen = ref(false)
useEscape(() => {
  if (drawerOpen.value) drawerOpen.value = false
  else modalOpen.value = false
})
const saving = ref(false)
const drawerOpen = ref(false)

async function download() {
  try {
    await exportInbound({ status: status.value, keyword: keyword.value })
    toast.push('入库单已导出')
  } catch (e) {
    toast.push(e.message, 'err')
  }
}
const currentOrder = ref(null)
const recommendation = ref(null)
const recLoading = ref(false)

const form = reactive({
  supplierName: '深圳华创电子',
  sku: 'WH-1001',
  productName: '降噪蓝牙耳机',
  qty: 120,
  dock: '1 号月台',
  eta: '2026-10-21T09:00'
})

const canCreate = computed(() => ['ADMIN', 'MANAGER', 'RECEIVER'].includes(auth.role))
const pages = computed(() => Math.max(1, Math.ceil(total.value / size)))

const statusLabel = {
  PENDING: '待收货', RECEIVED: '已收货', INSPECTING: '质检中', READY: '待上架', DONE: '已完成'
}
const statusTone = {
  PENDING: 'tag-info', RECEIVED: 'tag-info', INSPECTING: 'tag-warning', READY: 'tag-accent', DONE: 'tag-success'
}
const tabs = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '待收货' },
  { value: 'RECEIVED', label: '已收货' },
  { value: 'INSPECTING', label: '质检中' },
  { value: 'READY', label: '待上架' },
  { value: 'DONE', label: '已完成' }
]

const firstItem = (order) => order.items?.[0] || {}
const formatTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 16) : '—')

async function loadSummary() {
  try {
    const res = await inboundSummary()
    summary.value = res.data
  } catch {
    // Table error covers the visible state.
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await listInbound({
      page: page.value, size, status: status.value, keyword: keyword.value
    })
    rows.value = res.data.records
    total.value = res.data.total
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

async function receive(order) {
  try {
    await receiveInbound(order.id)
    toast.push(`${order.no} 已收货`)
    await Promise.all([load(), loadSummary()])
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

async function inspect(order) {
  try {
    await inspectInbound(order.id)
    toast.push(`${order.no} 质检完成，进入待上架`)
    await Promise.all([load(), loadSummary()])
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

async function openRecommendation(order) {
  currentOrder.value = order
  recommendation.value = null
  drawerOpen.value = true
  recLoading.value = true
  try {
    const res = await slotRecommendation(order.id)
    recommendation.value = res.data
  } catch (e) {
    toast.push(e.message, 'err')
  } finally {
    recLoading.value = false
  }
}

async function confirmPutaway() {
  if (!currentOrder.value || !recommendation.value?.candidates?.length) return
  try {
    const code = recommendation.value.candidates[0].code
    await putawayInbound(currentOrder.value.id, code)
    toast.push(`${currentOrder.value.no} 已按推荐库位 ${code} 上架`)
    drawerOpen.value = false
    await Promise.all([load(), loadSummary()])
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

function openCreate() {
  modalOpen.value = true
}

async function submitCreate() {
  saving.value = true
  try {
    await createInbound({ ...form })
    toast.push('入库单已创建，等待月台收货')
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
      <div class="page-kicker"><span class="dot"></span>作业 · 入库</div>
      <h1>入库作业</h1>
      <p class="sub">从预约到上架，收货、质检、库位推荐在同一条链路里完成。</p>
    </div>
    <div class="page-actions">
      <button class="btn" type="button" @click="download">
        <Icon name="download" :size="15" />导出
      </button>
      <button v-if="canCreate" class="btn btn-primary" type="button" @click="openCreate">
        <Icon name="plus" :size="15" />新建入库单
      </button>
    </div>
  </div>

  <div v-if="error" class="hint-box mb14" style="color: var(--danger)">
    <Icon name="triangle-alert" :size="15" /><span>入库数据加载失败：{{ error }}</span>
  </div>

  <section class="kpi-grid four" aria-label="入库概览">
    <div class="kpi">
      <div class="kpi-label"><Icon name="truck" :size="14" />入库单</div>
      <div class="kpi-value"><strong>{{ summary.total }}</strong><span class="unit">单</span></div>
      <div class="kpi-foot"><span class="delta">今日预约与历史合计</span><span class="muted" style="font-size: 9.5px">数据库实时值</span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="clock" :size="14" />待收货</div>
      <div class="kpi-value"><strong>{{ summary.pending }}</strong><span class="unit">单</span></div>
      <div class="kpi-foot"><span class="delta">等待月台</span><span class="muted" style="font-size: 9.5px">PENDING</span></div>
    </div>
    <div class="kpi hot">
      <div class="kpi-label"><Icon name="clipboard-list" :size="14" />待上架</div>
      <div class="kpi-value"><strong>{{ summary.ready }}</strong><span class="unit">单</span></div>
      <div class="kpi-foot"><span class="delta">可生成推荐</span><span class="muted" style="font-size: 9.5px">READY</span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="circle-check" :size="14" />已完成</div>
      <div class="kpi-value"><strong>{{ summary.done }}</strong><span class="unit">单</span></div>
      <div class="kpi-foot"><span class="delta up">已上架入账</span><span class="muted" style="font-size: 9.5px">DONE</span></div>
    </div>
  </section>

  <div class="grid grid-12">
    <section class="card col-8">
      <div class="card-head">
        <div class="t">
          <h2 class="card-title">入库单</h2>
          <span class="card-sub">共 <strong>{{ total }}</strong> 条记录</span>
        </div>
        <span class="tag tag-live tag-success"><span class="dot"></span>后端实时</span>
      </div>
      <div style="padding: 12px 18px 0">
        <div class="tabs" role="tablist" aria-label="入库状态筛选">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            class="tab"
            :class="{ 'is-active': status === tab.value }"
            type="button"
            role="tab"
            @click="status = tab.value"
          >{{ tab.label }}</button>
        </div>
        <div class="filter-bar">
          <div class="search-box">
            <Icon name="search" :size="15" />
            <input v-model="keyword" class="input" type="search" placeholder="搜索单号或供应商" aria-label="搜索入库单" />
          </div>
          <button class="btn btn-sm" type="button" @click="load">
            <Icon name="refresh-cw" :size="13" />刷新
          </button>
          <span class="spacer"></span>
          <span class="filter-count">显示 <strong>{{ total }}</strong> 条</span>
        </div>
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr><th>单号</th><th>供应商 / 月台</th><th>商品</th><th style="text-align: right">数量</th><th>预计到货</th><th>状态</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-for="order in rows" :key="order.id">
              <td><span class="code">{{ order.no }}</span></td>
              <td><div class="cell-main">{{ order.supplierName }}</div><div class="cell-sub">{{ order.dock }}</div></td>
              <td>
                <div class="cell-main">{{ firstItem(order).productName }}</div>
                <div class="cell-sub code">{{ firstItem(order).sku }}</div>
              </td>
              <td class="num">{{ firstItem(order).qty || 0 }}</td>
              <td class="tight">{{ formatTime(order.eta) }}</td>
              <td><span class="tag" :class="statusTone[order.status]"><span class="dot"></span>{{ statusLabel[order.status] || order.status }}</span></td>
              <td class="actions">
                <button v-if="order.status === 'PENDING'" class="btn btn-sm btn-primary" type="button" @click="receive(order)">收货</button>
                <button v-else-if="['RECEIVED', 'INSPECTING'].includes(order.status)" class="btn btn-sm btn-primary" type="button" @click="inspect(order)">质检</button>
                <button v-else-if="order.status === 'READY'" class="btn btn-sm btn-primary" type="button" @click="openRecommendation(order)">推荐库位</button>
                <button v-else class="btn btn-sm btn-ghost" type="button" @click="toast.push('已打开入库单 ' + order.no)">查看</button>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="7">
                <div class="empty-state">
                  <img src="/images/undraw/not-found.svg" alt="" />
                  <p>{{ loading ? '正在加载…' : '没有匹配的入库单' }}</p>
                </div>
              </td>
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
          <div class="t"><h2 class="card-title">收货节奏</h2><span class="card-sub">按后端入库单状态汇总</span></div>
          <span class="tag tag-accent">进行中</span>
        </div>
        <div class="card-body">
          <div class="stat-line"><span class="sl">入库单总数</span><span class="sv">{{ summary.total }} 单</span></div>
          <div class="stat-line"><span class="sl">待收货</span><span class="sv">{{ summary.pending }} 单</span></div>
          <div class="stat-line"><span class="sl">待上架</span><span class="sv">{{ summary.ready }} 单</span></div>
          <div class="stat-line"><span class="sl">已完成</span><span class="sv">{{ summary.done }} 单</span></div>
          <div class="mt12">
            <div class="flex between" style="font-size: 10.5px; color: var(--ink-3)">
              <span>完成进度</span><span class="mono">{{ summary.total ? Math.round(summary.done / summary.total * 100) : 0 }}%</span>
            </div>
            <div class="mini-bar" style="max-width: none; margin-top: 5px; height: 5px">
              <i :style="{ width: (summary.total ? summary.done / summary.total * 100 : 0) + '%' }"></i>
            </div>
          </div>
          <div class="illus-panel mt16">
            <img class="illus" src="/images/undraw/package-arrived.svg" alt="包裹到达插画" loading="lazy" />
          </div>
        </div>
      </section>

      <section class="card photo-card" style="min-height: 210px">
        <img src="/images/pexels/boxes.jpg" alt="待上架区域的纸箱堆放" loading="lazy" />
        <div class="photo-body">
          <div class="ph-kicker"><Icon name="map-pin" :size="15" />月台暂存区 · D 区</div>
          <strong>待上架纸箱</strong>
          <p>640 件 · 4 个入库单 · 平均等待 18 分钟</p>
          <span class="tag"><span class="dot"></span>照片来自 Pexels</span>
        </div>
      </section>
    </div>
  </div>

  <p class="chart-note" style="margin-top: 18px">
    上架推荐由后端调用 AHP 权重计算 + TOPSIS 排序，候选库位来自库位表的 FREE 记录。
  </p>

  <Teleport to="body">
    <template v-if="modalOpen">
      <div class="overlay is-open" @click="modalOpen = false"></div>
      <div class="modal is-open" role="dialog" aria-modal="true" aria-label="新建入库单">
        <div class="modal-head">
          <div><h2>新建入库单</h2><p>创建后进入待收货队列。</p></div>
          <button class="icon-btn" type="button" aria-label="关闭" @click="modalOpen = false"><Icon name="x" :size="18" /></button>
        </div>
        <form @submit.prevent="submitCreate">
          <div class="modal-body">
            <div class="form-grid">
              <div class="field">
                <label for="in-supplier">供应商</label>
                <input id="in-supplier" v-model="form.supplierName" class="input" required />
              </div>
              <div class="field">
                <label for="in-dock">到货月台</label>
                <select id="in-dock" v-model="form.dock" class="select">
                  <option>1 号月台</option><option>2 号月台</option><option>3 号月台</option><option>4 号月台</option>
                </select>
              </div>
              <div class="field">
                <label for="in-product">商品名称</label>
                <input id="in-product" v-model="form.productName" class="input" required />
              </div>
              <div class="field">
                <label for="in-sku">SKU 编码</label>
                <input id="in-sku" v-model="form.sku" class="input mono" required />
              </div>
              <div class="field">
                <label for="in-qty">数量（件）</label>
                <input id="in-qty" v-model.number="form.qty" class="input" type="number" min="1" required />
              </div>
              <div class="field">
                <label for="in-eta">预计到货</label>
                <input id="in-eta" v-model="form.eta" class="input" type="datetime-local" />
              </div>
            </div>
          </div>
          <div class="modal-foot">
            <button class="btn" type="button" @click="modalOpen = false">取消</button>
            <button class="btn btn-primary" type="submit" :disabled="saving">
              <Icon name="check" :size="15" />{{ saving ? '创建中…' : '创建入库单' }}
            </button>
          </div>
        </form>
      </div>
    </template>

    <template v-if="drawerOpen">
      <div class="overlay is-open" @click="drawerOpen = false"></div>
      <div class="drawer is-open" role="dialog" aria-modal="true" aria-label="上架库位推荐">
        <div class="drawer-head">
          <div>
            <h2>上架库位推荐</h2>
            <p>{{ currentOrder?.no }} · {{ currentOrder?.supplierName }}</p>
          </div>
          <button class="icon-btn" type="button" aria-label="关闭" @click="drawerOpen = false"><Icon name="x" :size="18" /></button>
        </div>
        <div class="drawer-body">
          <div class="algo-strip">
            <span class="algo-chip"><Icon name="sparkles" :size="15" />AHP-TOPSIS 推荐</span>
            <span class="algo-chip neutral">CR = {{ recommendation?.consistencyRatio ?? '—' }}</span>
            <span class="algo-chip neutral">4 项指标</span>
            <span class="algo-chip neutral">FREE 库位</span>
          </div>

          <div v-if="recLoading" class="hint-box"><Icon name="refresh-cw" :size="15" /><span>正在计算候选库位…</span></div>

          <template v-if="recommendation">
            <div class="stat-line"><span class="sl">待上架商品</span><span class="sv">{{ recommendation.sku }} {{ recommendation.productName }}</span></div>
            <div class="stat-line"><span class="sl">数量</span><span class="sv">{{ recommendation.qty }} 件</span></div>
            <div class="stat-line"><span class="sl">到货月台</span><span class="sv">{{ currentOrder?.dock }}</span></div>

            <h3 class="section-title mt20">指标权重 <span class="tag tag-neutral">AHP</span></h3>
            <div class="weights">
              <div class="weight-row">
                <span class="wlabel">周转率</span>
                <span class="weight-bar"><i :style="{ width: (recommendation.weights[0] * 100 / 0.34) + '%' }"></i></span>
                <span class="wval">{{ recommendation.weights[0].toFixed(2) }}</span>
              </div>
              <div class="weight-row">
                <span class="wlabel">订单相关性</span>
                <span class="weight-bar"><i :style="{ width: (recommendation.weights[1] * 100 / 0.34) + '%' }"></i></span>
                <span class="wval">{{ recommendation.weights[1].toFixed(2) }}</span>
              </div>
              <div class="weight-row">
                <span class="wlabel">拣货距离</span>
                <span class="weight-bar"><i :style="{ width: (recommendation.weights[2] * 100 / 0.34) + '%' }"></i></span>
                <span class="wval">{{ recommendation.weights[2].toFixed(2) }}</span>
              </div>
              <div class="weight-row">
                <span class="wlabel">体积适配</span>
                <span class="weight-bar"><i :style="{ width: (recommendation.weights[3] * 100 / 0.34) + '%' }"></i></span>
                <span class="wval">{{ recommendation.weights[3].toFixed(2) }}</span>
              </div>
            </div>

            <h3 class="section-title mt20">TOPSIS 排序 <span class="tag tag-accent">推荐 Top 1</span></h3>
            <div class="rank-list">
              <div v-for="(c, i) in recommendation.candidates" :key="c.code" class="rank-row" :class="{ top: i === 0 }">
                <span class="rk">{{ i + 1 }}</span>
                <span class="rloc">{{ c.code }}</span>
                <span class="rbar"><i :style="{ width: Math.round(c.score * 100) + '%' }"></i></span>
                <span class="rscore">{{ c.score.toFixed(3) }}</span>
              </div>
            </div>
            <div class="chart-note mt8">贴近度越高越接近理想解。Top 1 将作为默认上架库位。</div>

            <h3 class="section-title mt20">候选指标明细</h3>
            <div v-for="c in recommendation.candidates" :key="'m' + c.code" class="stat-line">
              <span class="sl mono">{{ c.code }}</span>
              <span class="sv">周转 {{ c.turnover }} · 相关 {{ c.correlation }} · 距离 {{ c.distance }} · 适配 {{ c.fit }}</span>
            </div>
          </template>
        </div>
        <div class="drawer-foot">
          <span class="hint">推荐结果可人工调整</span>
          <div class="flex">
            <button class="btn" type="button" @click="drawerOpen = false">取消</button>
            <button class="btn btn-primary" type="button" :disabled="!recommendation?.candidates?.length" @click="confirmPutaway">
              <Icon name="check" :size="15" />确认上架
            </button>
          </div>
        </div>
      </div>
    </template>
  </Teleport>
</template>
