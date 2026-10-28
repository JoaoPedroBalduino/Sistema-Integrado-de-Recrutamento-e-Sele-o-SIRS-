<script setup lang="ts">
defineProps<{ error: { statusCode?: number; statusMessage?: string } }>()
</script>
<template>
  <div class="error-page">
    <SirsMarca variante="simbolo" />
    <p class="eyebrow">SIRS · {{ error.statusCode || 500 }}</p>
    <h1>
      {{
        error.statusCode === 403
          ? 'Este espaço exige outro perfil.'
          : error.statusCode === 404
            ? 'Esta página não foi encontrada.'
            : 'Vamos tentar novamente?'
      }}
    </h1>
    <p>{{ error.statusMessage || 'O serviço está temporariamente indisponível.' }}</p>
    <button class="button primary" @click="clearError({ redirect: '/' })">Voltar ao início</button>
  </div>
</template>
