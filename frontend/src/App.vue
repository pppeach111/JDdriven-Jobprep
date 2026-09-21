<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import BrandMark from '@/components/BrandMark.vue'
import BeautifulStatus from '@/components/BeautifulStatus.vue'
import GoalSwitcher from '@/components/GoalSwitcher.vue'
import type { IconName } from '@/components/icons'
import { useCurrentGoal } from '@/composables/useCurrentGoal'

const route = useRoute()

const { state: goalState, syncGoalFromRoute, loadGoals } = useCurrentGoal()

const routeGoalId = computed(() => String(route.params.goalId ?? ''))

/**
 * 当前生效的 goalId：路由优先（用户此刻正在这个目标里），否则回落到全局「当前目标」。
 *
 * 这一层就是 §3.2「全局目标切换器」的落点：离开目标流程后 `route.params.goalId`
 * 为空，此前一级导航会因此把「能力与证据」渲染成不可用项，对已经建过目标的用户也说错。
 */
const goalId = computed(() => routeGoalId.value || goalState.currentGoalId || '')

/**
 * 一级导航（shared_docs/19-ui-design-system.md §3.1 固定 6 项）。
 *
 * 六项全部可点：一级导航是主闭环入口，用禁用态挡住其中一项，等于把
 * 「尚未满足前提」伪装成「功能不存在」（§2.4 状态必须诚实）。
 * 「能力与证据」必须挂在某个求职目标下，因此没有当前目标时它先落到创建页
 * 并带上 `intent`，由创建页说明「创建完成后即可查看能力与证据」。
 * 其余四项均有独立路由；这些页面尚无后端接口，页内使用明确标识的演示数据。
 */
interface PrimaryNav {
  key: string
  label: string
  icon: IconName
  to: string
  /** 需要用户先满足某个前提时，说明原因与去向。 */
  hint?: string
}

const primaryNav = computed<PrimaryNav[]>(() => [
  { key: 'goal-create', label: '求职目标', icon: 'target', to: '/goals/new' },
  {
    key: 'capability-map',
    label: '能力与证据',
    icon: 'layers',
    to: goalId.value ? `/goals/${goalId.value}/capability` : '/goals/new?intent=capability',
    hint: goalId.value ? undefined : '先创建求职目标，再查看该目标的能力与证据',
  },
  { key: 'jobs', label: '岗位发现', icon: 'briefcase', to: '/discovery' },
  { key: 'assessment', label: '测评与面试', icon: 'clipboard-check', to: '/assessment' },
  { key: 'learning', label: '学习计划', icon: 'book-open', to: '/learning' },
  { key: 'resume', label: '简历建议', icon: 'file-text', to: '/resume' },
])

const activeKey = computed(() => {
  const name = String(route.name ?? '')
  // 无当前目标时，「能力与证据」入口先落到创建页；此时仍让该项保持选中，
  // 否则用户会以为自己点错了入口（§2.4 状态必须诚实）。
  if (name === 'goal-create' && route.query.intent === 'capability') return 'capability-map'
  return name
})

/**
 * 目标建立流程条只属于「目标建立」这条流程（§5.4），
 * 在岗位发现等一级页面上显示会把两个层级混在一起，因此按路由收敛。
 *
 * 能力图谱不带流程条：它是这条流程的产出页，页面标题区已经承担
 * 「目标建立 · 步骤 3 / 3」与返回「目标 JD」的职责；两处同时高亮同一步骤
 * 会让一级导航与子流程混层（§3.1 / §5.4）。
 */
const FLOW_ROUTE_KEYS = ['goal-create', 'jd-import']
const showFlowbar = computed(() => FLOW_ROUTE_KEYS.includes(activeKey.value))

/**
 * 「演示数据」标签只属于尚未接入后端的四个一级页面。
 *
 * 目标建立流程（求职目标 / 目标 JD / 能力图谱）读取的是真实接口，
 * 在这些页面上挂「演示数据」属于错误标注（§2.4 状态必须诚实），
 * 因此顶栏标签按路由收敛，而不是全局常驻。
 */
const DEMO_ROUTE_KEYS = ['jobs', 'assessment', 'learning', 'resume']
const showDemoBadge = computed(() => DEMO_ROUTE_KEYS.includes(activeKey.value))

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

// 路由里的 goalId 是用户此刻正在操作的目标，同步进全局状态，
// 使离开目标流程后一级导航仍指向正确的目标（§3.2）。
watch(routeGoalId, (id) => syncGoalFromRoute(id), { immediate: true })

watch(
  () => route.fullPath,
  () => {
    if (drawerOpen.value) closeDrawer()
  },
)

