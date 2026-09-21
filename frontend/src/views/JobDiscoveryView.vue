<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import BeautifulButton from '@/components/BeautifulButton.vue'
import BeautifulContextCard from '@/components/BeautifulContextCard.vue'
import BeautifulEmptyState from '@/components/BeautifulEmptyState.vue'
import BeautifulPageHeader from '@/components/BeautifulPageHeader.vue'
import BeautifulStatus from '@/components/BeautifulStatus.vue'
import BeautifulTaskRow from '@/components/BeautifulTaskRow.vue'
import ConfidenceBadge from '@/components/ConfidenceBadge.vue'
import LevelCompare from '@/components/LevelCompare.vue'
import {
  DEMO_GOAL,
  DEMO_JOBS,
  JOB_AVAILABILITY_LABELS,
  JOB_OPEN_STATUS_LABELS,
  type DemoJob,
  type DemoJobAvailability,
  type DemoJobOpenStatus,
} from '@/data/demo'
import { formatDateTime, formatRatio } from '@/utils/format'

/**
 * 岗位发现（19-ui-design-system.md §7.4）。
 *
 * 布局决定：
 *   - 顶部保留目标摘要、筛选与最后刷新时间（§7.4 第一条）；
 *   - 桌面用结果列表 + 详情面板的主从布局（§5.3 宽屏 5:7，<1024 单列）；
 *   - 详情顺序固定为「是否可投 → 为什么匹配 → 硬门槛 → 能力差距 → 来源与申请入口」；
 *   - 来源、开放状态与核验时间不进折叠区，直接出现在列表摘要与详情中（§7.4 末条）。
 *
 * 数据来源：接口尚未实现，使用 frontend/src/data/demo.ts 中明确标识的演示数据，
 * 并在标题区用 BeautifulStatus 标注，符合 §14 的例外条件。
 */
const route = useRoute()

const FILTERS = [
  { key: 'ALL', label: '全部' },
  { key: 'READY', label: JOB_AVAILABILITY_LABELS.READY },
  { key: 'AFTER_GAP', label: JOB_AVAILABILITY_LABELS.AFTER_GAP },
  { key: 'STRETCH', label: JOB_AVAILABILITY_LABELS.STRETCH },
  { key: 'BLOCKED', label: JOB_AVAILABILITY_LABELS.BLOCKED },
  { key: 'UNVERIFIED', label: JOB_AVAILABILITY_LABELS.UNVERIFIED },
] as const

type FilterKey = (typeof FILTERS)[number]['key']

const filter = ref<FilterKey>('ALL')

/** §6.3：可投状态是分组与筛选，不是五种卡片皮肤，因此共用同一行样式。 */
function countOf(key: FilterKey): number {
  return key === 'ALL' ? DEMO_JOBS.length : DEMO_JOBS.filter((job) => job.availability === key).length
}

const visibleJobs = computed<DemoJob[]>(() =>
  filter.value === 'ALL' ? DEMO_JOBS : DEMO_JOBS.filter((job) => job.availability === filter.value),
)

/** 选中项由 URL 的 job 参数决定，保证可分享（§5.3）。 */
const selectedId = computed(() => {
  const requested = String(route.query.job ?? '')
  if (visibleJobs.value.some((job) => job.id === requested)) return requested
  return visibleJobs.value[0]?.id ?? ''
})

const selectedJob = computed<DemoJob | null>(
  () => visibleJobs.value.find((job) => job.id === selectedId.value) ?? null,
)

const currentFilterLabel = computed(
  () => FILTERS.find((item) => item.key === filter.value)?.label ?? '当前',
)

/** 核对点覆盖：给出分子分母，不折算成单一综合分（§9）。 */
const coverage = computed(() => {
  const job = selectedJob.value
  if (!job) return null
  const matched = job.matchFactors.reduce((sum, item) => sum + item.matched, 0)
  const total = job.matchFactors.reduce((sum, item) => sum + item.total, 0)
  return { matched, total }
})

function availabilityTone(tone: DemoJobAvailability): 'neutral' | 'accent' | 'good' | 'warn' | 'bad' {
  switch (tone) {
    case 'READY':
      return 'good'
    case 'AFTER_GAP':
      return 'warn'
    case 'STRETCH':
      return 'accent'
    case 'BLOCKED':
      return 'bad'
    default:
      return 'neutral'
  }
}

