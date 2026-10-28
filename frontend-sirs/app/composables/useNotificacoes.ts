export function useNotificacoes() {
  const mensagens = useState<
    Array<{ identificador: number; texto: string; tipo: 'sucesso' | 'erro' }>
  >('notificacoes', () => [])
  function remover(identificador: number) {
    mensagens.value = mensagens.value.filter((mensagem) => mensagem.identificador !== identificador)
  }
  function adicionar(texto: string, tipo: 'sucesso' | 'erro' = 'sucesso') {
    const identificador = Date.now() + Math.random()
    mensagens.value.push({ identificador, texto, tipo })
    if (import.meta.client) setTimeout(() => remover(identificador), 6000)
  }
  return { mensagens, adicionar, remover }
}
