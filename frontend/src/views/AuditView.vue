<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { auditStats, listAuditLogs } from '../api/warehouse'
import { exportAuditLogs } from '../api/exports'
import { useToastStore } from '../stores/toast'
import Icon from '../components/Icon.vue'
import { useEscape } from '../composables/useEscape'

const toast = useToastStore()
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const module = ref('')
const result = ref('')
const keyword = ref('')
const loading = ref(false)
const error = ref('')
const selected = ref(null)
const stats = ref({
  total: 0, loginCount: 0, loginSuccess: 0, loginFail: 0,
  sensitiveCount: 0, failureCount: 0, recentSensitive: []
})
useEscape(() => { selected.value = null })

const pages = computed(() => Math.max(1, Math.ceil(total.value / size)))
const moduleLabels = {
  AUTH: '登录', MASTERDATA: '资料', INBOUND: '入库', OUTBOUND: '出库',
  INVENTORY: '库存', LOCATION: '库位', STOCKTAKE: '盘点', RETURNS: '退货',
  DEVICE: '设备', SYSTEM: '系统'
}
const resultLabels = { SUCCESS: '成功', WARN: '警告', FAIL: '失败' }
const resultTone = { SUCCESS: 'tag-success', WARN: 'tag-warning', FAIL: 'tag-danger' }

const sensitive = computed(() => stats.value.recentSensitive || [])

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '—'
}

function targetText(value) {
  return String(value || '—').split(':').pop()
}

