import type { ValoresFormulario } from '#shared/types/formulario'
export function useFormulario(
  operacao: (valores: ValoresFormulario) => Promise<unknown>,
  atualizar?: () => Promise<unknown>,
) {
  const aberto = ref(false)
  const salvando = ref(false)
  const erroFormulario = shallowRef<unknown>(null)
  const inicial = ref<Record<string, unknown> | undefined>()
  function abrir(valores?: object) {
    inicial.value = valores ? ({ ...valores } as Record<string, unknown>) : undefined
    erroFormulario.value = null
    aberto.value = true
  }
  function fechar() {
    if (!salvando.value) aberto.value = false
  }
  async function salvar(valores: ValoresFormulario) {
    salvando.value = true
    erroFormulario.value = null
    try {
      await operacao(valores)
      aberto.value = false
      useNotificacoes().adicionar('Informações salvas com sucesso.')
      await atualizar?.()
    } catch (erro) {
      erroFormulario.value = erro
    } finally {
      salvando.value = false
    }
  }
  return { aberto, salvando, erroFormulario, inicial, abrir, fechar, salvar }
}
