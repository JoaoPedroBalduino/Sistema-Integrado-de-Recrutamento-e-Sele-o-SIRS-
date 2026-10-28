import { origemPermitida } from '../utils/politicas'
export default defineEventHandler((evento) => {
  setResponseHeaders(evento, {
    'X-Content-Type-Options': 'nosniff',
    'X-Frame-Options': 'DENY',
    'Referrer-Policy': 'strict-origin-when-cross-origin',
    'Permissions-Policy': 'camera=(), microphone=(), geolocation=()',
  })
  if (!evento.path.startsWith('/_nuxt/'))
    setResponseHeader(evento, 'Cache-Control', 'private, no-store')
  if (!evento.path.startsWith('/api/')) return
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(evento.method)) {
    if (!origemPermitida(getHeader(evento, 'origin'), useRuntimeConfig(evento).appOrigin))
      throw createError({ statusCode: 403, statusMessage: 'Origem da requisicao nao autorizada.' })
    if (Number(getHeader(evento, 'content-length') ?? 0) > 1048576)
      throw createError({ statusCode: 413, statusMessage: 'Requisicao muito grande.' })
    if (
      ['POST', 'PUT', 'PATCH'].includes(evento.method) &&
      !getHeader(evento, 'content-type')?.startsWith('application/json')
    )
      throw createError({ statusCode: 415, statusMessage: 'Envie JSON.' })
  }
})
