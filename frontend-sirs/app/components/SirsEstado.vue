<script setup lang="ts">
defineProps<{
  carregando?: boolean
  erro?: unknown
  vazio?: boolean
  titulo?: string
  descricao?: string
}>()
defineEmits<{ tentar: [] }>()
</script>
<template>
  <div v-if="carregando" class="state-panel" role="status">
    <SirsIcone nome="loading" class="spin" :tamanho="26" />
    <p>Carregando informações…</p>
  </div>
  <div v-else-if="erro" class="state-panel error-state" role="alert">
    <SirsIcone nome="alert" :tamanho="30" />
    <h3>Não foi possível carregar os dados</h3>
    <p>{{ descreverErro(erro).mensagem }}</p>
    <button class="button secondary" @click="$emit('tentar')">
      <SirsIcone nome="refresh" :tamanho="16" />Tentar novamente
    </button>
  </div>
  <div v-else-if="vazio" class="state-panel">
    <span class="empty-icon"><SirsIcone nome="layers" :tamanho="28" /></span>
    <h3>{{ titulo || 'Ainda não há registros' }}</h3>
    <p>{{ descricao || 'Os registros aparecerão aqui assim que forem cadastrados.' }}</p>
    <slot name="acao" />
  </div>
  <slot v-else />
</template>
