<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { inventorySummary, listInventory } from '../api/warehouse'
import { exportInventory } from '../api/exports'
import { useToastStore } from '../stores/toast'
import Icon from '../components/Icon.vue'
import { useEscape } from '../composables/useEscape'

const toast = useToastStore()
const rows = ref([])
const total = ref(0)
const summary = ref({ totalQty: 0, availableQty: 0, lockedQty: 0, lowCount: 0, skuCount: 0 })
const page = ref(1)
const size = 10
const keyword = ref('')
const category = ref('')
const status = ref('')
const loading = ref(false)
const error = ref('')
const selected = ref(null)
useEscape(() => { selected.value = null })

const pages = computed(() => Math.max(1, Math.ceil(total.value / size)))
const statusLabel = { NORMAL: '正常', LOW: '低库存', OUT: '缺货' }
const statusTone = { NORMAL: 'tag-success', LOW: 'tag-danger', OUT: 'tag-danger' }

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

async function download() {
  try {
    await exportInventory({ keyword: keyword.value, category: category.value, status: status.value })
    toast.push('库存明细已导出')
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

async function loadSummary() {
  try {
    const res = await inventorySummary()
    summary.value = res.data
  } catch {
    // The table error below is enough.
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await listInventory({
      page: page.value,
      size,
      keyword: keyword.value,
      category: category.value,
      status: status.value
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
watch([keyword, category, status], () => {
  clearTimeout(timer)
  timer = setTimeout(() => {
    page.value = 1
    load()
  }, 300)
})

function goPage(target) {
  if (target < 1 || target > pages.value || target === page.value) return
  page.value = target
  load()
}

onMounted(() => {
  loadSummary()
  load()
})
</script>

<template>
  <div class="page-head">
    <div>
      <div class="page-kicker"><span class="dot"></span>库存 · 实时</div>
      <h1>库存查询</h1>
      <p class="sub">按商品、分类与状态筛选，点击任意一行查看库存详情。</p>
    </div>
    <div class="page-actions">
      <button class="btn" type="button" @click="toast.push('盘点任务 CC-20261020-008 已创建')">
        <Icon name="clipboard-list" :size="15" />盘点任务
      </button>
      <button class="btn btn-primary" type="button" @click="download">
        <Icon name="download" :size="15" />导出 CSV
      </button>
    </div>
  </div>

  <div v-if="error" class="hint-box mb14" style="color: var(--danger)">
    <Icon name="triangle-alert" :size="15" /><span>库存加载失败：{{ error }}</span>
  </div>

  <section class="kpi-grid four" aria-label="库存概览">
    <div class="kpi">
      <div class="kpi-label"><Icon name="boxes" :size="14" />总库存</div>
      <div class="kpi-value"><strong>{{ formatNumber(summary.totalQty) }}</strong><span class="unit">件</span></div>
      <div class="kpi-foot"><span class="delta up">实时汇总</span><span class="muted" style="font-size: 9.5px">{{ summary.skuCount }} 个 SKU</span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="circle-check" :size="14" />可用库存</div>
      <div class="kpi-value"><strong>{{ formatNumber(summary.availableQty) }}</strong><span class="unit">件</span></div>
      <div class="kpi-foot"><span class="muted" style="font-size: 9.5px">已扣除锁定</span><span class="mini-bar"><i style="width: 91.7%"></i></span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="lock" :size="14" />锁定库存</div>
      <div class="kpi-value"><strong>{{ formatNumber(summary.lockedQty) }}</strong><span class="unit">件</span></div>
      <div class="kpi-foot"><span class="delta">拣货 / 复核占用</span><span class="muted" style="font-size: 9.5px">实时</span></div>
    </div>
    <div class="kpi hot">
      <div class="kpi-label"><Icon name="triangle-alert" :size="14" />预警 SKU</div>
      <div class="kpi-value"><strong>{{ summary.lowCount }}</strong><span class="unit">个</span></div>
      <div class="kpi-foot"><span class="delta down">低于安全库存</span><span class="muted" style="font-size: 9.5px">建议补货</span></div>
    </div>
  </section>

  <section class="card">
    <div class="card-head">
      <div class="t">
        <h2 class="card-title">库存明细</h2>
        <span class="card-sub">共 <strong>{{ total }}</strong> 条匹配记录</span>
      </div>
      <span class="tag tag-live tag-success"><span class="dot"></span>实时库存</span>
    </div>
    <div class="card-body" style="padding-top: 12px">
      <div class="filter-bar">
        <div class="search-box">
          <Icon name="search" :size="15" />
          <input v-model="keyword" class="input" type="search" placeholder="搜索 SKU、商品名或库位" aria-label="搜索库存" />
        </div>
        <select v-model="category" class="select" aria-label="分类筛选">
          <option value="">全部分类</option>
          <option>电子配件</option>
          <option>智能家居</option>
          <option>户外装备</option>
          <option>生活用品</option>
        </select>
        <select v-model="status" class="select" aria-label="状态筛选">
          <option value="">全部状态</option>
          <option value="NORMAL">正常</option>
          <option value="LOW">低库存</option>
          <option value="OUT">缺货</option>
        </select>
        <button class="btn btn-sm" type="button" @click="load">
          <Icon name="refresh-cw" :size="13" />刷新
        </button>
        <span class="spacer"></span>
        <span class="filter-count">显示 <strong>{{ total }}</strong> 条</span>
      </div>

      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>商品</th><th>分类</th><th>主库位</th>
              <th style="text-align: right">在库</th><th style="text-align: right">可用</th>
              <th style="text-align: right">锁定</th><th style="text-align: right">安全库存</th>
              <th>状态</th><th>更新时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" style="cursor: pointer" @click="selected = row">
              <td>
                <div class="cell-media">
                  <img class="thumb" :src="row.imageUrl || '/images/products/hub.jpg'" alt="" loading="lazy" />
                  <div><div class="cell-main">{{ row.name }}</div><div class="cell-sub code">{{ row.sku }}</div></div>
                </div>
              </td>
              <td>{{ row.category }}</td>
              <td class="code">{{ row.locationCode }}</td>
              <td class="num">{{ formatNumber(row.qty) }}</td>
              <td class="num">{{ formatNumber(row.available) }}</td>
              <td class="num">{{ formatNumber(row.lockedQty) }}</td>
              <td class="num">{{ formatNumber(row.safetyStock) }}</td>
              <td><span class="tag" :class="statusTone[row.status]"><span class="dot"></span>{{ statusLabel[row.status] || row.status }}</span></td>
              <td class="tight muted" style="font-size: 10.5px">{{ formatTime(row.updatedAt) }}</td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="9">
                <div class="empty-state">
                  <img src="/images/undraw/not-found.svg" alt="" />
                  <p>{{ loading ? '正在加载…' : '没有匹配的商品' }}</p>
                  <small>试试更换关键词或清除筛选条件</small>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div class="pagination">
      <span class="pg">显示 {{ total ? (page - 1) * size + 1 : 0 }}–{{ Math.min(page * size, total) }} / 共 {{ total }} 条</span>
      <button class="pg-btn" type="button" :disabled="page === 1" @click="goPage(page - 1)">上一页</button>
      <button
        v-for="p in pages"
        :key="p"
        class="pg-btn"
        :class="{ 'is-active': p === page }"
        type="button"
        @click="goPage(p)"
      >{{ p }}</button>
      <button class="pg-btn" type="button" :disabled="page === pages" @click="goPage(page + 1)">下一页</button>
    </div>
  </section>

  <p class="chart-note" style="margin-top: 18px">数据来自 /api/v1/inventory · 商品缩略图为 Picsum 占位图。</p>

  <Teleport to="body">
    <template v-if="selected">
      <div class="overlay is-open" @click="selected = null"></div>
      <div class="drawer is-open" role="dialog" aria-modal="true" aria-label="库存详情">
        <div class="drawer-head">
          <div>
            <h2>{{ selected.name }}</h2>
            <p>{{ selected.sku }} · {{ selected.category }}</p>
          </div>
          <button class="icon-btn" type="button" aria-label="关闭" @click="selected = null"><Icon name="x" :size="18" /></button>
        </div>
        <div class="drawer-body">
          <div class="flex" style="align-items: flex-start; gap: 14px">
            <img :src="selected.imageUrl || '/images/products/hub.jpg'" alt="" style="width: 96px; height: 96px; border-radius: 8px; object-fit: cover; border: 1px solid var(--line)" />
            <div class="grow">
              <span class="tag" :class="statusTone[selected.status]"><span class="dot"></span>{{ statusLabel[selected.status] }}</span>
              <div class="detail-grid" style="grid-template-columns: 1fr 1fr 1fr; margin-top: 10px; border-top: 0">
                <div><dt>在库</dt><dd class="num">{{ formatNumber(selected.qty) }}</dd></div>
                <div><dt>可用</dt><dd class="num">{{ formatNumber(selected.available) }}</dd></div>
                <div><dt>锁定</dt><dd class="num">{{ formatNumber(selected.lockedQty) }}</dd></div>
              </div>
            </div>
          </div>
          <h3 class="section-title mt20">库存属性</h3>
          <div class="stat-line"><span class="sl">主库位</span><span class="sv">{{ selected.locationCode }}</span></div>
          <div class="stat-line"><span class="sl">安全库存</span><span class="sv">{{ formatNumber(selected.safetyStock) }} 件</span></div>
          <div class="stat-line"><span class="sl">分类</span><span class="sv">{{ selected.category }}</span></div>
          <div class="stat-line"><span class="sl">最近更新</span><span class="sv">{{ formatTime(selected.updatedAt) }}</span></div>
          <div class="hint-box mt16">
            <Icon name="info" :size="15" />
            <span>库存流水与批次明细将在下一步接入 inventory_movement 表。</span>
          </div>
        </div>
        <div class="drawer-foot">
          <span class="hint">可用 = 在库 − 锁定</span>
          <div class="flex">
            <button class="btn" type="button" @click="toast.push('已生成补货建议')">补货建议</button>
            <button class="btn btn-primary" type="button" @click="toast.push('已生成移库单')">创建移库单</button>
          </div>
        </div>
      </div>
    </template>
  </Teleport>
</template>
