import type { $Fetch } from 'ofetch'
export interface ErroFormulario {
  mensagem: string
  campos: Record<string, string>
}
export function descreverErro(erro: unknown): ErroFormulario {
  const falha = erro as {
    data?: {
      data?: { detail?: string; errors?: Record<string, string> }
      statusMessage?: string
      detail?: string
    }
  }
  return {
    mensagem:
      falha?.data?.data?.detail ??
      falha?.data?.detail ??
      falha?.data?.statusMessage ??
      'Não foi possível concluir a operação. Tente novamente.',
    campos: falha?.data?.data?.errors ?? {},
  }
}
export function useApiSirs() {
  const consulta = useRequestFetch() as $Fetch
  return async <Resposta>(
    caminho: string,
    opcoes: {
      method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
      body?: Record<string, unknown>
      query?: Record<string, string | number | boolean | undefined>
    } = {},
  ) => {
    try {
      return (await consulta<Resposta>('/api/backend/' + caminho, {
        retry: 0,
        ...opcoes,
      })) as Resposta
    } catch (erro) {
      if (import.meta.client && (erro as { statusCode?: number }).statusCode === 401) {
        useState('usuario-atual').value = null
        await navigateTo('/login?expirada=1')
      }
      throw erro
    }
  }
}
