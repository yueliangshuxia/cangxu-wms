<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { analyticsOverview } from '../api/analytics'
import { exportAnalytics } from '../api/exports'
import { useThemeStore } from '../stores/theme'
import { useToastStore } from '../stores/toast'
import Icon from '../components/Icon.vue'

const theme = useThemeStore()
const toast = useToastStore()

const range = ref(30)
const overview = ref(null)
const loading = ref(false)
const error = ref('')
const flowEl = ref(null)
const categoryEl = ref(null)
const heatEl = ref(null)
const algoEl = ref(null)
let flowChart = null
let categoryChart = null
let heatChart = null
let algoChart = null

const trend = computed(() => overview.value?.trend || [])
const categories = computed(() => overview.value?.categories || [])
const zones = computed(() => overview.value?.zones || [])
const aisles = computed(() => overview.value?.aisles || [])
const algoData = computed(() => overview.value?.experiment?.strategies || [])
const experiment = computed(() => overview.value?.experiment || {})
const totalQty = computed(() => overview.value?.totalQty || 0)

const heatData = computed(() => {
  const zoneIndex = new Map(zones.value.map((zone, index) => [zone, index]))
  const aisleIndex = new Map(aisles.value.map((aisle, index) => [aisle, index]))
  return (overview.value?.heatmap || [])
    .filter((cell) => zoneIndex.has(cell.zone) && aisleIndex.has(cell.aisle))
    .map((cell) => [aisleIndex.get(cell.aisle), zoneIndex.get(cell.zone), cell.value])
})

const kpis = computed(() => {
  const k = overview.value?.kpis || { inboundQty: 0, outboundQty: 0, turnover: 0, utilization: 0 }
  return [
    { label: '入库总量', icon: 'arrow-down', value: formatNumber(k.inboundQty), unit: '件', note: `近 ${range.value} 天累计`, up: true },
    { label: '出库总量', icon: 'arrow-up', value: formatNumber(k.outboundQty), unit: '件', note: `近 ${range.value} 天累计`, up: true },
    { label: '库存周转率', icon: 'refresh-cw', value: Number(k.turnover).toFixed(2), unit: '次 / 月', note: '出库量 / 库存量', up: true },
    { label: '库位利用率', icon: 'map', value: Number(k.utilization).toFixed(1), unit: '%', note: '库位实时状态', up: true, hot: true }
  ]
})

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function colors() {
  const cs = getComputedStyle(document.documentElement)
  return {
    surface: cs.getPropertyValue('--surface').trim(),
    ink: cs.getPropertyValue('--ink').trim(),
    ink2: cs.getPropertyValue('--ink-2').trim(),
    ink3: cs.getPropertyValue('--ink-3').trim(),
    line: cs.getPropertyValue('--line').trim(),
    c1: cs.getPropertyValue('--c1').trim(),
    c2: cs.getPropertyValue('--c2').trim(),
    c3: cs.getPropertyValue('--c3').trim(),
    c4: cs.getPropertyValue('--c4').trim(),
    c5: cs.getPropertyValue('--c5').trim(),
    gold: cs.getPropertyValue('--gold').trim(),
    dark: document.documentElement.getAttribute('data-theme') === 'dark'
  }
}

