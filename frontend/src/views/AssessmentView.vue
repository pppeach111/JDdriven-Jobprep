<script setup lang="ts">
import { computed, ref } from 'vue'
import BeautifulButton from '@/components/BeautifulButton.vue'
import BeautifulContextCard from '@/components/BeautifulContextCard.vue'
import BeautifulEmptyState from '@/components/BeautifulEmptyState.vue'
import BeautifulPageHeader from '@/components/BeautifulPageHeader.vue'
import BeautifulStatus from '@/components/BeautifulStatus.vue'
import ConfidenceBadge from '@/components/ConfidenceBadge.vue'
import { DEMO_ASSESSMENT, DEMO_ASSESSMENT_REPORT, DEMO_ASSESSMENT_STAGES, DEMO_GOAL, type DemoAssessmentItem } from '@/data/demo'
import { formatDateTime, formatLevel } from '@/utils/format'

/**
 * 测评与面试（19-ui-design-system.md §7.5）。
 *
 * 布局决定：
 *   - 800px 聚焦阅读列，正文之外不引入干扰区（§7.5 第一条）；
 *   - 顶部固定显示阶段、题目进度、保存状态与「退出后可恢复」说明；
 *   - 题目、回答区与提交动作在固定位置渲染，切换题目时布局不跳动；
 *   - 评分报告先给能力变化与证据，再给长篇总结；报告不可用时说明原因，不画空图表。
 */
const draft = ref('')
const saveState = ref<'IDLE' | 'LOCAL'>('IDLE')

/** 当前题（演示数据固定在第 3 题，items 非空因此末项必然存在）。 */
const current = computed<DemoAssessmentItem>(() => {
  const items = DEMO_ASSESSMENT.items
  const found = items.find((item) => item.index === DEMO_ASSESSMENT.currentIndex)
  return found ?? items[items.length - 1]!
})

const answeredCount = computed(() => current.value.index - 1)

const STAGE_LABELS: Record<'DONE' | 'RUNNING' | 'PENDING', string> = {
  DONE: '已完成',
  RUNNING: '进行中',
  PENDING: '未开始',
}

function stageTone(state: 'DONE' | 'RUNNING' | 'PENDING'): 'neutral' | 'accent' | 'good' {
  if (state === 'DONE') return 'good'
  if (state === 'RUNNING') return 'accent'
  return 'neutral'
}

function saveLocalDraft() {
  // 演示阶段没有后端：只保存在当前会话，界面必须把这一点说清楚。
  saveState.value = 'LOCAL'
}
</script>

