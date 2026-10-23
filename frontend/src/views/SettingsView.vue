<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getSettings, saveSettings } from '../api/warehouse'
import { createUser, deleteUser, listRoles, listUsers, updateUser } from '../api/users'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'
import { useEscape } from '../composables/useEscape'
import Icon from '../components/Icon.vue'

const auth = useAuthStore()
const toast = useToastStore()

const tab = ref('warehouse')
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const tabs = [
  { value: 'warehouse', label: '仓库信息' },
  { value: 'users', label: '用户与角色' },
  { value: 'alerts', label: '预警规则' },
  { value: 'notify', label: '通知设置' }
]
const canEdit = computed(() => ['ADMIN', 'MANAGER'].includes(auth.role))

const form = reactive({
  name: '华东一号仓',
  code: 'WMS-SZ-001',
  address: '江苏省苏州市工业园区物流大道 88 号',
  timezone: 'Asia/Shanghai (UTC+8)',
  hours: '08:00 – 20:00',
  temp: '常温 15–28°C · 恒温 18–24°C',
  capacity: 1280
})

const users = ref([])
const userTotal = ref(0)
const roles = ref([])
const usersLoading = ref(false)
const userModalOpen = ref(false)
const editingUser = ref(null)
const userSaving = ref(false)
const userForm = reactive({
  username: '', realName: '', roleCode: 'RECEIVER', status: 'ACTIVE', password: ''
})
const roleTone = {
  ADMIN: 'tag-violet', MANAGER: 'tag-accent', RECEIVER: 'tag-info', INSPECTOR: 'tag-neutral'
}

const roleLabel = computed(() => Object.fromEntries(roles.value.map((role) => [role.code, role.label])))
const userSummary = computed(() => {
  const counts = roles.value.map((role) => {
    const count = users.value.filter((user) => user.roleCode === role.code).length
    return `${role.label} ${count}`
  })
  return `共 ${userTotal.value} 名成员 · ${counts.join(' · ')}`
})
useEscape(() => { userModalOpen.value = false })

const alertRules = reactive([
  { key: 'alert.lowStock', label: '低库存预警', desc: '可用库存低于安全库存时提醒，建议在每日 09:00 汇总一次', enabled: true },
  { key: 'alert.temp', label: '温湿度超限', desc: '温度高于 26°C 或湿度高于 65% 时立即通知设备运维', enabled: true },
  { key: 'alert.deviceOffline', label: '设备离线', desc: '设备连续 3 分钟无心跳时标记离线并通知', enabled: true },
  { key: 'alert.pickTimeout', label: '拣货超时', desc: '波次拣货超过预计时长 20% 时提醒班组长', enabled: false }
])

const notifyRules = reactive([
  { key: 'notify.site', label: '站内通知', desc: '在右上角通知中心接收作业与设备消息', enabled: true },
  { key: 'notify.email', label: '邮件摘要', desc: '每日 18:00 发送当日出入库与异常摘要', enabled: true },
  { key: 'notify.wecom', label: '企业微信推送', desc: '加急订单与设备故障推送到运维群', enabled: false },
  { key: 'notify.sound', label: '声音提醒', desc: '新异常进入工作台时播放提示音', enabled: false }
])

function apply(values) {
  form.name = values['warehouse.name'] ?? form.name
  form.code = values['warehouse.code'] ?? form.code
  form.address = values['warehouse.address'] ?? form.address
  form.timezone = values['warehouse.timezone'] ?? form.timezone
  form.hours = values['warehouse.hours'] ?? form.hours
  form.temp = values['warehouse.temp'] ?? form.temp
  form.capacity = Number(values['warehouse.capacity'] || form.capacity)
  alertRules.forEach((rule) => {
    if (values[rule.key] != null) rule.enabled = values[rule.key] === 'true'
  })
  notifyRules.forEach((rule) => {
    if (values[rule.key] != null) rule.enabled = values[rule.key] === 'true'
  })
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await getSettings()
    apply(res.data)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!canEdit.value) {
    toast.push('只有管理员或仓储主管可以保存系统设置', 'warn')
    return
  }
  saving.value = true
  try {
    const payload = {
      'warehouse.name': form.name,
      'warehouse.code': form.code,
      'warehouse.address': form.address,
      'warehouse.timezone': form.timezone,
      'warehouse.hours': form.hours,
      'warehouse.temp': form.temp,
      'warehouse.capacity': String(form.capacity)
    }
    alertRules.forEach((rule) => { payload[rule.key] = String(rule.enabled) })
    notifyRules.forEach((rule) => { payload[rule.key] = String(rule.enabled) })
    const res = await saveSettings(payload)
    apply(res.data)
    toast.push('设置已保存到后端')
  } catch (e) {
    toast.push(e.message, 'err')
  } finally {
    saving.value = false
  }
}

