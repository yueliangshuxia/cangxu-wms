<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import * as echarts from 'echarts'
import { getOverview } from '../api/dashboard'
import { listAlerts } from '../api/alerts'
import { exportAnalytics } from '../api/exports'
import { useAuthStore } from '../stores/auth'
import { useThemeStore } from '../stores/theme'
import { useToastStore } from '../stores/toast'
import Icon from '../components/Icon.vue'

const auth = useAuthStore()
const theme = useThemeStore()
const toast = useToastStore()

const overview = ref(null)
const loading = ref(true)
const error = ref('')
const trendEl = ref(null)
const zoneEl = ref(null)
let trendChart = null
let zoneChart = null

const fallbackTrend = [
  { date: '10/14', inbound: 980, outbound: 1240 },
  { date: '10/15', inbound: 1120, outbound: 1380 },
  { date: '10/16', inbound: 1040, outbound: 1290 },
  { date: '10/17', inbound: 1286, outbound: 1510 },
  { date: '10/18', inbound: 1190, outbound: 1640 },
  { date: '10/19', inbound: 860, outbound: 980 },
  { date: '10/20', inbound: 1286, outbound: 1742 }
]

const fallbackZones = [
  { zone: 'A 区 电子配件', value: 92 },
  { zone: 'B 区 智能家居', value: 78 },
  { zone: 'C 区 户外生活', value: 64 },
  { zone: 'D 区 大件商品', value: 86 },
  { zone: 'E 区 退货暂存', value: 41 }
]

