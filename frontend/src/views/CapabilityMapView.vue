<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ApiError, api } from '@/api/client'
import AppIcon from '@/components/AppIcon.vue'
import BeautifulButton from '@/components/BeautifulButton.vue'
import BeautifulContextCard from '@/components/BeautifulContextCard.vue'
import BeautifulEmptyState from '@/components/BeautifulEmptyState.vue'
import BeautifulLoadingState from '@/components/BeautifulLoadingState.vue'
import BeautifulPageHeader from '@/components/BeautifulPageHeader.vue'
import BeautifulStatus from '@/components/BeautifulStatus.vue'
import ConfidenceBadge from '@/components/ConfidenceBadge.vue'
import LevelCompare from '@/components/LevelCompare.vue'
import type { CapabilityMap, GapType, SkillCard } from '@/api/types'
import { formatDateTime, formatLevel } from '@/utils/format'

/**
 * 能力与证据（19-ui-design-system.md §7.3）。
 *
 * 2026-09-21 调整：
 *   - 去掉横幅式大标题，改用 §5.1 固定结构的页面标题区；
 *   - 主视图从「技能卡片网格」改为**可排序能力矩阵**，
 *     列固定为：能力 / 目标等级 / 当前等级 / 置信度 / 差距 / 证据数 / 下一动作（§7.3）；
 *   - 选中一行后在矩阵下方展示等级锚点、证据引用与冲突说明（§7.3 第二条）。
 *
 * 诚实性约束：
 *   - 契约要求等级与置信度同时呈现，因此两列不可省略，缺失显示「未知」而不是 0（§6.1）；
 *   - 排序时 null 恒排在末尾，避免把「未知」当成最小值排到最前面；
 *   - 后端尚未返回历史变化与等级锚点原文，页面显式说明缺失，不画占位图表（§14）。
 */
const props = defineProps<{ goalId: string }>()

const loading = ref(true)
const errorMessage = ref('')
const errorTraceId = ref('')
const map = ref<CapabilityMap | null>(null)

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

/** 差距的处置优先级：数字越大越需要先处理。null（无明显差距）排最后。 */
const GAP_SEVERITY: Record<GapType, number> = {
  QUALIFICATION_BLOCK: 7,
  CONFLICT: 6,
  KNOWLEDGE_GAP: 5,
  APPLICATION_GAP: 4,
  PRACTICE_GAP: 3,
  EXPRESSION_GAP: 2,
  EVIDENCE_GAP: 1,
}

const FILTERS = [
  { key: 'ALL', label: '全部' },
  { key: 'GAP', label: '存在差距' },
  { key: 'NO_EVIDENCE', label: '缺少证据' },
] as const

type FilterKey = (typeof FILTERS)[number]['key']
type SortKey =
  | 'name'
  | 'requiredLevel'
  | 'estimatedLevel'
  | 'confidence'
  | 'gap'
  | 'evidence'
  | 'nextAction'

const COLUMNS: Array<{ key: SortKey; label: string; numeric: boolean }> = [
  { key: 'name', label: '能力', numeric: false },
  { key: 'requiredLevel', label: '目标等级', numeric: true },
  { key: 'estimatedLevel', label: '当前等级', numeric: true },
  { key: 'confidence', label: '置信度', numeric: true },
  { key: 'gap', label: '差距', numeric: false },
  { key: 'evidence', label: '证据数', numeric: true },
  { key: 'nextAction', label: '下一动作', numeric: false },
]

const filter = ref<FilterKey>('ALL')
const sortKey = ref<SortKey>('gap')
const sortDir = ref<'asc' | 'desc'>('desc')
const selectedSkillId = ref('')

const skills = computed<SkillCard[]>(() => map.value?.skills ?? [])

const visibleSkills = computed(() => {
  if (filter.value === 'GAP') return skills.value.filter((skill) => skill.gapType !== null)
  if (filter.value === 'NO_EVIDENCE') {
    return skills.value.filter((skill) => skill.gapType === 'EVIDENCE_GAP')
  }
  return skills.value
})

const summary = computed(() => ({
  total: skills.value.length,
  withGap: skills.value.filter((skill) => skill.gapType !== null).length,
  noEvidence: skills.value.filter((skill) => skill.gapType === 'EVIDENCE_GAP').length,
}))

