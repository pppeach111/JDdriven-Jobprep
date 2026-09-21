<script setup lang="ts">
/**
 * 空状态 —— 19-ui-design-system.md §8「初始空状态」的固定三段式：
 *   为什么为空 → 开始动作（action 插槽）→ 所需输入（requiredInput）。
 *
 * 为什么不直接在页面里写两行灰字：
 *   §8 要求空状态必须说明原因与所需输入，这是 6 个数据页面共同的语义，
 *   各页面分别实现会漏掉字段或写成不一致的文案。本组件只承载结构，
 *   不放插画、不放营销文案（§8 明确禁止）。
 */
withDefaults(
  defineProps<{
    /** 为空的事实描述，例如「这个目标下还没有岗位结果」。 */
    title: string
    /** 为什么为空。必须是可解释的原因，不能只重复 title。 */
    reason: string
    /** 需要用户先准备什么输入，才可能产出内容。 */
    requiredInput?: string | null
  }>(),
  { requiredInput: null },
)
</script>

<template>
  <section class="bui-empty">
    <h2 class="bui-empty__title">{{ title }}</h2>
    <p class="bui-empty__reason">{{ reason }}</p>
    <p v-if="requiredInput" class="bui-empty__req">
      <span class="bui-empty__req-label">需要先准备</span>
      <span>{{ requiredInput }}</span>
    </p>
    <div v-if="$slots.action" class="bui-empty__action">
      <slot name="action" />
    </div>
  </section>
</template>