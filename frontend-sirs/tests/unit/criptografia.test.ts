import { describe, it, expect } from 'vitest'
import { cifrarSessao, decifrarSessao } from '../../server/utils/criptografia'
const segredo = 'segredo-ficticio-exclusivo-dos-testes-criptograficos'
const sessao = {
  accessToken: 'access-apenas-fixture',
  refreshToken: 'refresh-apenas-fixture',
  expiresAt: '2030-01-01T00:00:00Z',
}
describe('Cookie criptografado da sessão', () => {
  it('restaura a sessão original', () =>
    expect(decifrarSessao(cifrarSessao(sessao, segredo), segredo)).toEqual(sessao))
  it('não expõe tokens em texto claro', () => {
    const cookie = cifrarSessao(sessao, segredo)
    expect(cookie).not.toContain(sessao.accessToken)
    expect(cookie).not.toContain(sessao.refreshToken)
  })
  it('usa vetor aleatório a cada emissão', () =>
    expect(cifrarSessao(sessao, segredo)).not.toBe(cifrarSessao(sessao, segredo)))
  it('rejeita conteúdo adulterado', () => {
    const cookie = cifrarSessao(sessao, segredo)
    const partes = cookie.split('.')
    partes[2] = (partes[2]![0] === 'A' ? 'B' : 'A') + partes[2]!.slice(1)
    expect(decifrarSessao(partes.join('.'), segredo)).toBeNull()
  })
  it('rejeita outro segredo', () =>
    expect(decifrarSessao(cifrarSessao(sessao, segredo), segredo + '-outro')).toBeNull())
  it.each(['', 'a.b', 'a.b.c.d', '..', '%%.invalid.###'])('rejeita cookie malformado %s', (valor) =>
    expect(decifrarSessao(valor, segredo)).toBeNull(),
  )
  it('rejeita chave fraca', () => expect(() => cifrarSessao(sessao, 'curta')).toThrow())
  it('aceita modo local sem refresh token', () => {
    const local = { ...sessao, refreshToken: null }
    expect(decifrarSessao(cifrarSessao(local, segredo), segredo)).toEqual(local)
  })
})
