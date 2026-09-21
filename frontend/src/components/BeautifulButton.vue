<script setup lang="ts">
/**
 * 按钮 —— 页面动作的统一入口（19-ui-design-system.md §6.5）。
 *
 * 2026-09-21 扩展：增加可选 `to`。
 *   能力矩阵、岗位详情等位置需要「跳转到另一个页面的动作」，
 *   与普通按钮视觉完全一致。与其在各页面写 `<RouterLink class="btn btn--ghost">`
 *   这样的平行实现，不如在这里按 §4.6 的第 3 档「扩展共享组件」处理。
 *   传 `to` 时渲染为 RouterLink（此时 type/disabled 不适用）。
 */
withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'quiet' | 'danger'
    type?: 'button' | 'submit' | 'reset'
    disabled?: boolean
    /** 传入路由路径时渲染为导航链接，而不是按钮。 */
    to?: string | null
  }>(),
  {
    variant: 'secondary',
    type: 'button',
    disabled: false,
    to: null,
  },
)
</script>

<template>
  <RouterLink v-if="to" :to="to" class="bui-button" :class="`bui-button--${variant}`">
    <slot />
  </RouterLink>

  <button
    v-else
    class="bui-button"
    :class="`bui-button--${variant}`"
    :type="type"
    :disabled="disabled"
  >
    <slot />
  </button>
</template>
