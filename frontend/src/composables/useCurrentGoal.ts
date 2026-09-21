import { computed, reactive } from 'vue'
import { ApiError, api } from '@/api/client'
import type { JobGoal } from '@/api/types'

/**
 * 全局「当前求职目标」状态（19-ui-design-system.md §3.2）。
 *
 * 背景：能力与证据、目标 JD 都必须挂在某个目标下（路由 `/goals/:goalId/...`），
 * 而一级导航在离开目标流程后取不到 `route.params.goalId`。§3.2 要求
 * 「全局目标切换器置于导航顶部或页面标题区」，本模块是该切换器的唯一数据源。
 *
 * 约束：
 *   - 项目未使用 Pinia，且不新增依赖，因此用模块级 `reactive` 单例；
 *   - 目标的名称、状态等内容只来自 `GET /api/v1/goals`，本地存储只记住用户的选择
 *     （一个 id），不缓存目标名称，避免把陈旧数据当成事实（§2.4 状态必须诚实）；
 *   - 读取失败时保留本地选择但 `synced` 保持 false，界面必须显示「未同步」，
 *     不得让一个未经后端确认的目标看起来像事实。
 */

const STORAGE_KEY = 'jobprep.current-goal-id'

interface CurrentGoalState {
  goals: JobGoal[]
  currentGoalId: string | null
  loading: boolean
  /** 是否已成功从后端读取过目标列表。false 时 `currentGoalId` 只是本地记忆。 */
  synced: boolean
  errorMessage: string
}

function readStoredGoalId(): string | null {
  if (typeof window === 'undefined') return null
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    return raw && raw.trim() !== '' ? raw : null
  } catch {
    return null
  }
}

function persistGoalId(id: string | null): void {
  if (typeof window === 'undefined') return
  try {
    if (id) window.localStorage.setItem(STORAGE_KEY, id)
    else window.localStorage.removeItem(STORAGE_KEY)
  } catch {
    // 隐私模式等场景下 localStorage 不可用：选择只在本次会话内有效，不阻断使用
  }
}

const state = reactive<CurrentGoalState>({
  goals: [],
  currentGoalId: readStoredGoalId(),
  loading: false,
  synced: false,
  errorMessage: '',
})

/** 当前目标。后端列表尚未同步时可能为 null，界面不得据此断言「没有目标」。 */
const currentGoal = computed<JobGoal | null>(() => {
  if (state.currentGoalId === null) return null
  return state.goals.find((goal) => goal.id === state.currentGoalId) ?? null
})

/** 可切换的目标：归档目标不再出现在切换器里。 */
const selectableGoals = computed<JobGoal[]>(() =>
  state.goals.filter((goal) => goal.status === 'ACTIVE'),
)

function setCurrentGoalId(id: string | null): void {
  if (state.currentGoalId === id) return
  state.currentGoalId = id
  persistGoalId(id)
}

/** 路由里的 goalId 是用户此刻正在操作的目标，优先级高于本地记忆。 */
function syncGoalFromRoute(routeGoalId: string): void {
  if (!routeGoalId) return
  setCurrentGoalId(routeGoalId)
}

async function loadGoals(): Promise<void> {
  state.loading = true
  state.errorMessage = ''

  try {
    const response = await api.listGoals()
    state.goals = response.data
    state.synced = true

    // 列表是权威：本地记住的目标若已不在列表中（被删除或归档），
    // 必须清空选择，而不是继续指向一个不存在的目标（§2.4）。
    if (
      state.currentGoalId !== null &&
      !response.data.some((goal) => goal.id === state.currentGoalId)
    ) {
      setCurrentGoalId(null)
    }
  } catch (error) {
    state.synced = false
    state.errorMessage = error instanceof ApiError ? error.message : '读取求职目标失败。'
  } finally {
    state.loading = false
  }
}

/**
 * 新建目标后立即登记，省掉一次额外的列表往返。
 * 只接受后端返回的 `JobGoal`，不在前端伪造目标对象（§14）。
 */
function adoptGoal(goal: JobGoal): void {
  if (!state.goals.some((item) => item.id === goal.id)) {
    state.goals = [goal, ...state.goals]
  }
  state.synced = true
  setCurrentGoalId(goal.id)
}

export function useCurrentGoal() {
  return {
    state,
    currentGoal,
    selectableGoals,
    setCurrentGoalId,
    syncGoalFromRoute,
    loadGoals,
    adoptGoal,
  }
}