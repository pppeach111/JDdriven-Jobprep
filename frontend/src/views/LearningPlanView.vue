<script setup lang="ts">
import { computed } from 'vue'
import BeautifulButton from '@/components/BeautifulButton.vue'
import BeautifulContextCard from '@/components/BeautifulContextCard.vue'
import BeautifulPageHeader from '@/components/BeautifulPageHeader.vue'
import BeautifulStatus from '@/components/BeautifulStatus.vue'
import { DEMO_GOAL, DEMO_LEARNING_WEEK, type DemoLearningTask } from '@/data/demo'
import { formatDuration, formatRatio } from '@/utils/format'

/**
 * 学习计划（19-ui-design-system.md §7.6）。
 *
 * 布局决定：
 *   - 以「本周任务 + 时间预算」为主，不做自由拖拽看板（§7.6 第一条）；
 *   - 每项任务说明对应差距、预计时长、产物、验收标准与复测入口；
 *   - 依赖未满足时直接说明前置任务，不给出可开始的假动作；
 *   - 计划调整只展示原因，不静默移动用户已安排的内容。
 *
 * 视觉取舍：任务使用浅色背景区而不是白卡 + 阴影（§5.2 分组优先级），
 * 避免「每一段内容都放进一张白卡」的仪表盘观感。
 */
const week = DEMO_LEARNING_WEEK

/** 时间预算：分子分母都给，不做无法核算的百分比（§9）。 */
const budget = computed(() => {
  const budgetMinutes = week.budgetHours * 60
  const remaining = Math.max(0, budgetMinutes - week.plannedMinutes)
  return {
    budgetMinutes,
    remaining,
    percent: budgetMinutes > 0 ? Math.round((week.plannedMinutes / budgetMinutes) * 100) : 0,
  }
})

/** 未满足依赖的任务 → 其前置任务标题。 */
const blockedBy = computed<Record<string, string>>(() => {
  const result: Record<string, string> = {}
  for (const task of week.tasks) {
    if (!task.dependsOn) continue
    const dependency = week.tasks.find((item) => item.id === task.dependsOn)
    if (dependency && !dependency.done) result[task.id] = dependency.title
  }
  return result
})

function isBlocked(task: DemoLearningTask): boolean {
  return Boolean(blockedBy.value[task.id])
}

function taskStateLabel(task: DemoLearningTask): string {
  if (task.done) return '已完成'
  if (isBlocked(task)) return '依赖未满足'
  return '待开始'
}

function taskStateTone(task: DemoLearningTask): 'neutral' | 'accent' | 'good' | 'warn' {
  if (task.done) return 'good'
  if (isBlocked(task)) return 'warn'
  return 'accent'
}

const doneCount = computed(() => week.tasks.filter((task) => task.done).length)
</script>

