<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { deviceSummary, deviceTelemetry, listDevices, updateDevice } from '../api/warehouse'
import { useAuthStore } from '../stores/auth'
import { useThemeStore } from '../stores/theme'
import { useToastStore } from '../stores/toast'
import { useEscape } from '../composables/useEscape'
import Icon from '../components/Icon.vue'

const auth = useAuthStore()
const theme = useThemeStore()
const toast = useToastStore()
const canEdit = computed(() => ['ADMIN', 'MANAGER'].includes(auth.role))

const devices = ref([])
const summary = ref({ total: 0, online: 0, offline: 0, working: 0, charging: 0, temperature: 0, humidity: 0 })
const tempPoints = ref([])
const humidityPoints = ref([])
const loading = ref(false)
const error = ref('')
const tempEl = ref(null)
const humEl = ref(null)
const configOpen = ref(false)
const configSaving = ref(false)
const deviceForm = ref({ code: '', location: '', status: 'ONLINE', battery: null, signalStrength: '' })
let tempChart = null
let humChart = null
useEscape(() => { configOpen.value = false })

const agvs = computed(() => devices.value.filter((d) => d.type === 'AGV'))
const typeLabel = {
  EDGE_GATEWAY: '边缘网关', TEMP_HUMIDITY: '温湿度传感器', SMOKE: '烟雾传感器',
  DOOR_ACCESS: '门禁控制器', RFID_READER: 'RFID 读写器', AGV: '搬运机器人'
}
const statusLabel = { ONLINE: '在线', WORKING: '作业中', CHARGING: '充电中', OFFLINE: '离线' }
const statusTone = { ONLINE: 'tag-success', WORKING: 'tag-accent', CHARGING: 'tag-warning', OFFLINE: 'tag-danger' }

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(5, 16) : '—'
}

function renderCharts() {
  if (!tempEl.value || !humEl.value) return
  const cs = getComputedStyle(document.documentElement)
  const c = (name) => cs.getPropertyValue(name).trim()
  if (!tempChart) tempChart = echarts.init(tempEl.value)
  if (!humChart) humChart = echarts.init(humEl.value)

  const option = (points, color, threshold, unit, min, max) => ({
    animationDuration: 500,
    grid: { left: 38, right: 16, top: 16, bottom: 24 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: c('--surface'), borderColor: c('--line'),
      textStyle: { color: c('--ink'), fontSize: 11 }
    },
    xAxis: {
      type: 'category', data: points.map((p) => p.time), boundaryGap: false,
      axisLine: { lineStyle: { color: c('--line') } }, axisTick: { show: false },
      axisLabel: { color: c('--ink-3'), fontSize: 9, interval: 5 }
    },
    yAxis: {
      type: 'value', min, max,
      splitLine: { lineStyle: { color: c('--line'), type: 'dashed' } },
      axisLabel: { color: c('--ink-3'), fontSize: 9 }
    },
    series: [{
      type: 'line', data: points.map((p) => p.value), smooth: true, symbol: 'none',
      lineStyle: { width: 2, color },
      areaStyle: { color: color + '20' },
      markLine: {
        silent: true, symbol: 'none',
        lineStyle: { color: c('--gold'), type: 'dashed', width: 1 },
        label: { formatter: `阈值 ${threshold}${unit}`, color: c('--gold'), fontSize: 9 },
        data: [{ yAxis: threshold }]
      }
    }]
  })

  tempChart.setOption(option(tempPoints.value, c('--c1'), 26, '°C', 18, 28), true)
  humChart.setOption(option(humidityPoints.value, c('--c2'), 65, '%', 30, 70), true)
}