onMounted(() => {
  desktopMq = window.matchMedia(DESKTOP_MQ)
  desktopMq.addEventListener('change', onViewportChange)
  // 目标列表是顶栏切换器的唯一数据源；读取失败时切换器显示「未同步」而不是假装成功。
  void loadGoals()
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
          <BrandMark :size="19" variant="solid" />
          <span class="brand__text">
            <strong>职径</strong>
            <span class="brand__sub">JOBPREP</span>
          </span>
        </RouterLink>

        <nav class="navlinks" aria-label="主导航">
          <RouterLink
            v-for="item in primaryNav"
            :key="item.key"
            :to="item.to"
            class="navlink"
            :class="{ 'navlink--active': item.key === activeKey }"
            :aria-current="item.key === activeKey ? 'page' : undefined"
            :title="item.hint"
          >
            <AppIcon :name="item.icon" />
            <span class="txt">{{ item.label }}</span>
            <span v-if="item.hint" class="sr-only">（{{ item.hint }}）</span>
          </RouterLink>
        </nav>

        <div class="topnav__actions">
          <!-- §3.2：全局目标切换器置于导航顶部，是一级导航与各模块共用的当前目标来源 -->
          <GoalSwitcher class="topnav__goal" />

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

          <BeautifulStatus
            v-if="showDemoBadge"
            label="演示数据"
            tone="warn"
            title="当前页面使用演示数据，未连接真实后端"
          />

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

        <!-- 移动端顶栏放不下切换器，改在抽屉里提供（§3.2 / §3.3） -->
        <div class="drawer__goal">
          <GoalSwitcher block />
        </div>

        <nav class="drawer__nav">
          <RouterLink
            v-for="item in primaryNav"
            :key="item.key"
            :to="item.to"
            class="drawer__link"
            :class="{ 'drawer__link--active': item.key === activeKey }"
            :aria-current="item.key === activeKey ? 'page' : undefined"
            :title="item.hint"
            @click="closeDrawer"
          >
            <AppIcon :name="item.icon" :size="18" :stroke-width="1.9" />
            <span class="drawer__link-text">{{ item.label }}</span>
            <span v-if="item.hint" class="sr-only">（{{ item.hint }}）</span>
          </RouterLink>
        </nav>

        <p class="drawer__note">
          搜索、通知与账户体系尚未开放，因此此处只提供可用的导航入口。
        </p>
      </div>
    </div>

    <!-- 目标建立子流程（§5.4）：只在这条流程的页面上出现，避免与一级导航混层 -->
    <div v-if="showFlowbar" class="flowbar">
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
  /* §4.5：顶栏使用不透明表面 + 细边界，不再声明 backdrop-filter 合成层 */
  background-color: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-nav);
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

/* ---------- 品牌标记（logo 由 BrandMark.vue 自绘几何路径承载） ---------- */
.brand {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  flex-shrink: 0;
}

.brand__text {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.brand__text strong {
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.brand__sub {
  font-size: 9px;
  font-weight: 700;
  color: var(--color-text-subtle);
  letter-spacing: 0.18em;
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


/* ---------- 右侧动作区 ---------- */
.topnav__actions {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-shrink: 0;
  margin-left: auto;
}

/* 顶栏里的目标切换器不参与压缩，收窄由组件内部的断点处理 */
.topnav__goal {
  flex-shrink: 0;
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

/* 抽屉里的目标切换器与顶栏复用同一组件（§3.2 切换器不得散落到各模块） */
.drawer__goal {
  padding-top: 14px;
}

.drawer__nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 0 14px;
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

.drawer__link:hover {
  background-color: var(--color-surface-subtle);
  color: var(--color-text);
}

.drawer__link--active {
  background-color: var(--color-primary-soft);
  color: var(--color-primary);
}

.drawer__link-text {
  flex: 1;
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

/* Beautiful UI 外壳适配：不透明表面 + hairline 边界（§4.5 默认不透明表面）。
   尺寸一律沿用 §3.2 / §3.3 / §4.4 的规定值，不在此处另立一套紧凑刻度。 */
.topnav {
  background: var(--color-surface);
  border-bottom-color: var(--color-border);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

/* 品牌副标题：产品名视觉改造；字号沿用原值，仅提高字重与字距 */
.brand__sub {
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.14em;
}


.navlink--active {
  color: var(--color-primary);
  background: var(--color-primary-soft);
  box-shadow: inset 0 0 0 1px var(--color-border-brand);
}

.search,
.account,
.icon-btn {
  background: var(--color-surface);
  border-color: var(--color-border);
}

/* §4.4：图标按钮可视尺寸沿用 40px，点击区外扩 2px 补足 44×44px */
.icon-btn {
  position: relative;
}

.icon-btn::after {
  content: '';
  position: absolute;
  inset: -2px;
}

/* §1：不使用装饰性渐变，步骤分隔线用纯色 */
.steps__sep { background: var(--color-border-strong); }


</style>
