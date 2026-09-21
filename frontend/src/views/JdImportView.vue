<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError, api } from '@/api/client'
import BeautifulButton from '@/components/BeautifulButton.vue'
import BeautifulContextCard from '@/components/BeautifulContextCard.vue'
import BeautifulLoadingState from '@/components/BeautifulLoadingState.vue'
import BeautifulStatus from '@/components/BeautifulStatus.vue'
import ConfidenceBadge from '@/components/ConfidenceBadge.vue'
import type { JdParseResult, RequirementType, RequirementView } from '@/api/types'

const props = defineProps<{ goalId: string }>()
const router = useRouter()

const rawText = ref('')
const sourceUrl = ref('')
const analyzing = ref(false)
const analysisStage = ref('')
const errorMessage = ref('')
const errorTraceId = ref('')
const result = ref<JdParseResult | null>(null)
const importedSourceId = ref('')

const REQUIREMENT_LABELS: Record<RequirementType, string> = {
  HARD_GATE: '硬门槛',
  CORE: '核心要求',
  PREFERRED: '优先项',
  BONUS: '加分项',
}

const FIELD_LABELS: Record<string, string> = {
  skill: '技能',
  graduationYear: '毕业年份',
  educationLevel: '学历',
}

const TYPE_ORDER: RequirementType[] = ['HARD_GATE', 'CORE', 'PREFERRED', 'BONUS']

const grouped = computed(() => {
  const parsed = result.value
  if (!parsed) return []
  return TYPE_ORDER.map((type) => ({
    type,
    label: REQUIREMENT_LABELS[type],
    items: parsed.requirements.filter((item) => item.type === type),
  })).filter((group) => group.items.length > 0)
})

function tagClass(type: RequirementType): string {
  switch (type) {
    case 'HARD_GATE':
      return 'tag tag--bad'
    case 'CORE':
      return 'tag tag--accent'
    case 'PREFERRED':
      return 'tag tag--warn'
    default:
      return 'tag'
  }
}

function formatValue(value: Record<string, unknown> | null): string {
  if (!value) return '未知'
  const min = value.min
  const max = value.max
  if (typeof min === 'number' && typeof max === 'number') {
    return min === max ? String(min) : `${min} – ${max}`
  }
  if (min !== undefined) return String(min)
  return JSON.stringify(value)
}

function describe(item: RequirementView): string {
  if (item.field === 'skill') {
    const level = item.requiredLevel === null ? '等级未知' : `要求 L${item.requiredLevel}`
    return `${item.skillId ?? '未知技能'} · ${level}`
  }
  const fieldName = item.field ? (FIELD_LABELS[item.field] ?? item.field) : '未知字段'
  return `${fieldName} · ${formatValue(item.value)}`
}

function reset() {
  errorMessage.value = ''
  errorTraceId.value = ''
}

async function analyze() {
  reset()

  if (!rawText.value.trim()) {
    errorMessage.value = '请先粘贴岗位 JD 原文。'
    return
  }

  analyzing.value = true

  try {
    analysisStage.value = '正在保存 JD 原文'
    const imported = await api.importJd(props.goalId, {
      rawText: rawText.value,
      sourceUrl: sourceUrl.value.trim() || null,
    })
    importedSourceId.value = imported.data.sourceId

    analysisStage.value = '正在提取结构化要求'
    const parsed = await api.analyzeJd(props.goalId)
    result.value = parsed.data
  } catch (error) {
    if (error instanceof ApiError) {
      errorMessage.value = error.message
      errorTraceId.value = error.traceId
    } else {
      errorMessage.value = '解析过程中发生未知错误。'
    }
  } finally {
    analyzing.value = false
    analysisStage.value = ''
  }
}

function goToCapability() {
  void router.push(`/goals/${props.goalId}/capability`)
}

function loadSample() {
  rawText.value =
    '岗位职责：负责后端服务的设计与开发。\n' +
    '任职要求：2027届本科及以上学历，熟悉Java基础与集合框架，' +
    '熟悉Spring Boot与MyBatis，了解MySQL索引优化，了解Redis缓存设计，' +
    '有并发编程实践经验者优先。'
}
</script>

