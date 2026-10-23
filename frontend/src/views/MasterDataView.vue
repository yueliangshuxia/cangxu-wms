<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  createProduct,
  listCustomers,
  listProducts,
  listSuppliers
} from '../api/masterdata'
import { exportMasterdata } from '../api/exports'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import Icon from '../components/Icon.vue'
import { useEscape } from '../composables/useEscape'

const auth = useAuthStore()
const toast = useToastStore()

const activeTab = ref('products')
const keyword = ref('')
const status = ref('')
const loading = ref(false)
const error = ref('')
const products = ref([])
const suppliers = ref([])
const customers = ref([])
const productTotal = ref(0)
const supplierTotal = ref(0)
const customerTotal = ref(0)
const modalOpen = ref(false)
useEscape(() => { modalOpen.value = false })
const saving = ref(false)

async function download() {
  try {
    await exportMasterdata()
    toast.push('主数据已导出')
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

const form = reactive({
  name: '',
  sku: '',
  category: '电子配件',
  spec: '',
  unit: '件',
  tempType: 'NORMAL',
  safetyStock: 0,
  imageUrl: '/images/products/hub.jpg'
})

const canEdit = computed(() => ['ADMIN', 'MANAGER'].includes(auth.role))
const activeCount = computed(() => {
  if (activeTab.value === 'products') return productTotal.value
  if (activeTab.value === 'suppliers') return supplierTotal.value
  return customerTotal.value
})
const statusOptions = computed(() => {
  if (activeTab.value === 'products') {
    return [
      { value: 'ON_SALE', label: '在售' },
      { value: 'OFF_SALE', label: '停售' }
    ]
  }
  return [
    { value: 'ACTIVE', label: '合作中' },
    { value: 'EVALUATING', label: '评估中' },
    { value: 'SUSPENDED', label: '暂停' }
  ]
})

const productStatusLabel = { ON_SALE: '在售', OFF_SALE: '停售' }
const partnerStatusLabel = { ACTIVE: '合作中', EVALUATING: '评估中', SUSPENDED: '暂停' }
const statusTone = (status) => {
  if (['ON_SALE', 'ACTIVE'].includes(status)) return 'tag-success'
  if (['EVALUATING'].includes(status)) return 'tag-warning'
  return 'tag-danger'
}
const tempLabel = (value) => (value === 'CONSTANT' ? '恒温' : '常温')

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function loadProducts() {
  const res = await listProducts({ page: 1, size: 100, keyword: keyword.value, status: status.value })
  products.value = res.data.records
  productTotal.value = res.data.total
}

async function loadSuppliers() {
  const res = await listSuppliers({ page: 1, size: 100, keyword: keyword.value, status: status.value })
  suppliers.value = res.data.records
  supplierTotal.value = res.data.total
}

async function loadCustomers() {
  const res = await listCustomers({ page: 1, size: 100, keyword: keyword.value, status: status.value })
  customers.value = res.data.records
  customerTotal.value = res.data.total
}

async function loadActive() {
  loading.value = true
  error.value = ''
  try {
    if (activeTab.value === 'products') await loadProducts()
    else if (activeTab.value === 'suppliers') await loadSuppliers()
    else await loadCustomers()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  loading.value = true
  error.value = ''
  try {
    await Promise.all([loadProducts(), loadSuppliers(), loadCustomers()])
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

let debounceTimer
watch([keyword, status], () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(loadActive, 300)
})
watch(activeTab, () => {
  status.value = ''
  loadActive()
})

function openCreate() {
  if (!canEdit.value) {
    toast.push('只有管理员或仓储主管可以新增商品', 'warn')
    return
  }
  Object.assign(form, {
    name: '',
    sku: '',
    category: '电子配件',
    spec: '',
    unit: '件',
    tempType: 'NORMAL',
    safetyStock: 0,
    imageUrl: '/images/products/hub.jpg'
  })
  modalOpen.value = true
}

async function submitProduct() {
  saving.value = true
  try {
    await createProduct(form)
    toast.push('商品已创建，可在入库单中引用')
    modalOpen.value = false
    activeTab.value = 'products'
    await loadProducts()
  } catch (e) {
    toast.push(e.message, 'err')
  } finally {
    saving.value = false
  }
}

onMounted(loadAll)
</script>

<template>
  <div class="page-head">
    <div>
      <div class="page-kicker"><span class="dot"></span>资料 · 主数据</div>
      <h1>基础资料</h1>
      <p class="sub">商品、供应商与客户主数据。入库、出库和库存单据都引用这里的编码。</p>
    </div>
    <div class="page-actions">
      <button class="btn" type="button" @click="download">
        <Icon name="download" :size="15" />导出资料
      </button>
      <button v-if="canEdit" class="btn btn-primary" type="button" @click="openCreate">
        <Icon name="plus" :size="15" />新增商品
      </button>
    </div>
  </div>

  <div v-if="error" class="hint-box mb14" style="color: var(--danger)">
    <Icon name="triangle-alert" :size="15" />
    <span>主数据加载失败：{{ error }}。请确认后端已启动。</span>
  </div>

  <section class="kpi-grid four" aria-label="主数据概览">
    <div class="kpi">
      <div class="kpi-label"><Icon name="boxes" :size="14" />商品 SKU</div>
      <div class="kpi-value"><strong>{{ productTotal }}</strong><span class="unit">种</span></div>
      <div class="kpi-foot"><span class="delta">在售与停售合计</span><span class="muted" style="font-size: 9.5px">数据库实时值</span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="truck" :size="14" />供应商</div>
      <div class="kpi-value"><strong>{{ supplierTotal }}</strong><span class="unit">家</span></div>
      <div class="kpi-foot"><span class="delta up">合作中</span><span class="muted" style="font-size: 9.5px">主数据</span></div>
    </div>
    <div class="kpi">
      <div class="kpi-label"><Icon name="users" :size="14" />客户</div>
      <div class="kpi-value"><strong>{{ customerTotal }}</strong><span class="unit">个</span></div>
      <div class="kpi-foot"><span class="delta">电商 · 线下 · B2B</span><span class="muted" style="font-size: 9.5px">主数据</span></div>
    </div>
    <div class="kpi hot">
      <div class="kpi-label"><Icon name="shield-check" :size="14" />资料完整率</div>
      <div class="kpi-value"><strong>96.8</strong><span class="unit">%</span></div>
      <div class="kpi-foot"><span class="delta down">27 条待补充</span><span class="mini-bar"><i style="width: 96.8%"></i></span></div>
    </div>
  </section>

  <div class="grid grid-12">
    <section class="card col-8">
      <div class="card-body" style="padding-top: 6px">
        <div class="tabs" role="tablist" aria-label="主数据分类">
          <button class="tab" :class="{ 'is-active': activeTab === 'products' }" type="button" role="tab" @click="activeTab = 'products'">
            商品资料 <span class="count">{{ productTotal }}</span>
          </button>
          <button class="tab" :class="{ 'is-active': activeTab === 'suppliers' }" type="button" role="tab" @click="activeTab = 'suppliers'">
            供应商 <span class="count">{{ supplierTotal }}</span>
          </button>
          <button class="tab" :class="{ 'is-active': activeTab === 'customers' }" type="button" role="tab" @click="activeTab = 'customers'">
            客户 <span class="count">{{ customerTotal }}</span>
          </button>
        </div>

        <div class="filter-bar">
          <div class="search-box">
            <Icon name="search" :size="15" />
            <input v-model="keyword" class="input" type="search" placeholder="搜索编码、名称或联系人" aria-label="搜索主数据" />
          </div>
          <select v-model="status" class="select" aria-label="状态筛选">
            <option value="">全部状态</option>
            <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
          <button class="btn btn-sm" type="button" @click="loadActive">
            <Icon name="refresh-cw" :size="13" />刷新
          </button>
          <span class="spacer"></span>
          <span class="filter-count">显示 <strong>{{ activeCount }}</strong> 条</span>
        </div>

        <div v-if="activeTab === 'products'" class="table-wrap">
          <table class="table">
            <thead><tr><th>商品</th><th>分类</th><th>规格</th><th>单位</th><th>温区</th><th>状态</th><th>更新时间</th></tr></thead>
            <tbody>
              <tr v-for="p in products" :key="p.id">
                <td>
                  <div class="cell-media">
                    <img class="thumb" :src="p.imageUrl || '/images/products/hub.jpg'" alt="" loading="lazy" />
                    <div><div class="cell-main">{{ p.name }}</div><div class="cell-sub code">{{ p.sku }}</div></div>
                  </div>
                </td>
                <td>{{ p.category }}</td>
                <td>{{ p.spec || '—' }}</td>
                <td>{{ p.unit }}</td>
                <td><span class="tag" :class="p.tempType === 'CONSTANT' ? 'tag-info' : 'tag-neutral'">{{ tempLabel(p.tempType) }}</span></td>
                <td><span class="tag" :class="statusTone(p.status)"><span class="dot"></span>{{ productStatusLabel[p.status] || p.status }}</span></td>
                <td class="tight muted" style="font-size: 10.5px">{{ formatTime(p.updatedAt) }}</td>
              </tr>
              <tr v-if="!products.length">
                <td colspan="7">
                  <div class="empty-state">
                    <img src="/images/undraw/not-found.svg" alt="" />
                    <p>{{ loading ? '正在加载…' : '没有匹配的商品' }}</p>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="activeTab === 'suppliers'" class="table-wrap">
          <table class="table">
            <thead><tr><th>编码</th><th>供应商</th><th>联系人</th><th>电话</th><th>供货品类</th><th>状态</th></tr></thead>
            <tbody>
              <tr v-for="s in suppliers" :key="s.id">
                <td class="code">{{ s.code }}</td>
                <td><div class="cell-main">{{ s.name }}</div><div class="cell-sub">{{ s.category }}</div></td>
                <td>{{ s.contact || '—' }}</td>
                <td class="code">{{ s.phone || '—' }}</td>
                <td>{{ s.category || '—' }}</td>
                <td><span class="tag" :class="statusTone(s.status)"><span class="dot"></span>{{ partnerStatusLabel[s.status] || s.status }}</span></td>
              </tr>
              <tr v-if="!suppliers.length">
                <td colspan="6"><div class="empty-state"><img src="/images/undraw/not-found.svg" alt="" /><p>{{ loading ? '正在加载…' : '没有匹配的供应商' }}</p></div></td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="activeTab === 'customers'" class="table-wrap">
          <table class="table">
            <thead><tr><th>编码</th><th>客户</th><th>渠道</th><th>联系人</th><th>电话</th><th>状态</th></tr></thead>
            <tbody>
              <tr v-for="c in customers" :key="c.id">
                <td class="code">{{ c.code }}</td>
                <td><div class="cell-main">{{ c.name }}</div><div class="cell-sub">{{ c.channel }}</div></td>
                <td>{{ c.channel || '—' }}</td>
                <td>{{ c.contact || '—' }}</td>
                <td class="code">{{ c.phone || '—' }}</td>
                <td><span class="tag" :class="statusTone(c.status)"><span class="dot"></span>{{ partnerStatusLabel[c.status] || c.status }}</span></td>
              </tr>
              <tr v-if="!customers.length">
                <td colspan="6"><div class="empty-state"><img src="/images/undraw/not-found.svg" alt="" /><p>{{ loading ? '正在加载…' : '没有匹配的客户' }}</p></div></td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="flex between mt12" style="padding-bottom: 4px">
          <span class="chart-note">主数据变更会写入审计日志 · 删除前需检查未结单据</span>
          <span class="page-meta">API: /api/v1/products · /suppliers · /customers</span>
        </div>
      </div>
    </section>

    <div class="col-4 stack">
      <section class="card">
        <div class="card-head">
          <div class="t"><h2 class="card-title">资料完整率</h2><span class="card-sub">按必填字段统计</span></div>
          <span class="tag tag-warning">27 条待补充</span>
        </div>
        <div class="card-body">
          <div class="stat-line"><span class="sl">商品图片</span><span class="sv">98.2%</span></div>
          <div class="mini-bar" style="max-width: none; height: 4px; margin: 2px 0 10px"><i style="width: 98.2%"></i></div>
          <div class="stat-line"><span class="sl">供应商联系方式</span><span class="sv">100%</span></div>
          <div class="mini-bar" style="max-width: none; height: 4px; margin: 2px 0 10px"><i style="width: 100%"></i></div>
          <div class="stat-line"><span class="sl">客户渠道归属</span><span class="sv">94.6%</span></div>
          <div class="mini-bar" style="max-width: none; height: 4px; margin: 2px 0 10px"><i style="width: 94.6%"></i></div>
          <div class="stat-line"><span class="sl">商品温区</span><span class="sv">95.1%</span></div>
          <div class="mini-bar" style="max-width: none; height: 4px"><i style="width: 95.1%"></i></div>
          <div class="illus-panel mt16">
            <img class="illus" src="/images/undraw/checking-boxes.svg" alt="核对资料插画" loading="lazy" />
          </div>
        </div>
      </section>

      <section class="card">
        <div class="card-head">
          <div class="t"><h2 class="card-title">最近变更</h2><span class="card-sub">审计日志摘要</span></div>
        </div>
        <div class="card-body" style="padding-top: 10px">
          <div class="list">
            <div class="list-item">
              <span class="li-icon info"><Icon name="pencil" :size="15" /></span>
              <span class="li-body"><strong>WH-1007 规格更新</strong><small>1.5m / 100W · 操作员 陈屿</small></span>
              <span class="li-end">10:04</span>
            </div>
            <div class="list-item">
              <span class="li-icon ok"><Icon name="plus" :size="15" /></span>
              <span class="li-body"><strong>新增客户 CUS-008</strong><small>企业采购 · 启明制造</small></span>
              <span class="li-end">昨天</span>
            </div>
            <div class="list-item">
              <span class="li-icon warn"><Icon name="triangle-alert" :size="15" /></span>
              <span class="li-body"><strong>SUP-008 状态暂停</strong><small>南京云仓科技 · 待重新评估</small></span>
              <span class="li-end">10-18</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>

  <p class="chart-note" style="margin-top: 18px">主数据编码规则：商品 WH-####，供应商 SUP-###，客户 CUS-###。</p>

  <Teleport to="body">
    <template v-if="modalOpen">
      <div class="overlay is-open" @click="modalOpen = false"></div>
      <div class="modal is-open" role="dialog" aria-modal="true" aria-labelledby="product-modal-title">
        <div class="modal-head">
          <div>
            <h2 id="product-modal-title">新增商品</h2>
            <p>创建后可在入库单和库存查询中引用。</p>
          </div>
          <button class="icon-btn" type="button" aria-label="关闭" @click="modalOpen = false">
            <Icon name="x" :size="18" />
          </button>
        </div>
        <form @submit.prevent="submitProduct">
          <div class="modal-body">
            <div class="form-grid">
              <div class="field">
                <label for="p-name">商品名称</label>
                <input id="p-name" v-model="form.name" class="input" placeholder="例如：桌面收纳盒" required />
              </div>
              <div class="field">
                <label for="p-sku">SKU 编码</label>
                <input id="p-sku" v-model="form.sku" class="input mono" placeholder="WH-3003" required />
              </div>
              <div class="field">
                <label for="p-cat">分类</label>
                <select id="p-cat" v-model="form.category" class="select">
                  <option>电子配件</option>
                  <option>智能家居</option>
                  <option>户外装备</option>
                  <option>生活用品</option>
                </select>
              </div>
              <div class="field">
                <label for="p-spec">规格</label>
                <input id="p-spec" v-model="form.spec" class="input" placeholder="例如：30 × 20 × 15 cm" />
              </div>
              <div class="field">
                <label for="p-unit">单位</label>
                <select id="p-unit" v-model="form.unit" class="select">
                  <option>件</option>
                  <option>台</option>
                  <option>个</option>
                  <option>箱</option>
                </select>
              </div>
              <div class="field">
                <label for="p-temp">温区</label>
                <select id="p-temp" v-model="form.tempType" class="select">
                  <option value="NORMAL">常温</option>
                  <option value="CONSTANT">恒温</option>
                </select>
              </div>
              <div class="field">
                <label for="p-safety">安全库存</label>
                <input id="p-safety" v-model.number="form.safetyStock" class="input" type="number" min="0" />
              </div>
            </div>
          </div>
          <div class="modal-foot">
            <button class="btn" type="button" @click="modalOpen = false">取消</button>
            <button class="btn btn-primary" type="submit" :disabled="saving">
              <Icon name="check" :size="15" />{{ saving ? '创建中…' : '创建商品' }}
            </button>
          </div>
        </form>
      </div>
    </template>
  </Teleport>
</template>
