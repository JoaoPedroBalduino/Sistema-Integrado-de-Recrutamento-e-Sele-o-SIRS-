<script setup lang="ts">
const propriedades = withDefaults(defineProps<{ juntoAoFormulario?: boolean }>(), {
  juntoAoFormulario: false,
})
const { mensagens, remover } = useNotificacoes()
const rota = useRoute()
const painel = ref<HTMLElement>()
const destino = shallowRef<string | HTMLElement>('body')
const visivel = computed(() => propriedades.juntoAoFormulario || rota.path !== '/login')
async function atualizarPosicao() {
  if (propriedades.juntoAoFormulario || !painel.value) return
  destino.value = document.querySelector<HTMLElement>('dialog[open]') ?? 'body'
  await nextTick()
  if (!painel.value) return
  painel.value.hidePopover()
  if (visivel.value && mensagens.value.length) painel.value.showPopover()
}
let observador: MutationObserver | undefined
onMounted(() => {
  watch([visivel, mensagens], atualizarPosicao, { immediate: true, deep: true, flush: 'post' })
  if (!propriedades.juntoAoFormulario) {
    observador = new MutationObserver(atualizarPosicao)
    observador.observe(document.body, {
      attributes: true,
      attributeFilter: ['open'],
      subtree: true,
    })
  }
})
onBeforeUnmount(() => observador?.disconnect())
</script>
<template>
  <Teleport :to="destino" :disabled="juntoAoFormulario">
    <div
      v-show="visivel"
      ref="painel"
      :popover="juntoAoFormulario ? undefined : 'manual'"
      :class="juntoAoFormulario ? 'form-messages' : 'corner-messages'"
      aria-label="Mensagens do sistema"
    >
      <SirsMensagem
        v-for="mensagem in visivel ? mensagens : []"
        :key="mensagem.identificador"
        :mensagem="mensagem"
        :junto-ao-formulario="juntoAoFormulario"
        @fechar="remover(mensagem.identificador)"
      />
    </div>
  </Teleport>
</template>
<style scoped>
.corner-messages {
  position: fixed;
  inset: auto;
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  overflow: auto;
  max-height: calc(100dvh - 40px);
  top: 20px;
  right: 20px;
  z-index: 1000;
  width: min(380px, calc(100vw - 32px));
  display: grid;
  gap: 10px;
  pointer-events: none;
}
.form-messages {
  display: grid;
  gap: 10px;
  margin: 20px 0;
}
@media (max-width: 600px) {
  .corner-messages {
    top: 12px;
    right: 16px;
  }
}
</style>
