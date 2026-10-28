<script setup lang="ts">
import { nomesStatus, nomesPerfis } from '~/utils/formatacao'
defineProps<{ valor: string }>()
</script>
<template>
  <span class="badge" :data-status="valor"
    ><span class="badge-dot" />{{ nomesStatus[valor] || nomesPerfis[valor] || valor }}</span
  >
</template>
