import { caminhoPermitido } from '../../utils/politicas'
import { comAutenticacao } from '../../utils/backend'
export default defineEventHandler(async (evento) => {
  const caminho = getRouterParam(evento, 'path') ?? ''
  if (!caminhoPermitido(caminho))
    throw createError({ statusCode: 404, statusMessage: 'Recurso inexistente.' })
  if (!['GET', 'POST', 'PUT', 'PATCH', 'DELETE'].includes(evento.method))
    throw createError({ statusCode: 405 })
  const query: Record<string, string> = {}
  for (const [nome, valor] of Object.entries(getQuery(evento)))
    if (typeof valor === 'string' || typeof valor === 'number') query[nome] = String(valor)
  return await comAutenticacao(evento, caminho, {
    method: evento.method as 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE',
    body: ['POST', 'PUT', 'PATCH'].includes(evento.method) ? await readBody(evento) : undefined,
    query,
  })
})
