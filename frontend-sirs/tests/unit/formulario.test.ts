import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { ref, shallowRef, type Ref } from 'vue'
import { useFormulario } from '../../app/composables/useFormulario'
import { useNotificacoes } from '../../app/composables/useNotificacoes'

beforeEach(() => {
  const estados = new Map<string, Ref<unknown>>()
  vi.stubGlobal('useState', <T>(chave: string, inicializar: () => T) => {
    if (!estados.has(chave)) estados.set(chave, ref(inicializar()) as Ref<unknown>)
    return estados.get(chave) as Ref<T>
  })
  vi.stubGlobal('ref', ref)
  vi.stubGlobal('shallowRef', shallowRef)
  vi.stubGlobal('useNotificacoes', useNotificacoes)
  vi.stubGlobal('descreverErro', () => ({ mensagem: 'Falha ao gravar.', campos: {} }))
})
afterEach(() => vi.unstubAllGlobals())

describe('Mensagens do formulário', () => {
  it('confirma sucesso somente após salvar e atualizar a lista', async () => {
    const atualizar = vi.fn().mockResolvedValue(undefined)
    const formulario = useFormulario(vi.fn().mockResolvedValue(undefined), atualizar)
    formulario.abrir({ nome: 'Registro' })
    await formulario.salvar({ nome: 'Registro' })
    expect(atualizar).toHaveBeenCalledOnce()
    expect(formulario.aberto.value).toBe(false)
    expect(formulario.salvando.value).toBe(false)
    expect(useNotificacoes().mensagens.value[0]?.tipo).toBe('sucesso')
  })
  it('mantém o formulário aberto quando a gravação falha', async () => {
    const atualizar = vi.fn()
    const falha = new Error('Gravação rejeitada')
    const formulario = useFormulario(vi.fn().mockRejectedValue(falha), atualizar)
    formulario.abrir({ nome: 'Registro' })
    await formulario.salvar({ nome: 'Registro' })
    expect(formulario.aberto.value).toBe(true)
    expect(formulario.inicial.value?.nome).toBe('Registro')
    expect(formulario.erroFormulario.value).toBe(falha)
    expect(formulario.salvando.value).toBe(false)
    expect(atualizar).not.toHaveBeenCalled()
    expect(useNotificacoes().mensagens.value[0]?.tipo).toBe('erro')
  })
  it('distingue uma gravação bem-sucedida da falha posterior de atualização', async () => {
    const formulario = useFormulario(
      vi.fn().mockResolvedValue(undefined),
      vi.fn().mockRejectedValue(new Error('Consulta indisponível')),
    )
    formulario.abrir()
    await formulario.salvar({ nome: 'Registro' })
    expect(formulario.aberto.value).toBe(false)
    expect(formulario.erroFormulario.value).toBeNull()
    const mensagens = useNotificacoes().mensagens.value
    expect(mensagens).toHaveLength(1)
    expect(mensagens[0]).toMatchObject({
      tipo: 'alerta',
      titulo: 'Dados salvos; atualização pendente',
    })
    expect(mensagens[0]!.texto).toContain('As informações foram salvas')
  })
})
