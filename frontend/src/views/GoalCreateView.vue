<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ApiError, api } from '@/api/client'
import type { JobGoal } from '@/api/types'

const router = useRouter()

const form = reactive({
  name: '',
  city: '',
  employmentType: '',
  graduationYear: null as number | null,
  weeklyHours: null as number | null,
})

const submitting = ref(false)
const errorMessage = ref('')
const errorTraceId = ref('')

const existingGoals = ref<JobGoal[]>([])
const loadingGoals = ref(true)

/**
 * 招聘类型尚未在 14-contracts-and-schemas.md 冻结为枚举
 * （10-delivery-plan.md 约定在 CP-002/CP-003 冻结），
 * 因此这里只给示例值，并允许留空表示"暂不指定"。
 */
const EMPLOYMENT_OPTIONS = [
  { value: '', label: '暂不指定' },
  { value: 'INTERNSHIP', label: 'INTERNSHIP · 实习' },
  { value: 'CAMPUS', label: 'CAMPUS · 校招' },
]

async function loadGoals() {
  loadingGoals.value = true
  try {
    const response = await api.listGoals()
    existingGoals.value = response.data
  } catch (error) {
    // 列表加载失败不阻断创建流程，只在控制台留痕
    console.warn('加载已有目标失败', error)
    existingGoals.value = []
  } finally {
    loadingGoals.value = false
  }
}

function resetError() {
  errorMessage.value = ''
  errorTraceId.value = ''
}

async function submit() {
  resetError()

  if (!form.name.trim()) {
    errorMessage.value = '请填写目标名称。'
    return
  }

  submitting.value = true
  try {
    const response = await api.createGoal({
      name: form.name.trim(),
      jobFamily: 'JAVA_BACKEND',
      city: form.city.trim() || null,
      employmentType: form.employmentType || null,
      graduationYear: form.graduationYear,
      weeklyHours: form.weeklyHours,
    })
    await router.push(`/goals/${response.data.id}/jd`)
  } catch (error) {
    if (error instanceof ApiError) {
      errorMessage.value = error.message
      errorTraceId.value = error.traceId
    } else {
      errorMessage.value = '发生未知错误，请重试。'
    }
  } finally {
    submitting.value = false
  }
}

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString('zh-CN', { hour12: false })
  } catch {
    return iso
  }
}

onMounted(loadGoals)
</script>

<template>
  <div class="page">
    <section class="hero">
      <span class="eyebrow">Step 01 / 求职目标</span>
      <h1>先说明你要投什么，再谈差距</h1>
      <p class="hero__lead muted">
        目标决定了后续一切判断的基准。首版聚焦
        <strong>Java 后端实习 / 校招</strong>岗位族。
      </p>
    </section>

    <div class="layout">
      <!-- 表单 -->
      <section class="surface surface--lit card">
        <header class="card__head">
          <h2>创建求职目标</h2>
          <p class="dim">带 <span class="req">*</span> 的字段为必填。</p>
        </header>

        <form class="form" @submit.prevent="submit">
          <div class="field">
            <label class="field__label" for="goal-name">
              目标名称 <span class="req">*</span>
            </label>
            <input
              id="goal-name"
              v-model="form.name"
              class="input"
              type="text"
              maxlength="200"
              placeholder="例如：Java 后端开发实习生（2026 届）"
              autocomplete="off"
            />
          </div>

          <div class="grid">
            <div class="field">
              <label class="field__label" for="goal-city">意向城市</label>
              <input
                id="goal-city"
                v-model="form.city"
                class="input"
                type="text"
                maxlength="100"
                placeholder="例如：深圳"
                autocomplete="off"
              />
            </div>

            <div class="field">
              <label class="field__label" for="goal-employment">招聘类型</label>
              <select id="goal-employment" v-model="form.employmentType" class="select">
                <option
                  v-for="option in EMPLOYMENT_OPTIONS"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
              </select>
              <span class="field__hint">
                该字段尚未在契约中冻结为枚举，当前仅提供示例值。
              </span>
            </div>

            <div class="field">
              <label class="field__label" for="goal-year">毕业年份</label>
              <input
                id="goal-year"
                v-model.number="form.graduationYear"
                class="input"
                type="number"
                min="2000"
                max="2100"
                placeholder="2026"
              />
            </div>

            <div class="field">
              <label class="field__label" for="goal-hours">每周可投入小时</label>
              <input
                id="goal-hours"
                v-model.number="form.weeklyHours"
                class="input"
                type="number"
                min="1"
                max="168"
                placeholder="20"
              />
            </div>
          </div>

          <div class="readonly">
            <span class="field__label">岗位族</span>
            <span class="tag tag--accent">JAVA_BACKEND</span>
            <span class="field__hint">首版固定为 Java 后端岗位族。</span>
          </div>

          <p v-if="errorMessage" class="alert" role="alert">
            <span class="alert__text">{{ errorMessage }}</span>
            <span v-if="errorTraceId" class="alert__trace mono">traceId: {{ errorTraceId }}</span>
          </p>

          <div class="actions">
            <button class="btn btn--primary" type="submit" :disabled="submitting">
              {{ submitting ? '创建中…' : '创建并继续导入 JD' }}
            </button>
          </div>
        </form>
      </section>

      <!-- 已有目标 -->
      <aside class="surface card">
        <header class="card__head">
          <h2>已有目标</h2>
          <p class="dim">继续上次的进度。</p>
        </header>

        <div v-if="loadingGoals" class="list">
          <div v-for="n in 3" :key="n" class="skeleton item-skeleton"></div>
        </div>

        <p v-else-if="existingGoals.length === 0" class="empty dim">
          还没有目标。创建第一个后可在这里回到流程。
        </p>

        <ul v-else class="list">
          <li v-for="goal in existingGoals" :key="goal.id" class="item">
            <RouterLink :to="`/goals/${goal.id}/jd`" class="item__link">
              <span class="item__name">{{ goal.name }}</span>
              <span class="item__meta dim mono">{{ formatDate(goal.createdAt) }}</span>
            </RouterLink>
            <div class="item__tags">
              <span class="tag">{{ goal.jobFamily }}</span>
              <span v-if="goal.city" class="tag">{{ goal.city }}</span>
              <span v-if="goal.employmentType" class="tag">{{ goal.employmentType }}</span>
            </div>
          </li>
        </ul>
      </aside>
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
  grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.card {
  padding: 24px;
}

.card__head {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 20px;
}

.card__head p {
  font-size: 12.5px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.readonly {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding-top: 4px;
}

.req {
  color: var(--color-primary);
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
  padding-top: 4px;
}

/* ---------- 列表 ---------- */
.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-control);
  background-color: var(--color-surface-glass-strong);
  transition:
    border-color var(--dur-fast) var(--ease-out-expo),
    background-color var(--dur-fast) var(--ease-out-expo),
    transform var(--dur-fast) var(--ease-out-expo);
}

.item:hover {
  transform: translateY(-1px);
  border-color: var(--color-border-strong);
  background-color: var(--color-surface);
}

.item__link {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 14px 8px;
}

.item__name {
  font-size: 13.5px;
  font-weight: 550;
  color: var(--color-text);
}

.item__meta {
  font-size: 11px;
}

.item__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 14px 12px;
}

.item-skeleton {
  height: 74px;
}

.empty {
  font-size: 12.5px;
  padding: 8px 0;
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>