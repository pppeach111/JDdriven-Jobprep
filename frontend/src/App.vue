<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import type { IconName } from '@/components/icons'

const route = useRoute()

const goalId = computed(() => String(route.params.goalId ?? ''))

/**
 * 一级导航（shared_docs/19-ui-design-system.md §3.1 固定 6 项）。
 *
 * 尚未实现的路由不伪造跳转：渲染为不可用状态，并给出非颜色线索，
 * 遵守 §2.4 状态必须诚实 与 §14 不得用 Mock 冒充真实集成。
 */
interface PrimaryNav {
  key: string
  label: string
  icon: IconName
  to: string | null
}

const primaryNav = computed<PrimaryNav[]>(() => [
  { key: 'goal-create', label: '求职目标', icon: 'target', to: '/goals/new' },
  {
    key: 'capability-map',
    label: '能力与证据',
    icon: 'layers',
    to: goalId.value ? `/goals/${goalId.value}/capability` : null,
  },
  { key: 'jobs', label: '岗位发现', icon: 'briefcase', to: null },
  { key: 'assessment', label: '测评与面试', icon: 'clipboard-check', to: null },
  { key: 'learning', label: '学习计划', icon: 'book-open', to: null },
  { key: 'resume', label: '简历建议', icon: 'file-text', to: null },
])

const activeKey = computed(() => String(route.name ?? ''))

/**
 * 目标建立流程（§5.4 多步骤流程需显示当前步骤与可返回性）。
 * 这是"求职目标"内部的子流程，与上面的一级导航不是同一层级。
 */
const steps = computed(() => [
  { key: 'goal-create', label: '求职目标', to: '/goals/new', enabled: true },
  {
    key: 'jd-import',
    label: '目标 JD',
    to: goalId.value ? `/goals/${goalId.value}/jd` : null,
    enabled: Boolean(goalId.value),
  },
  {
    key: 'capability-map',
    label: '能力图谱',
    to: goalId.value ? `/goals/${goalId.value}/capability` : null,
    enabled: Boolean(goalId.value),
  },
])

/**
 * 移动端主导航抽屉（§3.3：`<768px` 一级导航链接收起进抽屉）。
 *
 * 无障碍约束：`role=dialog` + `aria-modal`；Esc 关闭；打开时把焦点移入抽屉并在
 * 抽屉内循环（Tab / Shift+Tab 不逃逸到背景）；关闭时把焦点归还触发按钮；
 * 打开期间锁定背景滚动；视口回到桌面档时自动关闭，避免留下不可见的活动对话框。
 */
const drawerOpen = ref(false)
const drawerRef = ref<HTMLElement | null>(null)
const menuBtnRef = ref<HTMLButtonElement | null>(null)

const DESKTOP_MQ = '(min-width: 768px)'
let desktopMq: MediaQueryList | null = null

function closeDrawer() {
  drawerOpen.value = false
}

function focusableInDrawer(): HTMLElement[] {
  const root = drawerRef.value
  if (!root) return []
  return Array.from(
    root.querySelectorAll<HTMLElement>('a[href], button:not([disabled])'),
  ).filter((el) => el.offsetParent !== null)
}

function onDrawerKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeDrawer()
    return
  }

  if (event.key !== 'Tab') return

  const items = focusableInDrawer()
  if (items.length === 0) return

  const first = items[0]
  const last = items[items.length - 1]
  const active = document.activeElement as HTMLElement | null
  const inside = active ? drawerRef.value?.contains(active) === true : false

  if (event.shiftKey && (!inside || active === first)) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && (!inside || active === last)) {
    event.preventDefault()
    first.focus()
  }
}

function onViewportChange(event: MediaQueryListEvent) {
  if (event.matches) closeDrawer()
}

watch(drawerOpen, async (open) => {
  if (open) {
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', onDrawerKeydown)
    await nextTick()
    focusableInDrawer()[0]?.focus()
    return
  }

  document.body.style.overflow = ''
  window.removeEventListener('keydown', onDrawerKeydown)
  const trigger = menuBtnRef.value
  // 视口变大导致的关闭不应把焦点丢给已隐藏的触发按钮
  if (trigger && trigger.offsetParent !== null) trigger.focus()
})

watch(
  () => route.fullPath,
  () => {
    if (drawerOpen.value) closeDrawer()
  },
)

onMounted(() => {
  desktopMq = window.matchMedia(DESKTOP_MQ)
  desktopMq.addEventListener('change', onViewportChange)
})

onBeforeUnmount(() => {
  desktopMq?.removeEventListener('change', onViewportChange)
  window.removeEventListener('keydown', onDrawerKeydown)
  document.body.style.overflow = ''
})
</script>

