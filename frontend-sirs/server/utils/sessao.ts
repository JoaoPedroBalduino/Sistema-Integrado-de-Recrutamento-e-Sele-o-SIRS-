import type { H3Event } from 'h3'
import { getCookie, setCookie, deleteCookie, createError } from 'h3'
import { cifrarSessao, decifrarSessao, type SessaoPrivada } from './criptografia'

function configuracaoCookie(evento: H3Event) {
  const configuracao = useRuntimeConfig(evento)
  const seguro = configuracao.appOrigin.startsWith('https://')
  return {
    nome: seguro ? '__Host-sirs-session' : 'sirs-session',
    seguro,
    segredo: configuracao.sessionSecret,
  }
}
export function lerSessao(evento: H3Event): SessaoPrivada | null {
  const configuracao = configuracaoCookie(evento)
  const cookie = getCookie(evento, configuracao.nome)
  return cookie ? decifrarSessao(cookie, configuracao.segredo) : null
}
export function salvarSessao(evento: H3Event, sessao: SessaoPrivada) {
  const configuracao = configuracaoCookie(evento)
  const valor = cifrarSessao(sessao, configuracao.segredo)
  if (valor.length > 3800)
    throw createError({
      statusCode: 502,
      statusMessage: 'Sessao recebida excede o tamanho permitido.',
    })
  setCookie(evento, configuracao.nome, valor, {
    httpOnly: true,
    secure: configuracao.seguro,
    sameSite: 'lax',
    path: '/',
    maxAge: sessao.refreshToken
      ? 604800
      : Math.max(1, Math.floor((Date.parse(sessao.expiresAt) - Date.now()) / 1000)),
  })
}
export function limparSessao(evento: H3Event) {
  const configuracao = configuracaoCookie(evento)
  deleteCookie(evento, configuracao.nome, {
    httpOnly: true,
    secure: configuracao.seguro,
    sameSite: 'lax',
    path: '/',
  })
}