function openStatusTone(status: DemoJobOpenStatus): 'neutral' | 'accent' | 'good' | 'warn' | 'bad' {
  if (status === 'OPEN') return 'good'
  if (status === 'CLOSED') return 'bad'
  return 'neutral'
}

function gateTone(met: boolean | null): 'neutral' | 'good' | 'bad' {
  if (met === true) return 'good'
  if (met === false) return 'bad'
  return 'neutral'
}

function gateLabel(met: boolean | null): string {
  if (met === true) return '满足'
  if (met === false) return '不满足'
  return '未知'
}

/** 摘要只报「不满足」与「待确认」的条数，绝不把未知说成满足。 */
function gateSummary(job: DemoJob): string {
  const blocked = job.hardGates.filter((gate) => gate.met === false).length
  const unknown = job.hardGates.filter((gate) => gate.met === null).length
  if (blocked > 0) return `${blocked} 项不满足`
  if (unknown > 0) return `${unknown} 项待确认`
  return job.hardGates.length > 0 ? '已确认满足' : '未提取到硬门槛'
}

/** §8 过期状态：超过 7 天未核验即提示重新核验，不再沿用绿色成功态。 */
const STALE_DAYS = 7

function isStale(job: DemoJob): boolean {
  if (!job.lastVerifiedAt) return true
  const elapsed = Date.now() - new Date(job.lastVerifiedAt).getTime()
  return elapsed > STALE_DAYS * 24 * 60 * 60 * 1000
}

function verifiedText(job: DemoJob): string {
  return job.lastVerifiedAt ? formatDateTime(job.lastVerifiedAt) : '从未核验'
}

function verdict(job: DemoJob): string {
  switch (job.availability) {
    case 'READY':
      return '当前估计已达到该岗位的核心要求，硬门槛已核验通过，可以现在投递。'
    case 'AFTER_GAP':
      return '存在可补齐的能力差距，补齐后匹配度会明显提高；现在投递也能进入筛选，但优势不足。'
    case 'STRETCH':
      return '要求的等级高于你当前的估计，属于冲刺岗位。建议先投递并准备差距说明，不要作为主投方向。'
    case 'BLOCKED':
      return '存在不满足的硬门槛，投递大概率无法通过筛选。建议确认门槛表述后再决定。'
    default:
      return '岗位开放状态尚未核验，无法判断是否仍在招聘，因此暂不给出可投结论。'
  }
}
</script>

