<script setup lang="ts">
defineProps<{ pagina: number; totalPaginas: number; total: number }>()
defineEmits<{ mudar: [pagina: number] }>()
</script>
<template>
  <nav class="pagination" aria-label="Paginação">
    <span
      >{{ total }} {{ total === 1 ? 'registro' : 'registros' }} · Página
      {{ totalPaginas ? pagina + 1 : 0 }} de {{ totalPaginas }}</span
    >
    <div>
      <button
        class="button secondary compact"
        :disabled="pagina <= 0"
        @click="$emit('mudar', pagina - 1)"
      >
        <SirsIcone nome="back" :tamanho="15" />Anterior</button
      ><button
        class="button secondary compact"
        :disabled="pagina + 1 >= totalPaginas"
        @click="$emit('mudar', pagina + 1)"
      >
        Próxima<SirsIcone nome="next" :tamanho="15" />
      </button>
    </div>
  </nav>
</template>
