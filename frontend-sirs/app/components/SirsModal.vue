<script setup lang="ts">
const props = defineProps<{ aberto: boolean; titulo: string }>()
const emitir = defineEmits<{ fechar: [] }>()
const identificadorTitulo = useId()
const janela = ref<HTMLDialogElement>()
watch(
  () => props.aberto,
  async (aberto) => {
    await nextTick()
    if (aberto && !janela.value?.open) janela.value?.showModal()
    if (!aberto) janela.value?.close()
  },
  { immediate: true },
)
onMounted(() => {
  if (props.aberto && !janela.value?.open) janela.value?.showModal()
})
</script>
<template>
  <dialog
    ref="janela"
    class="modal"
    :aria-labelledby="identificadorTitulo"
    @cancel.prevent="emitir('fechar')"
    @click="
      (evento) => {
        if (evento.target === janela) emitir('fechar')
      }
    "
  >
    <div class="modal-header">
      <div>
        <p class="eyebrow">SIRS · Gestão integrada</p>
        <h2 :id="identificadorTitulo">{{ titulo }}</h2>
      </div>
      <button class="icon-button" aria-label="Fechar janela" @click="emitir('fechar')">
        <SirsIcone nome="close" />
      </button>
    </div>
    <div class="modal-content"><slot /></div>
  </dialog>
</template>
