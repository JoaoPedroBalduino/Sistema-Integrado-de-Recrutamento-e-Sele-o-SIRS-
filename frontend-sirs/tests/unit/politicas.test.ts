import { describe, it, expect } from 'vitest'
import { caminhoPermitido, origemPermitida } from '../../server/utils/politicas'
import { destinoInterno } from '../../shared/utils/navegacao'
describe('Fronteira entre navegador e backend', () => {
  it.each([
    'vagas',
    'vagas/1',
    'vagas/12/etapas',
    'candidaturas/2/avaliacoes',
    'cadastros/areas',
    'privacidade/solicitacoes',
  ])('permite recurso interno %s', (caminho) => expect(caminhoPermitido(caminho)).toBe(true))
  it.each([
    'https://outro.site',
    '../usuarios',
    'vagas/../usuarios',
    'vagas/%2e%2e/usuarios',
    'vagas//1',
    'auth/login',
    '/vagas',
    'vagas?url=http://outro.site',
    'vagas/1\\usuarios',
  ])('rejeita recurso arbitrário %s', (caminho) => expect(caminhoPermitido(caminho)).toBe(false))
  it('aceita a origem exata', () =>
    expect(origemPermitida('https://sirs.example', 'https://sirs.example')).toBe(true))
  it.each([
    undefined,
    'null',
    'https://atacante.example',
    'http://sirs.example',
    'https://sirs.example:8443',
    'https://sirs.example/caminho',
    'https://sirs.example?x=1',
    'https://usuario@sirs.example',
  ])('rejeita origem inválida %s', (origem) =>
    expect(origemPermitida(origem, 'https://sirs.example')).toBe(false),
  )
  it('aceita destino interno', () => expect(destinoInterno('/vagas/42')).toBe('/vagas/42'))
  it.each([
    '//atacante.example',
    'https://atacante.example',
    '/\\atacante.example',
    '/%2f%2fatacante.example',
    '/login',
    '/\t/atacante.example',
    undefined,
  ])('rejeita redirecionamento externo %s', (destino) => expect(destinoInterno(destino)).toBe('/'))
})
