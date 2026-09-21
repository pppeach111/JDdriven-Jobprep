<script setup lang="ts">
/**
 * 品牌标记 —— 「职径」的几何标记。
 *
 * 设计说明：
 *   取「一条有起伏但整体向上的路径，终点是一个明确的目标」作为形状语义，
 *   对应产品名「职径」与主任务（把求职目标变成可执行的路径）。
 *   线条规格与 Lucide 一致（24px 网格、stroke-width 2、round 端点），
 *   因此与导航图标属于同一视觉家族（19-ui-design-system.md §13）。
 *
 * 这不是图标库图标，而是品牌 logo，因此不进入 icons.ts 的 Lucide 路径表；
 * 也不使用装饰性渐变（§1 已明确不再以渐变作为默认视觉）。
 */
withDefaults(
  defineProps<{
    /** 图标视觉尺寸，默认 20px（容器边长为 size + 14）。 */
    size?: number
    /**
     * solid：主色底 + 反白线条，用于顶栏等浅色环境；
     * soft：浅色底 + 主色线条，用于标题区、空状态等需要降噪的位置。
     */
    variant?: 'solid' | 'soft'
  }>(),
  { size: 20, variant: 'solid' },
)
</script>

<template>
  <span
    class="bui-brand-mark"
    :class="`bui-brand-mark--${variant}`"
    :style="{ width: `${size + 14}px`, height: `${size + 14}px` }"
  >
    <svg
      :width="size"
      :height="size"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d="M3.5 19.5 9 12.5l4.5 3.5L20.5 6" />
      <circle cx="20.5" cy="6" r="2" fill="currentColor" stroke="none" />
    </svg>
  </span>
</template>
