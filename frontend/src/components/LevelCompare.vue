<script setup lang="ts">
import { computed } from 'vue'

/**
 * 等级对比：岗位要求 vs 当前估计（0~5）。
 *
 * 设计要点：
 *   - 估计值缺失（null）时显示为"未知"，绝不画成 0 或当成已满足；
 *   - 要求刻度与估计填充分开表达，避免把两者混成一个分数。
 */
const props = defineProps<{
  required: number | null
  estimated: number | null
}>()

const MAX = 5

const ticks = computed(() => Array.from({ length: MAX + 1 }, (_, i) => i))

const requiredPercent = computed(() => clampPercent(props.required))
const estimatedPercent = computed(() => clampPercent(props.estimated))

const hasEstimate = computed(() => props.estimated !== null)

function clampPercent(value: number | null): number {
  if (value === null) return 0
  return Math.min(100, Math.max(0, (value / MAX) * 100))
}

function format(value: number | null): string {
  return value === null ? '未知' : value.toFixed(1)
}
</script>

<template>
  <div class="level">
    <div class="level__values">
      <span class="level__pair">
        <span class="level__key">要求</span>
        <span class="level__num mono">{{ format(required) }}</span>
      </span>
      <span class="level__arrow" aria-hidden="true">→</span>
      <span class="level__pair">
        <span class="level__key">估计</span>
        <span
          class="level__num mono"
          :class="{ 'level__num--unknown': !hasEstimate }"
        >
          {{ format(estimated) }}
        </span>
      </span>
    </div>

    <div class="level__track" role="img" :aria-label="`要求等级 ${format(required)}，当前估计 ${format(estimated)}`">
      <span
        v-if="hasEstimate"
        class="level__fill"
        :style="{ width: `${estimatedPercent}%` }"
      ></span>
      <span
        v-if="required !== null"
        class="level__marker"
        :style="{ left: `${requiredPercent}%` }"
        title="岗位要求"
      ></span>
      <span v-for="tick in ticks" :key="tick" class="level__tick"></span>
    </div>

    <div class="level__scale mono" aria-hidden="true">
      <span>0</span>
      <span>1</span>
      <span>2</span>
      <span>3</span>
      <span>4</span>
      <span>5</span>
    </div>
  </div>
</template>

<style scoped>
.level {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-width: 210px;
}

.level__values {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.level__pair {
  display: inline-flex;
  align-items: baseline;
  gap: 5px;
}

.level__key {
  font-size: 11px;
  color: var(--color-text-subtle);
}

.level__num {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}

.level__num--unknown {
  color: var(--color-neutral);
  font-weight: 500;
}

.level__arrow {
  color: var(--color-text-subtle);
  font-size: 11px;
}

.level__track {
  position: relative;
  height: 6px;
  border-radius: 999px;
  background-color: var(--color-neutral-bg);
  overflow: hidden;
}

.level__fill {
  position: absolute;
  inset-block: 0;
  left: 0;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--color-primary-soft), var(--color-primary));
  transition: width var(--dur-base) var(--ease-out-expo);
}

/* 要求刻度：竖向细线，表示目标线 */
.level__marker {
  position: absolute;
  top: -2px;
  bottom: -2px;
  width: 2px;
  margin-left: -1px;
  border-radius: 2px;
  background-color: var(--color-text);
  box-shadow: 0 0 0 1.5px var(--color-surface);
}

.level__tick {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 1px;
  background-color: var(--color-border-strong);
}

.level__tick:nth-child(3) {
  left: 20%;
}
.level__tick:nth-child(4) {
  left: 40%;
}
.level__tick:nth-child(5) {
  left: 60%;
}
.level__tick:nth-child(6) {
  left: 80%;
}

.level__scale {
  display: flex;
  justify-content: space-between;
  font-size: 9.5px;
  color: var(--color-text-subtle);
}
</style>