function formatIp(ip) {
  if (!ip) return '—'
  return ip === '0:0:0:0:0:0:0:1' || ip === '::1' ? '127.0.0.1' : ip
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [listRes, statsRes] = await Promise.all([
      listAuditLogs({
        page: page.value, size, module: module.value, result: result.value, keyword: keyword.value
      }),
      auditStats()
    ])
    rows.value = listRes.data.records
    total.value = listRes.data.total
    stats.value = statsRes.data
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function download() {
  try {
    await exportAuditLogs({ module: module.value, result: result.value, keyword: keyword.value })
    toast.push('操作日志已导出')
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

let timer
watch(keyword, () => {
  clearTimeout(timer)
  timer = setTimeout(() => { page.value = 1; load() }, 300)
})
watch([module, result], () => { page.value = 1; load() })

function goPage(target) {
  if (target < 1 || target > pages.value || target === page.value) return
  page.value = target
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-head">
    <div>
      <div class="page-kicker"><span class="dot"></span>系统 · 审计</div>
      <h1>操作日志</h1>
      <p class="sub">关键操作、权限变更与登录安全。日志保留 180 天，敏感操作需双人复核。</p>
    </div>
    <div class="page-actions">
      <button class="btn" type="button" @click="toast.push('日志保留策略：180 天')">
        <Icon name="sliders-horizontal" :size="15" />清理策略
      </button>
      <button class="btn btn-primary" type="button" @click="download">
        <Icon name="download" :size="15" />导出日志
      </button>
    </div>
  </div>

  <div v-if="error" class="hint-box mb14" style="color: var(--danger)">
    <Icon name="triangle-alert" :size="15" /><span>日志加载失败：{{ error }}</span>
  </div>

  <section class="kpi-grid four" aria-label="审计概览">
    <div class="kpi">
      <div class="kpi-label"><Icon name="file-text" :size="14" />日志总数</div>
      <div class="kpi-value"><strong>{{ total }}</strong><span class="unit">条</span></div>
      <div class="kpi-foot"><span class="delta up">数据库实时值</span><span class="muted" style="font-size: 9.5px">当前筛选</span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="log-out" :size="14" />登录次数</div>
      <div class="kpi-value"><strong>{{ stats.loginCount }}</strong><span class="unit">次</span></div>
      <div class="kpi-foot"><span class="delta">成功 {{ stats.loginSuccess }} · 失败 {{ stats.loginFail }}</span><span class="muted" style="font-size: 9.5px">数据库实时值</span></div>
    </div>
    <div class="kpi hot">
      <div class="kpi-label"><Icon name="shield-check" :size="14" />敏感操作</div>
      <div class="kpi-value"><strong>{{ stats.sensitiveCount }}</strong><span class="unit">条</span></div>
      <div class="kpi-foot"><span class="delta">导出 / 用户 / 库位 / 库存调整</span><span class="muted" style="font-size: 9.5px">规则匹配</span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="triangle-alert" :size="14" />异常尝试</div>
      <div class="kpi-value"><strong>{{ stats.failureCount }}</strong><span class="unit">次</span></div>
      <div class="kpi-foot"><span class="delta down">失败结果计数</span><span class="muted" style="font-size: 9.5px">含登录失败</span></div>
    </div>
  </section>

  <div class="grid grid-12">
    <section class="card col-8">
      <div class="card-head">
        <div class="t"><h2 class="card-title">审计日志</h2><span class="card-sub">共 <strong>{{ total }}</strong> 条记录</span></div>
        <span class="tag tag-live tag-success"><span class="dot"></span>实时写入</span>
      </div>
      <div class="card-body" style="padding-top: 12px">
        <div class="filter-bar">
          <div class="search-box">
            <Icon name="search" :size="15" />
            <input v-model="keyword" class="input" type="search" placeholder="搜索操作人、对象或 IP" aria-label="搜索日志" />
          </div>
          <select v-model="module" class="select" aria-label="模块筛选">
            <option value="">全部模块</option>
            <option value="AUTH">登录</option>
            <option value="MASTERDATA">资料</option>
            <option value="INBOUND">入库</option>
            <option value="OUTBOUND">出库</option>
            <option value="INVENTORY">库存</option>
            <option value="LOCATION">库位</option>
            <option value="DEVICE">设备</option>
            <option value="SYSTEM">系统</option>
          </select>
          <select v-model="result" class="select" aria-label="结果筛选">
            <option value="">全部结果</option>
            <option value="SUCCESS">成功</option>
            <option value="WARN">警告</option>
            <option value="FAIL">失败</option>
          </select>
          <button class="btn btn-sm" type="button" @click="load"><Icon name="refresh-cw" :size="13" />刷新</button>
          <span class="spacer"></span>
          <span class="filter-count">显示 <strong>{{ total }}</strong> 条</span>
        </div>
        <div class="table-wrap">
          <table class="table">
            <thead><tr><th>时间</th><th>操作人 / 角色</th><th>模块</th><th>操作</th><th>对象</th><th>IP</th><th>结果</th></tr></thead>
            <tbody>
              <tr v-for="log in rows" :key="log.id" style="cursor: pointer" @click="selected = log">
                <td class="mono" style="font-size: 10.5px">{{ formatTime(log.createdAt) }}</td>
                <td><div class="cell-main">{{ log.username || '—' }}</div><div class="cell-sub">{{ log.roleCode || '—' }}</div></td>
                <td><span class="tag tag-neutral">{{ moduleLabels[log.module] || log.module }}</span></td>
                <td class="tight">{{ log.action }}</td>
                <td class="code" style="font-size: 10px">{{ targetText(log.target) }}</td>
                <td class="code" style="font-size: 10px">{{ formatIp(log.ip) }}</td>
                <td><span class="tag" :class="resultTone[log.result]"><span class="dot"></span>{{ resultLabels[log.result] || log.result }}</span></td>
              </tr>
              <tr v-if="!rows.length">
                <td colspan="7"><div class="empty-state"><img src="/images/undraw/not-found.svg" alt="" /><p>{{ loading ? '正在加载…' : '没有匹配的日志' }}</p></div></td>
              </tr>
            </tbody>
          </table>
        </div>
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
          <div class="t"><h2 class="card-title">敏感操作</h2><span class="card-sub">今日需关注</span></div>
          <span class="tag tag-danger"><span class="dot"></span>{{ sensitive.length }} 条</span>
        </div>
        <div class="card-body" style="padding-top: 10px">
          <div class="list">
            <div v-for="item in sensitive" :key="item.title + item.time" class="list-item">
              <span class="li-icon warn"><Icon name="shield-check" :size="15" /></span>
              <span class="li-body"><strong>{{ item.title }}</strong><small>{{ item.meta }}</small></span>
              <span class="li-end">{{ item.time }}</span>
            </div>
            <div v-if="!sensitive.length" class="chart-note">暂无敏感操作记录。</div>
          </div>
        </div>
      </section>

      <section class="card">
        <div class="card-head">
          <div class="t"><h2 class="card-title">登录安全</h2><span class="card-sub">最近 24 小时</span></div>
          <span class="tag tag-success"><span class="dot"></span>防护中</span>
        </div>
        <div class="card-body">
          <div class="stat-line"><span class="sl">成功登录</span><span class="sv">{{ stats.loginSuccess }} 次</span></div>
          <div class="stat-line"><span class="sl">失败尝试</span><span class="sv">{{ stats.loginFail }} 次</span></div>
          <div class="stat-line"><span class="sl">敏感操作</span><span class="sv">{{ stats.sensitiveCount }} 条</span></div>
          <div class="stat-line"><span class="sl">异常结果</span><span class="sv">{{ stats.failureCount }} 条</span></div>
          <div class="illus-panel mt16">
            <img class="illus" src="/images/undraw/security.svg" alt="登录安全插画" loading="lazy" />
          </div>
          <button class="btn mt12" type="button" style="width: 100%" @click="toast.push('已打开登录策略')">
            <Icon name="shield-check" :size="15" />查看登录策略
          </button>
        </div>
      </section>
    </div>
  </div>

  <p class="chart-note" style="margin-top: 18px">日志来自 audit_log 表；登录、主数据、库位、入库、出库操作都会自动写入。</p>

  <Teleport to="body">
    <template v-if="selected">
      <div class="overlay is-open" @click="selected = null"></div>
      <div class="drawer is-open" role="dialog" aria-modal="true" aria-label="日志详情">
        <div class="drawer-head">
          <div><h2>日志详情</h2><p>AUD-{{ selected.id }} · {{ formatTime(selected.createdAt) }}</p></div>
          <button class="icon-btn" type="button" aria-label="关闭" @click="selected = null"><Icon name="x" :size="18" /></button>
        </div>
        <div class="drawer-body">
          <div class="stat-line"><span class="sl">操作人</span><span class="sv">{{ selected.username }} · {{ selected.roleCode }}</span></div>
          <div class="stat-line"><span class="sl">模块 / 操作</span><span class="sv">{{ moduleLabels[selected.module] || selected.module }} · {{ selected.action }}</span></div>
          <div class="stat-line"><span class="sl">对象</span><span class="sv code">{{ targetText(selected.target) }}</span></div>
          <div class="stat-line"><span class="sl">IP 地址</span><span class="sv">{{ formatIp(selected.ip) }}</span></div>
          <div class="stat-line"><span class="sl">结果</span><span class="tag" :class="resultTone[selected.result]"><span class="dot"></span>{{ resultLabels[selected.result] || selected.result }}</span></div>
          <div class="stat-line"><span class="sl">详情</span><span class="sv">{{ selected.detail || '—' }}</span></div>
          <div class="hint-box mt16">
            <Icon name="info" :size="15" /><span>真实系统会在这里展示请求 ID、User-Agent 与操作前后快照。</span>
          </div>
        </div>
        <div class="drawer-foot">
          <span class="hint">只读记录</span>
          <div class="flex">
            <button class="btn" type="button" @click="selected = null">关闭</button>
            <button class="btn btn-primary" type="button" @click="toast.push('已标记为已复核'); selected = null">标记已复核</button>
          </div>
        </div>
      </div>
    </template>
  </Teleport>
</template>
