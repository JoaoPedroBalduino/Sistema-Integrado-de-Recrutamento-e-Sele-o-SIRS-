import { comAutenticacao } from '../../utils/backend'
import { limparSessao } from '../../utils/sessao'
export default defineEventHandler(async (evento) => {
  const corpo = await readBody(evento)
  if (
    typeof corpo?.senhaAtual !== 'string' ||
    typeof corpo?.novaSenha !== 'string' ||
    corpo.novaSenha.length < 12 ||
    corpo.novaSenha.length > 72
  )
    throw createError({
      statusCode: 400,
      statusMessage: 'A nova senha deve conter entre 12 e 72 caracteres.',
    })
  await comAutenticacao(evento, 'auth/senha', {
    method: 'PUT',
    body: { senhaAtual: corpo.senhaAtual, novaSenha: corpo.novaSenha },
  })
  limparSessao(evento)
  return { sucesso: true }
})
