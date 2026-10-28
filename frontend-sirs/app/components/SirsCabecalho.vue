<script setup lang="ts">
defineProps<{ titulo: string; descricao?: string; categoria?: string }>()
</script>
<template>
  <div class="page-heading">
    <div>
      <p v-if="categoria" class="eyebrow">{{ categoria }}</p>
      <h1>{{ titulo }}</h1>
      <p v-if="descricao" class="page-description">{{ descricao }}</p>
    </div>
    <div class="heading-actions"><slot /></div>
  </div>
</template>
