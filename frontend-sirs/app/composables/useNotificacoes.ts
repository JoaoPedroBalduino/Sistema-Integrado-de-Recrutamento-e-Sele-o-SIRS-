import type { MensagemSistema, TipoMensagem } from '#shared/types/mensagens'
export function useNotificacoes() {
  const mensagens = useState<MensagemSistema[]>('notificacoes', () => [])
  const sequencia = useState<number>('notificacoes-sequencia', () => 0)
  const titulos: Record<TipoMensagem, string> = {
    informacao: 'Informação',
    sucesso: 'Operação concluída',
    erro: 'Não foi possível concluir',
    alerta: 'Atenção',
  }
  function remover(identificador: number) {
    mensagens.value = mensagens.value.filter((mensagem) => mensagem.identificador !== identificador)
  }
  function adicionar(texto: string, tipo: TipoMensagem = 'sucesso', titulo = titulos[tipo]) {
    const conteudo = texto.trim()
    if (!conteudo) return
    const existente = mensagens.value.find(
      (mensagem) =>
        mensagem.texto === conteudo && mensagem.tipo === tipo && mensagem.titulo === titulo,
    )
    if (existente) return existente.identificador
    const identificador = ++sequencia.value
    mensagens.value.push({ identificador, texto: conteudo, tipo, titulo })
    return identificador
  }
  return { mensagens, adicionar, remover }
}