function sortValue(skill: SkillCard, key: SortKey): number | string | null {
  switch (key) {
    case 'name':
      return skill.name
    case 'requiredLevel':
      return skill.requiredLevel
    case 'estimatedLevel':
      return skill.estimatedLevel
    case 'confidence':
      return skill.confidence
    case 'gap':
      return skill.gapType ? GAP_SEVERITY[skill.gapType] : null
    case 'evidence':
      return skill.evidenceIds.length
    case 'nextAction':
      return skill.nextAction?.type ?? null
  }
}

const sortedSkills = computed<SkillCard[]>(() => {
  const direction = sortDir.value === 'asc' ? 1 : -1
  return [...visibleSkills.value].sort((a, b) => {
    const left = sortValue(a, sortKey.value)
    const right = sortValue(b, sortKey.value)

    // 未知不参与比较：无论升序还是降序，「未知」都排在最后（§6.1）
    if (left === null && right === null) return a.name.localeCompare(b.name, 'zh-Hans-CN')
    if (left === null) return 1
    if (right === null) return -1

    if (typeof left === 'string' && typeof right === 'string') {
      return left.localeCompare(right, 'zh-Hans-CN') * direction
    }
    return (Number(left) - Number(right)) * direction
  })
})

/**
 * 分项差距总览的阅读顺序：先按差距处置优先级降序，同级按能力名。
 * 与表格的排序状态解耦——总览的顺序不应被用户临时的列排序打乱（§401）。
 */
const gapRankedSkills = computed<SkillCard[]>(() => {
  return [...skills.value].sort((a, b) => {
    const left = a.gapType ? GAP_SEVERITY[a.gapType] : null
    const right = b.gapType ? GAP_SEVERITY[b.gapType] : null

    // 未知不参与比较：无明显差距的排在最后，与表格排序沿用同一套 null 语义（§6.1）
    if (left === null && right === null) return a.name.localeCompare(b.name, 'zh-Hans-CN')
    if (left === null) return 1
    if (right === null) return -1
    if (right !== left) return right - left
    return a.name.localeCompare(b.name, 'zh-Hans-CN')
  })
})

/** 总览只回答「最该先处理哪几项」，完整清单留给下方矩阵（§401）。 */
const COMPARE_LIMIT = 8

const compareSkills = computed(() => gapRankedSkills.value.slice(0, COMPARE_LIMIT))
const compareRestCount = computed(() => Math.max(0, gapRankedSkills.value.length - COMPARE_LIMIT))

const selectedSkill = computed<SkillCard | null>(() => {
  const byId = sortedSkills.value.find((skill) => skill.skillId === selectedSkillId.value)
  return byId ?? sortedSkills.value[0] ?? null
})

function toggleSort(key: SortKey) {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
    return
  }
  sortKey.value = key
  sortDir.value = key === 'name' ? 'asc' : 'desc'
}

function sortMarkClass(key: SortKey): string {
  if (sortKey.value !== key) return ''
  return sortDir.value === 'asc' ? 'bui-sort__mark--asc' : 'bui-sort__mark--desc'
}

function ariaSort(key: SortKey): 'ascending' | 'descending' | 'none' {
  if (sortKey.value !== key) return 'none'
  return sortDir.value === 'asc' ? 'ascending' : 'descending'
}

