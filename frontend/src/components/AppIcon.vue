<script setup lang="ts">
import { computed } from 'vue'
import { ICONS, type IconName } from '@/components/icons'

const props = withDefaults(
  defineProps<{
    name: IconName
    /** 视觉尺寸，默认 16px（§13 允许 16/20/24） */
    size?: number
    /** 线宽，默认 1.9（§13 允许 1.8~2.1） */
    strokeWidth?: number
  }>(),
  { size: 16, strokeWidth: 1.9 },
)

/** 路径来自 icons.ts 中的常量表，非用户输入，v-html 安全 */
const inner = computed(() => ICONS[props.name])
</script>

<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
    v-html="inner"
  />
</template>

<style scoped>
svg {
  display: block;
  flex: 0 0 auto;
}
</style>
