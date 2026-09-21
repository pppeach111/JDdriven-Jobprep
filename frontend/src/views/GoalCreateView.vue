<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError, api } from '@/api/client'
import BeautifulButton from '@/components/BeautifulButton.vue'
import BeautifulContextCard from '@/components/BeautifulContextCard.vue'
import BeautifulEmptyState from '@/components/BeautifulEmptyState.vue'
import BeautifulLoadingState from '@/components/BeautifulLoadingState.vue'
import BeautifulPageHeader from '@/components/BeautifulPageHeader.vue'
import BeautifulStatus from '@/components/BeautifulStatus.vue'
import BeautifulTaskRow from '@/components/BeautifulTaskRow.vue'
import { useCurrentGoal } from '@/composables/useCurrentGoal'

/**
 * 求职目标（19-ui-design-system.md §7.1）。
 *
 * 2026-09-21 调整：
 *   - 去掉横幅式大标题（原 .hero + h1），改为 §5.1 固定结构的页面标题区，
 *     使 6 个一级页面的首屏结构一致；
 *   - 正文改为 720px 单列步骤表单；「已有目标」从右侧栏移到表单下方，
 *     避免与主表单争夺注意力，也消除与外壳断点体系不一致的 900px 内容断点；
 *   - 产品定位句并入标题区说明，让品牌不只在顶栏出现。
 *
 * 本页面连接真实后端接口（/api/v1/goals），不使用演示数据。
 */
const route = useRoute()
const router = useRouter()

/**
 * 已有目标列表与顶栏的目标切换器共用同一份全局状态（useCurrentGoal），
 * 避免页面列表与顶栏切换器各自维护数据源、出现「切换器里有、页面上没有」的矛盾（§2.4）。
 */
const { state: goalState, selectableGoals, loadGoals, adoptGoal } = useCurrentGoal()

const existingGoals = computed(() => selectableGoals.value)

/**
 * 从一级导航「能力与证据」进来时带 `intent=capability`。
 * 该入口不做禁用态（§3.2），改为先把用户引导到创建页，创建完成后直接进入能力图谱。
 */
const fromCapabilityIntent = computed(() => route.query.intent === 'capability')

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

    // 登记进全局状态：顶栏切换器与「能力与证据」入口立即可用，
    // 省掉一次额外的列表往返，也不在前端伪造目标对象（§14）。
    adoptGoal(response.data)

    const next = fromCapabilityIntent.value ? 'capability' : 'jd'
    await router.push(`/goals/${response.data.id}/${next}`)
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
</script>

<template>
  <div class="page">
    <BeautifulPageHeader
      context="目标建立 · 步骤 1 / 3"
      title="创建求职目标"
      description="职径把一份岗位 JD 变成可执行的求职路径。目标决定后续所有判断的基准，首版聚焦 Java 后端实习与校招岗位族。"
    >
      <template #meta>
        <span>岗位族</span>
        <BeautifulStatus label="JAVA_BACKEND" tone="accent" />
        <span aria-hidden="true">·</span>
        <span>带 <span class="req">*</span> 的字段为必填</span>
        <span aria-hidden="true">·</span>
        <span>数据来自本地后端接口</span>
      </template>
    </BeautifulPageHeader>

    <div class="bui-form-col form-col">
      <!-- 从一级导航「能力与证据」进来时的引导（§3.2：该入口不做禁用态，改为引导前置条件） -->
      <BeautifulContextCard
        v-if="fromCapabilityIntent"
        eyebrow="来自「能力与证据」"
        text="能力与证据挂在一个求职目标下，所以这里先创建目标。创建完成后会直接进入该目标的能力图谱；图谱由目标 JD 解析生成，按页面提示导入 JD 即可。"
        tone="neutral"
      />

      <section class="surface card">
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
            <span class="field__hint">用于区分不同投递方向，之后可随时修改。</span>
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
              <span class="field__hint">用于计算学习计划的时间预算。</span>
            </div>
          </div>

          <p v-if="errorMessage" class="alert" role="alert">
            <span class="alert__text">{{ errorMessage }}</span>
            <span v-if="errorTraceId" class="alert__trace mono">traceId: {{ errorTraceId }}</span>
          </p>

          <div class="actions">
            <BeautifulButton variant="primary" type="submit" :disabled="submitting">
              {{
                submitting
                  ? '创建中…'
                  : fromCapabilityIntent
                    ? '创建并查看能力图谱'
                    : '创建并继续导入 JD'
              }}
            </BeautifulButton>
          </div>
        </form>
      </section>

      <!-- 已有目标：从右侧栏移到表单下方，避免与主表单争夺注意力 -->
      <section class="bui-section">
        <div class="bui-section__head">
          <h2 class="bui-section__title">已有目标</h2>
          <span class="bui-section__note">继续上次的进度</span>
        </div>

        <div v-if="!goalState.synced && !goalState.errorMessage" class="loading-row">
          <BeautifulLoadingState label="正在读取已有目标" variant="Dots" />
        </div>

        <BeautifulEmptyState
          v-else-if="goalState.errorMessage"
          title="已有目标读取失败"
          :reason="`${goalState.errorMessage}创建新目标不受影响，也可以重试读取。`"
          required-input="本地后端服务"
        >
          <template #action>
            <BeautifulButton variant="secondary" type="button" @click="loadGoals">
              重试
            </BeautifulButton>
          </template>
        </BeautifulEmptyState>

        <BeautifulEmptyState
          v-else-if="existingGoals.length === 0"
          title="还没有求职目标"
          reason="目标列表为空，因此这里没有可继续的进度。创建第一个目标后，它会出现在这里。"
          required-input="一个岗位方向（首版固定为 Java 后端岗位族）"
        />

        <ul v-else class="list">
          <li v-for="goal in existingGoals" :key="goal.id" class="item">
            <RouterLink :to="`/goals/${goal.id}/jd`" class="item__link">
              <BeautifulTaskRow
                expanded
                :title="goal.name"
                :meta="`${goal.jobFamily} · 创建于 ${formatDate(goal.createdAt)}`"
                status="继续"
                tone="accent"
              >
                <div class="item__tags">
                  <span v-if="goal.city" class="bui-chip">{{ goal.city }}</span>
                  <span v-if="goal.employmentType" class="bui-chip">{{ goal.employmentType }}</span>
                  <span v-if="goal.graduationYear" class="bui-chip">
                    {{ goal.graduationYear }} 届
                  </span>
                </div>
              </BeautifulTaskRow>
            </RouterLink>
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
  gap: 24px;
}

.form-col {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.card {
  padding: 22px;
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
  display: flex;
  justify-content: flex-end;
}

/* ---------- 已有目标 ---------- */
.loading-row {
  padding: 6px 0;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.item__link {
  display: block;
  border-radius: var(--radius-control);
}

.item__link :deep(.bui-task-row) {
  transition: border-color var(--dur-fast) ease;
}

.item__link:hover :deep(.bui-task-row) {
  border-color: var(--color-border-strong);
}

.item__tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

@media (max-width: 767px) {
  .grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
