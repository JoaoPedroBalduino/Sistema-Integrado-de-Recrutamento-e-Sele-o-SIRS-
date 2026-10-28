import type { RespostaUsuario, Perfil } from '#shared/types/api'
import { possuiPerfil } from '~/utils/permissoes'
export function useAutenticacao() {
  const usuario = useState<RespostaUsuario | null>('usuario-atual', () => null)
  const inicializada = useState('autenticacao-inicializada', () => false)
  const consulta = useRequestFetch()
  async function inicializar() {
    if (inicializada.value) return
    const cabecalhoCookie = import.meta.server ? useResponseHeader('set-cookie') : null
    const resposta = await consulta<{ usuario: RespostaUsuario | null }>('/api/auth/session', {
      onResponse({ response }) {
        const cookie = response.headers.get('set-cookie')
        if (cookie && cabecalhoCookie) cabecalhoCookie.value = cookie
      },
    })
    usuario.value = resposta.usuario
    inicializada.value = true
  }
  async function entrar(email: string, senha: string) {
    const resposta = await $fetch<{ usuario: RespostaUsuario }>('/api/auth/login', {
      method: 'POST',
      body: { email, senha },
    })
    usuario.value = resposta.usuario
    inicializada.value = true
  }
  async function sair() {
    try {
      await $fetch('/api/auth/logout', { method: 'POST', body: {} })
    } finally {
      usuario.value = null
      inicializada.value = true
      await navigateTo('/login')
    }
  }
  return {
    usuario,
    inicializar,
    entrar,
    sair,
    permite: (perfis: readonly Perfil[]) => possuiPerfil(usuario.value?.perfil, perfis),
  }
}
