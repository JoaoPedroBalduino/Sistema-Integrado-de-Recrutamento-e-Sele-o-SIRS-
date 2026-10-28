<script setup lang="ts">
import type { MensagemSistema, TipoMensagem } from '#shared/types/mensagens'
const propriedades = defineProps<{ mensagem: MensagemSistema; juntoAoFormulario?: boolean }>()
const emitir = defineEmits<{ fechar: [] }>()
const identificador = useId()
const icones: Record<TipoMensagem, string> = {
  informacao: 'info',
  sucesso: 'success',
  erro: 'error',
  alerta: 'warning',
}
let temporizador: ReturnType<typeof setTimeout> | undefined
function pausar() {
  clearTimeout(temporizador)
}
function programarFechamento() {
  pausar()
  if (propriedades.mensagem.tipo === 'sucesso' && !propriedades.juntoAoFormulario)
    temporizador = setTimeout(() => emitir('fechar'), 8000)
}
onMounted(() => {
  programarFechamento()
})
onBeforeUnmount(pausar)
</script>
<template>
  <article
    class="message-notice"
    :class="{ 'message-floating': !juntoAoFormulario }"
    :data-tipo="mensagem.tipo"
    :role="mensagem.tipo === 'erro' || mensagem.tipo === 'alerta' ? 'alert' : 'status'"
    :aria-labelledby="identificador + '-titulo'"
    :aria-describedby="identificador + '-descricao'"
    aria-atomic="true"
    @mouseenter="pausar"
    @mouseleave="programarFechamento"
    @focusin="pausar"
    @focusout="programarFechamento"
  >
    <SirsIcone class="message-icon" :nome="icones[mensagem.tipo]" :tamanho="20" />
    <div class="message-content">
      <h2 :id="identificador + '-titulo'">{{ mensagem.titulo }}</h2>
      <p :id="identificador + '-descricao'">{{ mensagem.texto }}</p>
    </div>
    <button
      class="message-close"
      type="button"
      aria-label="Fechar mensagem"
      @click="emitir('fechar')"
    >
      <SirsIcone nome="close" :tamanho="17" />
    </button>
  </article>
</template>
<style scoped>
.message-notice {
  --notice-color: #6552c4;
  --notice-border: #e1dcf4;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  box-sizing: border-box;
  padding: 16px;
  border: 1px solid var(--notice-border);
  border-left: 3px solid var(--notice-color);
  border-radius: 10px;
  background: #fff;
  color: #29334a;
  pointer-events: auto;
}
.message-floating {
  box-shadow: 0 8px 28px #18233a14;
}
.message-notice[data-tipo='sucesso'] {
  --notice-color: #18734f;
  --notice-border: #cce6d9;
}
.message-notice[data-tipo='erro'] {
  --notice-color: #b53b4a;
  --notice-border: #f0d4da;
}
.message-notice[data-tipo='alerta'] {
  --notice-color: #986619;
  --notice-border: #eeddbb;
}
.message-icon {
  flex-shrink: 0;
  margin-top: 1px;
  color: var(--notice-color);
}
.message-content {
  flex: 1;
  min-width: 0;
}
h2 {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
}
p {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.65;
  color: #68748c;
  overflow-wrap: anywhere;
  white-space: pre-line;
}
.message-close {
  display: inline-flex;
  flex-shrink: 0;
  border: 0;
  padding: 4px;
  margin: -4px -4px 0 0;
  border-radius: 5px;
  background: transparent;
  color: #7b859a;
  cursor: pointer;
}
.message-close:hover {
  background: #f0f2f7;
  color: #29334a;
}
.message-close:focus-visible {
  outline: 2px solid #8675d9;
  outline-offset: 3px;
}
</style>
