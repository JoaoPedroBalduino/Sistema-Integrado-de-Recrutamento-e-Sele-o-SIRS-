import type { Perfil } from '#shared/types/api'
export default defineNuxtRouteMiddleware(async (destino) => {
  const autenticacao = useAutenticacao()
  await autenticacao.inicializar()
  if (destino.path === '/login') {
    if (autenticacao.usuario.value) return navigateTo('/')
    return
  }
  if (!autenticacao.usuario.value)
    return navigateTo({ path: '/login', query: { destino: destino.fullPath } })
  const perfis = destino.meta.perfis as Perfil[] | undefined
  if (perfis && !autenticacao.permite(perfis))
    return abortNavigation(
      createError({
        statusCode: 403,
        statusMessage: 'Acesso negado.',
        message: 'Seu perfil não possui acesso a esta página.',
      }),
    )
})
