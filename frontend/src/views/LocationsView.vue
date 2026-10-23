<script setup>
import { computed, onMounted, ref } from 'vue'
import { listLocations, locationSummary, updateLocationStatus } from '../api/warehouse'
import { exportLocations } from '../api/exports'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import Icon from '../components/Icon.vue'

const auth = useAuthStore()
const toast = useToastStore()
const locations = ref([])
const summary = ref({ total: 0, occupied: 0, free: 0, locked: 0, check: 0 })
const selectedCode = ref('A-01-03')
const loading = ref(false)
const error = ref('')

const canEdit = computed(() => ['ADMIN', 'MANAGER'].includes(auth.role))
const selected = computed(() => locations.value.find((l) => l.code === selectedCode.value) || locations.value[0])
const utilization = computed(() => (summary.value.total ? (summary.value.occupied / summary.value.total * 100).toFixed(1) : '0.0'))

const statusLabel = { OCCUPIED: '已占用', FREE: '空闲', LOCKED: '锁定', CHECK: '待盘点' }
const statusTone = { OCCUPIED: 'tag-neutral', FREE: 'tag-success', LOCKED: 'tag-warning', CHECK: 'tag-violet' }
const statusClass = (status) => ({ OCCUPIED: 'occ', FREE: 'free', LOCKED: 'lock', CHECK: 'check' }[status] || 'free')

