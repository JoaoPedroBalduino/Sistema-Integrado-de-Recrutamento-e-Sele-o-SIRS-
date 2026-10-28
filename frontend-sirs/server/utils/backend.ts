import type { H3Event } from 'h3'
import { createError } from 'h3'
import { createHash } from 'node:crypto'
import type { RespostaAutenticacao } from '#shared/types/api'
import type { SessaoPrivada } from './criptografia'
import { lerSessao, limparSessao, salvarSessao } from './sessao'

interface OpcoesBackend {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: Record<string, unknown>
  query?: Record<string, string | number | boolean | undefined>
}
interface FalhaBackend {
  statusCode?: number
  response?: { status?: number }
  data?: { detail?: string; errors?: Record<string, string>; requestId?: string }
}
const renovacoes = new Map<string, { promessa: Promise<RespostaAutenticacao>; criadaEm: number }>()

export async function chamarBackend<T>(
  evento: H3Event,
  caminho: string,
  opcoes: OpcoesBackend = {},
  token?: string,
): Promise<T> {
  try {
    return (await $fetch<T>(
      useRuntimeConfig(evento).backendBaseUrl.replace(/\/$/, '') + '/' + caminho,
      {
        ...opcoes,
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
        timeout: 20000,
        retry: 0,
        redirect: 'error',
      },
    )) as T
  } catch (falha) {
    const erro = falha as FalhaBackend
    const status = erro.response?.status ?? erro.statusCode ?? 503
    throw createError({
      statusCode: status >= 400 && status <= 599 ? status : 503,
      statusMessage:
        status === 401
          ? 'Autenticacao necessaria.'
          : status === 403
            ? 'Acesso negado.'
            : 'Falha ao consultar o SIRS.',
      data: {
        detail:
          erro.data?.detail ??
          (status === 503
            ? 'O backend esta indisponivel. Tente novamente em instantes.'
            : 'A operacao nao foi concluida.'),
        errors: erro.data?.errors,
        requestId: erro.data?.requestId,
      },
    })
  }
}
function privada(resposta: RespostaAutenticacao): SessaoPrivada {
  return {
    accessToken: resposta.accessToken,
    refreshToken: resposta.refreshToken ?? null,
    expiresAt: resposta.expiresAt,
  }
}
export function registrarAutenticacao(evento: H3Event, resposta: RespostaAutenticacao) {
  salvarSessao(evento, privada(resposta))
  return { usuario: resposta.user }
}

async function renovar(evento: H3Event, sessao: SessaoPrivada) {
  if (!sessao.refreshToken)
    throw createError({ statusCode: 401, statusMessage: 'Sessao expirada.' })
  const agora = Date.now()
  for (const [chave, entrada] of renovacoes)
    if (agora - entrada.criadaEm > 10000) renovacoes.delete(chave)
  if (renovacoes.size > 500) renovacoes.clear()
  const chave = createHash('sha256').update(sessao.refreshToken).digest('hex')
  let entrada = renovacoes.get(chave)
  if (!entrada) {
    entrada = {
      criadaEm: agora,
      promessa: chamarBackend<RespostaAutenticacao>(evento, 'auth/refresh', {
        method: 'POST',
        body: { refreshToken: sessao.refreshToken },
      }),
    }
    renovacoes.set(chave, entrada)
    entrada.promessa.catch(() => renovacoes.delete(chave))
  }
  const resposta = await entrada.promessa
  salvarSessao(evento, privada(resposta))
  return privada(resposta)
}

export async function comAutenticacao<T>(
  evento: H3Event,
  caminho: string,
  opcoes: OpcoesBackend = {},
): Promise<T> {
  let sessao = lerSessao(evento)
  if (!sessao) throw createError({ statusCode: 401, statusMessage: 'Entre para continuar.' })
  try {
    if (Date.parse(sessao.expiresAt) <= Date.now() + 15000) sessao = await renovar(evento, sessao)
    try {
      return await chamarBackend<T>(evento, caminho, opcoes, sessao.accessToken)
    } catch (erro) {
      if ((erro as FalhaBackend).statusCode !== 401) throw erro
      sessao = await renovar(evento, sessao)
      return await chamarBackend<T>(evento, caminho, opcoes, sessao.accessToken)
    }
  } catch (erro) {
    if ((erro as FalhaBackend).statusCode === 401) limparSessao(evento)
    throw erro
  }
}
