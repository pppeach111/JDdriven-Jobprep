<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ApiError, api } from '@/api/client'
import ConfidenceBadge from '@/components/ConfidenceBadge.vue'
import LevelCompare from '@/components/LevelCompare.vue'
import type { CapabilityMap, GapType, SkillCard } from '@/api/types'

const props = defineProps<{ goalId: string }>()

const loading = ref(true)
const errorMessage = ref('')
const errorTraceId = ref('')
const map = ref<CapabilityMap | null>(null)
const filter = ref<'ALL' | 'GAP' | 'NO_EVIDENCE'>('ALL')

const GAP_LABELS: Record<GapType, string> = {
  QUALIFICATION_BLOCK: '资格不符',
  KNOWLEDGE_GAP: '知识差距',
  APPLICATION_GAP: '应用差距',
  PRACTICE_GAP: '实践差距',
  EVIDENCE_GAP: '缺少证据',
  EXPRESSION_GAP: '表达差距',
  CONFLICT: '证据冲突',
}

const DIMENSION_LABELS: Record<string, string> = {
  concept: '概念理解',
  codeReading: '代码阅读',
  diagnosis: '问题诊断',
  design: '方案设计',
  expression: '表达沟通',
  practice: '工程实践',
}

const FILTERS = [
  { key: 'ALL', label: '全部' },
  { key: 'GAP', label: '存在差距' },
  { key: 'NO_EVIDENCE', label: '缺少证据' },
] as const

const skills = computed<SkillCard[]>(() => map.value?.skills ?? [])

const visibleSkills = computed(() => {
  if (filter.value === 'GAP') {
    return skills.value.filter((s) => s.gapType !== null)
  }
  if (filter.value === 'NO_EVIDENCE') {
    return skills.value.filter((s) => s.gapType === 'EVIDENCE_GAP')
  }
  return skills.value
})

const summary = computed(() => {
  const total = skills.value.length
  const withGap = skills.value.filter((s) => s.gapType !== null).length
  const noEvidence = skills.value.filter((s) => s.gapType === 'EVIDENCE_GAP').length
  return { total, withGap, noEvidence }
})

function gapTagClass(gapType: GapType | null): string {
  switch (gapType) {
    case 'EVIDENCE_GAP':
      return 'tag tag--unknown'
    case 'KNOWLEDGE_GAP':
    case 'QUALIFICATION_BLOCK':
    case 'CONFLICT':
      return 'tag tag--bad'
    case 'APPLICATION_GAP':
    case 'PRACTICE_GAP':
    case 'EXPRESSION_GAP':
      return 'tag tag--warn'
    default:
      return 'tag tag--good'
  }
}

function gapLabel(gapType: GapType | null): string {
  if (gapType === null) return '无明显差距'
  return GAP_LABELS[gapType] ?? gapType
}