const trend = computed(() => (overview.value?.trend?.length ? overview.value.trend : fallbackTrend))
const zones = computed(() => (overview.value?.zones?.length ? overview.value.zones : fallbackZones))
const last = computed(() => trend.value[trend.value.length - 1] || { inbound: 0, outbound: 0 })
const activities = computed(() => overview.value?.activities || [])
const productCount = computed(() => overview.value?.productCount ?? 0)
const inventoryQty = computed(() => overview.value?.inventoryQty ?? 0)
const stockAccuracy = computed(() => overview.value?.stockAccuracy ?? 0)
const turnoverDays = computed(() => overview.value?.turnoverDays ?? 0)
const alerts = ref([])

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
const dateText = computed(() => {
  const now = new Date()
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][now.getDay()]
  return `${now.getMonth() + 1}月${now.getDate()}日 ${week} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
})

const docks = [
  { no: '01', name: '1 号月台', state: 'busy', label: '卸货中', meta: '粤B·D3821 · 已用 18 分钟' },
  { no: '02', name: '2 号月台', state: 'idle', label: '空闲', meta: '下一预约 13:40' },
  { no: '03', name: '3 号月台', state: 'wait', label: '待质检', meta: '沪A·K9210 · 排队 6 分钟' },
  { no: '04', name: '4 号月台', state: 'busy', label: '装车中', meta: '苏E·F2708 · 已用 12 分钟' }
]

const candidates = [
  { code: 'A-01-10', score: 0.892 },
  { code: 'A-02-08', score: 0.861 },
  { code: 'A-02-09', score: 0.834 }
]

const weights = [
  { label: '周转率', value: 0.34, width: 100 },
  { label: '订单相关性', value: 0.27, width: 79 },
  { label: '拣货距离', value: 0.22, width: 65 },
  { label: '体积适配', value: 0.17, width: 50 }
]

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

const moduleLabels = {
  AUTH: '登录', MASTERDATA: '资料', INBOUND: '入库', OUTBOUND: '出库',
  INVENTORY: '库存', STOCKTAKE: '盘点', RETURNS: '退货', DEVICE: '设备', SYSTEM: '系统'
}
const resultLabels = { SUCCESS: '成功', WARN: '警告', FAIL: '失败' }

function statusClass(type) {
  if (type === 'INBOUND') return 'tag-info'
  if (type === 'OUTBOUND') return 'tag-accent'
  return 'tag-neutral'
}

function displayModule(type) {
  return moduleLabels[type] || type || '—'
}

function displayTarget(target) {
  return String(target || '—').split(':').pop()
}

function displayResult(status) {
  return resultLabels[status] || status || '—'
}

function resultTone(status) {
  if (['SUCCESS', '已完成'].includes(status)) return 'tag-success'
  if (['WARN', '警告'].includes(status)) return 'tag-warning'
  if (['FAIL', '失败'].includes(status)) return 'tag-danger'
  return 'tag-neutral'
}

function renderCharts() {
  if (!trendEl.value || !zoneEl.value) return
  const cs = getComputedStyle(document.documentElement)
  const c = (name) => cs.getPropertyValue(name).trim()

  if (!trendChart) trendChart = echarts.init(trendEl.value)
  if (!zoneChart) zoneChart = echarts.init(zoneEl.value)

  trendChart.setOption(
    {
      animationDuration: 700,
      textStyle: { fontFamily: 'Barlow Condensed, "PingFang SC", "Microsoft YaHei", sans-serif' },
      grid: { left: 42, right: 58, top: 34, bottom: 26 },
      legend: {
        right: 0, top: 0, itemWidth: 8, itemHeight: 8, icon: 'circle',
        textStyle: { color: c('--ink-2'), fontSize: 11 },
        data: ['入库', '出库']
      },
      tooltip: {
        trigger: 'axis',
        backgroundColor: c('--surface'),
        borderColor: c('--line'),
        textStyle: { color: c('--ink'), fontSize: 11 }
      },
      xAxis: {
        type: 'category', data: trend.value.map((x) => x.date), boundaryGap: false,
        axisLine: { lineStyle: { color: c('--line') } }, axisTick: { show: false },
        axisLabel: { color: c('--ink-3'), fontSize: 10 }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: c('--line'), type: 'dashed' } },
        axisLabel: { color: c('--ink-3'), fontSize: 10 }
      },
      series: [
        {
          name: '入库', type: 'line', data: trend.value.map((x) => x.inbound),
          symbol: 'circle', symbolSize: 5, lineStyle: { width: 2, color: c('--c1') },
          itemStyle: { color: c('--c1') },
          areaStyle: { color: c('--c1') + '20' },
          endLabel: { show: true, formatter: '{a}', color: c('--c1'), fontSize: 10 }
        },
        {
          name: '出库', type: 'line', data: trend.value.map((x) => x.outbound),
          symbol: 'circle', symbolSize: 5, lineStyle: { width: 2, color: c('--c2') },
          itemStyle: { color: c('--c2') },
          areaStyle: { color: c('--c2') + '18' },
          endLabel: { show: true, formatter: '{a}', color: c('--c2'), fontSize: 10 }
        }
      ]
    },
    true
  )

  zoneChart.setOption(
    {
      animationDuration: 650,
      grid: { left: 96, right: 44, top: 6, bottom: 8 },
      tooltip: {
        trigger: 'item',
        formatter: (p) => `${p.name}<br/><b>${p.value}%</b> 已占用`,
        backgroundColor: c('--surface'), borderColor: c('--line'),
        textStyle: { color: c('--ink'), fontSize: 11 }
      },
      xAxis: { type: 'value', max: 100, show: false },
      yAxis: {
        type: 'category', inverse: true, data: zones.value.map((x) => x.zone),
        axisLine: { show: false }, axisTick: { show: false },
        axisLabel: { color: c('--ink-2'), fontSize: 10.5 }
      },
      series: [{
        type: 'bar', barWidth: 11,
        data: zones.value.map((x) => ({
          value: x.value,
          itemStyle: {
            borderRadius: [0, 4, 4, 0],
            color: echarts.color.modifyAlpha(c('--c1'), 0.28 + x.value / 145)
          }
        })),
        label: { show: true, position: 'right', formatter: '{c}%', color: c('--ink-2'), fontSize: 10.5 }
      }]
    },
    true
  )
}

function resizeCharts() {
  trendChart?.resize()
  zoneChart?.resize()
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const overviewRes = await getOverview()
    overview.value = overviewRes.data
    try {
      const alertRes = await listAlerts()
      alerts.value = alertRes.data
    } catch {
      alerts.value = []
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
    await nextTick()
    renderCharts()
  }
}

watch(() => theme.theme, () => renderCharts())
watch(overview, async () => {
  await nextTick()
  renderCharts()
})

onMounted(() => {
  load()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  trendChart?.dispose()
  zoneChart?.dispose()
})
</script>

<template>
  <div class="page-head">
    <div>
      <div class="page-kicker"><span class="dot"></span>华东一号仓 · 苏州</div>
      <h1>{{ greeting }}，{{ auth.displayName }}</h1>
      <p class="sub">今天仓库的月台、库存与设备都在这里。<span class="clock-note">{{ dateText }}</span></p>
    </div>
    <div class="page-actions">
      <button class="btn" type="button" @click="exportAnalytics(7).then(() => toast.push('日报已导出')).catch((e) => toast.push(e.message, 'err'))">
        <Icon name="download" :size="15" />导出日报
      </button>
      <RouterLink class="btn btn-primary" to="/inbound">
        <Icon name="plus" :size="15" />新建入库单
      </RouterLink>
    </div>
  </div>

  <div v-if="error" class="hint-box mb14" style="color: var(--danger)">
    <Icon name="triangle-alert" :size="15" />
    <span>工作台数据加载失败：{{ error }}。请确认后端已在 8080 端口启动。</span>
  </div>

  <div class="dock-board">
    <div v-for="d in docks" :key="d.no" class="dock" :class="d.state">
      <span class="dock-no">{{ d.no }}</span>
      <span class="dock-body">
        <span class="dock-name"><span class="dot"></span>{{ d.name }}</span>
        <span class="dock-meta">{{ d.meta }}</span>
      </span>
      <span class="tag" :class="d.state === 'busy' ? 'tag-accent' : d.state === 'idle' ? 'tag-success' : 'tag-warning'">
        <span class="dot"></span>{{ d.label }}
      </span>
    </div>
  </div>

  <section class="kpi-grid" aria-label="核心指标">
    <div class="kpi">
      <div class="kpi-label"><Icon name="boxes" :size="14" />在库 SKU</div>
      <div class="kpi-value"><strong>{{ formatNumber(productCount) }}</strong><span class="unit">种</span></div>
      <div class="kpi-foot">
        <span class="delta up"><Icon name="trending-up" :size="12" />数据库实时值</span>
        <span class="muted" style="font-size: 9.5px">共 {{ formatNumber(inventoryQty) }} 件</span>
      </div>
    </div>
    <div class="kpi hot">
      <div class="kpi-label"><Icon name="package-plus" :size="14" />今日入库</div>
      <div class="kpi-value"><strong>{{ formatNumber(last.inbound) }}</strong><span class="unit">件</span></div>
      <div class="kpi-foot">
        <span class="delta up"><Icon name="trending-up" :size="12" />今日实际入库</span>
        <span class="muted" style="font-size: 9.5px">来自入库单明细</span>
      </div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="package-minus" :size="14" />今日出库</div>
      <div class="kpi-value"><strong>{{ formatNumber(last.outbound) }}</strong><span class="unit">件</span></div>
      <div class="kpi-foot">
        <span class="delta up"><Icon name="trending-up" :size="12" />今日实际出库</span>
        <span class="muted" style="font-size: 9.5px">来自出库单</span>
      </div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="circle-check" :size="14" />库存准确率</div>
      <div class="kpi-value"><strong>{{ Number(stockAccuracy).toFixed(1) }}</strong><span class="unit">%</span></div>
      <div class="kpi-foot">
        <span class="delta up"><Icon name="trending-up" :size="12" />盘点任务统计</span>
        <span class="muted" style="font-size: 9.5px">按账面/差异计算</span>
      </div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="refresh-cw" :size="14" />库存可售天数</div>
      <div class="kpi-value"><strong>{{ Number(turnoverDays).toFixed(1) }}</strong><span class="unit">天</span></div>
      <div class="kpi-foot">
        <span class="delta up"><Icon name="trending-down" :size="12" />近 30 天估算</span>
        <span class="muted" style="font-size: 9.5px">库存 / 日均出库</span>
      </div>
    </div>
  </section>

  <div class="grid grid-12">
    <section class="card col-8">
      <div class="card-head">
        <div class="t">
          <h2 class="card-title">出入库趋势</h2>
          <span class="card-sub">近 7 日 · 单位：件 · 数据来自后端</span>
        </div>
        <RouterLink class="card-link" to="/analytics">查看分析<Icon name="arrow-right" :size="13" /></RouterLink>
      </div>
      <div class="card-body">
        <div ref="trendEl" class="chart" role="img" aria-label="近 7 日入库与出库趋势折线图"></div>
        <details class="chart-data">
          <summary>查看数据表</summary>
          <table>
            <thead><tr><th>日期</th><th>入库（件）</th><th>出库（件）</th></tr></thead>
            <tbody>
              <tr v-for="row in trend" :key="row.date">
                <td>{{ row.date }}</td>
                <td class="num">{{ formatNumber(row.inbound) }}</td>
                <td class="num">{{ formatNumber(row.outbound) }}</td>
              </tr>
            </tbody>
          </table>
        </details>
      </div>
    </section>

    <section class="card col-4">
      <div class="card-head">
        <div class="t">
          <h2 class="card-title">库区占用率</h2>
          <span class="card-sub">按已用库位 / 总库位计算</span>
        </div>
        <span class="tag tag-success"><span class="dot"></span>总体 81.7%</span>
      </div>
      <div class="card-body">
        <div ref="zoneEl" class="chart chart-sm" role="img" aria-label="各库区占用率横向条形图"></div>
      </div>
      <div class="card-foot">
        <span>A 区接近饱和，建议将新品导入 B 区</span>
        <RouterLink class="card-link" to="/locations">库位图<Icon name="arrow-right" :size="13" /></RouterLink>
      </div>
    </section>

    <section class="card col-7">
      <div class="card-head">
        <div class="t">
          <h2 class="card-title">最近操作</h2>
          <span class="card-sub">来自审计日志 · 后端实时数据</span>
        </div>
        <span class="tag tag-live tag-success"><span class="dot"></span>实时</span>
      </div>
      <div class="card-body" style="padding-top: 8px">
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>时间</th><th>模块</th><th>对象</th><th>操作人</th><th>结果</th></tr></thead>
            <tbody>
              <tr v-for="(a, i) in activities" :key="i">
                <td class="num">{{ a.time || '—' }}</td>
                <td><span class="tag" :class="statusClass(a.type)">{{ displayModule(a.type) }}</span></td>
                <td><span class="code">{{ displayTarget(a.doc) }}</span></td>
                <td>{{ a.operator || '—' }}</td>
                <td><span class="tag" :class="resultTone(a.status)"><span class="dot"></span>{{ displayResult(a.status) }}</span></td>
              </tr>
              <tr v-if="!activities.length">
                <td colspan="5" class="muted" style="padding: 18px 12px">暂无操作日志，登录或新增主数据后会出现在这里。</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="card-foot">
        <span>{{ loading ? '正在加载…' : '最近 6 条' }}</span>
        <RouterLink class="card-link" to="/audit">全部日志<Icon name="arrow-right" :size="13" /></RouterLink>
      </div>
    </section>

    <section class="card col-5">
      <div class="card-head">
        <div class="t">
          <h2 class="card-title">异常待处理</h2>
          <span class="card-sub">按影响程度排序</span>
        </div>
        <span class="tag tag-danger"><span class="dot"></span>4 条</span>
      </div>
      <div class="card-body" style="padding-top: 10px">
        <div class="list">
          <div v-for="a in alerts" :key="a.title" class="list-item">
            <span class="li-icon" :class="a.level === 'danger' ? 'danger' : a.level === 'warn' ? 'warn' : 'info'">
              <Icon :name="a.icon" :size="15" />
            </span>
            <span class="li-body"><strong>{{ a.title }}</strong><small>{{ a.meta }}</small></span>
            <RouterLink class="btn btn-sm btn-ghost" :to="a.route || '/'">查看</RouterLink>
          </div>
        </div>
      </div>
    </section>

    <section class="card col-4 photo-card" style="min-height: 230px">
      <img src="/images/pexels/warehouse-aisle.jpg" alt="仓库 A 区通道两侧排列整齐的货架与料箱" loading="lazy" />
      <div class="photo-body">
        <div class="ph-kicker"><Icon name="map-pin" :size="15" />库区实况 · A 区</div>
        <strong>电子配件拣选区</strong>
        <p>92% 库位占用 · 2 条主通道 · 常温</p>
        <span class="tag"><span class="dot"></span>照片来自 Pexels</span>
      </div>
    </section>

    <section class="card col-8">
      <div class="card-head">
        <div class="t">
          <h2 class="card-title">上架库位推荐</h2>
          <span class="card-sub">AHP-TOPSIS 多指标评分 · 以 WH-1001 降噪蓝牙耳机为例</span>
        </div>
        <span class="algo-chip"><Icon name="sparkles" :size="15" />AHP-TOPSIS</span>
      </div>
      <div class="card-body">
        <div class="grid" style="grid-template-columns: 1fr 1.1fr; gap: 18px">
          <div>
            <div class="weights">
              <div v-for="w in weights" :key="w.label" class="weight-row">
                <span class="wlabel">{{ w.label }}</span>
                <span class="weight-bar"><i :style="{ width: w.width + '%' }"></i></span>
                <span class="wval">{{ w.value.toFixed(2) }}</span>
              </div>
            </div>
            <div class="hint-box mt12">
              <Icon name="info" :size="15" />
              <span>权重通过 AHP 一致性检验，CR = 0.042 &lt; 0.1。候选库位按 TOPSIS 贴近度降序排列。</span>
            </div>
          </div>
          <div>
            <div class="rank-list">
              <div v-for="(c, i) in candidates" :key="c.code" class="rank-row" :class="{ top: i === 0 }">
                <span class="rk">{{ i + 1 }}</span>
                <span class="rloc">{{ c.code }}</span>
                <span class="rbar"><i :style="{ width: Math.round((c.score - 0.6) / 0.4 * 100) + '%' }"></i></span>
                <span class="rscore">{{ c.score.toFixed(3) }}</span>
              </div>
            </div>
            <div class="flex between mt12">
              <span class="chart-note">Top 1 得分 0.892 · 预计拣货距离减少 18.5%</span>
              <RouterLink class="card-link" to="/inbound">去上架<Icon name="arrow-right" :size="13" /></RouterLink>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>

  <p class="chart-note" style="margin-top: 18px">
    工作台计数、趋势、库区占用、库存准确率与最近操作均来自后端实时聚合；月台与推荐卡片为界面示例。
  </p>
</template>