function renderCharts() {
  if (!flowEl.value || !categoryEl.value || !heatEl.value || !algoEl.value || !overview.value) return
  const c = colors()
  if (!flowChart) flowChart = echarts.init(flowEl.value)
  if (!categoryChart) categoryChart = echarts.init(categoryEl.value)
  if (!heatChart) heatChart = echarts.init(heatEl.value)
  if (!algoChart) algoChart = echarts.init(algoEl.value)

  const tooltip = {
    backgroundColor: c.surface, borderColor: c.line,
    textStyle: { color: c.ink, fontSize: 11 }
  }

  flowChart.setOption({
    animationDuration: 600,
    grid: { left: 42, right: 20, top: 34, bottom: 26 },
    legend: { right: 0, top: 0, itemWidth: 8, itemHeight: 8, icon: 'circle', textStyle: { color: c.ink2, fontSize: 11 } },
    tooltip: { trigger: 'axis', ...tooltip },
    xAxis: {
      type: 'category', data: trend.value.map((x) => x.date), boundaryGap: false,
      axisLine: { lineStyle: { color: c.line } }, axisTick: { show: false },
      axisLabel: { color: c.ink3, fontSize: 9.5 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: c.line, type: 'dashed' } },
      axisLabel: { color: c.ink3, fontSize: 9.5 }
    },
    series: [
      {
        name: '入库', type: 'line', data: trend.value.map((x) => x.inbound),
        symbol: 'none', lineStyle: { width: 2, color: c.c1 }, areaStyle: { color: c.c1 + '18' }
      },
      {
        name: '出库', type: 'line', data: trend.value.map((x) => x.outbound),
        symbol: 'none', lineStyle: { width: 2, color: c.c2 }, areaStyle: { color: c.c2 + '14' }
      }
    ]
  }, true)

  categoryChart.setOption({
    animationDuration: 600,
    color: [c.c1, c.c2, c.c3, c.c4, c.c5],
    title: {
      text: formatNumber(totalQty.value), subtext: '件 · 总库存', left: '34%', top: '40%', textAlign: 'center',
      textStyle: { color: c.ink, fontSize: 22, fontWeight: 600 },
      subtextStyle: { color: c.ink3, fontSize: 10 }
    },
    tooltip: { trigger: 'item', formatter: '{b}<br/><b>{c}%</b>', ...tooltip },
    legend: {
      bottom: 0, left: 'center', icon: 'circle', itemWidth: 7, itemHeight: 7,
      textStyle: { color: c.ink2, fontSize: 10 }, data: categories.value.map((x) => x.name)
    },
    series: [{
      type: 'pie', radius: ['54%', '76%'], center: ['34%', '46%'],
      itemStyle: { borderColor: c.surface, borderWidth: 2, borderRadius: 3 },
      label: { formatter: '{d}%', color: c.ink2, fontSize: 10 },
      labelLine: { length: 8, length2: 8, lineStyle: { color: c.line } },
      data: categories.value
    }]
  }, true)

  heatChart.setOption({
    animationDuration: 600,
    grid: { left: 42, right: 16, top: 8, bottom: 54 },
    tooltip: {
      position: 'top',
      formatter: (p) => `${zones.value[p.value[1]]} 区 · ${aisles.value[p.value[0]]} 巷道<br/>利用率 <b>${p.value[2]}%</b>`,
      ...tooltip
    },
    xAxis: {
      type: 'category', data: aisles.value, axisLine: { lineStyle: { color: c.line } },
      axisTick: { show: false }, axisLabel: { color: c.ink3, fontSize: 9.5 }
    },
    yAxis: {
      type: 'category', data: zones.value, inverse: true, axisLine: { show: false },
      axisTick: { show: false }, axisLabel: { color: c.ink2, fontSize: 10 }
    },
    visualMap: {
      min: 30, max: 100, orient: 'horizontal', left: 'center', bottom: 4,
      itemWidth: 11, itemHeight: 90, text: ['高', '低'],
      textStyle: { color: c.ink3, fontSize: 9.5 },
      inRange: { color: c.dark ? ['#33261A', '#7A3A18', '#E85D1A', '#FFB07A'] : ['#FBE6DA', '#F3B48F', '#E4570F', '#8F3006'] }
    },
    series: [{
      type: 'heatmap',
      data: heatData.value.map((d) => ({ value: d, label: { color: d[2] >= 72 ? (c.dark ? '#2A1A10' : '#FFFFFF') : c.ink } })),
      itemStyle: { borderColor: c.surface, borderWidth: 2, borderRadius: 2 },
      label: { show: true, formatter: (p) => p.value[2], fontSize: 9 }
    }]
  }, true)

  algoChart.setOption({
    animationDuration: 600,
    grid: { left: 88, right: 52, top: 8, bottom: 24 },
    tooltip: { trigger: 'item', formatter: (p) => `${p.name}<br/>平均拣货距离 <b>${p.value} m</b>`, ...tooltip },
    xAxis: {
      type: 'value', name: 'm / 单',
      splitLine: { lineStyle: { color: c.line, type: 'dashed' } },
      axisLabel: { color: c.ink3, fontSize: 9.5 }
    },
    yAxis: {
      type: 'category', inverse: true, data: algoData.value.map((x) => x.method),
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: c.ink2, fontSize: 11 }
    },
    series: [{
      type: 'bar', barWidth: 18,
      data: algoData.value.map((x, i) => ({
        value: x.distance,
        itemStyle: { borderRadius: [0, 4, 4, 0], color: i === algoData.value.length - 1 ? c.c1 : (c.dark ? '#4A5348' : '#C3C9BD') }
      })),
      label: { show: true, position: 'right', formatter: '{c} m', color: c.ink2, fontSize: 10.5 }
    }]
  }, true)
}