function resizeCharts() {
  tempChart?.resize()
  humChart?.resize()
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [deviceRes, summaryRes, tempRes, humRes] = await Promise.all([
      listDevices(), deviceSummary(),
      deviceTelemetry('TH-A01', 'temp', 24),
      deviceTelemetry('TH-A01', 'humidity', 24)
    ])
    devices.value = deviceRes.data
    summary.value = summaryRes.data
    tempPoints.value = tempRes.data
    humidityPoints.value = humRes.data
    await nextTick()
    renderCharts()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function refresh() {
  load()
  toast.push('设备状态已刷新')
}

function openConfig(device = devices.value[0]) {
  if (!device) {
    toast.push('暂无可配置设备', 'warn')
    return
  }
  deviceForm.value = {
    code: device.code,
    location: device.location || '',
    status: device.status || 'ONLINE',
    battery: device.battery,
    signalStrength: device.signalStrength || ''
  }
  configOpen.value = true
}

function switchConfigDevice(code) {
  const device = devices.value.find((item) => item.code === code)
  if (device) openConfig(device)
}

async function saveConfig() {
  configSaving.value = true
  try {
    await updateDevice(deviceForm.value.code, {
      location: deviceForm.value.location,
      status: deviceForm.value.status,
      battery: deviceForm.value.battery,
      signalStrength: deviceForm.value.signalStrength
    })
    toast.push(`${deviceForm.value.code} 配置已保存`)
    configOpen.value = false
    await load()
  } catch (e) {
    toast.push(e.message, 'err')
  } finally {
    configSaving.value = false
  }
}

watch(() => theme.theme, () => renderCharts())

onMounted(() => {
  load()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  tempChart?.dispose()
  humChart?.dispose()
})
</script>

<template>
  <div class="page-head">
    <div>
      <div class="page-kicker"><span class="dot"></span>物联 · MQTT</div>
      <h1>设备监控</h1>
      <p class="sub">传感器、AGV 与 RFID 读写器通过 MQTT 接入，异常状态自动进入预警队列。</p>
    </div>
    <div class="page-actions">
      <button class="btn" type="button" @click="refresh">
        <Icon name="refresh-cw" :size="15" />刷新
      </button>
      <button v-if="canEdit" class="btn btn-primary" type="button" @click="openConfig()">
        <Icon name="sliders-horizontal" :size="15" />设备配置
      </button>
    </div>
  </div>

  <div v-if="error" class="hint-box mb14" style="color: var(--danger)">
    <Icon name="triangle-alert" :size="15" /><span>设备数据加载失败：{{ error }}</span>
  </div>

  <section class="device-grid" aria-label="实时环境">
    <div class="device">
      <div class="device-top">
        <span class="device-name"><Icon name="thermometer" :size="15" />A 区温度</span>
        <span class="tag tag-live tag-success"><span class="dot"></span>正常</span>
      </div>
      <div class="device-val"><strong>{{ Number(summary.temperature).toFixed(1) }}</strong><span>°C</span></div>
      <div class="device-foot"><span>阈值 26°C</span><span class="mono">TH-A01</span></div>
      <div class="spark"><i style="width: 76%; background: var(--c1)"></i></div>
    </div>
    <div class="device">
      <div class="device-top">
        <span class="device-name"><Icon name="droplets" :size="15" />A 区湿度</span>
        <span class="tag tag-live tag-success"><span class="dot"></span>正常</span>
      </div>
      <div class="device-val"><strong>{{ Number(summary.humidity).toFixed(0) }}</strong><span>%</span></div>
      <div class="device-foot"><span>阈值 65%</span><span class="mono">TH-A01</span></div>
      <div class="spark"><i style="width: 48%; background: var(--c2)"></i></div>
    </div>
    <div class="device">
      <div class="device-top">
        <span class="device-name"><Icon name="wifi" :size="15" />在线设备</span>
        <span class="tag" :class="summary.offline ? 'tag-warning' : 'tag-success'"><span class="dot"></span>{{ summary.offline }} 台离线</span>
      </div>
      <div class="device-val"><strong>{{ summary.online + summary.working + summary.charging }}</strong><span>/ {{ summary.total }} 台</span></div>
      <div class="device-foot"><span>网关在线</span><span class="mono">MQTT</span></div>
      <div class="spark"><i style="width: 92%; background: var(--green)"></i></div>
    </div>
    <div class="device">
      <div class="device-top">
        <span class="device-name"><Icon name="activity" :size="15" />消息延迟</span>
        <span class="tag tag-success"><span class="dot"></span>稳定</span>
      </div>
      <div class="device-val"><strong>38</strong><span>ms</span></div>
      <div class="device-foot"><span>QoS 1 · 1,240 msg/min</span><span class="mono">EDGE-01</span></div>
      <div class="spark"><i style="width: 38%; background: var(--c3)"></i></div>
    </div>
  </section>

  <div class="grid grid-12">
    <section class="card col-6">
      <div class="card-head">
        <div class="t"><h2 class="card-title">温度趋势</h2><span class="card-sub">A 区 · 最近 24 小时 · 阈值 26°C</span></div>
        <span class="tag tag-neutral">TH-A01</span>
      </div>
      <div class="card-body">
        <div ref="tempEl" class="chart chart-sm" role="img" aria-label="A 区最近 24 小时温度折线图"></div>
        <div v-if="!tempPoints.length" class="chart-note">暂无遥测数据，启动 MQTT 模拟器后会自动写入。</div>
      </div>
    </section>
    <section class="card col-6">
      <div class="card-head">
        <div class="t"><h2 class="card-title">湿度趋势</h2><span class="card-sub">A 区 · 最近 24 小时 · 阈值 65%</span></div>
        <span class="tag tag-neutral">TH-A01</span>
      </div>
      <div class="card-body">
        <div ref="humEl" class="chart chart-sm" role="img" aria-label="A 区最近 24 小时湿度折线图"></div>
        <div v-if="!humidityPoints.length" class="chart-note">暂无遥测数据。</div>
      </div>
    </section>

    <section class="card col-8">
      <div class="card-head">
        <div class="t"><h2 class="card-title">设备清单</h2><span class="card-sub">共 {{ devices.length }} 台 · 后端实时状态</span></div>
        <span class="tag tag-live tag-success"><span class="dot"></span>实时</span>
      </div>
      <div class="card-body" style="padding-top: 8px">
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>设备</th><th>位置</th><th>状态</th><th style="text-align: right">电量</th><th style="text-align: right">信号</th><th>最近心跳</th></tr></thead>
            <tbody>
              <tr v-for="d in devices" :key="d.code">
                <td><div class="cell-media"><span class="li-icon" style="width: 26px; height: 26px; border-radius: 5px"><Icon name="cpu" :size="14" /></span><div><div class="cell-main">{{ d.code }}</div><div class="cell-sub">{{ typeLabel[d.type] || d.type }}</div></div></div></td>
                <td>{{ d.location }}</td>
                <td><span class="tag" :class="statusTone[d.status]"><span class="dot"></span>{{ statusLabel[d.status] || d.status }}</span></td>
                <td class="num">{{ d.battery == null ? '—' : d.battery + '%' }}</td>
                <td class="num" style="font-size: 10px">{{ d.signalStrength || '—' }}</td>
                <td class="tight muted" style="font-size: 10.5px">{{ formatTime(d.lastHeartbeat) }}</td>
              </tr>
              <tr v-if="!devices.length">
                <td colspan="6"><div class="empty-state"><img src="/images/undraw/not-found.svg" alt="" /><p>{{ loading ? '正在加载…' : '暂无设备' }}</p></div></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="card-foot">
        <span>模拟器启动后，遥测数据会写入 device_telemetry 表</span>
        <button class="card-link" type="button" @click="toast.push('已打开设备日志')">设备日志<Icon name="arrow-right" :size="13" /></button>
      </div>
    </section>

    <div class="col-4 stack">
      <section class="card">
        <div class="card-head">
          <div class="t"><h2 class="card-title">AGV 车队</h2><span class="card-sub">{{ agvs.length }} 台 · {{ agvs.filter(a => a.status === 'WORKING').length }} 台搬运中</span></div>
          <span class="tag tag-accent">调度中</span>
        </div>
        <div class="card-body" style="padding-top: 10px">
          <div class="list">
            <div v-for="a in agvs" :key="a.code" class="list-item">
              <span class="li-icon" :class="a.status === 'WORKING' ? 'info' : a.status === 'OFFLINE' ? 'danger' : 'ok'"><Icon name="bot" :size="15" /></span>
              <span class="li-body">
                <strong>{{ a.code }} · {{ a.status === 'WORKING' ? '搬运中' : a.status === 'CHARGING' ? '充电中' : '空闲' }}</strong>
                <small>电量 {{ a.battery == null ? '—' : a.battery + '%' }} · {{ a.location }}</small>
                <span class="spark" style="margin-top: 6px"><i :style="{ width: (a.battery || 0) + '%', background: (a.battery || 0) < 25 ? 'var(--danger)' : (a.battery || 0) < 50 ? 'var(--gold)' : 'var(--green)' }"></i></span>
              </span>
              <span class="li-end">{{ a.battery == null ? '—' : a.battery + '%' }}</span>
            </div>
            <div v-if="!agvs.length" class="chart-note">暂无 AGV 设备。</div>
          </div>
        </div>
      </section>

      <section class="card photo-card" style="min-height: 200px">
        <img src="/images/pexels/forklift.jpg" alt="仓库内作业中的叉车" loading="lazy" />
        <div class="photo-body">
          <div class="ph-kicker"><Icon name="forklift" :size="15" />作业区 · A 区</div>
          <strong>叉车作业中</strong>
          <p>2 台叉车 · 平均搬运 12 分钟 / 单</p>
          <span class="tag"><span class="dot"></span>照片来自 Pexels</span>
        </div>
      </section>

      <section class="card">
        <div class="card-body">
          <div class="side-note">
            <img class="illus illus-sm" src="/images/undraw/server-status.svg" alt="服务器状态插画" loading="lazy" />
            <div class="txt">
              <strong>数据链路</strong>
              <p>MQTT Subscriber 已就绪；设置 MQTT_ENABLED=true 即可接入真实 broker。</p>
              <span class="tag tag-success mt8"><span class="dot"></span>链路待激活</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>

  <p class="chart-note" style="margin-top: 18px">
    环境数据来自 device_telemetry 表；启动 Python 模拟器后，设备页会自动看到新的遥测点。
  </p>

  <Teleport to="body">
    <template v-if="configOpen">
      <div class="overlay is-open" @click="configOpen = false"></div>
      <div class="modal is-open" role="dialog" aria-modal="true" aria-label="设备配置">
        <div class="modal-head">
          <div><h2>设备配置</h2><p>调整位置、状态、电量与信号描述，修改会写入审计日志。</p></div>
          <button class="icon-btn" type="button" aria-label="关闭" @click="configOpen = false"><Icon name="x" :size="18" /></button>
        </div>
        <form @submit.prevent="saveConfig">
          <div class="modal-body">
            <div class="form-grid">
              <div class="field full">
                <label for="dev-code">设备</label>
                <select id="dev-code" class="select" :value="deviceForm.code" @change="switchConfigDevice($event.target.value)">
                  <option v-for="d in devices" :key="d.code" :value="d.code">{{ d.code }} · {{ typeLabel[d.type] || d.type }}</option>
                </select>
              </div>
              <div class="field">
                <label for="dev-location">位置</label>
                <input id="dev-location" v-model="deviceForm.location" class="input" />
              </div>
              <div class="field">
                <label for="dev-status">状态</label>
                <select id="dev-status" v-model="deviceForm.status" class="select">
                  <option value="ONLINE">在线</option>
                  <option value="WORKING">作业中</option>
                  <option value="CHARGING">充电中</option>
                  <option value="OFFLINE">离线</option>
                </select>
              </div>
              <div class="field">
                <label for="dev-battery">电量（%）</label>
                <input id="dev-battery" v-model.number="deviceForm.battery" class="input" type="number" min="0" max="100" />
              </div>
              <div class="field">
                <label for="dev-signal">信号</label>
                <input id="dev-signal" v-model="deviceForm.signalStrength" class="input mono" placeholder="-51 dBm" />
              </div>
            </div>
          </div>
          <div class="modal-foot">
            <button class="btn" type="button" @click="configOpen = false">取消</button>
            <button class="btn btn-primary" type="submit" :disabled="configSaving">{{ configSaving ? '保存中…' : '保存配置' }}</button>
          </div>
        </form>
      </div>
    </template>
  </Teleport>
</template>
