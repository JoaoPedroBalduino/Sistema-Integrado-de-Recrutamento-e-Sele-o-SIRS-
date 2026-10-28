<script setup lang="ts">
const { mensagens, remover } = useNotificacoes()
const mensagemAtual = computed(() => mensagens.value[0])
</script>
<template>
  <SirsMensagem
    v-if="mensagemAtual"
    :key="mensagemAtual.identificador"
    :mensagem="mensagemAtual"
    @fechar="remover(mensagemAtual.identificador)"
  />
</template>
