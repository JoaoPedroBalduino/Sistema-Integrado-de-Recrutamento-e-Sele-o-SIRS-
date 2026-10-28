<script setup lang="ts">
defineProps<{
  titulo: string
  valor: string | number
  descricao?: string
  icone: string
  cor?: string
}>()
</script>
<template>
  <article class="metric-card">
    <div class="metric-top">
      <span>{{ titulo }}</span
      ><span class="metric-icon" :class="cor"><SirsIcone :nome="icone" :tamanho="20" /></span>
    </div>
    <strong>{{ valor }}</strong>
    <p v-if="descricao">{{ descricao }}</p>
  </article>
</template>