function toggle(rule) {
  rule.enabled = !rule.enabled
  toast.push(`${rule.label}已${rule.enabled ? '开启' : '关闭'}，点击保存后生效`)
}

async function loadUsers() {
  if (!canEdit.value) return
  usersLoading.value = true
  try {
    const [userRes, roleRes] = await Promise.all([
      listUsers({ page: 1, size: 100 }),
      listRoles()
    ])
    users.value = userRes.data.records
    userTotal.value = userRes.data.total
    roles.value = roleRes.data
  } catch (e) {
    toast.push(e.message, 'err')
  } finally {
    usersLoading.value = false
  }
}

function openCreateUser() {
  editingUser.value = null
  Object.assign(userForm, { username: '', realName: '', roleCode: 'RECEIVER', status: 'ACTIVE', password: '' })
  userModalOpen.value = true
}

function openEditUser(user) {
  editingUser.value = user
  Object.assign(userForm, {
    username: user.username, realName: user.realName, roleCode: user.roleCode,
    status: user.status, password: ''
  })
  userModalOpen.value = true
}

async function submitUser() {
  userSaving.value = true
  try {
    const payload = {
      username: userForm.username,
      realName: userForm.realName,
      roleCode: userForm.roleCode,
      status: userForm.status,
      password: userForm.password || null
    }
    if (editingUser.value) {
      await updateUser(editingUser.value.id, payload)
      toast.push('用户信息已更新')
    } else {
      await createUser(payload)
      toast.push('用户已创建')
    }
    userModalOpen.value = false
    await loadUsers()
  } catch (e) {
    toast.push(e.message, 'err')
  } finally {
    userSaving.value = false
  }
}

