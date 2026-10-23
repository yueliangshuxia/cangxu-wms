<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  createStocktake,
  listStocktakes,
  scanStocktake,
  startStocktake,
  stocktakeDiffs,
  stocktakeSummary
} from '../api/warehouse'
import { exportStocktakes } from '../api/exports'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { useEscape } from '../composables/useEscape'
import Icon from '../components/Icon.vue'

const auth = useAuthStore()
const toast = useToastStore()

const tasks = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const filter = ref('')
const keyword = ref('')
const summary = ref({ total: 0, pending: 0, inProgress: 0, done: 0 })
const diffs = ref([])
const selectedId = ref(null)
const scanInput = ref('')
const modalOpen = ref(false)
const saving = ref(false)
const loading = ref(false)
const error = ref('')

const form = reactive({
  scope: 'A 区 · 电子配件',
  type: '循环盘点',
  owner: '周可',
  plannedAt: '10-21 09:00'
})

async function download() {
  try {
    await exportStocktakes({ status: filter.value })
    toast.push('盘点任务已导出')
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

const canOperate = computed(() => ['ADMIN', 'MANAGER', 'INSPECTOR'].includes(auth.role))
const canCreate = computed(() => ['ADMIN', 'MANAGER'].includes(auth.role))
const pages = computed(() => Math.max(1, Math.ceil(total.value / size)))
const tabs = [
  { value: '', label: '全部', key: 'total' },
  { value: 'IN_PROGRESS', label: '进行中', key: 'inProgress' },
  { value: 'PENDING', label: '待开始', key: 'pending' },
  { value: 'DONE', label: '已完成', key: 'done' }
]
const statusLabel = { PENDING: '待开始', IN_PROGRESS: '进行中', DONE: '已完成' }
const statusTone = { PENDING: 'tag-info', IN_PROGRESS: 'tag-accent', DONE: 'tag-success' }
const actionLabel = { ADJUST: '调整库存', REVIEWED: '已复核' }

const selected = computed(() => tasks.value.find((t) => t.id === selectedId.value) || tasks.value[0])
const progress = computed(() => selected.value?.total ? Math.round(selected.value.counted / selected.value.total * 100) : 0)
const totalDiff = computed(() => diffs.value.reduce((n, d) => n + Math.abs(d.diffQty || 0), 0))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [taskRes, summaryRes, diffRes] = await Promise.all([
      listStocktakes({ page: page.value, size, status: filter.value, keyword: keyword.value }),
      stocktakeSummary(),
      stocktakeDiffs()
    ])
    tasks.value = taskRes.data.records
    total.value = taskRes.data.total
    summary.value = summaryRes.data
    diffs.value = diffRes.data
    const preferred = tasks.value.find((t) => t.id === selectedId.value)
      || tasks.value.find((t) => t.status === 'IN_PROGRESS')
      || tasks.value[0]
    if (preferred) selectedId.value = preferred.id
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

async function startTask(task) {
  if (!canOperate.value) {
    toast.push('当前角色不能执行盘点操作', 'warn')
    return
  }
  try {
    await startStocktake(task.id)
    selectedId.value = task.id
    toast.push(`${task.no} 已开始，等待扫码`)
    await load()
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

async function scan() {
  if (!selected.value) return
  if (!canOperate.value) {
    toast.push('当前角色不能执行盘点操作', 'warn')
    return
  }
  if (selected.value.status !== 'IN_PROGRESS') {
    toast.push('请先选择一个进行中的盘点任务', 'warn')
    return
  }
  try {
    const code = scanInput.value.trim()
    await scanStocktake(selected.value.id, code)
    toast.push(`已扫描 ${code || '库位'} · 计数 +1`)
    scanInput.value = ''
    await load()
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

async function createTask() {
  saving.value = true
  try {
    await createStocktake({ ...form })
    toast.push('盘点任务已创建，可在 PDA 领取')
    modalOpen.value = false
    filter.value = ''
    page.value = 1
    await load()
  } catch (e) {
    toast.push(e.message, 'err')
  } finally {
    saving.value = false
  }
}

useEscape(() => { modalOpen.value = false })
onMounted(load)
</script>

<template>
  <div class="page-head">
    <div>
      <div class="page-kicker"><span class="dot"></span>作业 · 盘点</div>
      <h1>盘点管理</h1>
      <p class="sub">循环盘点、全盘与抽盘任务；扫码录入、差异复核与库存调整。</p>
    </div>
    <div class="page-actions">
      <button class="btn" type="button" @click="download"><Icon name="download" :size="15" />导出盘点</button>
      <button v-if="canCreate" class="btn btn-primary" type="button" @click="modalOpen = true"><Icon name="plus" :size="15" />新建盘点任务</button>
    </div>
  </div>

  <div v-if="error" class="hint-box mb14" style="color: var(--danger)">
    <Icon name="triangle-alert" :size="15" /><span>盘点数据加载失败：{{ error }}</span>
  </div>

  <section class="kpi-grid four" aria-label="盘点概览">
    <div class="kpi">
      <div class="kpi-label"><Icon name="clipboard-list" :size="14" />盘点任务</div>
      <div class="kpi-value"><strong>{{ summary.total }}</strong><span class="unit">个</span></div>
      <div class="kpi-foot"><span class="delta">进行中 {{ summary.inProgress }}</span><span class="muted" style="font-size: 9.5px">后端实时值</span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="map" :size="14" />盘点库位</div>
      <div class="kpi-value"><strong>1,280</strong><span class="unit">个</span></div>
      <div class="kpi-foot"><span class="delta up">覆盖 A–E 区</span><span class="muted" style="font-size: 9.5px">规划容量</span></div>
    </div>
    <div class="kpi hot">
      <div class="kpi-label"><Icon name="circle-check" :size="14" />账实一致率</div>
      <div class="kpi-value"><strong>99.2</strong><span class="unit">%</span></div>
      <div class="kpi-foot"><span class="delta down">差异 {{ totalDiff }} 件</span><span class="muted" style="font-size: 9.5px">目标 ≥ 99%</span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="triangle-alert" :size="14" />差异件数</div>
      <div class="kpi-value"><strong>{{ totalDiff }}</strong><span class="unit">件</span></div>
      <div class="kpi-foot"><span class="delta">待调整与已复核</span><span class="muted" style="font-size: 9.5px">来自差异明细</span></div>
    </div>
  </section>

  <div class="grid grid-12">
    <section class="card col-8">
      <div class="card-head">
        <div class="t"><h2 class="card-title">盘点任务</h2><span class="card-sub">共 <strong>{{ total }}</strong> 个任务</span></div>
        <span class="tag tag-live tag-success"><span class="dot"></span>PDA 同步中</span>
      </div>
      <div style="padding: 12px 18px 0">
        <div class="tabs" role="tablist" aria-label="盘点状态筛选">
          <button v-for="tab in tabs" :key="tab.value" class="tab" :class="{ 'is-active': filter === tab.value }" type="button" role="tab" @click="filter = tab.value">
            {{ tab.label }} <span class="count">{{ summary[tab.key] || 0 }}</span>
          </button>
        </div>
        <div class="filter-bar">
          <div class="search-box">
            <Icon name="search" :size="15" />
            <input v-model="keyword" class="input" type="search" placeholder="搜索任务号、库区或负责人" aria-label="搜索盘点任务" />
          </div>
          <button class="btn btn-sm" type="button" @click="load"><Icon name="refresh-cw" :size="13" />刷新</button>
          <span class="spacer"></span>
          <span class="filter-count">显示 <strong>{{ total }}</strong> 条</span>
        </div>
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>任务号</th><th>盘点范围</th><th>类型</th><th>负责人</th><th>进度</th><th>状态</th><th>开始时间</th><th></th></tr></thead>
          <tbody>
            <tr v-for="task in tasks" :key="task.id">
              <td><span class="code">{{ task.no }}</span></td>
              <td><div class="cell-main">{{ task.scope }}</div><div class="cell-sub">{{ task.type }}</div></td>
              <td>{{ task.type }}</td>
              <td>{{ task.owner }}</td>
              <td><div class="flex" style="gap: 6px"><span class="mini-bar" style="max-width: 64px"><i :style="{ width: (task.total ? Math.round(task.counted / task.total * 100) : 0) + '%' }"></i></span><span class="mono" style="font-size: 10px">{{ task.total ? Math.round(task.counted / task.total * 100) : 0 }}%</span></div></td>
              <td><span class="tag" :class="statusTone[task.status]"><span class="dot"></span>{{ statusLabel[task.status] || task.status }}</span></td>
              <td class="tight" style="font-size: 10.5px; color: var(--ink-3)">{{ task.startedAt }}</td>
              <td class="actions">
                <button v-if="task.status === 'IN_PROGRESS'" class="btn btn-sm btn-primary" type="button" @click="selectedId = task.id">录入盘点</button>
                <button v-else-if="task.status === 'PENDING'" class="btn btn-sm" type="button" @click="startTask(task)">开始</button>
                <button v-else class="btn btn-sm btn-ghost" type="button" @click="toast.push('已打开 ' + task.no + ' 盘点报告')">查看</button>
              </td>
            </tr>
            <tr v-if="!tasks.length"><td colspan="8"><div class="empty-state"><img src="/images/undraw/not-found.svg" alt="" /><p>{{ loading ? '正在加载…' : '没有匹配的盘点任务' }}</p></div></td></tr>
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
          <div class="t"><h2 class="card-title">当前任务进度</h2><span class="card-sub">{{ selected?.no }} · {{ selected?.scope }}</span></div>
          <span class="tag" :class="statusTone[selected?.status]">{{ statusLabel[selected?.status] || '—' }}</span>
        </div>
        <div class="card-body">
          <div class="flex between" style="align-items: flex-end">
            <div class="kpi-value" style="margin-top: 0"><strong>{{ progress }}</strong><span class="unit">%</span></div>
            <span class="page-meta">{{ selected?.counted }} / {{ selected?.total }} 库位</span>
          </div>
          <div class="mini-bar" style="max-width: none; height: 7px; margin-top: 10px"><i :style="{ width: progress + '%' }"></i></div>
          <div class="stat-line mt12"><span class="sl">已盘数量</span><span class="sv">{{ selected?.counted }}</span></div>
          <div class="stat-line"><span class="sl">差异件数</span><span class="sv">{{ selected?.diffCount }}</span></div>
          <div class="stat-line"><span class="sl">负责人</span><span class="sv">{{ selected?.owner }}</span></div>
          <div class="field mt16">
            <label for="scan-input">扫码盘点</label>
            <div class="flex">
              <input id="scan-input" v-model="scanInput" class="input mono" placeholder="扫描库位码或商品条码" style="flex: 1" @keyup.enter="scan" />
              <button class="btn btn-primary" type="button" @click="scan"><Icon name="scan-barcode" :size="15" />模拟扫描</button>
            </div>
          </div>
          <div class="illus-panel mt16">
            <img class="illus" src="/images/undraw/checking-boxes.svg" alt="盘点核对插画" loading="lazy" />
          </div>
        </div>
      </section>

      <section class="card">
        <div class="card-head">
          <div class="t"><h2 class="card-title">差异处理</h2><span class="card-sub">按后端差异明细</span></div>
          <span class="tag tag-warning">{{ diffs.length }} 条</span>
        </div>
        <div class="card-body" style="padding-top: 10px">
          <div class="list">
            <div v-for="d in diffs.slice(0, 4)" :key="d.id" class="list-item">
              <span class="li-icon" :class="d.handleAction === 'REVIEWED' ? 'info' : 'warn'"><Icon name="triangle-alert" :size="15" /></span>
              <span class="li-body"><strong>{{ d.sku }} {{ d.productName }} · {{ d.diffQty > 0 ? '+' : '' }}{{ d.diffQty }}</strong><small>{{ d.locationCode }} · {{ d.reason }}</small></span>
              <button class="btn btn-sm btn-ghost" type="button" @click="toast.push('已打开 ' + d.locationCode + ' 差异调整')">{{ actionLabel[d.handleAction] || d.handleAction }}</button>
            </div>
            <div v-if="!diffs.length" class="chart-note">暂无差异</div>
          </div>
        </div>
      </section>
    </div>
  </div>

  <section class="card mt14">
    <div class="card-head">
      <div class="t"><h2 class="card-title">差异明细</h2><span class="card-sub">来自 stocktake_item 表</span></div>
      <button class="btn btn-sm" type="button" @click="download"><Icon name="download" :size="13" />导出</button>
    </div>
    <div class="card-body" style="padding-top: 8px">
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>商品</th><th>库位</th><th style="text-align: right">账面</th><th style="text-align: right">实盘</th><th style="text-align: right">差异</th><th>原因</th><th>处理</th></tr></thead>
          <tbody>
            <tr v-for="d in diffs" :key="d.id">
              <td><div class="cell-main">{{ d.productName }}</div><div class="cell-sub code">{{ d.sku }}</div></td>
              <td class="code">{{ d.locationCode }}</td>
              <td class="num">{{ d.bookQty }}</td>
              <td class="num">{{ d.actualQty }}</td>
              <td class="num" :style="{ color: d.diffQty < 0 ? 'var(--danger)' : 'var(--green)' }">{{ d.diffQty > 0 ? '+' : '' }}{{ d.diffQty }}</td>
              <td>{{ d.reason }}</td>
              <td><span class="tag" :class="d.handleAction === 'REVIEWED' ? 'tag-success' : 'tag-warning'"><span class="dot"></span>{{ actionLabel[d.handleAction] || d.handleAction }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>

  <p class="chart-note" style="margin-top: 18px">盘点任务、扫码计数与差异明细来自后端接口；库存调整与差异复核将在下一步接入。</p>

  <Teleport to="body">
    <template v-if="modalOpen">
      <div class="overlay is-open" @click="modalOpen = false"></div>
      <div class="modal is-open" role="dialog" aria-modal="true" aria-label="新建盘点任务">
        <div class="modal-head">
          <div><h2>新建盘点任务</h2><p>创建后进入待开始队列。</p></div>
          <button class="icon-btn" type="button" aria-label="关闭" @click="modalOpen = false"><Icon name="x" :size="18" /></button>
        </div>
        <form @submit.prevent="createTask">
          <div class="modal-body">
            <div class="form-grid">
              <div class="field">
                <label for="st-scope">盘点范围</label>
                <select id="st-scope" v-model="form.scope" class="select">
                  <option>全仓 · A–E 区</option><option>A 区 · 电子配件</option><option>B 区 · 智能家居</option><option>C 区 · 户外与生活</option><option>D 区 · 大件商品</option><option>E 区 · 退货暂存</option>
                </select>
              </div>
              <div class="field">
                <label for="st-type">盘点类型</label>
                <select id="st-type" v-model="form.type" class="select"><option>循环盘点</option><option>全盘</option><option>抽盘</option></select>
              </div>
              <div class="field">
                <label for="st-owner">负责人</label>
                <select id="st-owner" v-model="form.owner" class="select"><option>周可</option><option>林舟</option><option>陈屿</option></select>
              </div>
              <div class="field">
                <label for="st-date">计划日期</label>
                <input id="st-date" v-model="form.plannedAt" class="input" />
              </div>
            </div>
          </div>
          <div class="modal-foot">
            <button class="btn" type="button" @click="modalOpen = false">取消</button>
            <button class="btn btn-primary" type="submit" :disabled="saving"><Icon name="check" :size="15" />{{ saving ? '创建中…' : '创建任务' }}</button>
          </div>
        </form>
      </div>
    </template>
  </Teleport>
</template>
