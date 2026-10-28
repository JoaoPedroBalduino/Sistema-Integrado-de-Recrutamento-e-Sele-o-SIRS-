export default defineNitroPlugin(() => {
  const configuracao = useRuntimeConfig()
  if (configuracao.sessionSecret.length < 32)
    throw new Error(
      'Configure NUXT_SESSION_SECRET com pelo menos 32 caracteres no ambiente privado.',
    )
  const origem = new URL(configuracao.appOrigin)
  const backend = new URL(configuracao.backendBaseUrl)
  if (
    !['http:', 'https:'].includes(origem.protocol) ||
    origem.username ||
    origem.password ||
    origem.pathname !== '/'
  )
    throw new Error('NUXT_APP_ORIGIN deve conter apenas a origem da aplicacao.')
  if (
    !['http:', 'https:'].includes(backend.protocol) ||
    backend.username ||
    backend.password ||
    backend.search ||
    backend.hash
  )
    throw new Error('NUXT_BACKEND_BASE_URL invalida.')
  if (
    process.env.NODE_ENV === 'production' &&
    !['localhost', '127.0.0.1', '[::1]'].includes(origem.hostname) &&
    origem.protocol !== 'https:'
  )
    throw new Error('Use HTTPS para a origem publica da aplicacao.')
})