<template>
  <div class="page">
    <BeautifulPageHeader
      title="测评与面试"
      description="按能力项逐题作答，测评结果只用于校准能力等级与置信度，不会替代你的实际产物证据。"
    >
      <template #meta>
        <BeautifulStatus
          label="演示数据"
          tone="warn"
          title="本页使用固定演示数据，未连接真实后端"
        />
        <span>{{ DEMO_ASSESSMENT.title }}</span>
        <span aria-hidden="true">·</span>
        <span>{{ DEMO_GOAL.name }}</span>
      </template>

      <template #actions>
        <BeautifulButton variant="secondary" type="button" disabled title="退出与恢复依赖后端任务接口，尚未实现">
          退出测评
        </BeautifulButton>
      </template>
    </BeautifulPageHeader>

    <div class="bui-focus-col focus">
      <!-- 阶段（§6.4：显示阶段而不是假百分比） -->
      <section class="stages" aria-label="测评阶段">
        <ol class="stages__list">
          <li
            v-for="stage in DEMO_ASSESSMENT_STAGES"
            :key="stage.key"
            class="stage"
            :class="`stage--${stage.state.toLowerCase()}`"
          >
            <span class="stage__label">{{ stage.label }}</span>
            <BeautifulStatus :label="STAGE_LABELS[stage.state]" :tone="stageTone(stage.state)" />
          </li>
        </ol>
        <p class="dim meta-line">
          阶段按顺序推进，时间不可预估时不显示剩余时间；离开页面不会丢作答。
        </p>
      </section>

      <!-- 进度与保存状态 -->
      <div class="bui-toolbar">
        <p class="progress">
          第 <span class="mono">{{ current.index }}</span> / <span class="mono">{{ DEMO_ASSESSMENT.total }}</span> 题
          <span class="dim">· 已完成 {{ answeredCount }} 题</span>
        </p>
        <p class="dim meta-line">
          保存状态：{{ saveState === 'LOCAL' ? '已保存到本地（本次会话）' : `上次保存 ${formatDateTime(DEMO_ASSESSMENT.savedAt)}` }}
        </p>
      </div>

      <BeautifulContextCard
        eyebrow="退出后可恢复"
        :text="DEMO_ASSESSMENT.resumedNote"
        tone="neutral"
      />

      <!-- 题目与回答区：位置固定，不随题目切换跳变 -->
      <section class="surface question">
        <header class="question__head">
          <span class="bui-kicker">第 {{ current.index }} 题 · {{ current.skillName }}</span>
          <h2 class="question__prompt">{{ current.prompt }}</h2>
        </header>

        <div class="field question__field">
          <label class="field__label" :for="`answer-${current.id}`">你的回答</label>
          <textarea
            :id="`answer-${current.id}`"
            v-model="draft"
            class="textarea question__input"
            placeholder="按你的真实理解作答。不确定的部分可以直接写「不确定」，这比猜测更有价值。"
          ></textarea>
          <span class="field__hint">
            作答内容仅保存在本地，演示数据不会上传到任何服务。
          </span>
        </div>

        <div class="question__actions">
          <BeautifulButton variant="secondary" type="button" @click="saveLocalDraft">
            保存草稿
          </BeautifulButton>
          <BeautifulButton
            variant="primary"
            type="button"
            disabled
            title="评分接口尚未实现，接入后才会提交"
          >
            提交本题并继续
          </BeautifulButton>
        </div>
      </section>

      <!-- 面试追问：说明「为什么问」，不泄露评分答案 -->
      <section class="bui-section">
        <div class="bui-section__head">
          <h3 class="bui-section__title">面试追问（下一题）</h3>
          <span class="bui-section__note">追问围绕你上一题的回答展开</span>
        </div>

        <BeautifulContextCard
          v-if="DEMO_ASSESSMENT.followUp.whyAsked"
          eyebrow="为什么问这一题"
          :text="DEMO_ASSESSMENT.followUp.whyAsked"
          tone="neutral"
        />

        <p class="follow-up">{{ DEMO_ASSESSMENT.followUp.prompt }}</p>
        <p class="dim meta-line">
          追问只说明提问意图，不提供参考答案；评分标准不会在作答前公开。
        </p>
      </section>

      <hr class="bui-rule" />

      <!-- 评分报告 -->
      <section class="bui-section">
        <div class="bui-section__head">
          <h3 class="bui-section__title">评分报告</h3>
          <span class="bui-section__note">能力变化优先于文字总结</span>
        </div>

        <BeautifulEmptyState
          v-if="!DEMO_ASSESSMENT_REPORT.available"
          title="评分报告尚未生成"
          :reason="DEMO_ASSESSMENT_REPORT.unavailableReason"
          required-input="完成并提交本次测评的全部题目"
        />

        <template v-else>
          <ul class="changes">
            <li v-for="change in DEMO_ASSESSMENT_REPORT.levelChanges" :key="change.skillId" class="change">
              <div class="change__head">
                <strong>{{ change.name }}</strong>
                <span class="change__levels mono">
                  {{ formatLevel(change.from) }} → {{ formatLevel(change.to) }}
                </span>
              </div>
              <p class="change__reason">{{ change.reason }}</p>
              <p class="dim meta-line">
                {{ change.evidenceCount }} 条证据 · 置信度 {{ Math.round(change.confidence * 100) }}%
              </p>
              <ConfidenceBadge :value="change.confidence" compact />
            </li>
          </ul>
          <p v-if="DEMO_ASSESSMENT_REPORT.summary" class="report-summary">
            {{ DEMO_ASSESSMENT_REPORT.summary }}
          </p>
        </template>
      </section>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.focus {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.meta-line {
  font-size: 11.5px;
  line-height: 1.6;
}

/* ---------- 阶段 ---------- */
.stages {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stages__list {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin: 0;
  padding: 0;
  list-style: none;
}

.stage {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 11px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-control);
  background: var(--color-surface);
}

.stage--running {
  border-color: var(--color-border-brand);
  background: var(--color-primary-soft);
}

.stage__label {
  font-size: 12px;
  font-weight: 600;
}

.stage--pending .stage__label {
  color: var(--color-text-muted);
  font-weight: 500;
}

/* ---------- 进度 ---------- */
.progress {
  font-size: 13px;
  font-weight: 600;
}

/* ---------- 题目 ---------- */
.question {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 22px;
}

.question__head {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.question__prompt {
  font-size: 15.5px;
  font-weight: 700;
  line-height: 1.6;
}

.question__field {
  gap: 9px;
}

/* 回答区固定高度：切换题目时不改变页面结构（§7.5） */
.question__input {
  min-height: 240px;
}

.question__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

/* ---------- 追问 ---------- */
.follow-up {
  font-size: 13px;
  line-height: 1.7;
  color: var(--color-text);
}

/* ---------- 评分报告 ---------- */
.changes {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.change {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 0;
  border-top: 1px solid var(--color-border-hairline);
}

.change:first-child {
  padding-top: 0;
  border-top: 0;
}

.change__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.change__head strong {
  font-size: 13.5px;
  font-weight: 700;
}

.change__levels {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-primary);
}

.change__reason {
  font-size: 12.5px;
  line-height: 1.65;
  color: var(--color-text-muted);
}

.report-summary {
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--color-text-muted);
}
</style>