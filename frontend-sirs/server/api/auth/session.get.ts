import type { RespostaUsuario } from '#shared/types/api'
import { comAutenticacao } from '../../utils/backend'
import { lerSessao } from '../../utils/sessao'
export default defineEventHandler(async (evento) => {
  if (!lerSessao(evento)) return { usuario: null }
  try {
    return { usuario: await comAutenticacao<RespostaUsuario>(evento, 'auth/me') }
  } catch (erro) {
    if ((erro as { statusCode?: number }).statusCode === 401) return { usuario: null }
    throw erro
  }
})
