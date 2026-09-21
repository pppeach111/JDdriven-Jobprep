<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    tone?: 'neutral' | 'accent' | 'good' | 'warn' | 'bad'
  }>(),
  { tone: 'neutral' },
)
</script>

<template>
  <span class="bui-status" :class="`bui-status--${tone}`">
    <span class="bui-status__dot" aria-hidden="true"></span>
    <span>{{ label }}</span>
  </span>
</template>
