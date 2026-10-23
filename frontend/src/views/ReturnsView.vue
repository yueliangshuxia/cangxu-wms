<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { createReturn, inspectReturn, listReturns, returnSummary } from '../api/warehouse'
import { exportReturns } from '../api/exports'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { useEscape } from '../composables/useEscape'
import Icon from '../components/Icon.vue'

const auth = useAuthStore()
const toast = useToastStore()

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const filter = ref('')
const keyword = ref('')
const summary = ref({
  total: 0, pending: 0, ready: 0, inStock: 0, scrapped: 0,
  returnRate: 0, resellRate: 0, repairRate: 0, scrapRate: 0, reasons: []
})
const loading = ref(false)
const error = ref('')
const selected = ref(null)
const drawerOpen = ref(false)
const disposition = ref('resell')
const note = ref('')
const saving = ref(false)
const createOpen = ref(false)
const createSaving = ref(false)
const createForm = ref({
  customerName: '天猫旗舰店', productName: '', sku: '', qty: 1, reason: '七天无理由', appliedAt: ''
})

const canInspect = computed(() => ['ADMIN', 'MANAGER', 'INSPECTOR'].includes(auth.role))
const canCreate = computed(() => ['ADMIN', 'MANAGER', 'RECEIVER'].includes(auth.role))
const pages = computed(() => Math.max(1, Math.ceil(total.value / size)))
const tabs = [
  { value: '', label: '全部', key: 'total' },
  { value: 'PENDING_INSPECTION', label: '待质检', key: 'pending' },
  { value: 'READY_TO_PUTAWAY', label: '待上架', key: 'ready' },
  { value: 'IN_STOCK', label: '已入库', key: 'inStock' },
  { value: 'SCRAPPED', label: '已报废', key: 'scrapped' }
]
const statusLabel = {
  PENDING_INSPECTION: '待质检', READY_TO_PUTAWAY: '待上架', IN_STOCK: '已入库', SCRAPPED: '已报废'
}
const statusTone = {
  PENDING_INSPECTION: 'tag-warning', READY_TO_PUTAWAY: 'tag-accent', IN_STOCK: 'tag-success', SCRAPPED: 'tag-danger'
}
const reasons = computed(() => summary.value.reasons || [])
const reasonTotal = computed(() => reasons.value.reduce((sum, item) => sum + item.value, 0))
const imageBySku = {
  'WH-1001': '/images/products/headphone.jpg', 'WH-1002': '/images/products/keyboard.jpg',
  'WH-1003': '/images/products/powerbank.jpg', 'WH-1004': '/images/products/mouse.jpg',
  'WH-2001': '/images/products/lamp.jpg', 'WH-2002': '/images/products/speaker.jpg',
  'WH-3001': '/images/products/backpack.jpg', 'WH-1007': '/images/products/cable.jpg'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [returnRes, summaryRes] = await Promise.all([
      listReturns({ page: page.value, size, status: filter.value, keyword: keyword.value }),
      returnSummary()
    ])
    rows.value = returnRes.data.records
    total.value = returnRes.data.total
    summary.value = summaryRes.data
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
watch(filter, () => { page.value = 1; load() })

function goPage(target) {
  if (target < 1 || target > pages.value || target === page.value) return
  page.value = target
  load()
}

function openDrawer(row) {
  selected.value = row
  disposition.value = row.disposition || 'resell'
  note.value = row.note || ''
  drawerOpen.value = true
}

async function saveInspection() {
  if (!selected.value) return
  if (!canInspect.value) {
    toast.push('当前角色不能提交质检结果', 'warn')
    return
  }
  saving.value = true
  try {
    await inspectReturn(selected.value.id, { disposition: disposition.value, note: note.value })
    toast.push(
      disposition.value === 'scrap' ? '已判定报废，等待主管复核'
        : disposition.value === 'repair' ? '已转维修区，退货单已更新'
          : '已判定可再上架，等待上架推荐'
    )
    drawerOpen.value = false
    await load()
  } catch (e) {
    toast.push(e.message, 'err')
  } finally {
    saving.value = false
  }
}

async function download() {
  try {
    await exportReturns({ status: filter.value, keyword: keyword.value })
    toast.push('退货单已导出')
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

function openCreate() {
  createForm.value = {
    customerName: '天猫旗舰店', productName: '', sku: '', qty: 1, reason: '七天无理由', appliedAt: ''
  }
  createOpen.value = true
}

async function submitCreate() {
  createSaving.value = true
  try {
    await createReturn({ ...createForm.value })
    toast.push('退货单已创建，等待质检')
    createOpen.value = false
    await load()
  } catch (e) {
    toast.push(e.message, 'err')
  } finally {
    createSaving.value = false
  }
}

useEscape(() => {
  if (createOpen.value) createOpen.value = false
  else drawerOpen.value = false
})
onMounted(load)
</script>

<template>
  <div class="page-head">
    <div>
      <div class="page-kicker"><span class="dot"></span>作业 · 逆向物流</div>
      <h1>退货管理</h1>
      <p class="sub">退货登记、质检判定与处置：再上架、维修或报废，全程留痕。</p>
    </div>
    <div class="page-actions">
      <button class="btn" type="button" @click="download"><Icon name="download" :size="15" />导出</button>
      <button v-if="canCreate" class="btn btn-primary" type="button" @click="openCreate"><Icon name="plus" :size="15" />新建退货单</button>
    </div>
  </div>

  <div v-if="error" class="hint-box mb14" style="color: var(--danger)">
    <Icon name="triangle-alert" :size="15" /><span>退货数据加载失败：{{ error }}</span>
  </div>

  <section class="kpi-grid four" aria-label="退货概览">
    <div class="kpi hot">
      <div class="kpi-label"><Icon name="rotate-ccw" :size="14" />待处理退货</div>
      <div class="kpi-value"><strong>{{ summary.pending + summary.ready }}</strong><span class="unit">单</span></div>
      <div class="kpi-foot"><span class="delta">待质检 {{ summary.pending }} · 待上架 {{ summary.ready }}</span><span class="muted" style="font-size: 9.5px">后端实时值</span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="package" :size="14" />退货单总数</div>
      <div class="kpi-value"><strong>{{ summary.total }}</strong><span class="unit">单</span></div>
      <div class="kpi-foot"><span class="delta up">已入库 {{ summary.inStock }}</span><span class="muted" style="font-size: 9.5px">已报废 {{ summary.scrapped }}</span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="chart-line" :size="14" />退货率</div>
      <div class="kpi-value"><strong>{{ Number(summary.returnRate).toFixed(1) }}</strong><span class="unit">%</span></div>
      <div class="kpi-foot"><span class="delta up">退货件数 / 出库件数</span><span class="muted" style="font-size: 9.5px">后端实时值</span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="circle-check" :size="14" />可再上架率</div>
      <div class="kpi-value"><strong>{{ Number(summary.resellRate).toFixed(1) }}</strong><span class="unit">%</span></div>
      <div class="kpi-foot"><span class="delta">维修 {{ Number(summary.repairRate).toFixed(0) }}% · 报废 {{ Number(summary.scrapRate).toFixed(0) }}%</span><span class="mini-bar"><i :style="{ width: Math.min(100, summary.resellRate) + '%' }"></i></span></div>
    </div>
  </section>

  <div class="grid grid-12">
    <section class="card col-8">
      <div class="card-head">
        <div class="t"><h2 class="card-title">退货单</h2><span class="card-sub">共 <strong>{{ total }}</strong> 条记录</span></div>
        <span class="tag tag-live tag-warning"><span class="dot"></span>质检队列</span>
      </div>
      <div style="padding: 12px 18px 0">
        <div class="tabs" role="tablist" aria-label="退货状态筛选">
          <button v-for="tab in tabs" :key="tab.value" class="tab" :class="{ 'is-active': filter === tab.value }" type="button" role="tab" @click="filter = tab.value">
            {{ tab.label }} <span class="count">{{ summary[tab.key] || 0 }}</span>
          </button>
        </div>
        <div class="filter-bar">
          <div class="search-box">
            <Icon name="search" :size="15" />
            <input v-model="keyword" class="input" type="search" placeholder="搜索退货单、客户或商品" aria-label="搜索退货单" />
          </div>
          <button class="btn btn-sm" type="button" @click="load"><Icon name="refresh-cw" :size="13" />刷新</button>
          <span class="spacer"></span>
          <span class="filter-count">显示 <strong>{{ total }}</strong> 条</span>
        </div>
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>退货单</th><th>客户</th><th>商品</th><th style="text-align: right">数量</th><th>原因</th><th>状态</th><th>申请时间</th><th></th></tr></thead>
          <tbody>
            <tr v-for="r in rows" :key="r.id">
              <td><span class="code">{{ r.no }}</span></td>
              <td><div class="cell-main">{{ r.customerName }}</div><div class="cell-sub">退货入库 · E 区暂存</div></td>
              <td><div class="cell-main">{{ r.productName }}</div><div class="cell-sub code">{{ r.sku }}</div></td>
              <td class="num">{{ r.qty }}</td>
              <td>{{ r.reason }}</td>
              <td><span class="tag" :class="statusTone[r.status]"><span class="dot"></span>{{ statusLabel[r.status] || r.status }}</span></td>
              <td class="tight muted" style="font-size: 10.5px">{{ r.appliedAt }}</td>
              <td class="actions"><button class="btn btn-sm" :class="r.status === 'PENDING_INSPECTION' ? 'btn-primary' : 'btn-ghost'" type="button" @click="openDrawer(r)">{{ r.status === 'PENDING_INSPECTION' ? '质检' : '查看' }}</button></td>
            </tr>
            <tr v-if="!rows.length"><td colspan="8"><div class="empty-state"><img src="/images/undraw/not-found.svg" alt="" /><p>{{ loading ? '正在加载…' : '没有匹配的退货单' }}</p></div></td></tr>
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
          <div class="t"><h2 class="card-title">退货原因</h2><span class="card-sub">全部退货单 · 按单量占比</span></div>
          <span class="tag tag-neutral">{{ summary.total }} 单</span>
        </div>
        <div class="card-body">
          <div class="weights">
            <div v-for="(r, i) in reasons" :key="r.label" class="weight-row">
              <span class="wlabel">{{ r.label }}</span>
              <span class="weight-bar"><i :style="{ width: (reasonTotal ? r.value / reasonTotal * 100 : 0) + '%', background: i === 0 ? 'var(--accent)' : 'var(--blue)' }"></i></span>
              <span class="wval">{{ r.value }} 单</span>
            </div>
          </div>
          <div class="hint-box mt12">
            <Icon name="info" :size="15" />
            <span>“七天无理由”占比最高，建议把退货暂存区靠近质检台，减少二次搬运。</span>
          </div>
        </div>
      </section>

      <section class="card">
        <div class="card-head">
          <div class="t"><h2 class="card-title">处置结构</h2><span class="card-sub">近 30 天质检结论</span></div>
        </div>
        <div class="card-body">
          <div class="stat-line"><span class="sl">可再上架</span><span class="sv">{{ Number(summary.resellRate).toFixed(1) }}%</span></div>
          <div class="mini-bar" style="max-width: none; height: 4px; margin: 2px 0 10px"><i :style="{ width: Math.min(100, summary.resellRate) + '%' }"></i></div>
          <div class="stat-line"><span class="sl">返修</span><span class="sv">{{ Number(summary.repairRate).toFixed(1) }}%</span></div>
          <div class="mini-bar" style="max-width: none; height: 4px; margin: 2px 0 10px"><i :style="{ width: Math.min(100, summary.repairRate) + '%', background: 'var(--blue)' }"></i></div>
          <div class="stat-line"><span class="sl">报废</span><span class="sv">{{ Number(summary.scrapRate).toFixed(1) }}%</span></div>
          <div class="mini-bar" style="max-width: none; height: 4px"><i :style="{ width: Math.min(100, summary.scrapRate) + '%', background: 'var(--danger)' }"></i></div>
          <div class="illus-panel mt16">
            <img class="illus" src="/images/undraw/heavy-box.svg" alt="退货包裹插画" loading="lazy" />
          </div>
        </div>
      </section>
    </div>
  </div>

  <p class="chart-note" style="margin-top: 18px">退货单、质检处置、原因分布与统计指标均来自后端接口。</p>

  <Teleport to="body">
    <template v-if="drawerOpen && selected">
      <div class="overlay is-open" @click="drawerOpen = false"></div>
      <div class="drawer is-open" role="dialog" aria-modal="true" aria-label="退货质检">
        <div class="drawer-head">
          <div><h2>退货质检</h2><p>{{ selected.no }} · {{ selected.customerName }}</p></div>
          <button class="icon-btn" type="button" aria-label="关闭" @click="drawerOpen = false"><Icon name="x" :size="18" /></button>
        </div>
        <div class="drawer-body">
          <div class="flex" style="align-items: flex-start; gap: 12px">
            <img :src="imageBySku[selected.sku] || '/images/products/hub.jpg'" alt="" style="width: 82px; height: 82px; border-radius: 7px; object-fit: cover; border: 1px solid var(--line)" />
            <div class="grow">
              <div class="cell-main">{{ selected.productName }}</div>
              <div class="cell-sub code">{{ selected.sku }} · × {{ selected.qty }}</div>
              <div class="mt8"><span class="tag tag-warning"><span class="dot"></span>{{ selected.reason }}</span></div>
            </div>
            <span class="tag" :class="statusTone[selected.status]">{{ statusLabel[selected.status] }}</span>
          </div>

          <h3 class="section-title mt20">质检项</h3>
          <div class="setting-row"><div class="st"><strong>外观完好</strong><small>无明显划痕、磕碰、污渍</small></div><label class="switch"><input type="checkbox" checked /><span class="track"></span></label></div>
          <div class="setting-row"><div class="st"><strong>功能正常</strong><small>开机、连接、按键测试通过</small></div><label class="switch"><input type="checkbox" checked /><span class="track"></span></label></div>
          <div class="setting-row"><div class="st"><strong>配件齐全</strong><small>说明书、数据线、包装内衬</small></div><label class="switch"><input type="checkbox" /><span class="track"></span></label></div>

          <h3 class="section-title mt20">处置结论</h3>
          <div class="stack">
            <label class="flex" style="gap: 8px; cursor: pointer; font-size: 12px"><input v-model="disposition" type="radio" value="resell" style="accent-color: var(--accent)" />可再上架 · 回到正常库存</label>
            <label class="flex" style="gap: 8px; cursor: pointer; font-size: 12px"><input v-model="disposition" type="radio" value="repair" style="accent-color: var(--accent)" />返修 · 转维修区</label>
            <label class="flex" style="gap: 8px; cursor: pointer; font-size: 12px"><input v-model="disposition" type="radio" value="scrap" style="accent-color: var(--accent)" />报废 · 计入损耗</label>
          </div>

          <div class="field mt16">
            <label for="return-note">质检备注</label>
            <textarea id="return-note" v-model="note" class="textarea" placeholder="记录外观、功能、配件情况"></textarea>
          </div>
          <div class="hint-box mt12">
            <Icon name="info" :size="15" />
            <span>选择“可再上架”后，系统会生成上架推荐；选择“报废”需要主管复核。</span>
          </div>
        </div>
        <div class="drawer-foot">
          <span class="hint">质检结果会写入审计日志</span>
          <div class="flex">
            <button class="btn" type="button" @click="drawerOpen = false">取消</button>
            <button class="btn btn-primary" type="button" :disabled="saving" @click="saveInspection"><Icon name="check" :size="15" />{{ saving ? '提交中…' : '提交质检结果' }}</button>
          </div>
        </div>
      </div>
    </template>
  </Teleport>

  <Teleport to="body">
    <template v-if="createOpen">
      <div class="overlay is-open" @click="createOpen = false"></div>
      <div class="modal is-open" role="dialog" aria-modal="true" aria-label="新建退货单">
        <div class="modal-head">
          <div><h2>新建退货单</h2><p>登记后进入待质检队列。</p></div>
          <button class="icon-btn" type="button" aria-label="关闭" @click="createOpen = false"><Icon name="x" :size="18" /></button>
        </div>
        <form @submit.prevent="submitCreate">
          <div class="modal-body">
            <div class="form-grid">
              <div class="field">
                <label for="rt-customer">客户</label>
                <input id="rt-customer" v-model="createForm.customerName" class="input" required />
              </div>
              <div class="field">
                <label for="rt-reason">退货原因</label>
                <select id="rt-reason" v-model="createForm.reason" class="select">
                  <option>七天无理由</option>
                  <option>质量问题</option>
                  <option>发错货</option>
                  <option>包装破损</option>
                  <option>其他</option>
                </select>
              </div>
              <div class="field">
                <label for="rt-product">商品名称</label>
                <input id="rt-product" v-model="createForm.productName" class="input" required />
              </div>
              <div class="field">
                <label for="rt-sku">SKU</label>
                <input id="rt-sku" v-model="createForm.sku" class="input mono" />
              </div>
              <div class="field">
                <label for="rt-qty">数量</label>
                <input id="rt-qty" v-model.number="createForm.qty" class="input" type="number" min="1" required />
              </div>
              <div class="field">
                <label for="rt-time">申请时间</label>
                <input id="rt-time" v-model="createForm.appliedAt" class="input" placeholder="留空自动生成" />
              </div>
            </div>
          </div>
          <div class="modal-foot">
            <button class="btn" type="button" @click="createOpen = false">取消</button>
            <button class="btn btn-primary" type="submit" :disabled="createSaving">{{ createSaving ? '提交中…' : '创建退货单' }}</button>
          </div>
        </form>
      </div>
    </template>
  </Teleport>
</template>