function dimensionEntries(skill: SkillCard): Array<{ key: string; label: string; value: number }> {
  if (!skill.dimensions) return []
  return Object.entries(skill.dimensions).map(([key, value]) => ({
    key,
    label: DIMENSION_LABELS[key] ?? key,
    value,
  }))
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  errorTraceId.value = ''

  try {
    const response = await api.getCapabilityMap(props.goalId)
    map.value = response.data
  } catch (error) {
    map.value = null
    if (error instanceof ApiError) {
      errorMessage.value = error.message
      errorTraceId.value = error.traceId
    } else {
      errorMessage.value = '加载能力图谱失败。'
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => props.goalId, load)
</script>

<template>
  <div class="page">
    <section class="hero">
      <span class="eyebrow">Step 03 / 能力图谱</span>
      <h1>差距不是一句话，是等级与置信度</h1>
      <p class="hero__lead muted">
        每项能力同时给出<strong>岗位要求</strong>、<strong>当前估计</strong>与
        <strong>置信度</strong>。没有证据的能力保持未知，不折算成 0 分。
      </p>
    </section>

    <div v-if="loading" class="grid">
      <div v-for="n in 6" :key="n" class="skeleton card-skeleton"></div>
    </div>

    <p v-else-if="errorMessage" class="alert" role="alert">
      <span class="alert__text">{{ errorMessage }}</span>
      <span v-if="errorTraceId" class="alert__trace mono">traceId: {{ errorTraceId }}</span>
      <button class="btn btn--ghost btn--sm" type="button" @click="load">重试</button>
    </p>

    <template v-else>
      <div class="toolbar">
        <div class="stats">
          <span class="stat">
            <span class="stat__num mono">{{ summary.total }}</span>
            <span class="stat__label dim">项能力</span>
          </span>
          <span class="stat">
            <span class="stat__num mono">{{ summary.withGap }}</span>
            <span class="stat__label dim">项存在差距</span>
          </span>
          <span class="stat">
            <span class="stat__num mono">{{ summary.noEvidence }}</span>
            <span class="stat__label dim">项缺少证据</span>
          </span>
        </div>

        <div class="filters" role="group" aria-label="筛选">
          <button
            v-for="item in FILTERS"
            :key="item.key"
            class="filter"
            :class="{ 'filter--active': filter === item.key }"
            type="button"
            @click="filter = item.key"
          >
            {{ item.label }}
          </button>
        </div>
      </div>

      <p v-if="visibleSkills.length === 0" class="empty dim">
        当前筛选下没有能力项。请先在目标 JD 中完成解析。
      </p>

      <div v-else class="grid">
        <article v-for="skill in visibleSkills" :key="skill.skillId" class="surface skill">
          <header class="skill__head">
            <h2 class="skill__name">{{ skill.name }}</h2>
            <span :class="gapTagClass(skill.gapType)">{{ gapLabel(skill.gapType) }}</span>
          </header>

          <p class="skill__id mono dim">{{ skill.skillId }}</p>

          <LevelCompare :required="skill.requiredLevel" :estimated="skill.estimatedLevel" />

          <div class="skill__conf">
            <span class="field__label">置信度</span>
            <ConfidenceBadge :value="skill.confidence" />
          </div>

          <p v-if="skill.estimatedLevel === null" class="skill__note dim">
            尚未采集到可验证证据，因此等级保持未知。
          </p>

          <div v-if="dimensionEntries(skill).length > 0" class="dims">
            <span
              v-for="dim in dimensionEntries(skill)"
              :key="dim.key"
              class="dim-item"
              :title="`${dim.label}：${dim.value}`"
            >
              <span class="dim-item__label">{{ dim.label }}</span>
              <span class="dim-item__value mono">{{ dim.value }}</span>
            </span>
          </div>

          <footer v-if="skill.nextAction" class="skill__next">
            <span class="tag tag--accent">{{ skill.nextAction.type }}</span>
            <p class="skill__next-text">{{ skill.nextAction.reason }}</p>
          </footer>
        </article>
      </div>

      <div class="actions">
        <RouterLink class="btn btn--ghost" :to="`/goals/${goalId}/jd`">
          重新解析 JD
        </RouterLink>
        <RouterLink class="btn btn--ghost" to="/goals/new">新建目标</RouterLink>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 26px;
}

.hero {
  display: flex;
  flex-direction: column;
  gap: 9px;
  max-width: 720px;
}

.hero__lead {
  font-size: 14.5px;
}

.hero__lead strong {
  color: var(--color-text);
  font-weight: 600;
}

/* ---------- 工具栏 ---------- */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
}

.stats {
  display: flex;
  gap: 22px;
}

.stat {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
}

.stat__num {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.stat__label {
  font-size: 11.5px;
}

.filters {
  display: flex;
  gap: 4px;
  padding: 3px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background-color: var(--color-surface-glass-strong);
}

.filter {
  padding: 5px 13px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--color-text-muted);
  font: inherit;
  font-size: 12.5px;
  cursor: pointer;
  transition:
    color var(--dur-fast) var(--ease-out-expo),
    background-color var(--dur-fast) var(--ease-out-expo);
}

.filter:hover {
  color: var(--color-text);
}

.filter--active {
  color: var(--color-text);
  background-color: var(--color-primary-soft);
  box-shadow: inset 0 0 0 1px var(--color-border-brand);
}

/* ---------- 技能网格 ---------- */
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 18px;
}

.skill {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px;
  transition:
    border-color var(--dur-base) var(--ease-out-expo),
    transform var(--dur-base) var(--ease-out-expo);
}

.skill:hover {
  transform: translateY(-2px);
  border-color: var(--color-border-strong);
}

.skill__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.skill__name {
  font-size: 15.5px;
}

.skill__id {
  font-size: 10.5px;
  margin-top: -8px;
}

.skill__conf {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
}

.skill__note {
  font-size: 11.5px;
  line-height: 1.55;
}

.dims {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.dim-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background-color: var(--color-surface-glass-strong);
  font-size: 10.5px;
}

.dim-item__label {
  color: var(--color-text-subtle);
}

.dim-item__value {
  color: var(--color-text);
  font-weight: 600;
}

.skill__next {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
}

.skill__next-text {
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.6;
}

.card-skeleton {
  height: 236px;
}

.empty {
  font-size: 13px;
  padding: 34px 0;
  text-align: center;
}

.actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding-top: 8px;
}

.alert {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 13px 15px;
  border: 1px solid var(--color-danger);
  border-radius: var(--radius-control);
  background-color: var(--color-danger-bg);
  font-size: 13px;
}

.alert__text {
  color: var(--color-danger);
}

.alert__trace {
  font-size: 11px;
  color: var(--color-text-subtle);
  word-break: break-all;
}

.btn--sm {
  padding: 5px 11px;
  font-size: 12px;
}
</style>