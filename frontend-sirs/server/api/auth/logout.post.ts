import { comAutenticacao } from '../../utils/backend'
import { limparSessao } from '../../utils/sessao'
export default defineEventHandler(async (evento) => {
  try {
    await comAutenticacao(evento, 'auth/logout', { method: 'POST' })
  } finally {
    limparSessao(evento)
  }
  return { sucesso: true }
})
