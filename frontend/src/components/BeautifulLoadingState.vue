<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    label?: string
    variant?: 'Drive' | 'Dots' | 'Orbit'
  }>(),
  { label: '正在处理', variant: 'Drive' },
)

const tenths = ref(0)
let timer: number | undefined

onMounted(() => {
  timer = window.setInterval(() => {
    tenths.value += 1
  }, 100)
})

onBeforeUnmount(() => {
  if (timer !== undefined) window.clearInterval(timer)
})

const elapsed = computed(() => {
  const seconds = tenths.value / 10
  return seconds < 60
    ? `${seconds.toFixed(1)}s`
    : `${Math.floor(seconds / 60)}m ${(seconds % 60).toFixed(1)}s`
})

const delays = computed<(number | null)[]>(() => {
  if (props.variant === 'Orbit') return [0, 110, 220, 770, null, 330, 660, 550, 440]
  return [180, 90, 180, 90, 0, 90, 180, 90, 180]
})
</script>

<template>
  <div class="bui-loading" role="status" :aria-label="`${label}，已用时 ${elapsed}`">
    <span class="bui-loading__grid" aria-hidden="true">
      <span
        v-for="(delay, index) in delays"
        :key="index"
        class="bui-loading__cell"
        :class="{ 'bui-loading__cell--round': variant === 'Dots' }"
        :style="{
          opacity: delay === null ? 0.12 : 0.2,
          animation: delay === null ? 'none' : `bui-pixel-on 950ms ease-in-out ${delay}ms infinite`,
        }"
      ></span>
    </span>
    <span class="bui-loading__label">{{ label }}</span>
    <span class="bui-loading__time mono">{{ elapsed }}</span>
  </div>
</template>
