export default defineNuxtConfig({
  compatibilityDate: '2026-10-28',
  devtools: { enabled: false },
  css: ['@fontsource-variable/inter', '~/assets/css/main.css'],
  runtimeConfig: {
    backendBaseUrl: 'http://127.0.0.1:8080/api/v1',
    sessionSecret: '',
    appOrigin: 'http://127.0.0.1:3000',
  },
  app: {
    head: {
      htmlAttrs: { lang: 'pt-BR' },
      titleTemplate: '%s � SIRS',
      meta: [
        {
          name: 'description',
          content:
            'Sistema Integrado de Recrutamento e Sele��o. Pessoas, processos e decis�es em um s� lugar.',
        },
        { name: 'robots', content: 'noindex, nofollow' },
      ],
      link: [{ rel: 'icon', type: 'image/png', href: '/favicon.png' }],
    },
  },
  typescript: { strict: true },
  sourcemap: { client: false, server: false },
  nitro: { preset: 'node-server' },
})