<template>
  <div class="page">
    <BeautifulPageHeader
      title="岗位发现"
      description="按目标筛选岗位：先看是否可投，再看匹配原因与硬门槛。每条结果都带来源和最后核验时间。"
    >
      <template #meta>
        <BeautifulStatus
          label="演示数据"
          tone="warn"
          title="本页使用固定演示数据，未连接真实后端"
        />
        <span>{{ DEMO_GOAL.name }}</span>
        <span aria-hidden="true">·</span>
        <span>{{ DEMO_GOAL.city }} · {{ DEMO_GOAL.employmentType }}</span>
        <span aria-hidden="true">·</span>
        <span class="mono">数据基准 {{ formatDateTime(DEMO_GOAL.dataUpdatedAt) }}</span>
      </template>

      <template #actions>
        <BeautifulButton
          variant="primary"
          type="button"
          disabled
          title="岗位检索接口尚未实现，接入后此按钮才会生效"
        >
          刷新岗位发现
        </BeautifulButton>
      </template>
    </BeautifulPageHeader>

    <div class="bui-toolbar">
      <div class="bui-filters" role="group" aria-label="按可投状态筛选">
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
          <span class="filter__count mono">{{ countOf(item.key) }}</span>
        </button>
      </div>

      <p class="dim meta-line">
        共 {{ visibleJobs.length }} 条结果 · 最后刷新 {{ formatDateTime(DEMO_GOAL.dataUpdatedAt) }}
      </p>
    </div>

    <BeautifulEmptyState
      v-if="visibleJobs.length === 0"
      title="当前筛选下没有岗位"
      :reason="`「${currentFilterLabel}」分组下没有结果。本页只有 5 条演示数据，因此部分分组为空属正常，不代表真实检索为空。`"
      required-input="至少导入并解析一份目标 JD，供系统提取岗位要求"
    >
      <template #action>
        <BeautifulButton variant="secondary" type="button" @click="filter = 'ALL'">
          查看全部岗位
        </BeautifulButton>
      </template>
    </BeautifulEmptyState>

    <div v-else class="bui-split">
      <!-- 结果列表 -->
      <div class="bui-list">
        <RouterLink
          v-for="job in visibleJobs"
          :key="job.id"
          :to="{ name: 'jobs', query: { job: job.id } }"
          class="job-item"
          :class="{ 'job-item--active': job.id === selectedId }"
        >
          <BeautifulTaskRow
            expanded
            :title="job.title"
            :meta="`${job.company} · ${job.locations.join(' / ')} · ${job.employmentType}`"
            :status="JOB_AVAILABILITY_LABELS[job.availability]"
            :tone="availabilityTone(job.availability)"
            :muted="job.openStatus !== 'OPEN'"
          >
            <div class="job-item__flags">
              <BeautifulStatus
                :label="JOB_OPEN_STATUS_LABELS[job.openStatus]"
                :tone="openStatusTone(job.openStatus)"
              />
              <span class="bui-chip">硬门槛：{{ gateSummary(job) }}</span>
            </div>
            <p class="job-item__verified dim">
              最后核验 {{ verifiedText(job) }}
            </p>
          </BeautifulTaskRow>
        </RouterLink>
      </div>

      <!-- 详情面板：顺序固定，不用折叠区藏来源与核验时间 -->
      <aside v-if="selectedJob" class="surface job-detail">
        <header class="job-detail__head">
          <div class="job-detail__title-block">
            <h2 class="job-detail__title">{{ selectedJob.title }}</h2>
            <p class="dim job-detail__sub">
              {{ selectedJob.company }} · {{ selectedJob.locations.join(' / ') }} ·
              {{ selectedJob.employmentType }}
            </p>
          </div>
          <BeautifulStatus
            :label="JOB_AVAILABILITY_LABELS[selectedJob.availability]"
            :tone="availabilityTone(selectedJob.availability)"
          />
        </header>

        <section class="bui-section">
          <div class="bui-section__head">
            <h3 class="bui-section__title">是否可投</h3>
            <BeautifulStatus
              :label="JOB_OPEN_STATUS_LABELS[selectedJob.openStatus]"
              :tone="openStatusTone(selectedJob.openStatus)"
            />
          </div>
          <p class="job-detail__verdict">{{ verdict(selectedJob) }}</p>
          <p class="dim meta-line">最后核验 {{ verifiedText(selectedJob) }}</p>
          <BeautifulContextCard
            v-if="isStale(selectedJob)"
            eyebrow="状态可能已过期"
            text="该岗位距上次核验已超过 7 天，开放状态可能已经变化。重新核验之前，请勿把当前状态当作结论。"
            tone="warn"
          />
        </section>

        <hr class="bui-rule" />

        <section class="bui-section">
          <div class="bui-section__head">
            <h3 class="bui-section__title">为什么匹配</h3>
            <span v-if="coverage" class="bui-section__note mono">
              核对点覆盖 {{ formatRatio(coverage.matched, coverage.total, '项') }}
            </span>
          </div>

          <div v-for="factor in selectedJob.matchFactors" :key="factor.label" class="factor">
            <div class="factor__head">
              <span class="factor__label">{{ factor.label }}</span>
              <span class="dim mono">{{ factor.matched }} / {{ factor.total }}</span>
            </div>
            <div class="bui-meter">
              <span class="bui-meter__track">
                <span
                  class="bui-meter__fill"
                  :style="{
                    width: `${factor.total > 0 ? Math.round((factor.matched / factor.total) * 100) : 0}%`,
                  }"
                ></span>
              </span>
              <span class="bui-meter__value">
                {{ factor.total > 0 ? Math.round((factor.matched / factor.total) * 100) : 0 }}%
              </span>
            </div>
            <p class="dim meta-line">{{ factor.note }}</p>
          </div>

          <p class="dim meta-line">
            以上按核对点分别计数，不折算成单一综合分；匹配构成随时可查看，避免只给一个百分比。
          </p>
        </section>

        <hr class="bui-rule" />

        <section class="bui-section">
          <h3 class="bui-section__title">硬门槛</h3>
          <p v-if="selectedJob.hardGates.length === 0" class="dim">
            未从原文提取到硬门槛，因此不做门槛判断。
          </p>
          <ul v-else class="gates">
            <li v-for="gate in selectedJob.hardGates" :key="gate.label" class="gate">
              <BeautifulStatus :label="gateLabel(gate.met)" :tone="gateTone(gate.met)" />
              <div class="gate__body">
                <strong>{{ gate.label }}</strong>
                <span class="dim">{{ gate.note }}</span>
              </div>
            </li>
          </ul>
        </section>

        <hr class="bui-rule" />

        <section class="bui-section">
          <h3 class="bui-section__title">能力差距</h3>
          <p v-if="selectedJob.gaps.length === 0" class="dim">
            该岗位没有识别出能力差距。注意这不代表能力已被证明，只说明未提取到对应要求。
          </p>
          <ul v-else class="gaps">
            <li v-for="gap in selectedJob.gaps" :key="gap.skillId" class="gap">
              <div class="gap__head">
                <strong>{{ gap.name }}</strong>
                <span class="gap__conf">
                  <span class="dim conf-key">置信度</span>
                  <ConfidenceBadge :value="gap.confidence" compact />
                </span>
              </div>
              <LevelCompare :required="gap.requiredLevel" :estimated="gap.estimatedLevel" />
            </li>
          </ul>
        </section>

        <hr class="bui-rule" />

        <section class="bui-section">
          <h3 class="bui-section__title">来源与申请入口</h3>
          <div class="bui-kv">
            <span class="bui-kv__key">来源</span>
            <span class="bui-kv__value">{{ selectedJob.source.name }}</span>
          </div>
          <div class="bui-kv">
            <span class="bui-kv__key">最后核验时间</span>
            <span class="bui-kv__value mono">{{ verifiedText(selectedJob) }}</span>
          </div>
          <div class="bui-kv">
            <span class="bui-kv__key">申请入口</span>
            <span class="bui-kv__value">
              <a
                v-if="selectedJob.source.url"
                :href="selectedJob.source.url"
                target="_blank"
                rel="noopener noreferrer nofollow"
              >
                前往官方招聘页
              </a>
              <span v-else class="dim">来源未提供可校验的官方链接，因此不提供跳转</span>
            </span>
          </div>
          <p class="dim meta-line">
            只有来源为官方且链接通过校验时才会出现申请入口；演示数据不含可校验链接。
          </p>
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.meta-line {
  font-size: 11.5px;
  line-height: 1.6;
}