function gapTone(gapType: GapType | null): 'neutral' | 'accent' | 'good' | 'warn' | 'bad' {
  switch (gapType) {
    case 'EVIDENCE_GAP':
      return 'neutral'
    case 'KNOWLEDGE_GAP':
    case 'QUALIFICATION_BLOCK':
    case 'CONFLICT':
      return 'bad'
    case 'APPLICATION_GAP':
    case 'PRACTICE_GAP':
    case 'EXPRESSION_GAP':
      return 'warn'
    default:
      return 'good'
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

/** §8 过期：距最近更新时间超过 7 天即提示重新估计。 */
const STALE_DAYS = 7

function isStale(skill: SkillCard): boolean {
  if (!skill.updatedAt) return false
  const elapsed = Date.now() - new Date(skill.updatedAt).getTime()
  return elapsed > STALE_DAYS * 24 * 60 * 60 * 1000
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  errorTraceId.value = ''

  try {
    const response = await api.getCapabilityMap(props.goalId)
    map.value = response.data
    selectedSkillId.value = ''
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
    <BeautifulPageHeader
      context="目标建立 · 步骤 3 / 3"
      title="能力与证据"
      :back-to="`/goals/${goalId}/jd`"
      back-label="回到目标 JD"
      description="每项能力同时给出岗位要求、当前估计与置信度。没有证据的能力保持未知，不折算成 0 分。"
    >
      <template #meta>
        <template v-if="!loading && !errorMessage">
          <span class="mono">{{ summary.total }} 项能力</span>
          <span aria-hidden="true">·</span>
          <span class="mono">{{ summary.withGap }} 项存在差距</span>
          <span aria-hidden="true">·</span>
          <span class="mono">{{ summary.noEvidence }} 项缺少证据</span>
        </template>
        <span v-else>数据来自 /api/v1/goals/&lt;goalId&gt;/capability-map</span>
      </template>

      <template #actions>
        <BeautifulButton variant="secondary" :to="`/goals/${goalId}/jd`">重新解析 JD</BeautifulButton>
        <BeautifulButton variant="primary" to="/goals/new">新建目标</BeautifulButton>
      </template>
    </BeautifulPageHeader>

    <div v-if="loading" class="loading-panel surface">
      <BeautifulLoadingState label="正在读取能力与证据" variant="Dots" />
    </div>

    <p v-else-if="errorMessage" class="alert" role="alert">
      <span class="alert__text">{{ errorMessage }}</span>
      <span v-if="errorTraceId" class="alert__trace mono">traceId: {{ errorTraceId }}</span>
      <BeautifulButton variant="secondary" type="button" @click="load">重试</BeautifulButton>
    </p>

    <BeautifulEmptyState
      v-else-if="skills.length === 0"
      title="这个目标还没有能力项"
      reason="能力矩阵由目标 JD 解析结果生成，当前目标尚未导入可解析的 JD，因此没有任何能力项，而不是所有能力都得 0 分。"
      required-input="一份目标岗位 JD 原文"
    >
      <template #action>
        <BeautifulButton variant="primary" :to="`/goals/${goalId}/jd`">去导入目标 JD</BeautifulButton>
      </template>
    </BeautifulEmptyState>

    <template v-else>
      <div class="bui-toolbar">
        <div class="bui-filters" role="group" aria-label="筛选能力项">
          <button
            v-for="item in FILTERS"
            :key="item.key"
            type="button"
            class="filter"
            :class="{ 'filter--active': filter === item.key }"
            :aria-pressed="filter === item.key"
            @click="filter = item.key"
          >
            {{ item.label }}
            <span class="filter__count mono">
              {{
                item.key === 'ALL'
                  ? summary.total
                  : item.key === 'GAP'
                    ? summary.withGap
                    : summary.noEvidence
              }}
            </span>
          </button>
        </div>
        <p class="dim meta-line">共 {{ sortedSkills.length }} 行 · 点击任意行查看证据与维度</p>
      </div>

      <BeautifulEmptyState
        v-if="sortedSkills.length === 0"
        title="当前筛选下没有能力项"
        reason="该筛选条件没有匹配到任何能力项。请切换筛选条件，或先完成目标 JD 的解析。"
        required-input="至少一份已解析的目标 JD"
      />

      <template v-else>
        <!-- 分项差距总览（§4.6 / §401：匹配构成用分项条形图，不用单一总分或环形分数） -->
        <section class="surface compare">
          <header class="compare__head">
            <h2 class="compare__title">分项差距总览</h2>
            <p class="compare__note">
              按差距处置优先级排序，逐项对比「要求 / 估计」并标注置信度。
              不折算成单一总分或匹配度百分比：没有证据的能力保持未知，不参与排序。
            </p>
          </header>

          <ul class="compare__list">
            <li v-for="skill in compareSkills" :key="skill.skillId" class="compare__item">
              <div class="compare__label">
                <span class="compare__name">{{ skill.name }}</span>
                <span class="compare__tags">
                  <BeautifulStatus :label="gapLabel(skill.gapType)" :tone="gapTone(skill.gapType)" />
                  <ConfidenceBadge :value="skill.confidence" compact />
                </span>
              </div>
              <LevelCompare :required="skill.requiredLevel" :estimated="skill.estimatedLevel" />
            </li>
          </ul>

          <p v-if="compareRestCount > 0" class="compare__more dim">
            另有 {{ compareRestCount }} 项未在此展示，完整清单见下方能力矩阵。
          </p>
        </section>

        <div class="bui-table-wrap">
          <table class="bui-table">
            <caption>
              能力矩阵。表头可排序；「未知」表示后端未返回该值，排序时始终排在最后。
            </caption>
            <thead>
              <tr>
                <th v-for="column in COLUMNS" :key="column.key" scope="col" :aria-sort="ariaSort(column.key)">
                  <button type="button" class="bui-sort" :class="{ 'bui-sort--active': sortKey === column.key }" @click="toggleSort(column.key)">
                    <span>{{ column.label }}</span>
                    <span class="bui-sort__mark" :class="sortMarkClass(column.key)">
                      <AppIcon name="chevron-right" :size="12" :stroke-width="2" />
                    </span>
                  </button>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="skill in sortedSkills"
                :key="skill.skillId"
                class="bui-table__row"
                :class="{ 'bui-table__row--selected': selectedSkill?.skillId === skill.skillId }"
                tabindex="0"
                :aria-current="selectedSkill?.skillId === skill.skillId ? 'true' : undefined"
                @click="selectedSkillId = skill.skillId"
                @keydown.enter.prevent="selectedSkillId = skill.skillId"
                @keydown.space.prevent="selectedSkillId = skill.skillId"
              >
                <th scope="row">
                  <span class="cell-skill">
                    <span class="cell-skill__name">{{ skill.name }}</span>
                    <span class="cell-skill__id mono">{{ skill.skillId }}</span>
                  </span>
                </th>
                <td class="bui-table__num" data-label="目标等级">{{ formatLevel(skill.requiredLevel) }}</td>
                <td
                  class="bui-table__num"
                  :class="{ 'cell-unknown': skill.estimatedLevel === null }"
                  data-label="当前等级"
                >
                  {{ formatLevel(skill.estimatedLevel) }}
                </td>
                <td data-label="置信度">
                  <ConfidenceBadge :value="skill.confidence" compact />
                </td>
                <td data-label="差距">
                  <BeautifulStatus :label="gapLabel(skill.gapType)" :tone="gapTone(skill.gapType)" />
                </td>
                <td class="bui-table__num" data-label="证据数">
                  {{ skill.evidenceIds.length }} 条
                </td>
                <td class="bui-table__next" data-label="下一动作">{{ skill.nextAction?.type ?? '暂无' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 详情区：等级锚点、证据引用、冲突与缺失说明（§7.3） -->
        <section v-if="selectedSkill" class="surface skill-detail">
          <header class="skill-detail__head">
            <div>
              <h2 class="skill-detail__title">{{ selectedSkill.name }}</h2>
              <p class="dim mono skill-detail__id">{{ selectedSkill.skillId }}</p>
            </div>
            <BeautifulStatus :label="gapLabel(selectedSkill.gapType)" :tone="gapTone(selectedSkill.gapType)" />
          </header>

          <div class="skill-detail__levels">
            <LevelCompare
              :required="selectedSkill.requiredLevel"
              :estimated="selectedSkill.estimatedLevel"
            />
            <div class="skill-detail__conf">
              <span class="field__label">置信度</span>
              <ConfidenceBadge :value="selectedSkill.confidence" />
              <span class="dim meta-line">
                最近更新 {{ formatDateTime(selectedSkill.updatedAt) }} ·
                {{ selectedSkill.evidenceIds.length }} 条证据
              </span>
            </div>
          </div>

          <BeautifulContextCard
            v-if="selectedSkill.estimatedLevel === null"
            eyebrow="证据状态"
            text="尚未采集到可验证证据，因此等级保持未知。未知不等于能力为 0，也不等于满足要求。"
            tone="warn"
          />

          <BeautifulContextCard
            v-if="selectedSkill.gapType === 'CONFLICT'"
            eyebrow="证据冲突"
            text="不同来源的证据指向不一致的结论。两种结论并列保留在此，不静默选择对系统更有利的一条。"
            tone="warn"
          />

          <div v-if="dimensionEntries(selectedSkill).length > 0" class="anchors">
            <span class="field__label">维度锚点</span>
            <ul class="anchors__list">
              <li v-for="dim in dimensionEntries(selectedSkill)" :key="dim.key" class="anchor">
                <span class="anchor__label">{{ dim.label }}</span>
                <span class="anchor__value mono">{{ dim.value }}</span>
              </li>
            </ul>
          </div>

          <div class="evidence">
            <span class="field__label">证据引用</span>
            <p v-if="selectedSkill.sourceQuotes.length === 0" class="dim meta-line">
              后端未返回该能力的原文引用，因此这里无法展示来源片段。
            </p>
            <ul v-else class="evidence__list">
              <li v-for="(quote, index) in selectedSkill.sourceQuotes" :key="index" class="quote">
                {{ quote }}
              </li>
            </ul>
            <p class="dim meta-line">
              证据的完整列表与逐条可信度、来源类型、时间需要证据接口支持，当前契约尚未提供，
              因此这里只展示后端已返回的原文引用，不补造证据。
            </p>
          </div>

          <BeautifulContextCard
            v-if="selectedSkill.nextAction"
            :eyebrow="`下一动作 · ${selectedSkill.nextAction.type}`"
            :text="selectedSkill.nextAction.reason"
            tone="good"
          />

          <p v-if="isStale(selectedSkill)" class="stale dim">
            该能力距最近更新已超过 7 天，等级与置信度可能已经变化，建议重新估计后再做投递决策。
          </p>

          <p class="dim meta-line">
            后端尚未返回该能力的历史变化，因此这里不展示等级变化曲线（历史变化需要快照接口）。
          </p>
        </section>
      </template>
    </template>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.meta-line {
  font-size: 11.5px;
  line-height: 1.6;
}

/* ---------- 分项差距总览（§401：分项条形图，不折算单一总分） ---------- */
.compare {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 22px;
}

.compare__head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.compare__title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text);
}

.compare__note {
  max-width: 76ch;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--color-text-muted);
}

.compare__list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.compare__item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 1.3fr);
  align-items: center;
  gap: 16px;
}

.compare__label {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  min-width: 0;
}

.compare__name {
  overflow: hidden;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--color-text);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.compare__tags {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.compare__more {
  font-size: 12px;
}

@media (max-width: 767px) {
  .compare__item {
    grid-template-columns: minmax(0, 1fr);
    gap: 8px;
  }
}

/* ---------- 矩阵单元格 ---------- */
.cell-skill {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cell-skill__name {
  color: var(--color-text);
  font-size: 12.5px;
  font-weight: 700;
}

.cell-skill__id {
  color: var(--color-text-subtle);
  font-size: 10px;
  font-weight: 400;
}

.cell-unknown {
  color: var(--color-text-subtle);
}

/* ---------- 详情 ---------- */
.skill-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
}

.skill-detail__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.skill-detail__title {
  font-size: 16px;
  letter-spacing: -0.01em;
}

.skill-detail__id {
  font-size: 10.5px;
}

.skill-detail__levels {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skill-detail__conf {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.anchors {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.anchors__list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.anchor {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-surface-subtle);
  font-size: 10.5px;
}

.anchor__label {
  color: var(--color-text-subtle);
}

.anchor__value {
  color: var(--color-text);
  font-weight: 600;
}

.evidence {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.evidence__list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.quote {
  padding: 9px 11px;
  border: 1px solid var(--color-border);
  border-left: 3px solid var(--color-border-strong);
  border-radius: var(--radius-control);
  background: var(--color-surface-subtle);
  font-size: 12px;
  line-height: 1.7;
  color: var(--color-text-muted);
}

.stale {
  font-size: 11.5px;
  line-height: 1.65;
}

/* ---------- 加载与错误 ---------- */
.loading-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 150px;
  padding: 20px;
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
</style>
