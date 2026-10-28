<script setup lang="ts">
import type { MensagemSistema, TipoMensagem } from '#shared/types/mensagens'
const propriedades = defineProps<{ mensagem: MensagemSistema }>()
const emitir = defineEmits<{ fechar: [] }>()
const identificador = useId()
const janela = ref<HTMLDialogElement>()
const botaoContinuar = ref<HTMLButtonElement>()
const icones: Record<TipoMensagem, string> = {
  informacao: 'info',
  sucesso: 'success',
  erro: 'error',
  alerta: 'warning',
}
let elementoAnterior: HTMLElement | null = null
onMounted(() => {
  elementoAnterior = document.activeElement instanceof HTMLElement ? document.activeElement : null
  if (!elementoAnterior || elementoAnterior === document.body) {
    elementoAnterior = document.querySelector<HTMLElement>(
      'dialog[open] button[type="submit"]:not(:disabled), main button[type="submit"]:not(:disabled)',
    )
  }
  janela.value?.showModal()
  botaoContinuar.value?.focus()
})
function fechar() {
  janela.value?.close()
  emitir('fechar')
  if (
    elementoAnterior?.isConnected &&
    elementoAnterior.getClientRects().length &&
    !elementoAnterior.matches(':disabled')
  ) {
    elementoAnterior.focus({ preventScroll: true })
  } else {
    const conteudo = document.querySelector<HTMLElement>('main')
    if (conteudo) {
      conteudo.tabIndex = -1
      conteudo.focus({ preventScroll: true })
    }
  }
}
function clicarFora(evento: MouseEvent) {
  if (evento.target !== janela.value || !janela.value) return
  const limites = janela.value.getBoundingClientRect()
  if (
    evento.clientX < limites.left ||
    evento.clientX > limites.right ||
    evento.clientY < limites.top ||
    evento.clientY > limites.bottom
  )
    fechar()
}
onBeforeUnmount(() => janela.value?.close())
</script>
<template>
  <dialog
    ref="janela"
    class="message-dialog"
    :data-tipo="mensagem.tipo"
    :role="mensagem.tipo === 'erro' || mensagem.tipo === 'alerta' ? 'alertdialog' : 'dialog'"
    aria-modal="true"
    :aria-labelledby="identificador + '-titulo'"
    :aria-describedby="identificador + '-descricao'"
    @cancel.prevent="fechar"
    @click="clicarFora"
  >
    <button
      class="message-close icon-button"
      type="button"
      aria-label="Fechar mensagem"
      @click="fechar"
    >
      <SirsIcone nome="close" :tamanho="19" />
    </button>
    <div class="message-body">
      <span class="message-icon"
        ><SirsIcone :nome="icones[propriedades.mensagem.tipo]" :tamanho="27"
      /></span>
      <p class="message-label">SIRS · MENSAGEM DO SISTEMA</p>
      <h2 :id="identificador + '-titulo'">{{ mensagem.titulo }}</h2>
      <p :id="identificador + '-descricao'" class="message-description">{{ mensagem.texto }}</p>
    </div>
    <footer class="message-footer">
      <span>Você pode continuar após fechar esta mensagem.</span
      ><button ref="botaoContinuar" type="button" class="button primary" @click="fechar">
        Entendi<SirsIcone nome="check" :tamanho="16" />
      </button>
    </footer>
  </dialog>
</template>
<style scoped>
.message-dialog {
  --message-color: #6653c5;
  --message-surface: #efecfc;
  width: min(460px, calc(100vw - 32px));
  max-height: calc(100dvh - 40px);
  margin: auto;
  padding: 0;
  border: 1px solid #e1e5ee;
  border-radius: 18px;
  color: #29334a;
  background: #fff;
  box-shadow: 0 24px 90px #16132a33;
  overflow-y: auto;
}
.message-dialog::backdrop {
  background: #171c365e;
  backdrop-filter: blur(3px);
}
.message-dialog[data-tipo='sucesso'] {
  --message-color: #18734f;
  --message-surface: #e8f5ee;
}
.message-dialog[data-tipo='erro'] {
  --message-color: #b53b4a;
  --message-surface: #fcedef;
}
.message-dialog[data-tipo='alerta'] {
  --message-color: #9b6b19;
  --message-surface: #fff4dc;
}
.message-close {
  position: absolute;
  top: 16px;
  right: 16px;
}
.message-body {
  padding: 34px 30px 30px;
}
.message-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  border-radius: 17px;
  color: var(--message-color);
  background: var(--message-surface);
  margin-bottom: 24px;
}
.message-label {
  color: #7e879a;
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.8px;
  margin-bottom: 10px;
}
h2 {
  font-size: 23px;
  font-weight: 600;
  line-height: 1.3;
  letter-spacing: -0.5px;
  padding-right: 10px;
}
.message-description {
  margin-top: 14px;
  color: #68748c;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-line;
  overflow-wrap: anywhere;
}
.message-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22px;
  padding: 20px 28px;
  background: #f8f9fc;
  border-top: 1px solid #edf0f5;
}
.message-footer > span {
  color: #7b859a;
  font-size: 10px;
  line-height: 1.7;
  max-width: 175px;
}
.message-footer .button {
  min-height: 42px;
  min-width: 110px;
}
.message-footer button:focus-visible {
  outline: 3px solid #b9b0ee;
  outline-offset: 3px;
}
@media (max-width: 480px) {
  .message-body {
    padding: 28px 24px;
  }
  .message-footer {
    padding: 18px 24px;
    gap: 12px;
  }
  h2 {
    font-size: 21px;
  }
  .message-footer > span {
    max-width: 130px;
  }
}
</style>
