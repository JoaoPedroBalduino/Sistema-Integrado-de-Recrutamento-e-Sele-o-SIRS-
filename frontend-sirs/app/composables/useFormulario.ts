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
    } catch (erro) {
      erroFormulario.value = erro
      useNotificacoes().adicionar(descreverErro(erro).mensagem, 'erro')
      return
    } finally {
      salvando.value = false
    }
    aberto.value = false
    try {
      await atualizar?.()
      useNotificacoes().adicionar('Informações salvas com sucesso.')
    } catch {
      useNotificacoes().adicionar(
        'As informações foram salvas, mas a lista não foi atualizada. Recarregue a página para consultar os dados mais recentes.',
        'alerta',
        'Dados salvos; atualização pendente',
      )
    }
  }
  return { aberto, salvando, erroFormulario, inicial, abrir, fechar, salvar }
}
