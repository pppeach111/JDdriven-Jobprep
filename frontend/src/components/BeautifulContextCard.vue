<script setup lang="ts">
defineProps<{
  eyebrow: string
  text: string
  source?: string | null
  tone?: 'neutral' | 'warn' | 'good'
}>()
</script>

<template>
  <article class="bui-context" :class="tone ? `bui-context--${tone}` : undefined">
    <div class="bui-context__head">
      <span class="bui-kicker">{{ eyebrow }}</span>
      <span v-if="source" class="bui-context__source mono">{{ source }}</span>
    </div>
    <p class="bui-context__text">{{ text }}</p>
    <slot />
  </article>
</template>
