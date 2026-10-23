<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useThemeStore } from '../stores/theme'
import { useToastStore } from '../stores/toast'
import Icon from '../components/Icon.vue'
import ToastHost from '../components/ToastHost.vue'

const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()
const toast = useToastStore()

const username = ref('')
const password = ref('')
const submitting = ref(false)
const error = ref('')

function fillDemo() {
  username.value = 'chenyu'
  password.value = 'wms2026'
  toast.push('演示账号已填入')
}

async function submit() {
  error.value = ''
  submitting.value = true
  try {
    await auth.login(username.value, password.value)
    router.push('/dashboard')
  } catch (e) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-visual" aria-label="产品介绍">
      <img src="/images/pexels/warehouse-dark.jpg" alt="仓库通道两侧高耸的货架" />
      <div class="login-brand">
        <span class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M4 7h16M4 12h16M4 17h10" />
            <circle cx="18" cy="17" r="2" fill="currentColor" stroke="none" />
          </svg>
        </span>
        <span class="brand-text"><strong>仓序</strong><small>CANGXU WMS</small></span>
      </div>

      <div class="login-hero">
        <div class="lh-kicker"><span class="dot"></span>华东一号仓 · 系统在线</div>
        <h1>让每一次上架，<br />都有依据。</h1>
        <p>把月台、库位、库存与设备放进同一张作业图。AHP-TOPSIS 货位推荐，让拣货少走回头路。</p>
        <div class="login-stats">
          <div><strong>1,280</strong><span>规划库位</span></div>
          <div><strong>99.2%</strong><span>库存准确率</span></div>
          <div><strong>24</strong><span>在线设备</span></div>
        </div>
      </div>

      <span class="login-credit">PHOTO · PEXELS / OTHMANE ETTALBI</span>
    </section>

    <section class="login-side">
      <div class="login-card">
        <p class="eyebrow">WAREHOUSE MANAGEMENT SYSTEM</p>
        <h2>登录仓序 WMS</h2>
        <p class="lead">使用你的仓储账号进入工作台。</p>

        <form class="login-form" @submit.prevent="submit">
          <div class="field">
            <label for="login-user">账号</label>
            <input
              id="login-user"
              v-model="username"
              class="input"
              autocomplete="username"
              placeholder="工号或邮箱"
              required
            />
          </div>
          <div class="field">
            <label for="login-pass">密码</label>
            <input
              id="login-pass"
              v-model="password"
              class="input"
              type="password"
              autocomplete="current-password"
              placeholder="请输入密码"
              required
            />
          </div>
          <div class="login-row">
            <label class="flex" style="gap: 6px; cursor: pointer">
              <input type="checkbox" checked style="accent-color: var(--accent)" />记住这台设备
            </label>
            <button type="button" style="color: var(--accent); font-size: 11.5px" @click="toast.push('请联系仓储主管重置密码')">
              忘记密码？
            </button>
          </div>

          <div v-if="error" class="hint-box" style="color: var(--danger); border-color: var(--danger)">
            <Icon name="triangle-alert" :size="15" />
            <span>{{ error }}</span>
          </div>

          <button class="btn btn-primary login-submit" type="submit" :disabled="submitting">
            <Icon name="arrow-right" :size="16" />
            {{ submitting ? '正在验证…' : '进入工作台' }}
          </button>
        </form>

        <div class="login-hint">
          演示账号：<code>chenyu</code> / <code>wms2026</code>
          <button type="button" style="color: var(--accent); font-weight: 500; margin-left: 4px" @click="fillDemo">
            一键填入
          </button>
          <br />真实后端登录，不发送任何多余凭据。
        </div>

        <div class="login-foot">
          <span>© 2026 仓序 WMS · 毕业设计</span>
          <button class="theme-btn" type="button" @click="theme.toggle()">
            <Icon :name="theme.theme === 'dark' ? 'sun' : 'moon'" :size="15" />
          </button>
        </div>

        <div class="login-illus">
          <img src="/images/undraw/logistics.svg" alt="物流配送插画" />
        </div>
      </div>
    </section>
  </div>
  <ToastHost />
</template>
