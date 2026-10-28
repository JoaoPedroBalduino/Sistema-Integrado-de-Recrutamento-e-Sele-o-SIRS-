<script setup lang="ts">
const { mensagens, remover } = useNotificacoes()
</script>
<template>
  <div class="toast-stack" aria-live="polite">
    <div
      v-for="mensagem in mensagens"
      :key="mensagem.identificador"
      class="toast"
      :class="mensagem.tipo"
    >
      <SirsIcone :nome="mensagem.tipo === 'sucesso' ? 'success' : 'alert'" /><span>{{
        mensagem.texto
      }}</span
      ><button
        class="icon-button"
        aria-label="Fechar notificação"
        @click="remover(mensagem.identificador)"
      >
        <SirsIcone nome="close" :tamanho="16" />
      </button>
    </div>
  </div>
</template>
