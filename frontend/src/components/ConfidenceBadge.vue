<script setup lang="ts">
import { computed } from 'vue'

/**
 * 置信度指示器。
 *
 * 契约（14-contracts-and-schemas.md 第 5 节 / CapabilityMap 注释）要求：
 * 界面必须同时展示等级与置信度，不得只给单一分数。
 * 因此本组件始终把"置信度较低"这一事实显式呈现，而不是藏起来。
 */
const props = defineProps<{
  value: number | null
  compact?: boolean
}>()

const percent = computed(() =>
  props.value === null ? null : Math.round(props.value * 100),
)

const tone = computed(() => {
  if (props.value === null) return 'unknown'
  if (props.value >= 0.7) return 'high'
  if (props.value >= 0.4) return 'mid'
  return 'low'
})

const label = computed(() => {
  switch (tone.value) {
    case 'high':
      return '较高'
    case 'mid':
      return '中等'
    case 'low':
      return '较低'
    default:
      return '未知'
  }
})
</script>

<template>
  <span
    class="conf"
    :class="[`conf--${tone}`, { 'conf--compact': compact }]"
    :title="percent === null ? '后端未返回置信度' : `置信度 ${percent}%`"
  >
    <span class="conf__bar" aria-hidden="true">
      <span class="conf__fill" :style="{ width: `${percent ?? 0}%` }"></span>
    </span>
    <span class="conf__text mono">
      {{ percent === null ? '—' : `${percent}%` }}
    </span>
    <span class="conf__label">{{ label }}</span>
  </span>
</template>

<style scoped>
.conf {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--color-text-muted);
}

.conf__bar {
  width: 40px;
  height: 3px;
  border-radius: 999px;
  background-color: var(--color-neutral-bg);
  overflow: hidden;
}

.conf__fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background-color: currentColor;
  transition: width var(--dur-base) var(--ease-out-expo);
}

.conf__text {
  font-size: 11px;
  color: var(--color-text);
}

.conf__label {
  color: var(--color-text-subtle);
}

.conf--high {
  color: var(--color-success);
}
.conf--mid {
  color: var(--color-warning);
}
.conf--low {
  color: var(--color-danger);
}
.conf--unknown {
  color: var(--color-neutral);
}

.conf--compact .conf__bar {
  width: 26px;
}
.conf--compact .conf__label {
  display: none;
}
</style>