async function download() {
  try {
    await exportLocations()
    toast.push('库位数据已导出')
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

const zoneGroups = computed(() => {
  const map = new Map()
  for (const location of locations.value) {
    if (!map.has(location.zoneCode)) {
      map.set(location.zoneCode, {
        code: location.zoneCode,
        name: location.zoneName,
        tempType: location.tempType,
        rows: new Map()
      })
    }
    const zone = map.get(location.zoneCode)
    if (!zone.rows.has(location.rowNo)) zone.rows.set(location.rowNo, [])
    zone.rows.get(location.rowNo).push(location)
  }
  return Array.from(map.values()).map((zone) => ({
    ...zone,
    rows: Array.from(zone.rows.entries())
      .sort((a, b) => a[0] - b[0])
      .map(([rowNo, cells]) => ({ rowNo, cells: cells.sort((a, b) => a.colNo - b.colNo) }))
  }))
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [locationRes, summaryRes] = await Promise.all([listLocations(), locationSummary()])
    locations.value = locationRes.data
    summary.value = summaryRes.data
    if (!locations.value.some((l) => l.code === selectedCode.value) && locations.value.length) {
      selectedCode.value = locations.value[0].code
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function select(location) {
  selectedCode.value = location.code
}

async function toggleLock() {
  if (!selected.value || !canEdit.value) {
    toast.push('只有管理员或仓储主管可以修改库位状态', 'warn')
    return
  }
  const next = selected.value.status === 'LOCKED' ? 'FREE' : 'LOCKED'
  try {
    await updateLocationStatus(selected.value.code, next)
    toast.push(`${selected.value.code} 已${next === 'LOCKED' ? '锁定' : '释放'}`)
    await load()
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

onMounted(load)
</script>

<template>
  <div class="page-head">
    <div>
      <div class="page-kicker"><span class="dot"></span>库存 · 库位</div>
      <h1>库位管理</h1>
      <p class="sub">库区平面图、占用状态与温区属性。点击库位查看详情，锁定或释放库位。</p>
    </div>
    <div class="page-actions">
      <button class="btn" type="button" @click="download">
        <Icon name="download" :size="15" />导出库位
      </button>
      <button class="btn btn-primary" type="button" @click="toast.push('已创建全仓盘点任务')">
        <Icon name="clipboard-check" :size="15" />库位盘点
      </button>
    </div>
  </div>

  <div v-if="error" class="hint-box mb14" style="color: var(--danger)">
    <Icon name="triangle-alert" :size="15" /><span>库位加载失败：{{ error }}</span>
  </div>

  <section class="kpi-grid four" aria-label="库位概览">
    <div class="kpi">
      <div class="kpi-label"><Icon name="map" :size="14" />总库位</div>
      <div class="kpi-value"><strong>{{ summary.total }}</strong><span class="unit">个</span></div>
      <div class="kpi-foot"><span class="muted" style="font-size: 9.5px">A–E 区 · 常温 / 恒温</span></div>
    </div>
    <div class="kpi hot">
      <div class="kpi-label"><Icon name="boxes" :size="14" />已占用</div>
      <div class="kpi-value"><strong>{{ summary.occupied }}</strong><span class="unit">个</span></div>
      <div class="kpi-foot"><span class="delta up">利用率 {{ utilization }}%</span><span class="mini-bar"><i :style="{ width: utilization + '%' }"></i></span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="circle-check" :size="14" />空闲</div>
      <div class="kpi-value"><strong>{{ summary.free }}</strong><span class="unit">个</span></div>
      <div class="kpi-foot"><span class="muted" style="font-size: 9.5px">可接收上架</span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="lock" :size="14" />锁定 / 待盘点</div>
      <div class="kpi-value"><strong>{{ summary.locked + summary.check }}</strong><span class="unit">个</span></div>
      <div class="kpi-foot"><span class="muted" style="font-size: 9.5px">锁定 {{ summary.locked }} · 待盘点 {{ summary.check }}</span></div>
    </div>
  </section>

  <div class="map-layout">
    <section class="card">
      <div class="card-head">
        <div class="t">
          <h2 class="card-title">1F 库区平面图</h2>
          <span class="card-sub">每个方块代表一个库位，悬停查看编码 · {{ loading ? '加载中…' : '已同步' }}</span>
        </div>
        <div class="legend" aria-label="库位状态图例">
          <span><i class="swatch occ"></i>已占用</span>
          <span><i class="swatch free"></i>空闲</span>
          <span><i class="swatch lock"></i>锁定</span>
          <span><i class="swatch check"></i>待盘点</span>
        </div>
      </div>
      <div class="card-body">
        <div class="floor-map" aria-label="仓库库位平面图">
          <div v-for="zone in zoneGroups" :key="zone.code" class="zone-block">
            <div class="zone-label">
              <strong>{{ zone.code }}</strong>
              <span>{{ zone.name }} · {{ zone.tempType === 'CONSTANT' ? '恒温' : '常温' }}</span>
              <span class="zone-stats">
                {{ zone.rows.reduce((n, row) => n + row.cells.filter(c => c.status === 'OCCUPIED').length, 0) }}/{{ zone.rows.reduce((n, row) => n + row.cells.length, 0) }} 已占用
              </span>
            </div>
            <template v-for="(row, idx) in zone.rows" :key="row.rowNo">
              <div class="racks">
                <button
                  v-for="cell in row.cells"
                  :key="cell.code"
                  class="loc"
                  :class="[statusClass(cell.status), { 'is-selected': cell.code === selectedCode }]"
                  type="button"
                  :title="cell.code + ' · ' + statusLabel[cell.status]"
                  :aria-label="cell.code + ' ' + statusLabel[cell.status]"
                  @click="select(cell)"
                ></button>
              </div>
              <div v-if="idx < zone.rows.length - 1" class="aisle-gap"></div>
            </template>
          </div>
        </div>
        <div class="flex between mt12" style="flex-wrap: wrap; gap: 8px">
          <span class="chart-note">选中库位会以橙色描边高亮 · 主通道位于各巷道之间</span>
          <span class="page-meta">API: /api/v1/locations</span>
        </div>
      </div>
    </section>

    <aside class="card loc-detail">
      <div v-if="selected" class="card-pad">
        <div class="flex between">
          <span class="tag" :class="statusTone[selected.status]"><span class="dot"></span>{{ statusLabel[selected.status] }}</span>
          <span class="page-meta">{{ selected.zoneName }}</span>
        </div>
        <div class="detail-code mt12">{{ selected.code }}</div>
        <p class="card-sub" style="margin-top: 2px">{{ selected.tempType === 'CONSTANT' ? '恒温' : '常温' }} · {{ selected.zoneCode === 'D' ? '托盘位' : '料箱位' }}</p>
        <div class="detail-grid">
          <div><dt>容量</dt><dd>{{ selected.capacity }} 件</dd></div>
          <div><dt>当前商品</dt><dd class="code" style="font-size: 11px">{{ selected.currentSku || '— 空闲 —' }}</dd></div>
          <div><dt>巷道</dt><dd class="mono">{{ selected.zoneCode }}-{{ String(selected.rowNo).padStart(2, '0') }}</dd></div>
          <div><dt>列</dt><dd class="mono">{{ String(selected.colNo).padStart(2, '0') }}</dd></div>
        </div>
        <div class="stack mt16">
          <button class="btn btn-primary" type="button" @click="toast.push('已生成 ' + selected.code + ' 的盘点任务')">生成盘点任务</button>
          <button class="btn" type="button" @click="toggleLock">{{ selected.status === 'LOCKED' ? '解除锁定' : '锁定库位' }}</button>
        </div>
        <div class="hint-box mt12">
          <Icon name="info" :size="15" />
          <span>库位推荐由 AHP-TOPSIS 计算，综合周转率、订单相关性、拣货距离与体积适配四项指标。</span>
        </div>
      </div>
    </aside>
  </div>

  <p class="chart-note" style="margin-top: 18px">库位状态同时使用填充、纹理与图例文字，避免只依赖颜色区分。</p>
</template>
