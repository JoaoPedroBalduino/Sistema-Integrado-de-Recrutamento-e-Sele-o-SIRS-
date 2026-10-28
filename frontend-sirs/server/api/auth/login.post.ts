import type { RespostaAutenticacao } from '#shared/types/api'
import { chamarBackend, registrarAutenticacao } from '../../utils/backend'
export default defineEventHandler(async (evento) => {
  const corpo = await readBody(evento)
  if (
    typeof corpo?.email !== 'string' ||
    typeof corpo?.senha !== 'string' ||
    corpo.email.length > 255 ||
    corpo.senha.length > 72
  )
    throw createError({ statusCode: 400, statusMessage: 'Informe email e senha validos.' })
  const resposta = await chamarBackend<RespostaAutenticacao>(evento, 'auth/login', {
    method: 'POST',
    body: { email: corpo.email, senha: corpo.senha },
  })
  return registrarAutenticacao(evento, resposta)
})
