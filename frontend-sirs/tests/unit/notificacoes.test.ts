import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { ref, type Ref } from 'vue'
import { useNotificacoes } from '../../app/composables/useNotificacoes'

const estados = new Map<string, Ref<unknown>>()
beforeEach(() => {
  estados.clear()
  vi.stubGlobal('useState', <T>(chave: string, inicializar: () => T) => {
    if (!estados.has(chave)) estados.set(chave, ref(inicializar()) as Ref<unknown>)
    return estados.get(chave) as Ref<T>
  })
})
afterEach(() => {
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

describe('Fila de mensagens do sistema', () => {
  it('usa sucesso como padrão, com título e identificador serializáveis', () => {
    const notificacoes = useNotificacoes()
    expect(notificacoes.adicionar('Registro salvo.')).toBe(1)
    expect(notificacoes.mensagens.value[0]).toEqual({
      identificador: 1,
      texto: 'Registro salvo.',
      tipo: 'sucesso',
      titulo: 'Operação concluída',
    })
  })
  it.each(['informacao', 'sucesso', 'erro', 'alerta'] as const)(
    'aceita mensagens de %s',
    (tipo) => {
      const notificacoes = useNotificacoes()
      notificacoes.adicionar('Mensagem', tipo, 'Título específico')
      expect(notificacoes.mensagens.value[0]).toMatchObject({ tipo, titulo: 'Título específico' })
    },
  )
  it('não repete mensagens idênticas que ainda aguardam leitura', () => {
    const notificacoes = useNotificacoes()
    const identificador = notificacoes.adicionar(' Falha. ', 'erro')
    expect(notificacoes.adicionar('Falha.', 'erro')).toBe(identificador)
    expect(notificacoes.mensagens.value).toHaveLength(1)
  })
  it('preserva mensagens distintas com o mesmo texto', () => {
    const notificacoes = useNotificacoes()
    notificacoes.adicionar('Mensagem', 'erro')
    notificacoes.adicionar('Mensagem', 'alerta')
    expect(notificacoes.mensagens.value).toHaveLength(2)
  })
  it('entrega a próxima mensagem após a anterior ser removida', () => {
    const notificacoes = useNotificacoes()
    const primeira = notificacoes.adicionar('Primeira')!
    notificacoes.adicionar('Segunda', 'informacao')
    notificacoes.remover(primeira)
    expect(notificacoes.mensagens.value.map((mensagem) => mensagem.texto)).toEqual(['Segunda'])
  })
  it('compartilha a fila entre chamadas sem usar variáveis globais do módulo', () => {
    useNotificacoes().adicionar('Uma mensagem')
    expect(useNotificacoes().mensagens.value).toHaveLength(1)
  })
  it('ignora mensagens vazias', () => {
    const notificacoes = useNotificacoes()
    expect(notificacoes.adicionar('  ')).toBeUndefined()
    expect(notificacoes.mensagens.value).toHaveLength(0)
  })
  it('aguarda o usuário, sem descartar a mensagem por tempo', () => {
    vi.useFakeTimers()
    const notificacoes = useNotificacoes()
    notificacoes.adicionar('Leia esta mensagem', 'alerta')
    vi.advanceTimersByTime(60000)
    expect(notificacoes.mensagens.value).toHaveLength(1)
  })
  it('mantém a fila ao remover um identificador inexistente', () => {
    const notificacoes = useNotificacoes()
    notificacoes.adicionar('Mensagem')
    notificacoes.remover(99)
    expect(notificacoes.mensagens.value).toHaveLength(1)
  })
})
