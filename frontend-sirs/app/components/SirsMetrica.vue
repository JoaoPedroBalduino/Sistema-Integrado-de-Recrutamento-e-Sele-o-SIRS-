<script setup lang="ts">
defineProps<{
  titulo: string
  valor: string | number
  descricao?: string
}>()
</script>
<template>
  <article class="metric-card">
    <div class="metric-top">
      <span>{{ titulo }}</span>
    </div>
    <strong>{{ valor }}</strong>
    <p v-if="descricao">{{ descricao }}</p>
  </article>
</template>
