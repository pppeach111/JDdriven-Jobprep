<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import { useCurrentGoal } from '@/composables/useCurrentGoal'

/**
 * 全局目标切换器（19-ui-design-system.md §3.2：置于导航顶部，不能散落在各模块内）。
 *
 * 顶栏（桌面）与移动端主导航抽屉复用同一组件，数据源同为 `useCurrentGoal`，
 * 目标列表只来自 `GET /api/v1/goals`。
 *
 * 诚实性约束：
 *   - 后端列表未成功读取时不编造目标名，只显示本地记住的选择并标注「未同步」（§2.4）；
 *   - 在目标流程页切换目标时同步改写地址栏的 goalId，避免顶栏与 URL 互相矛盾（§2.4）。
 */
defineProps<{ block?: boolean }>()

const route = useRoute()
const router = useRouter()
const { state, currentGoal, selectableGoals, setCurrentGoalId } = useCurrentGoal()

/** 首项文案要说清「为什么这里是空的」，而不是留一个无意义的空白项。 */
const placeholder = computed(() => {
  if (state.loading && !state.synced) return '正在读取目标…'
  if (state.errorMessage) return '目标列表读取失败'
  if (state.synced && selectableGoals.value.length === 0) return '尚未创建目标'
  return '未选择目标'
})

/** 本地记住了目标但后端尚未确认时必须显式标注，不能让它看起来像事实。 */
const unsynced = computed(() => state.currentGoalId !== null && !state.synced)

const busy = computed(() => state.loading && !state.synced)

function onChange(event: Event) {
  const next = (event.target as HTMLSelectElement).value
  setCurrentGoalId(next === '' ? null : next)

  // 不在目标流程页时只需换全局状态，让一级导航跟上即可。
  const routeGoalId = String(route.params.goalId ?? '')
  if (!routeGoalId) return

  // 在目标流程页时地址栏与顶栏必须一致，否则刷新后会回到旧目标（§2.4）。
  if (next === '') {
    void router.push('/goals/new')
    return
  }
  const suffix = route.name === 'jd-import' ? 'jd' : 'capability'
  void router.push(`/goals/${next}/${suffix}`)
}
</script>

<template>
  <div class="goal-switch" :class="{ 'goal-switch--block': block }">
    <AppIcon name="target" :size="16" :stroke-width="1.9" />
    <select
      class="goal-switch__select"
      aria-label="当前求职目标"
      :value="state.currentGoalId ?? ''"
      :disabled="busy && selectableGoals.length === 0"
      :title="
        unsynced
          ? '本地记住了上次选择的目标，但尚未从后端确认它仍然存在'
          : '切换当前求职目标'
      "
      @change="onChange"
    >
      <option v-if="state.currentGoalId && !currentGoal" :value="state.currentGoalId">
        已选目标（未同步）
      </option>
      <option value="">{{ placeholder }}</option>
      <option v-for="goal in selectableGoals" :key="goal.id" :value="goal.id">
        {{ goal.name }}
      </option>
    </select>
    <span class="goal-switch__caret" aria-hidden="true">
      <AppIcon name="chevron-right" :size="14" :stroke-width="2" />
    </span>
    <span v-if="unsynced" class="goal-switch__flag">未同步</span>
  </div>
</template>

<style scoped>
.goal-switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  max-width: 224px;
  padding: 0 12px;
  background-color: var(--color-surface);
  border: 1px solid var(--color-border-brand);
  border-radius: var(--radius-pill);
  color: var(--color-text-muted);
}

.goal-switch__select {
  flex: 0 1 auto;
  min-width: 0;
  max-width: 150px;
  border: 0;
  background: transparent;
  color: var(--color-text);
  font-family: inherit;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  cursor: pointer;
}

.goal-switch__select:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
  border-radius: var(--radius-control);
}

.goal-switch__select:disabled {
  cursor: progress;
}

.goal-switch__caret {
  display: inline-grid;
  flex-shrink: 0;
  place-items: center;
  transform: rotate(90deg);
  color: var(--color-text-subtle);
}

/* 「未同步」是中性事实而不是错误，因此用中性标记，不借用警告色（§4.1） */
.goal-switch__flag {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  background-color: var(--color-neutral-bg);
  color: var(--color-neutral);
  font-size: 11px;
  font-weight: 700;
}

/* 抽屉变体：撑满可用宽度 */
.goal-switch--block {
  display: flex;
  width: 100%;
  max-width: none;
}

.goal-switch--block .goal-switch__select {
  flex: 1;
  max-width: none;
}

/* §3.2 的 1024~1359px 档：横向空间收窄，切换器随之收窄 */
@media (max-width: 1359px) {
  .goal-switch:not(.goal-switch--block) {
    max-width: 186px;
  }

  .goal-switch:not(.goal-switch--block) .goal-switch__select {
    max-width: 124px;
  }
}

@media (max-width: 1099px) {
  .goal-switch:not(.goal-switch--block) {
    max-width: 160px;
  }

  .goal-switch:not(.goal-switch--block) .goal-switch__select {
    max-width: 98px;
  }
}

/* §3.3 <768px：顶栏放不下，切换器只保留抽屉里的 block 变体 */
@media (max-width: 767px) {
  .goal-switch:not(.goal-switch--block) {
    display: none;
  }
}
</style>