<template>
  <div class="page">
    <section class="hero">
      <span class="eyebrow">Step 02 / 目标 JD</span>
      <h1>粘贴真实 JD，让要求可被逐条追溯</h1>
      <p class="hero__lead muted">
        解析只抽取原文明确表达的内容。原文没有的信息会进入
        <strong>未知字段</strong>，不会被猜测成"满足"。
      </p>
    </section>

    <div class="layout">
      <!-- 输入 -->
      <section class="surface surface--lit card">
        <header class="card__head">
          <h2>JD 原文</h2>
          <BeautifulButton variant="secondary" type="button" @click="loadSample">
            填入示例
          </BeautifulButton>
        </header>

        <div class="form">
          <div class="field">
            <label class="field__label" for="jd-url">来源地址（可选）</label>
            <input
              id="jd-url"
              v-model="sourceUrl"
              class="input"
              type="text"
              maxlength="1000"
              placeholder="https://careers.example.com/jobs/123"
              autocomplete="off"
            />
          </div>

          <div class="field">
            <label class="field__label" for="jd-text">
              JD 正文 <span class="req">*</span>
            </label>
            <textarea
              id="jd-text"
              v-model="rawText"
              class="textarea"
              placeholder="把岗位描述与任职要求完整粘贴到这里…"
            ></textarea>
            <span class="field__hint">
              内容会被当作不可信文本处理，仅用于结构化分析。
            </span>
          </div>

          <p v-if="errorMessage" class="alert" role="alert">
            <span class="alert__text">{{ errorMessage }}</span>
            <span v-if="errorTraceId" class="alert__trace mono">traceId: {{ errorTraceId }}</span>
          </p>

          <div class="actions">
            <BeautifulButton variant="primary" type="button" :disabled="analyzing" @click="analyze">
              {{ analyzing ? '解析中…' : '导入并解析' }}
            </BeautifulButton>
            <RouterLink class="btn btn--ghost" to="/goals/new">返回上一步</RouterLink>
          </div>
        </div>
      </section>

      <!-- 结果 -->
      <section class="surface card result">
        <header class="card__head">
          <h2>解析结果</h2>
          <p v-if="result" class="dim">
            来源 <span class="mono">{{ importedSourceId.slice(0, 8) }}</span>
            · 岗位族
            <BeautifulStatus :label="result.jobFamily.value" tone="accent" />
          </p>
          <p v-else class="dim">解析完成后在此显示结构化要求。</p>
        </header>

          <div v-if="!result" class="placeholder dim">
            <BeautifulLoadingState v-if="analyzing" :label="analysisStage" variant="Drive" />
            <p v-else>尚未解析。</p>
        </div>

        <template v-else>
          <!-- 未知字段：必须显式呈现 -->
          <div v-if="result.unknowns.length > 0" class="unknowns">
            <BeautifulStatus :label="`未知 ${result.unknowns.length} 项`" tone="neutral" />
            <ul class="unknowns__list">
              <li v-for="field in result.unknowns" :key="field" class="mono">
                {{ field }}
              </li>
            </ul>
            <p class="field__hint">
              这些字段在原文中未出现，保持未知，不会被当作满足。
            </p>
          </div>

          <div v-if="result.warnings.length > 0" class="warnings">
            <p v-for="(warning, i) in result.warnings" :key="i" class="warnings__item">
              {{ warning }}
            </p>
          </div>

          <div v-if="grouped.length === 0" class="placeholder dim">
            <p>未识别出结构化要求，请检查 JD 文本或补充内容。</p>
          </div>

          <div v-else class="groups">
            <section v-for="group in grouped" :key="group.type" class="group">
              <h3 class="group__title">
                <span :class="tagClass(group.type)">{{ group.label }}</span>
                <span class="dim mono">{{ group.items.length }}</span>
              </h3>

              <ul class="reqs">
                <li v-for="item in group.items" :key="item.id" class="requirement">
                  <div class="req__head">
                    <span class="req__main">{{ describe(item) }}</span>
                    <span v-if="!item.explicit" class="tag tag--warn">推断</span>
                    <ConfidenceBadge :value="item.confidence" compact />
                  </div>
                  <BeautifulContextCard
                    v-if="item.sourceQuote"
                    eyebrow="原文依据"
                    :text="item.sourceQuote"
                    tone="neutral"
                  />
                </li>
              </ul>
            </section>
          </div>

          <div class="actions actions--end">
            <BeautifulButton variant="primary" type="button" @click="goToCapability">
              查看能力图谱
            </BeautifulButton>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 30px;
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

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.card {
  padding: 24px;
}

.card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.card__head h2 {
  margin-bottom: 4px;
}

.card__head p {
  font-size: 12.5px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.req {
  color: var(--color-primary);
}

.btn--sm {
  padding: 6px 12px;
  font-size: 12px;
}

.alert {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 11px 13px;
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

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.actions--end {
  justify-content: flex-end;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid var(--color-border);
}

/* ---------- 结果 ---------- */
.placeholder {
  padding: 26px 0;
  font-size: 12.5px;
  text-align: center;
}

.result {
  position: sticky;
  top: 86px;
}

.unknowns {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 13px 14px;
  margin-bottom: 18px;
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-control);
  background-color: var(--color-neutral-bg);
}

.unknowns__list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.unknowns__list li {
  padding: 2px 8px;
  border-radius: 6px;
  background-color: var(--color-neutral-bg);
  font-size: 11px;
  color: var(--color-text-muted);
}

.warnings {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
}

.warnings__item {
  padding: 9px 12px;
  border-left: 2px solid var(--color-warning);
  border-radius: 0 var(--radius-control) var(--radius-control) 0;
  background-color: var(--color-warning-bg);
  font-size: 12.5px;
  color: var(--color-warning);
}

.groups {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-height: 460px;
  overflow-y: auto;
  padding-right: 4px;
}

.group__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 12.5px;
}

.reqs {
  display: flex;
  flex-direction: column;
  gap: 9px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.requirement {
  padding: 11px 13px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-control);
  background-color: var(--color-surface-glass-strong);
}

.req__head {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-wrap: wrap;
}

.req__main {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
}

.req__quote {
  margin: 7px 0 0;
  padding-left: 10px;
  border-left: 2px solid var(--color-border-strong);
  font-size: 12px;
  color: var(--color-text-subtle);
  line-height: 1.6;
}

@media (max-width: 980px) {
  .layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .result {
    position: static;
  }
}
</style>