<template>
  <div class="app">
    <!-- 顶部横向导航（§3.2）：品牌 → 一级导航 → 搜索 → 通知 → 用户 -->
    <header class="topnav">
      <div class="topnav__inner">
        <RouterLink to="/goals/new" class="brand">
          <span class="brand__mark" aria-hidden="true"></span>
          <span class="brand__text">
            <strong>职径</strong>
            <span class="brand__sub">Jobprep</span>
          </span>
        </RouterLink>

        <nav class="navlinks" aria-label="主导航">
          <template v-for="item in primaryNav" :key="item.key">
            <RouterLink
              v-if="item.to"
              :to="item.to"
              class="navlink"
              :class="{ 'navlink--active': item.key === activeKey }"
            >
              <AppIcon :name="item.icon" />
              <span class="txt">{{ item.label }}</span>
            </RouterLink>
            <span
              v-else
              class="navlink navlink--disabled"
              aria-disabled="true"
              :title="`${item.label}（尚未开放）`"
            >
              <AppIcon :name="item.icon" />
              <span class="txt">{{ item.label }}</span>
              <span class="sr-only">（尚未开放）</span>
            </span>
          </template>
        </nav>

        <div class="topnav__actions">
          <!-- 搜索与通知尚无后端，按 §14 不伪造可用性，明确标注不可用 -->
          <span class="search search--disabled" title="搜索尚未开放">
            <AppIcon name="search" />
            <span class="search__text">搜索能力、证据或岗位</span>
            <span class="sr-only">（尚未开放）</span>
          </span>

          <button
            type="button"
            class="icon-btn"
            disabled
            title="通知尚未开放"
            aria-label="通知（尚未开放）"
          >
            <AppIcon name="bell" :size="18" :stroke-width="1.8" />
          </button>

          <!-- §13 禁止虚构用户头像，因此不渲染假用户，只暴露真实状态 -->
          <span class="account" title="账户体系尚未开放">
            <AppIcon name="user" :size="17" :stroke-width="1.8" />
            <span class="account__text">未登录</span>
            <span class="sr-only">（账户体系尚未开放）</span>
          </span>

          <span class="tag tag--warn" title="当前页面使用演示数据，未连接真实后端">演示数据</span>

          <!-- 移动端导航入口（§3.3）：仅在 <768px 显示，桌面档隐藏 -->
          <button
            ref="menuBtnRef"
            type="button"
            class="icon-btn icon-btn--live menu-btn"
            :aria-expanded="drawerOpen"
            aria-controls="mobile-nav-drawer"
            aria-label="打开主导航"
            @click="drawerOpen = true"
          >
            <AppIcon name="menu" :size="18" :stroke-width="1.9" />
          </button>
        </div>
      </div>
    </header>

    <!-- 移动端主导航抽屉（§3.3：<768px 一级导航链接收起进抽屉） -->
    <div v-if="drawerOpen" class="drawer-layer">
      <div class="drawer__scrim" @click="closeDrawer"></div>

      <div
        id="mobile-nav-drawer"
        ref="drawerRef"
        class="drawer"
        role="dialog"
        aria-modal="true"
        aria-label="主导航"
      >
        <div class="drawer__head">
          <span class="drawer__title">主导航</span>
          <button
            type="button"
            class="icon-btn icon-btn--live"
            aria-label="关闭主导航"
            @click="closeDrawer"
          >
            <AppIcon name="x" :size="18" :stroke-width="1.9" />
          </button>
        </div>

        <nav class="drawer__nav">
          <template v-for="item in primaryNav" :key="item.key">
            <RouterLink
              v-if="item.to"
              :to="item.to"
              class="drawer__link"
              :class="{ 'drawer__link--active': item.key === activeKey }"
            >
              <AppIcon :name="item.icon" :size="18" :stroke-width="1.9" />
              <span class="drawer__link-text">{{ item.label }}</span>
            </RouterLink>
            <span v-else class="drawer__link drawer__link--disabled" aria-disabled="true">
              <AppIcon :name="item.icon" :size="18" :stroke-width="1.9" />
              <span class="drawer__link-text">{{ item.label }}</span>
              <span class="drawer__flag">尚未开放</span>
            </span>
          </template>
        </nav>

        <p class="drawer__note">
          搜索、通知与账户体系尚未开放，因此此处只提供可用的导航入口。
        </p>
      </div>
    </div>

    <!-- 目标建立子流程（§5.4）：非玻璃层，避免第二层大面积模糊 -->
    <div class="flowbar">
      <div class="flowbar__inner">
        <span class="flowbar__label">目标建立</span>
        <nav class="steps" aria-label="目标建立流程">
          <template v-for="(step, index) in steps" :key="step.key">
            <span v-if="index > 0" class="steps__sep" aria-hidden="true"></span>
            <RouterLink
              v-if="step.to && step.enabled"
              :to="step.to"
              class="steps__item"
              :class="{ 'steps__item--active': step.key === activeKey }"
              :aria-current="step.key === activeKey ? 'step' : undefined"
            >
              {{ step.label }}
            </RouterLink>
            <span v-else class="steps__item steps__item--disabled" aria-disabled="true">
              {{ step.label }}
              <span class="sr-only">（需要先创建求职目标）</span>
            </span>
          </template>
        </nav>
      </div>
    </div>

    <main class="main">
      <RouterView />
    </main>

    <footer class="footer">
      <span class="dim">
        证据驱动 · 未知即未知 · 每条结论都可回溯到原文或证据
      </span>
    </footer>
  </div>
