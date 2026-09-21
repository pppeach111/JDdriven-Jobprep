<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'

/**
 * 页面标题区 —— 19-ui-design-system.md §5.1 的固定结构：
 *   上下文/返回关系 → 标题与一句说明 → 状态元信息 → 右侧主操作。
 *
 * 该结构在 6 个一级页面中完全一致，因此抽成共享适配，
 * 避免各页面重复实现「大标题 + 副标题」并产生不一致的视觉语言。
 *
 * 约束：
 *   - 标题区最多两个按钮（§5.1），调用方通过 actions 插槽传入，本组件不添加额外动作；
 *   - 状态元信息（数据新鲜度、来源、演示数据标记等）放在 meta 插槽，
 *     使「状态必须诚实」（§2.4）成为结构的一部分，而不是页面的可选装饰。
 */
withDefaults(
  defineProps<{
    /** 页面的上下文关系，例如「目标建立 · 步骤 3 / 3」。 */
    context?: string | null
    /** 返回目标路径；提供时渲染返回关系。 */
    backTo?: string | null
    /** 返回动作的文案，使用「动词 + 对象」（§6.5）。 */
    backLabel?: string
    title: string
    /** 标题下的一句话说明。 */
    description?: string | null
  }>(),
  { context: null, backTo: null, backLabel: '返回', description: null },
)
</script>

<template>
  <header class="bui-page-header">
    <div v-if="backTo || context" class="bui-page-header__context">
      <RouterLink v-if="backTo" :to="backTo" class="bui-page-header__back">
        <AppIcon name="arrow-right" :size="12" class="bui-page-header__back-icon" />
        <span>{{ backLabel }}</span>
      </RouterLink>
      <span v-if="context" class="bui-page-header__crumb">{{ context }}</span>
    </div>

    <div class="bui-page-header__row">
      <div class="bui-page-header__main">
        <h1 class="bui-page-header__title">{{ title }}</h1>
        <p v-if="description" class="bui-page-header__desc">{{ description }}</p>
      </div>

      <div v-if="$slots.actions" class="bui-page-header__actions">
        <slot name="actions" />
      </div>
    </div>

    <div v-if="$slots.meta" class="bui-page-header__meta">
      <slot name="meta" />
    </div>
  </header>
</template>