/* ---------- 结果列表项：包装 BeautifulTaskRow，只补可点击与选中态 ---------- */
.job-item {
  display: block;
  border-radius: var(--radius-control);
}

.job-item :deep(.bui-task-row) {
  transition:
    border-color var(--dur-fast) ease,
    box-shadow var(--dur-fast) ease;
}

.job-item:hover :deep(.bui-task-row) {
  border-color: var(--color-border-strong);
}

.job-item--active :deep(.bui-task-row) {
  border-color: var(--color-primary);
  box-shadow: inset 0 0 0 1px var(--color-border-brand);
}

.job-item__flags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.job-item__verified {
  font-size: 11px;
}

/* ---------- 详情面板 ---------- */
.job-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 20px;
}

.job-detail__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.job-detail__title-block {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.job-detail__title {
  font-size: 17px;
  letter-spacing: -0.01em;
}

.job-detail__sub {
  font-size: 12px;
}

.job-detail__verdict {
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--color-text);
}

/* ---------- 匹配构成 ---------- */
.factor {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.factor__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.factor__label {
  font-size: 12.5px;
  font-weight: 600;
}

/* ---------- 硬门槛 ---------- */
.gates,
.gaps {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.gate {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.gate__body {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.gate__body strong {
  font-size: 12.5px;
  font-weight: 600;
}

.gate__body span {
  font-size: 11.5px;
  line-height: 1.6;
}

/* ---------- 能力差距 ---------- */
.gap {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 0;
  border-top: 1px solid var(--color-border-hairline);
}

.gap:first-child {
  padding-top: 0;
  border-top: 0;
}

.gap__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.gap__head strong {
  font-size: 13px;
  font-weight: 700;
}

.gap__conf {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.conf-key {
  font-size: 11px;
}
</style>