async function toggleUserStatus(user) {
  const status = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  try {
    await updateUser(user.id, {
      username: user.username, realName: user.realName, roleCode: user.roleCode,
      status, password: null
    })
    toast.push(`${user.username} 已${status === 'ACTIVE' ? '启用' : '停用'}`)
    await loadUsers()
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

async function removeUser(user) {
  if (!window.confirm(`确定删除用户 ${user.username}？`)) return
  try {
    await deleteUser(user.id)
    toast.push('用户已删除')
    await loadUsers()
  } catch (e) {
    toast.push(e.message, 'err')
  }
}

onMounted(() => {
  load()
  loadUsers()
})
</script>

<template>
  <div class="page-head">
    <div>
      <div class="page-kicker"><span class="dot"></span>系统 · 配置</div>
      <h1>系统设置</h1>
      <p class="sub">仓库资料、用户角色、预警阈值与通知偏好。</p>
    </div>
    <div class="page-actions">
      <button class="btn" type="button" :disabled="loading" @click="load"><Icon name="rotate-ccw" :size="15" />重置</button>
      <button class="btn btn-primary" type="button" :disabled="saving || !canEdit" @click="save"><Icon name="check" :size="15" />{{ saving ? '保存中…' : '保存设置' }}</button>
    </div>
  </div>

  <div v-if="error" class="hint-box mb14" style="color: var(--danger)">
    <Icon name="triangle-alert" :size="15" /><span>设置加载失败：{{ error }}</span>
  </div>

  <div class="grid grid-12">
    <section class="card col-8">
      <div class="card-body" style="padding-top: 6px">
        <div class="tabs" role="tablist" aria-label="设置分区">
          <button v-for="t in tabs" :key="t.value" class="tab" :class="{ 'is-active': tab === t.value }" type="button" role="tab" @click="tab = t.value">{{ t.label }}</button>
        </div>

        <div v-if="tab === 'warehouse'">
          <div class="form-grid">
            <div class="field"><label for="s-name">仓库名称</label><input id="s-name" v-model="form.name" class="input" /></div>
            <div class="field"><label for="s-code">仓库编码</label><input id="s-code" v-model="form.code" class="input mono" /></div>
            <div class="field full"><label for="s-addr">仓库地址</label><input id="s-addr" v-model="form.address" class="input" /></div>
            <div class="field"><label for="s-tz">时区</label><select id="s-tz" v-model="form.timezone" class="select"><option>Asia/Shanghai (UTC+8)</option><option>Asia/Tokyo (UTC+9)</option></select></div>
            <div class="field"><label for="s-hours">作业时段</label><input id="s-hours" v-model="form.hours" class="input" /></div>
            <div class="field"><label for="s-temp">温区设置</label><select id="s-temp" v-model="form.temp" class="select"><option>常温 15–28°C · 恒温 18–24°C</option><option>仅常温</option><option>常温 + 冷藏</option></select></div>
            <div class="field"><label for="s-cap">设计库位数</label><input id="s-cap" v-model.number="form.capacity" class="input" type="number" /></div>
          </div>
          <div class="form-actions">
            <button class="btn" type="button" @click="load">还原</button>
            <button class="btn btn-primary" type="button" :disabled="saving || !canEdit" @click="save">保存仓库信息</button>
          </div>
        </div>

        <div v-if="tab === 'users'">
          <div class="flex between mt12 mb12">
            <p class="card-sub">{{ canEdit ? userSummary : '仅管理员或仓储主管可以查看用户与角色' }}</p>
            <button v-if="canEdit" class="btn btn-sm btn-primary" type="button" @click="openCreateUser"><Icon name="plus" :size="13" />新增成员</button>
          </div>
          <div v-if="!canEdit" class="hint-box">
            <Icon name="lock" :size="15" /><span>当前角色没有用户管理权限。</span>
          </div>
          <div v-else class="table-wrap">
            <table class="table">
              <thead><tr><th>成员</th><th>角色</th><th>状态</th><th>创建时间</th><th></th></tr></thead>
              <tbody>
                <tr v-for="u in users" :key="u.id">
                  <td><div class="cell-media"><span class="avatar" style="width: 28px; height: 28px; font-size: 11px">{{ u.realName.slice(0, 1) }}</span><div><div class="cell-main">{{ u.realName }}</div><div class="cell-sub">{{ u.username }}</div></div></div></td>
                  <td><span class="tag" :class="roleTone[u.roleCode]">{{ roleLabel[u.roleCode] || u.roleCode }}</span></td>
                  <td><span class="tag" :class="u.status === 'ACTIVE' ? 'tag-success' : 'tag-warning'"><span class="dot"></span>{{ u.status === 'ACTIVE' ? '启用' : '停用' }}</span></td>
                  <td class="muted" style="font-size: 10.5px">{{ String(u.createdAt || '').replace('T', ' ').slice(0, 16) }}</td>
                  <td class="actions">
                    <button class="btn btn-sm btn-ghost" type="button" @click="openEditUser(u)">编辑</button>
                    <button class="btn btn-sm btn-ghost" type="button" @click="toggleUserStatus(u)">{{ u.status === 'ACTIVE' ? '停用' : '启用' }}</button>
                    <button class="btn btn-sm btn-ghost" type="button" @click="removeUser(u)">删除</button>
                  </td>
                </tr>
                <tr v-if="!users.length">
                  <td colspan="5"><div class="empty-state"><p>{{ usersLoading ? '正在加载…' : '暂无用户' }}</p></div></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-if="tab === 'alerts'">
          <div v-for="rule in alertRules" :key="rule.key" class="setting-row">
            <div class="st"><strong>{{ rule.label }}</strong><small>{{ rule.desc }}</small></div>
            <label class="switch"><input type="checkbox" :checked="rule.enabled" @change="toggle(rule)" /><span class="track"></span></label>
          </div>
          <div class="form-actions">
            <button class="btn" type="button" @click="toast.push('已打开阈值配置')">阈值配置</button>
            <button class="btn btn-primary" type="button" :disabled="saving || !canEdit" @click="save">保存预警规则</button>
          </div>
        </div>

        <div v-if="tab === 'notify'">
          <div v-for="rule in notifyRules" :key="rule.key" class="setting-row">
            <div class="st"><strong>{{ rule.label }}</strong><small>{{ rule.desc }}</small></div>
            <label class="switch"><input type="checkbox" :checked="rule.enabled" @change="toggle(rule)" /><span class="track"></span></label>
          </div>
          <div class="form-actions">
            <button class="btn btn-primary" type="button" :disabled="saving || !canEdit" @click="save">保存通知设置</button>
          </div>
        </div>
      </div>
    </section>

    <div class="col-4 stack">
      <section class="card">
        <div class="card-body">
          <div class="illus-panel">
            <img class="illus" src="/images/undraw/security.svg" alt="安全与权限插画" loading="lazy" />
          </div>
          <h2 class="section-title mt16">权限说明</h2>
          <p class="chart-note" style="font-size: 11.5px; line-height: 1.65">
            角色按最小权限原则配置：系统管理员可管理用户；仓储主管可配置规则与导出报表；收货员处理入库、上架与出库；
            质检员可提交退货质检结果。所有敏感操作写入审计日志。
          </p>
          <div class="stat-line mt12"><span class="sl">审计日志保留</span><span class="sv">180 天</span></div>
          <div class="stat-line"><span class="sl">双人复核</span><span class="sv">已开启</span></div>
          <div class="stat-line"><span class="sl">登录保护</span><span class="sv">连续 5 次锁定</span></div>
        </div>
      </section>

      <section class="card">
        <div class="card-head">
          <div class="t"><h2 class="card-title">接口状态</h2><span class="card-sub">系统集成</span></div>
        </div>
        <div class="card-body">
          <div class="stat-line"><span class="sl">WMS 后端 API</span><span class="tag tag-success"><span class="dot"></span>正常</span></div>
          <div class="stat-line"><span class="sl">MQTT Broker</span><span class="tag tag-success"><span class="dot"></span>待激活</span></div>
          <div class="stat-line"><span class="sl">ERP 同步</span><span class="tag tag-success"><span class="dot"></span>正常</span></div>
          <div class="stat-line"><span class="sl">短信网关</span><span class="tag tag-warning"><span class="dot"></span>延迟</span></div>
          <div class="hint-box mt12">
            <Icon name="info" :size="15" />
            <span>仓库信息、预警和通知开关保存到 system_setting 表；用户与角色来自 sys_user 表。</span>
          </div>
        </div>
      </section>
    </div>
  </div>

  <Teleport to="body">
    <template v-if="userModalOpen">
      <div class="overlay is-open" @click="userModalOpen = false"></div>
      <div class="modal is-open" role="dialog" aria-modal="true" aria-label="用户信息">
        <div class="modal-head">
          <div><h2>{{ editingUser ? '编辑用户' : '新增用户' }}</h2><p>角色决定可执行的操作，密码至少 6 位。</p></div>
          <button class="icon-btn" type="button" aria-label="关闭" @click="userModalOpen = false"><Icon name="x" :size="18" /></button>
        </div>
        <form @submit.prevent="submitUser">
          <div class="modal-body">
            <div class="form-grid">
              <div class="field">
                <label for="user-account">账号</label>
                <input id="user-account" v-model="userForm.username" class="input mono" :disabled="!!editingUser" required />
              </div>
              <div class="field">
                <label for="user-name">姓名</label>
                <input id="user-name" v-model="userForm.realName" class="input" required />
              </div>
              <div class="field">
                <label for="user-role">角色</label>
                <select id="user-role" v-model="userForm.roleCode" class="select">
                  <option v-for="role in roles" :key="role.code" :value="role.code">{{ role.label }}</option>
                </select>
              </div>
              <div class="field">
                <label for="user-status">状态</label>
                <select id="user-status" v-model="userForm.status" class="select">
                  <option value="ACTIVE">启用</option>
                  <option value="DISABLED">停用</option>
                </select>
              </div>
              <div class="field full">
                <label for="user-password">{{ editingUser ? '重置密码（留空不修改）' : '初始密码' }}</label>
                <input id="user-password" v-model="userForm.password" class="input" type="password" minlength="6" :required="!editingUser" />
              </div>
            </div>
          </div>
          <div class="modal-foot">
            <button class="btn" type="button" @click="userModalOpen = false">取消</button>
            <button class="btn btn-primary" type="submit" :disabled="userSaving">{{ userSaving ? '保存中…' : '保存' }}</button>
          </div>
        </form>
      </div>
    </template>
  </Teleport>
</template>
