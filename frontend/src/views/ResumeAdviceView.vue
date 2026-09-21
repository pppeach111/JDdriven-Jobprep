<script setup lang="ts">
import { ref } from 'vue'
import BeautifulButton from '@/components/BeautifulButton.vue'
import BeautifulContextCard from '@/components/BeautifulContextCard.vue'
import BeautifulPageHeader from '@/components/BeautifulPageHeader.vue'
import BeautifulStatus from '@/components/BeautifulStatus.vue'
import {
  DEMO_GOAL,
  DEMO_RESUME_ADVICE,
  DEMO_RESUME_SECTIONS,
  type DemoResumeAdvice,
  type DemoResumeEvidence,
} from '@/data/demo'

/**
 * 简历建议（19-ui-design-system.md §7.7）。
 *
 * 布局决定：
 *   - 桌面使用「原文 / 建议」并排对照，移动使用标签切换（<1024 单栏）；
 *   - 每条建议绑定证据，并提供接受、编辑、忽略三个动作；
 *   - 未确认内容不写回原文，因此接受后只改本条的确认状态；
 *   - 风险提示挂在具体建议内部，不用全页免责声明替代逐条校验。
 *
 * 按钮层级：列表内动作一律不用主色实心，避免十几条建议各自竞争主操作（§6.5
 * 「主按钮每个视图一个」），页面主操作保留在标题区。
 */
const advices = ref<DemoResumeAdvice[]>(DEMO_RESUME_ADVICE.map((item) => ({ ...item })))

const pane = ref<'original' | 'advice'>('advice')

const ADVICE_STATE_LABELS: Record<DemoResumeAdvice['state'], string> = {
  PENDING: '待确认',
  ACCEPTED: '已接受',
  EDITED: '待编辑',
  IGNORED: '已忽略',
}

function stateTone(state: DemoResumeAdvice['state']): 'neutral' | 'accent' | 'good' | 'warn' {
  if (state === 'ACCEPTED') return 'good'
  if (state === 'IGNORED') return 'neutral'
  if (state === 'EDITED') return 'warn'
  return 'accent'
}

function evidenceTone(direction: DemoResumeEvidence['direction']): 'neutral' | 'good' | 'warn' {
  if (direction === 'SUPPORT') return 'good'
  if (direction === 'WEAKEN') return 'warn'
  return 'neutral'
}

function evidenceLabel(direction: DemoResumeEvidence['direction']): string {
  if (direction === 'SUPPORT') return '支持'
  if (direction === 'WEAKEN') return '削弱'
  return '中性'
}

function setState(id: string, state: DemoResumeAdvice['state']) {
  const target = advices.value.find((item) => item.id === id)
  if (target) target.state = state
}

const acceptedCount = () =>
  advices.value.filter((item) => item.state === 'ACCEPTED').length
</script>