<template>
  <div class="page">
    <BeautifulPageHeader
      title="学习计划"
      description="本次计划以本周任务和时间预算为主。每项任务都对应一个具体差距，并写明产物、验收标准和复测入口。"
    >
      <template #meta>
        <BeautifulStatus
          label="演示数据"
          tone="warn"
          title="本页使用固定演示数据，未连接真实后端"
        />
        <span>{{ DEMO_GOAL.name }}</span>
        <span aria-hidden="true">·</span>
        <span class="mono">{{ week.weekLabel }}</span>
      </template>

      <template #actions>
        <BeautifulButton
          variant="primary"
          type="button"
          disabled
          title="计划生成接口尚未实现，接入后此按钮才会生效"
        >
          重新生成本周计划
        </BeautifulButton>
      </template>
    </BeautifulPageHeader>

    <!-- 时间预算 -->
    <section class="budget">
      <div class="budget__head">
        <h2 class="budget__title">本周时间预算</h2>
        <span class="budget__nums mono">
          {{ formatRatio(week.plannedMinutes, budget.budgetMinutes, '分钟') }}
        </span>
      </div>
      <div class="bui-meter">
        <span class="bui-meter__track">
          <span class="bui-meter__fill" :style="{ width: `${budget.percent}%` }"></span>
        </span>
        <span class="bui-meter__value">{{ budget.percent }}%</span>
      </div>
      <p class="dim budget__note">
        登记的可投入时间 {{ week.budgetHours }} 小时，已安排
        {{ formatDuration(week.plannedMinutes) }}，剩余 {{ formatDuration(budget.remaining) }}。
        预算来自你在求职目标中登记的每周可投入小时数。
      </p>
    </section>

    <!-- 计划调整说明：只解释原因，不静默改日程 -->
    <BeautifulContextCard
      eyebrow="本次计划调整"
      :text="week.adjustedNote"
      tone="neutral"
    />

    <!-- 本周任务 -->
    <section class="bui-section">
      <div class="bui-section__head">
        <h3 class="bui-section__title">本周任务</h3>
        <span class="bui-section__note">
          共 {{ week.tasks.length }} 项 · 已完成 {{ doneCount }} 项
        </span>
      </div>

      <div class="tasks">
        <article
          v-for="task in week.tasks"
          :key="task.id"
          class="task"
          :class="{ 'task--done': task.done, 'task--blocked': isBlocked(task) }"
        >
          <header class="task__head">
            <h4 class="task__title">{{ task.title }}</h4>
            <BeautifulStatus :label="taskStateLabel(task)" :tone="taskStateTone(task)" />
          </header>

          <div class="task__flags">
            <span class="bui-chip">对应差距：{{ task.gapLabel }}</span>
            <span class="bui-chip">预计 {{ formatDuration(task.estimatedMinutes) }}</span>
          </div>

          <div class="task__block">
            <span class="task__key">产物</span>
            <p class="task__value">{{ task.artifact }}</p>
          </div>

          <div class="task__block">
            <span class="task__key">验收标准</span>
            <ul class="task__list">
              <li v-for="item in task.acceptance" :key="item">{{ item }}</li>
            </ul>
          </div>

          <div class="task__block">
            <span class="task__key">复测入口</span>
            <p class="task__value">{{ task.retest }}</p>
          </div>

          <BeautifulContextCard
            v-if="isBlocked(task)"
            eyebrow="依赖未满足"
            :text="`前置任务「${blockedBy[task.id]}」尚未完成，因此本任务暂不可开始。完成前置任务后这里会变为可开始。`"
            tone="warn"
          />

          <div class="task__actions">
            <BeautifulButton
              variant="secondary"
              type="button"
              :disabled="isBlocked(task) || task.done"
              :title="
                task.done
                  ? '任务已完成'
                  : isBlocked(task)
                    ? '前置任务未完成'
                    : '任务状态接口尚未实现，接入后此按钮才会生效'
              "
            >
              {{ task.done ? '已完成' : '开始任务' }}
            </BeautifulButton>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

/* ---------- 时间预算 ---------- */
.budget {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 720px;
}

.budget__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.budget__title {
  font-size: 13.5px;
  font-weight: 700;
}

.budget__nums {
  font-size: 12.5px;
  color: var(--color-text-muted);
}

.budget__note {
  font-size: 11.5px;
  line-height: 1.7;
}

/* ---------- 任务：浅色背景区，不做白卡 + 阴影 ---------- */
.tasks {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
  border: 1px solid transparent;
  border-radius: var(--radius-card);
  background: var(--color-surface-subtle);
}

.task--done {
  background: var(--color-success-bg);
}

.task--blocked {
  background: var(--color-warning-bg);
}

.task__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.task__title {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.55;
}

.task__flags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.task__block {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.task__key {
  color: var(--color-text-subtle);
  font-size: 10.5px;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.task__value {
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--color-text);
}

.task__list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 0;
  padding-left: 17px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--color-text);
}

.task__actions {
  display: flex;
  justify-content: flex-end;
}
</style>