function resize() {
  flowChart?.resize()
  categoryChart?.resize()
  heatChart?.resize()
  algoChart?.resize()
}

async function load(days = range.value) {
  loading.value = true
  error.value = ''
  try {
    const res = await analyticsOverview(days)
    overview.value = res.data
    await nextTick()
    renderCharts()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function switchRange(value) {
  if (range.value === value) return
  range.value = value
  await load(value)
  toast.push(`已切换到近 ${value} 天真实统计`)
}

async function exportReport() {
  try {
    await exportAnalytics(range.value)
    toast.push('分析报告已导出')
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

watch(() => theme.theme, () => renderCharts())
onMounted(() => {
  load()
  window.addEventListener('resize', resize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  flowChart?.dispose()
  categoryChart?.dispose()
  heatChart?.dispose()
  algoChart?.dispose()
})
</script>

<template>
  <div class="page-head">
    <div>
      <div class="page-kicker"><span class="dot"></span>分析 · 运营与算法</div>
      <h1>数据分析</h1>
      <p class="sub">出入库节奏、库存结构，以及 AHP-TOPSIS 上架策略的仿真结果。</p>
    </div>
    <div class="page-actions">
      <div class="flex" style="gap: 4px">
        <button class="btn btn-sm" :class="{ 'btn-primary': range === 7 }" type="button" @click="switchRange(7)">近 7 天</button>
        <button class="btn btn-sm" :class="{ 'btn-primary': range === 30 }" type="button" @click="switchRange(30)">近 30 天</button>
      </div>
      <button class="btn" type="button" :disabled="loading" @click="exportReport">
        <Icon name="file-text" :size="15" />导出报告
      </button>
    </div>
  </div>

  <div v-if="error" class="hint-box mb14" style="color: var(--danger)">
    <Icon name="triangle-alert" :size="15" /><span>分析数据加载失败：{{ error }}</span>
  </div>

  <section class="kpi-grid four" aria-label="分析概览">
    <div v-for="k in kpis" :key="k.label" class="kpi" :class="{ hot: k.hot }">
      <div class="kpi-label"><Icon :name="k.icon" :size="14" />{{ k.label }}</div>
      <div class="kpi-value"><strong>{{ k.value }}</strong><span class="unit">{{ k.unit }}</span></div>
      <div class="kpi-foot">
        <span class="delta" :class="{ up: k.up }">{{ k.note }}</span>
        <span class="muted" style="font-size: 9.5px">数据库实时值</span>
      </div>
    </div>
  </section>

  <div class="grid grid-12">
    <section class="card col-8">
      <div class="card-head">
        <div class="t"><h2 class="card-title">出入库趋势</h2><span class="card-sub">近 {{ range }} 天 · 单位：件 · 数据来自订单表</span></div>
        <span class="tag tag-neutral">{{ range }} 天</span>
      </div>
      <div class="card-body">
        <div ref="flowEl" class="chart chart-lg" role="img" aria-label="出入库趋势折线图"></div>
        <details class="chart-data">
          <summary>查看数据表</summary>
          <table>
            <thead><tr><th>日期</th><th>入库（件）</th><th>出库（件）</th></tr></thead>
            <tbody>
              <tr v-for="row in trend" :key="row.date"><td>{{ row.date }}</td><td class="num">{{ row.inbound }}</td><td class="num">{{ row.outbound }}</td></tr>
            </tbody>
          </table>
        </details>
      </div>
    </section>

    <section class="card col-4">
      <div class="card-head">
        <div class="t"><h2 class="card-title">品类库存占比</h2><span class="card-sub">按库存件数 · 总计 {{ formatNumber(totalQty) }} 件</span></div>
      </div>
      <div class="card-body">
        <div ref="categoryEl" class="chart chart-lg" role="img" aria-label="品类库存占比环形图"></div>
        <details class="chart-data">
          <summary>查看数据表</summary>
          <table>
            <thead><tr><th>品类</th><th style="text-align: right">占比</th></tr></thead>
            <tbody><tr v-for="c in categories" :key="c.name"><td>{{ c.name }}</td><td class="num">{{ c.value }}%</td></tr></tbody>
          </table>
        </details>
      </div>
    </section>

    <section class="card col-6">
      <div class="card-head">
        <div class="t"><h2 class="card-title">库位利用率热力</h2><span class="card-sub">{{ zones.join('–') }} 区 × {{ aisles.join('–') }} 巷道 · 颜色越深占用越高</span></div>
        <span class="tag tag-warning">实时库位</span>
      </div>
      <div class="card-body">
        <div ref="heatEl" class="chart" role="img" aria-label="库位利用率热力图"></div>
      </div>
    </section>

    <section class="card col-6">
      <div class="card-head">
        <div class="t"><h2 class="card-title">上架策略对比</h2><span class="card-sub">平均拣货行走距离 · {{ formatNumber(experiment.orders) }} 条历史订单仿真</span></div>
        <span class="algo-chip"><Icon name="sparkles" :size="15" />AHP-TOPSIS</span>
      </div>
      <div class="card-body">
        <div class="flex wrap" style="gap: 10px; margin-bottom: 8px">
          <div><div class="kpi-label">拣货距离</div><div class="kpi-value"><strong style="font-size: 28px">{{ Number(experiment.distanceImprovement || 0) > 0 ? '-' : '' }}{{ Math.abs(Number(experiment.distanceImprovement || 0)).toFixed(1) }}</strong><span class="unit">%</span></div></div>
          <div style="width: 1px; height: 34px; background: var(--line); margin: 0 6px"></div>
          <div><div class="kpi-label">库位利用率</div><div class="kpi-value"><strong style="font-size: 28px">+{{ Number(experiment.utilizationImprovement || 0).toFixed(1) }}</strong><span class="unit">%</span></div></div>
        </div>
        <div ref="algoEl" class="chart chart-sm" role="img" aria-label="三种上架策略平均拣货距离对比条形图"></div>
        <details class="chart-data">
          <summary>查看实验数据</summary>
          <table>
            <thead><tr><th>策略</th><th style="text-align: right">拣货距离</th><th style="text-align: right">履行时间</th><th style="text-align: right">利用率</th></tr></thead>
            <tbody>
              <tr v-for="a in algoData" :key="a.method"><td>{{ a.method }}</td><td class="num">{{ a.distance }} m</td><td class="num">{{ a.time }} min</td><td class="num">{{ a.utilization }}%</td></tr>
            </tbody>
          </table>
        </details>
      </div>
    </section>

    <section class="card col-12">
      <div class="card-head">
        <div class="t"><h2 class="card-title">实验说明</h2><span class="card-sub">论文第 6 章的算法对比实验在系统中的可视化呈现</span></div>
      </div>
      <div class="card-body">
        <div class="grid" style="grid-template-columns: 1.4fr .6fr; gap: 20px; align-items: center">
          <div>
            <p style="font-size: 12.5px; line-height: 1.7; color: var(--ink-2); max-width: 70ch">
              以 {{ formatNumber(experiment.orders) }} 条历史出库订单、{{ experiment.skus }} 个 SKU 为输入，分别采用随机上架、ABC 分类和 AHP-TOPSIS 三种策略生成货位方案，
              在相同订单序列下模拟拣货。AHP-TOPSIS 相比随机策略平均拣货距离下降 {{ Math.abs(Number(experiment.distanceImprovement || 0)).toFixed(1) }}%，
              订单履行时间缩短 {{ Math.abs(Number(experiment.timeImprovement || 0)).toFixed(1) }}%，库位利用率提升 {{ Number(experiment.utilizationImprovement || 0).toFixed(1) }} 个百分点。
            </p>
            <div class="flex wrap mt12" style="gap: 8px">
              <span class="tag tag-neutral">AHP 一致性 CR = {{ Number(experiment.consistencyRatio || 0).toFixed(3) }}</span>
              <span class="tag tag-neutral">TOPSIS 贴近度排序</span>
              <span class="tag tag-neutral">Apriori 关联规则</span>
              <span class="tag tag-neutral">同订单序列仿真</span>
            </div>
            <p class="chart-note mt8" v-if="experiment.generatedAt">实验生成时间：{{ experiment.generatedAt }} · {{ experiment.source }}</p>
          </div>
          <div class="illus-panel">
            <img class="illus" src="/images/undraw/real-time-analytics.svg" alt="实时数据分析插画" loading="lazy" />
          </div>
        </div>
      </div>
    </section>
  </div>

  <p class="chart-note" style="margin-top: 18px">运营图表来自数据库实时聚合；策略对比来自 experiments/run_experiment.py 的可复现实验输出。</p>
</template>