<template>
  <div class="page">
    <BeautifulPageHeader
      title="简历建议"
      description="每条建议都绑定证据，并保留你的原文。未经你确认，系统不会替换简历里的任何内容。"
    >
      <template #meta>
        <BeautifulStatus
          label="演示数据"
          tone="warn"
          title="本页使用固定演示数据，未连接真实后端"
        />
        <span>{{ DEMO_GOAL.name }}</span>
        <span aria-hidden="true">·</span>
        <span>{{ advices.length }} 条建议 · 已接受 {{ acceptedCount() }} 条</span>
      </template>

      <template #actions>
        <BeautifulButton
          variant="primary"
          type="button"
          disabled
          title="简历导入与解析接口尚未实现，接入后此按钮才会生效"
        >
          导入最新简历
        </BeautifulButton>
      </template>
    </BeautifulPageHeader>

    <!-- 移动端标签切换（桌面隐藏，两栏同时可见） -->
    <div class="resume-tabs" role="tablist" aria-label="简历对照视图">
      <button
        type="button"
        class="filter"
        role="tab"
        :aria-selected="pane === 'original'"
        :class="{ 'filter--active': pane === 'original' }"
        @click="pane = 'original'"
      >
        简历原文
      </button>
      <button
        type="button"
        class="filter"
        role="tab"
        :aria-selected="pane === 'advice'"
        :class="{ 'filter--active': pane === 'advice' }"
        @click="pane = 'advice'"
      >
        修改建议
      </button>
    </div>

    <div class="advice-layout">
      <!-- 原文栏 -->
      <section class="pane pane--original" :class="{ 'pane--hidden': pane !== 'original' }">
        <div class="bui-section__head">
          <h2 class="bui-section__title">简历原文</h2>
          <span class="bui-section__note">未确认的建议不会改动这里</span>
        </div>

        <ul class="sections">
          <li v-for="section in DEMO_RESUME_SECTIONS" :key="section.key" class="section">
            <span class="section__label">{{ section.label }}</span>
            <p class="section__text">{{ section.text }}</p>
          </li>
        </ul>
      </section>

      <!-- 建议栏 -->
      <section class="pane pane--advice" :class="{ 'pane--hidden': pane !== 'advice' }">
        <div class="bui-section__head">
          <h2 class="bui-section__title">修改建议</h2>
          <span class="bui-section__note">每条都带依据与风险提示</span>
        </div>

        <ul class="advices">
          <li
            v-for="advice in advices"
            :key="advice.id"
            class="advice"
            :class="`advice--${advice.state.toLowerCase()}`"
          >
            <header class="advice__head">
              <span class="bui-kicker">{{ advice.section }}</span>
              <BeautifulStatus :label="ADVICE_STATE_LABELS[advice.state]" :tone="stateTone(advice.state)" />
            </header>

            <div class="advice__compare">
              <div class="advice__side">
                <span class="advice__key">原文</span>
                <p class="advice__original">{{ advice.original }}</p>
              </div>
              <div class="advice__side">
                <span class="advice__key">建议改写</span>
                <p class="advice__suggestion">{{ advice.suggestion }}</p>
              </div>
            </div>

            <p class="advice__reason">{{ advice.reason }}</p>

            <BeautifulContextCard
              :eyebrow="`依据 · ${advice.evidence.sourceType}（${evidenceLabel(advice.evidence.direction)}）`"
              :text="advice.evidence.summary"
              :source="advice.evidence.at"
              :tone="evidenceTone(advice.evidence.direction)"
            />

            <BeautifulContextCard
              v-if="advice.risk"
              eyebrow="风险提示"
              :text="advice.risk"
              tone="warn"
            />

            <p v-if="advice.state === 'ACCEPTED'" class="advice__state dim">
              已记录你的确认。演示环境不会写回简历文件；接入后仍需你在导出前再次确认写入范围。
            </p>

            <div class="advice__actions">
              <BeautifulButton
                variant="quiet"
                type="button"
                :disabled="advice.state === 'IGNORED'"
                @click="setState(advice.id, 'IGNORED')"
              >
                忽略
              </BeautifulButton>
              <BeautifulButton
                variant="quiet"
                type="button"
                :disabled="advice.state === 'EDITED'"
                @click="setState(advice.id, 'EDITED')"
              >
                编辑后再用
              </BeautifulButton>
              <BeautifulButton
                variant="secondary"
                type="button"
                :disabled="advice.state === 'ACCEPTED'"
                @click="setState(advice.id, 'ACCEPTED')"
              >
                接受这条建议
              </BeautifulButton>
            </div>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ---------- 移动端标签：桌面隐藏 ---------- */
.resume-tabs {
  display: none;
  gap: 3px;
  padding: 3px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-control);
  background: var(--color-surface);
}

/* ---------- 并排对照（§7.7） ---------- */
.advice-layout {
  display: grid;
  grid-template-columns: minmax(0, 5fr) minmax(0, 7fr);
  gap: 20px;
  align-items: start;
}

.pane {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

/* ---------- 原文栏：浅色背景区，不做白卡堆叠 ---------- */
.sections {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin: 0;
  padding: 16px 18px;
  border-radius: var(--radius-card);
  background: var(--color-surface-subtle);
  list-style: none;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.section__label {
  color: var(--color-text-subtle);
  font-size: 10.5px;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.section__text {
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--color-text);
}

/* ---------- 建议 ---------- */
.advices {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.advice {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 18px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-surface);
}

.advice--ignored {
  background: var(--color-surface-subtle);
}

.advice--accepted {
  border-color: rgba(10, 117, 104, 0.3);
}

.advice__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.advice__compare {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 12px;
}

.advice__side {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: var(--radius-control);
  background: var(--color-surface-subtle);
}

.advice__key {
  color: var(--color-text-subtle);
  font-size: 10.5px;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.advice__original {
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--color-text-muted);
}

.advice__suggestion {
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--color-text);
  white-space: pre-line;
}

.advice__reason {
  font-size: 12px;
  line-height: 1.7;
  color: var(--color-text-muted);
}

.advice__state {
  font-size: 11.5px;
  line-height: 1.65;
}

.advice__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  flex-wrap: wrap;
}

/* ---------- 单栏：移动与窄屏（<1024） ---------- */
@media (max-width: 1023px) {
  .advice-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .resume-tabs {
    display: flex;
  }

  .pane--hidden {
    display: none;
  }
}

@media (max-width: 767px) {
  .advice__actions {
    justify-content: flex-start;
  }
}
</style>