</template>

<style scoped>
.app {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* ---------- 顶部横向导航 ---------- */
.topnav {
  position: sticky;
  top: 0;
  z-index: 40;
  height: 70px;
  background-color: var(--color-surface-glass);
  border-bottom: 1px solid var(--color-border-glass);
  box-shadow: var(--shadow-nav);
  /* §4.5：只为随滚动重绘的固定层声明合成层，卡片不逐个声明 */
  will-change: backdrop-filter;
}

@supports (backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px)) {
  .topnav {
    backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
    -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  }
}

.topnav__inner {
  display: flex;
  align-items: center;
  gap: 20px;
  height: 100%;
  max-width: 1460px;
  margin: 0 auto;
  padding: 0 32px;
}

/* ---------- 品牌标记（渐变仅限此处与主按钮，§4.1） ---------- */
.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.brand__mark {
  position: relative;
  width: 34px;
  height: 34px;
  border-radius: 11px;
  background: var(--color-primary-gradient);
  box-shadow: var(--shadow-brand);
}

.brand__mark::after {
  content: '';
  position: absolute;
  inset: 10px;
  border-radius: 4px;
  background-color: var(--color-surface);
}

.brand__text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.brand__text strong {
  font-size: 15.5px;
  font-weight: 800;
  letter-spacing: 0.01em;
}

.brand__sub {
  font-size: 10.5px;
  font-weight: 500;
  color: var(--color-text-subtle);
  letter-spacing: 0.04em;
}

/* ---------- 一级导航 ---------- */
.navlinks {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.navlink {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 38px;
  padding: 0 14px;
  border-radius: var(--radius-pill);
  font-size: 13.5px;
  font-weight: 700;
  color: var(--color-text-muted);
  white-space: nowrap;
  transition:
    background-color var(--dur-fast) var(--ease-out-expo),
    color var(--dur-fast) var(--ease-out-expo);
}

.navlink:not(.navlink--active):hover {
  background-color: var(--color-surface-glass-strong);
  color: var(--color-text);
}

.navlink--active {
  background: var(--color-primary-gradient);
  color: var(--color-text-on-brand);
  box-shadow: var(--shadow-brand);
}

.navlink--disabled {
  color: var(--color-text-subtle);
  cursor: not-allowed;
}

/* ---------- 右侧动作区 ---------- */
.topnav__actions {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-shrink: 0;
  margin-left: auto;
}

.search {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  width: 250px;
  height: 40px;
  padding: 0 16px;
  background-color: var(--color-surface-glass-strong);
  border: 1px solid var(--color-border-glass);
  border-radius: var(--radius-pill);
  color: var(--color-text-subtle);
  font-size: 13px;
}

.search--disabled {
  cursor: not-allowed;
}

.search__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.icon-btn {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  padding: 0;
  color: var(--color-text-subtle);
  background-color: var(--color-surface-glass-strong);
  border: 1px solid var(--color-border-glass);
  border-radius: var(--radius-pill);
  cursor: not-allowed;
  opacity: 0.7;
}

.account {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 40px;
  padding: 0 15px;
  background-color: var(--color-surface-glass-strong);
  border: 1px solid var(--color-border-glass);
  border-radius: var(--radius-pill);
  color: var(--color-text-subtle);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

/* 可点击的图标按钮：与 .icon-btn 的“尚未开放”态在视觉上必须可区分（§2.4） */
.icon-btn--live {
  color: var(--color-text-muted);
  cursor: pointer;
  opacity: 1;
  transition:
    color var(--dur-fast) var(--ease-out-expo),
    background-color var(--dur-fast) var(--ease-out-expo),
    border-color var(--dur-fast) var(--ease-out-expo);
}

.icon-btn--live:hover {
  color: var(--color-text);
  background-color: var(--color-surface);
  border-color: var(--color-border-brand);
}

/* 汉堡入口默认不占位，只在移动档出现 */
.menu-btn {
  display: none;
}

/* ---------- 移动端主导航抽屉（§3.3） ---------- */
.drawer-layer {
  position: fixed;
  inset: 0;
  z-index: 60;
}

.drawer__scrim {
  position: absolute;
  inset: 0;
  background-color: var(--color-scrim);
}

/* 抽屉用不透明表面而非玻璃：避免在内容之上再叠一层大面积模糊（§4.5） */
.drawer {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  width: min(320px, 86vw);
  padding: 16px;
  overflow-y: auto;
  background-color: var(--color-surface);
  border-left: 1px solid var(--color-border);
  box-shadow: var(--shadow-float);
  animation: drawer-in var(--dur-base) var(--ease-out-expo);
}

@keyframes drawer-in {
  from {
    transform: translateX(100%);
  }
  to {
    transform: translateX(0);
  }
}

.drawer__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--color-border-hairline);
}

