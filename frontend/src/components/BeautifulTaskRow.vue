<script setup lang="ts">
/**
 * 任务行 —— 列表摘要的统一载体（19-ui-design-system.md §4.6）。
 *
 * 2026-09-21 扩展（CP-010 后续）：
 *   岗位结果、学习任务、简历建议需要在摘要行下附一行业务信息
 *   （如岗位的开放状态与最后核验时间、任务的验收标准与产物）。
 *   与其在 4 个页面各写一套「标题 + 说明 + 状态」的平行实现，
 *   这里增加 `expanded` 属性与默认插槽：默认仍是单行紧凑摘要，
 *   `expanded` 时允许插槽内容整行换行，承载页面自己的业务汇总信息。
 *
 * 该扩展不改变既有调用方的行为（未传插槽时渲染结果与扩展前一致）。
 */
withDefaults(
  defineProps<{
    title: string
    meta?: string
    status?: string
    tone?: 'neutral' | 'accent' | 'good' | 'warn' | 'bad'
    /** 开启后允许插槽内容换行，用于承载额外汇总信息。 */
    expanded?: boolean
    /** 降噪态：用于已过期、已关闭、未选中但仍需展示的条目。 */
    muted?: boolean
  }>(),
  { meta: '', status: '', tone: 'neutral', expanded: false, muted: false },
)
</script>

<template>
  <div
    class="bui-task-row"
    :class="{ 'bui-task-row--expanded': expanded, 'bui-task-row--muted': muted }"
  >
    <span class="bui-task-row__marker" :class="`bui-task-row__marker--${tone}`" aria-hidden="true"></span>
    <div class="bui-task-row__copy">
      <strong>{{ title }}</strong>
      <span v-if="meta" class="bui-task-row__meta">{{ meta }}</span>
    </div>
    <span v-if="status" class="bui-task-row__status">{{ status }}</span>
    <div v-if="$slots.default" class="bui-task-row__extra">
      <slot />
    </div>
  </div>
</template>