.drawer__title {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--color-text-muted);
}

.drawer__nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 0;
}

.drawer__link {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 0 14px;
  border-radius: var(--radius-control);
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-muted);
  transition:
    background-color var(--dur-fast) var(--ease-out-expo),
    color var(--dur-fast) var(--ease-out-expo);
}

.drawer__link:not(.drawer__link--disabled):hover {
  background-color: var(--color-surface-subtle);
  color: var(--color-text);
}

.drawer__link--active {
  background-color: var(--color-primary-soft);
  color: var(--color-primary);
}

.drawer__link--disabled {
  color: var(--color-text-subtle);
  cursor: not-allowed;
}

.drawer__link-text {
  flex: 1;
}

.drawer__flag {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-subtle);
}

.drawer__note {
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid var(--color-border-hairline);
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-subtle);
}

/* ---------- 目标建立子流程 ---------- */
.flowbar {
  background-color: var(--color-surface-sunken);
  border-bottom: 1px solid var(--color-border-hairline);
}

.flowbar__inner {
  display: flex;
  align-items: center;
  gap: 16px;
  max-width: 1460px;
  margin: 0 auto;
  padding: 9px 32px;
}

.flowbar__label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--color-text-subtle);
  flex-shrink: 0;
}

.steps {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.steps__item {
  padding: 4px 12px;
  border-radius: var(--radius-pill);
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-text-muted);
  transition:
    color var(--dur-fast) var(--ease-out-expo),
    background-color var(--dur-fast) var(--ease-out-expo);
}

.steps__item:not(.steps__item--disabled):hover {
  color: var(--color-text);
  background-color: var(--color-surface-glass-strong);
}

.steps__item--active {
  color: var(--color-primary);
  background-color: var(--color-primary-soft);
}

.steps__item--disabled {
  color: var(--color-text-subtle);
  cursor: not-allowed;
}

.steps__sep {
  width: 14px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--color-border-strong), transparent);
}

/* ---------- 主体 ---------- */
.main {
  flex: 1;
  width: 100%;
  max-width: 1460px;
  margin: 0 auto;
  padding: 32px 32px 56px;
}

.footer {
  border-top: 1px solid var(--color-border-hairline);
  padding: 18px 32px;
  text-align: center;
  font-size: 11.5px;
}

/* ---------- 收窄：只留图标（§3.2 的 1024~1359px 档） ---------- */
@media (max-width: 1359px) {
  .topnav__inner,
  .flowbar__inner,
  .main,
  .footer {
    padding-left: 24px;
    padding-right: 24px;
  }

  .search {
    width: 190px;
  }
}

@media (max-width: 1099px) {
  .navlink .txt {
    display: none;
  }

  .navlink {
    padding: 0 11px;
  }

  .search {
    width: auto;
  }

  .search__text {
    display: none;
  }

  .account__text {
    display: none;
  }
}

/* ---------- 移动：一级导航收进抽屉（§3.3） ---------- */
@media (max-width: 767px) {
  .topnav {
    height: 62px;
  }

  .menu-btn {
    display: grid;
  }

  .topnav__inner,
  .flowbar__inner,
  .main,
  .footer {
    padding-left: 16px;
    padding-right: 16px;
  }

  .navlinks {
    display: none;
  }

  .search {
    display: none;
  }

  .flowbar__label {
    display: none;
  }

  .main {
    padding-top: 22px;
    padding-bottom: 44px;
  }
}
</style>
